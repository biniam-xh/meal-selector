package com.mealselector.app.data

data class MealState(
    val meals: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val isLocked: Boolean = true,
    val lockByDefault: Boolean = true,
    val lockOnRotate: Boolean = true,
    val groupCode: String? = null,
    val isInitializing: Boolean = true,
    val errorMessage: String? = null
) {
    val hasGroup: Boolean get() = groupCode != null

    val currentMeal: String?
        get() = meals.getOrNull(currentIndex)
}

object MealDefaults {
    val DEFAULT_MEALS = listOf(
        "Pizza",
        "Pasta",
        "Tacos",
        "Sushi",
        "Burgers",
        "Salad",
        "Stir Fry"
    )
}

object FirestoreFields {
    const val MEALS = "meals"
    const val CURRENT_INDEX = "currentIndex"
    const val IS_LOCKED = "isLocked"
    const val LOCK_BY_DEFAULT = "lockByDefault"
    const val LOCK_ON_ROTATE = "lockOnRotate"
}

object FirestorePaths {
    const val GROUPS = "groups"
}

sealed class GroupSetupResult {
    data object Success : GroupSetupResult()
    data class Error(val message: String) : GroupSetupResult()
}
