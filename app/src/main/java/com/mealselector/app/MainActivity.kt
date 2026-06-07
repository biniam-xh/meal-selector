package com.mealselector.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mealselector.app.ui.GroupSetupScreen
import com.mealselector.app.ui.LoadingScreen
import com.mealselector.app.ui.MainScreen
import com.mealselector.app.ui.SettingsScreen
import com.mealselector.app.ui.theme.MealSelectorTheme
import com.mealselector.app.viewmodel.MealViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MealSelectorTheme {
                val viewModel: MealViewModel = viewModel()
                val mealState by viewModel.mealState.collectAsStateWithLifecycle()
                val isSetupLoading by viewModel.isSetupLoading.collectAsStateWithLifecycle()
                val setupError by viewModel.setupError.collectAsStateWithLifecycle()

                when {
                    mealState.isInitializing -> LoadingScreen()
                    !mealState.hasGroup -> {
                        GroupSetupScreen(
                            isLoading = isSetupLoading,
                            errorMessage = setupError,
                            onCreateGroup = viewModel::createGroup,
                            onJoinGroup = viewModel::joinGroup,
                            onClearError = viewModel::clearSetupError
                        )
                    }
                    else -> {
                        val navController = rememberNavController()

                        LaunchedEffect(mealState.groupCode) {
                            if (mealState.hasGroup) {
                                viewModel.applyLockOnAppStart()
                            }
                        }

                        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                            if (mealState.hasGroup && !mealState.isInitializing) {
                                viewModel.applyLockOnAppStart()
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = "main"
                        ) {
                            composable("main") {
                                MainScreen(
                                    mealState = mealState,
                                    onNextMeal = viewModel::rotateToNext,
                                    onToggleLock = viewModel::toggleLock,
                                    onOpenSettings = { navController.navigate("settings") }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    mealState = mealState,
                                    onAddMeal = viewModel::addMeal,
                                    onRemoveMeal = viewModel::removeMeal,
                                    onMoveMealUp = viewModel::moveMealUp,
                                    onMoveMealDown = viewModel::moveMealDown,
                                    onLockByDefaultChange = viewModel::setLockByDefault,
                                    onLockOnRotateChange = viewModel::setLockOnRotate,
                                    onLeaveGroup = {
                                        viewModel.leaveGroup()
                                        navController.popBackStack()
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
