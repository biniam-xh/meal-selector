package com.mealselector.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meal_selector")

private val GROUP_CODE_KEY = stringPreferencesKey("group_code")

@OptIn(ExperimentalCoroutinesApi::class)
class MealRepository(private val context: Context) {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    val mealState: Flow<MealState> = context.dataStore.data
        .map { prefs -> prefs[GROUP_CODE_KEY]?.uppercase() }
        .distinctUntilChanged()
        .flatMapLatest { groupCode ->
            if (groupCode.isNullOrBlank()) {
                flowOf(
                    MealState(
                        isInitializing = false,
                        groupCode = null
                    )
                )
            } else {
                observeGroup(groupCode)
            }
        }

    suspend fun createGroup(): GroupSetupResult {
        return try {
            ensureSignedIn()
            repeat(MAX_CODE_ATTEMPTS) {
                val code = generateGroupCode()
                val doc = firestore.collection(FirestorePaths.GROUPS).document(code)
                if (!doc.get().await().exists()) {
                    doc.set(
                        mapOf(
                            FirestoreFields.MEALS to MealDefaults.DEFAULT_MEALS,
                            FirestoreFields.CURRENT_INDEX to 0,
                            FirestoreFields.IS_LOCKED to true,
                            FirestoreFields.LOCK_BY_DEFAULT to true,
                            FirestoreFields.LOCK_ON_ROTATE to true,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    saveGroupCode(code)
                    return GroupSetupResult.Success
                }
            }
            GroupSetupResult.Error("Could not create a group code. Please try again.")
        } catch (e: Exception) {
            GroupSetupResult.Error(friendlyError(e))
        }
    }

    suspend fun joinGroup(rawCode: String): GroupSetupResult {
        val code = rawCode.trim().uppercase()
        if (code.length != GROUP_CODE_LENGTH) {
            return GroupSetupResult.Error("Enter a valid 6-character group code.")
        }

        return try {
            ensureSignedIn()
            val doc = firestore.collection(FirestorePaths.GROUPS).document(code).get().await()
            if (!doc.exists()) {
                return GroupSetupResult.Error("Group not found. Check the code and try again.")
            }
            saveGroupCode(code)
            GroupSetupResult.Success
        } catch (e: Exception) {
            GroupSetupResult.Error(friendlyError(e))
        }
    }

    suspend fun leaveGroup() {
        context.dataStore.edit { prefs ->
            prefs.remove(GROUP_CODE_KEY)
        }
    }

    suspend fun toggleLock() {
        val code = currentGroupCode() ?: return
        runCatching {
            firestore.runTransaction { transaction ->
                val ref = firestore.collection(FirestorePaths.GROUPS).document(code)
                val snapshot = transaction.get(ref)
                val locked = snapshot.getBoolean(FirestoreFields.IS_LOCKED) ?: true
                transaction.update(ref, FirestoreFields.IS_LOCKED, !locked)
            }.await()
        }
    }

    suspend fun rotateToNext() {
        val code = currentGroupCode() ?: return
        runCatching {
            firestore.runTransaction { transaction ->
                val ref = firestore.collection(FirestorePaths.GROUPS).document(code)
                val snapshot = transaction.get(ref)
                if (snapshot.getBoolean(FirestoreFields.IS_LOCKED) == true) return@runTransaction

                val meals = snapshot.readMeals()
                if (meals.isEmpty()) return@runTransaction

                val currentIndex = snapshot.readCurrentIndex(meals.lastIndex)
                val updates = mutableMapOf<String, Any>(
                    FirestoreFields.CURRENT_INDEX to (currentIndex + 1) % meals.size
                )
                if (snapshot.readLockOnRotate()) {
                    updates[FirestoreFields.IS_LOCKED] = true
                }
                transaction.update(ref, updates)
            }.await()
        }
    }

    suspend fun applyLockOnAppStart() {
        val code = currentGroupCode() ?: return
        runCatching {
            firestore.runTransaction { transaction ->
                val ref = firestore.collection(FirestorePaths.GROUPS).document(code)
                val snapshot = transaction.get(ref)
                if (!snapshot.readLockByDefault()) return@runTransaction
                if (snapshot.getBoolean(FirestoreFields.IS_LOCKED) == true) return@runTransaction
                transaction.update(ref, FirestoreFields.IS_LOCKED, true)
            }.await()
        }
    }

    suspend fun setLockByDefault(enabled: Boolean) {
        val code = currentGroupCode() ?: return
        runCatching {
            firestore.runTransaction { transaction ->
                val ref = firestore.collection(FirestorePaths.GROUPS).document(code)
                transaction.get(ref)
                val updates = mutableMapOf<String, Any>(
                    FirestoreFields.LOCK_BY_DEFAULT to enabled
                )
                if (enabled) {
                    updates[FirestoreFields.IS_LOCKED] = true
                }
                transaction.update(ref, updates)
            }.await()
        }
    }

    suspend fun setLockOnRotate(enabled: Boolean) {
        val code = currentGroupCode() ?: return
        runCatching {
            firestore.collection(FirestorePaths.GROUPS).document(code)
                .update(FirestoreFields.LOCK_ON_ROTATE, enabled)
                .await()
        }
    }

    suspend fun addMeal(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        val code = currentGroupCode() ?: return
        runCatching {
            firestore.runTransaction { transaction ->
                val ref = firestore.collection(FirestorePaths.GROUPS).document(code)
                val snapshot = transaction.get(ref)
                val meals = snapshot.readMeals().toMutableList()
                meals.add(trimmed)
                transaction.update(ref, FirestoreFields.MEALS, meals)
            }.await()
        }
    }

    suspend fun removeMeal(index: Int) {
        val code = currentGroupCode() ?: return
        runCatching {
            firestore.runTransaction { transaction ->
                val ref = firestore.collection(FirestorePaths.GROUPS).document(code)
                val snapshot = transaction.get(ref)
                val meals = snapshot.readMeals().toMutableList()
                if (index !in meals.indices) return@runTransaction

                val currentIndex = snapshot.readCurrentIndex(meals.lastIndex)
                meals.removeAt(index)
                val newIndex = MealListLogic.indexAfterRemove(currentIndex, index, meals.size)

                transaction.update(
                    ref,
                    mapOf(
                        FirestoreFields.MEALS to meals,
                        FirestoreFields.CURRENT_INDEX to newIndex
                    )
                )
            }.await()
        }
    }

    suspend fun moveMealUp(index: Int) {
        if (index <= 0) return
        moveMeal(index, index - 1)
    }

    suspend fun moveMealDown(index: Int) {
        moveMeal(index, index + 1)
    }

    private suspend fun moveMeal(fromIndex: Int, toIndex: Int) {
        val code = currentGroupCode() ?: return
        runCatching {
            firestore.runTransaction { transaction ->
                val ref = firestore.collection(FirestorePaths.GROUPS).document(code)
                val snapshot = transaction.get(ref)
                applyReorder(transaction, ref, snapshot, fromIndex, toIndex)
            }.await()
        }
    }

    private fun applyReorder(
        transaction: com.google.firebase.firestore.Transaction,
        ref: com.google.firebase.firestore.DocumentReference,
        snapshot: com.google.firebase.firestore.DocumentSnapshot,
        fromIndex: Int,
        toIndex: Int
    ) {
        val meals = snapshot.readMeals()
        if (fromIndex !in meals.indices || toIndex !in meals.indices) return

        val currentIndex = snapshot.readCurrentIndex(meals.lastIndex)
        val reordered = MealListLogic.reorder(meals, fromIndex, toIndex)
        val newIndex = MealListLogic.indexAfterReorder(reordered, meals, currentIndex)

        transaction.update(
            ref,
            mapOf(
                FirestoreFields.MEALS to reordered,
                FirestoreFields.CURRENT_INDEX to newIndex
            )
        )
    }

    private fun observeGroup(groupCode: String): Flow<MealState> = callbackFlow {
        trySend(MealState(groupCode = groupCode, isInitializing = true))

        var registration: ListenerRegistration? = null
        runCatching { ensureSignedIn() }
            .onFailure { error ->
                trySend(
                    MealState(
                        groupCode = groupCode,
                        isInitializing = false,
                        errorMessage = friendlyError(error)
                    )
                )
                close()
                return@callbackFlow
            }

        registration = firestore.collection(FirestorePaths.GROUPS)
            .document(groupCode)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        MealState(
                            groupCode = groupCode,
                            isInitializing = false,
                            errorMessage = friendlyError(error)
                        )
                    )
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(
                        MealState(
                            groupCode = groupCode,
                            isInitializing = false,
                            errorMessage = "This group no longer exists."
                        )
                    )
                    return@addSnapshotListener
                }

                val meals = snapshot.readMeals()
                val lastIndex = (meals.size - 1).coerceAtLeast(0)
                trySend(
                    MealState(
                        meals = meals,
                        currentIndex = snapshot.readCurrentIndex(lastIndex),
                        isLocked = snapshot.getBoolean(FirestoreFields.IS_LOCKED) ?: true,
                        lockByDefault = snapshot.readLockByDefault(),
                        lockOnRotate = snapshot.readLockOnRotate(),
                        groupCode = groupCode,
                        isInitializing = false
                    )
                )
            }

        awaitClose { registration?.remove() }
    }

    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    private suspend fun currentGroupCode(): String? {
        return context.dataStore.data.map { it[GROUP_CODE_KEY]?.uppercase() }.first()
    }

    private suspend fun saveGroupCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[GROUP_CODE_KEY] = code.uppercase()
        }
    }

    private fun generateGroupCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(GROUP_CODE_LENGTH) {
            repeat(GROUP_CODE_LENGTH) {
                append(chars.random())
            }
        }
    }

    private fun friendlyError(error: Throwable): String {
        return when (error) {
            is FirebaseFirestoreException -> when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Firebase permission denied. Check Firestore rules and Anonymous Auth."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "No connection. Check your internet and try again."
                else -> error.message ?: "Something went wrong."
            }
            else -> error.message ?: "Something went wrong."
        }
    }

    private companion object {
        const val GROUP_CODE_LENGTH = 6
        const val MAX_CODE_ATTEMPTS = 8
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.readMeals(): List<String> {
    return (get(FirestoreFields.MEALS) as? List<*>)
        ?.mapNotNull { it as? String }
        ?.filter { it.isNotBlank() }
        ?: MealDefaults.DEFAULT_MEALS
}

private fun com.google.firebase.firestore.DocumentSnapshot.readCurrentIndex(lastIndex: Int): Int {
    return ((getLong(FirestoreFields.CURRENT_INDEX) ?: 0L).toInt()).coerceIn(0, lastIndex)
}

private fun com.google.firebase.firestore.DocumentSnapshot.readLockByDefault(): Boolean {
    return getBoolean(FirestoreFields.LOCK_BY_DEFAULT) ?: true
}

private fun com.google.firebase.firestore.DocumentSnapshot.readLockOnRotate(): Boolean {
    return getBoolean(FirestoreFields.LOCK_ON_ROTATE) ?: true
}
