package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.game.ui.GameScreen
import com.example.game.ui.GarageScreen
import com.example.game.ui.LevelSelectScreen
import com.example.game.ui.MainMenuScreen
import com.example.game.ui.SettingsScreen
import com.example.game.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    MAIN_MENU,
    GAME,
    LEVEL_SELECT,
    GARAGE,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: GameViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.MAIN_MENU) }

    when (currentScreen) {
        Screen.MAIN_MENU -> {
            MainMenuScreen(
                viewModel = viewModel,
                onPlayCampaign = {
                    viewModel.startLevel(1)
                    currentScreen = Screen.GAME
                },
                onPlayEndless = {
                    viewModel.startEndlessMode()
                    currentScreen = Screen.GAME
                },
                onOpenLevelSelect = {
                    currentScreen = Screen.LEVEL_SELECT
                },
                onOpenGarage = {
                    currentScreen = Screen.GARAGE
                },
                onOpenSettings = {
                    currentScreen = Screen.SETTINGS
                }
            )
        }

        Screen.GAME -> {
            BackHandler {
                viewModel.pauseGame()
                currentScreen = Screen.MAIN_MENU
            }
            GameScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }

        Screen.LEVEL_SELECT -> {
            BackHandler {
                currentScreen = Screen.MAIN_MENU
            }
            LevelSelectScreen(
                viewModel = viewModel,
                onStartLevel = { levelId ->
                    viewModel.startLevel(levelId)
                    currentScreen = Screen.GAME
                },
                onNavigateBack = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }

        Screen.GARAGE -> {
            BackHandler {
                currentScreen = Screen.MAIN_MENU
            }
            GarageScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }

        Screen.SETTINGS -> {
            BackHandler {
                currentScreen = Screen.MAIN_MENU
            }
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }
    }
}
