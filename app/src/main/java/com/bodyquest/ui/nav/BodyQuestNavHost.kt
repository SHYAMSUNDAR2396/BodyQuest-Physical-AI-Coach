package com.bodyquest.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bodyquest.ui.screens.*

@Composable
fun BodyQuestNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(onContinue = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Routes.HOME) {
            HomeScreen(
                onStartWorkout = { navController.navigate(Routes.WORKOUT_SELECTION) },
                onProgress = { navController.navigate(Routes.PROGRESS) },
                onProfile = { navController.navigate(Routes.PROFILE) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.WORKOUT_SELECTION) {
            WorkoutSelectionScreen(
                onBack = { navController.popBackStack() },
                onSelect = { workoutId -> navController.navigate(Routes.exerciseSelection(workoutId)) },
            )
        }

        composable(Routes.EXERCISE_SELECTION) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: "full_body"
            ExerciseSelectionScreen(
                workoutId = workoutId,
                onBack = { navController.popBackStack() },
                onSelect = { exerciseId -> navController.navigate(Routes.calibration(exerciseId)) },
            )
        }

        composable(Routes.CALIBRATION) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: "squat"
            CalibrationScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
                onReady = { navController.navigate(Routes.liveCoach(exerciseId)) },
            )
        }

        composable(Routes.LIVE_COACH) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: "squat"
            LiveCoachScreen(
                exerciseId = exerciseId,
                onBack = { navController.popBackStack() },
                onFinishSet = { navController.navigate(Routes.setSummary("demo")) },
            )
        }

        composable(Routes.SET_SUMMARY) {
            SetSummaryScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.WORKOUT_SUMMARY) {
            PendingScreen(
                title = "Workout Summary",
                phaseNote = "Session totals and adaptive recommendation arrive with Phases 8–9.",
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROGRESS) {
            PendingScreen(
                title = "Progress",
                phaseNote = "Form quality, consistency, and adherence graphs arrive with Phase 8 (storage).",
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE) {
            PendingScreen(
                title = "Profile",
                phaseNote = "User configuration arrives with Phase 8 (storage).",
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            PendingScreen(
                title = "Settings",
                phaseNote = "Voice, haptics, camera, privacy, AI mode, and units land with Phases 10–11.",
                onBack = { navController.popBackStack() },
            )
        }
    }
}
