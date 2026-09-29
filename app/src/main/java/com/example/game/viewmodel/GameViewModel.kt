package com.example.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.LevelRecord
import com.example.data.UserProgress
import com.example.game.audio.GameAudioEngine
import com.example.game.math.Camera3D
import com.example.game.math.Vec3
import com.example.game.physics.BallPhysics
import com.example.game.physics.CollisionEvent
import com.example.game.physics.LevelConfig
import com.example.game.physics.LevelFactory
import com.example.game.physics.TrackElement
import com.example.game.renderer.BallSkin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class GameStatus {
    PLAYING,
    PAUSED,
    LEVEL_COMPLETE,
    GAME_OVER
}

enum class GameMode {
    CAMPAIGN,
    ENDLESS
}

data class TouchInputState(
    val isTouching: Boolean = false,
    val touchStartX: Float = 0f,
    val touchStartY: Float = 0f,
    val touchCurrentX: Float = 0f,
    val touchCurrentY: Float = 0f,
    val forceX: Float = 0f, // -1f (left) to +1f (right)
    val forceZ: Float = 0f  // -1f (back) to +1f (forward)
)

data class GameUiState(
    val status: GameStatus = GameStatus.PLAYING,
    val mode: GameMode = GameMode.CAMPAIGN,
    val currentLevelId: Int = 1,
    val levelName: String = "",
    val levelSubtitle: String = "",
    val elapsedTimeSeconds: Float = 0f,
    val gemsCollected: Int = 0,
    val totalGemsInLevel: Int = 0,
    val speedKmh: Float = 0f,
    val starsEarned: Int = 0,
    val endlessScore: Int = 0,
    val endlessBestScore: Int = 0,
    val lives: Int = 3,
    val selectedSkin: BallSkin = BallSkin.ALL_SKINS[0],
    val userTotalGems: Int = 0,
    val showTouchGuide: Boolean = true,
    val sensitivity: Float = 1.0f,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)

sealed class HapticEvent {
    object LightTap : HapticEvent()
    object MediumImpact : HapticEvent()
    object HeavyImpact : HapticEvent()
    object Success : HapticEvent()
    object Warning : HapticEvent()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val audioEngine = GameAudioEngine()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 10)
    val hapticEvents: SharedFlow<HapticEvent> = _hapticEvents.asSharedFlow()

    // Database levels & progress
    val allLevels: StateFlow<List<LevelRecord>>
    val userProgress: StateFlow<UserProgress?>

    // Realtime physics & rendering objects
    val ballPhysics = BallPhysics()
    val camera = Camera3D()
    var currentLevelConfig: LevelConfig = LevelFactory.getLevel(1)
        private set
    val activeTrackElements = mutableListOf<TrackElement>()

    // Touch input state
    val touchInput = MutableStateFlow(TouchInputState())

    // Endless mode tracking
    private var endlessNextSpawnZ = 40f
    private var endlessChunkCounter = 0

    // Game loop
    private var gameLoopJob: Job? = null
    var animTime: Float = 0f
        private set

    init {
        val db = GameDatabase.getDatabase(application)
        repository = GameRepository(db.gameDao())
        allLevels = MutableStateFlow<List<LevelRecord>>(emptyList())
        userProgress = MutableStateFlow<UserProgress?>(null)

        viewModelScope.launch {
            repository.initializeDefaultLevels(5)
            repository.allLevels.collect { records ->
                (allLevels as MutableStateFlow).value = records
            }
        }

        viewModelScope.launch {
            repository.userProgress.collect { progress ->
                (userProgress as MutableStateFlow).value = progress
                progress?.let { p ->
                    audioEngine.isSoundEnabled = p.soundEnabled
                    _uiState.value = _uiState.value.copy(
                        userTotalGems = p.totalGems,
                        selectedSkin = BallSkin.getSkin(p.selectedSkinId),
                        endlessBestScore = p.endlessHighScore,
                        sensitivity = p.sensitivity,
                        soundEnabled = p.soundEnabled,
                        hapticsEnabled = p.hapticsEnabled,
                        showTouchGuide = p.showTouchGuide
                    )
                }
            }
        }

        startLevel(1)
    }

    fun startLevel(levelId: Int) {
        val config = LevelFactory.getLevel(levelId)
        currentLevelConfig = config
        activeTrackElements.clear()
        // Deep copy of level elements so collected gems reset
        activeTrackElements.addAll(LevelFactory.getLevel(levelId).elements)

        ballPhysics.reset(config.startPosition)
        camera.position = config.startPosition + Vec3(0f, 4.5f, -7.5f)
        camera.target = config.startPosition

        val gemCount = activeTrackElements.count { it is com.example.game.physics.GemElement }

        _uiState.value = _uiState.value.copy(
            status = GameStatus.PLAYING,
            mode = GameMode.CAMPAIGN,
            currentLevelId = levelId,
            levelName = config.name,
            levelSubtitle = config.subtitle,
            elapsedTimeSeconds = 0f,
            gemsCollected = 0,
            totalGemsInLevel = gemCount,
            starsEarned = 0,
            lives = 3
        )

        restartGameLoop()
    }

    fun startEndlessMode() {
        activeTrackElements.clear()
        // Base start platform
        activeTrackElements.add(
            com.example.game.physics.PlatformElement(
                position = Vec3(0f, 0f, 10f),
                size = Vec3(6f, 1f, 20f),
                color = androidx.compose.ui.graphics.Color(0xFF1E293B),
                borderColor = androidx.compose.ui.graphics.Color(0xFF38BDF8)
            )
        )
        endlessNextSpawnZ = 20f
        endlessChunkCounter = 0

        // Spawn first 3 chunks ahead
        for (i in 0..2) {
            val chunk = LevelFactory.generateEndlessChunk(endlessChunkCounter++, endlessNextSpawnZ)
            activeTrackElements.addAll(chunk)
            endlessNextSpawnZ += 40f
        }

        val startPos = Vec3(0f, 1.5f, 2f)
        ballPhysics.reset(startPos)
        camera.position = startPos + Vec3(0f, 4.5f, -7.5f)
        camera.target = startPos

        _uiState.value = _uiState.value.copy(
            status = GameStatus.PLAYING,
            mode = GameMode.ENDLESS,
            levelName = "Endless Roll",
            levelSubtitle = "Infinite momentum challenge",
            elapsedTimeSeconds = 0f,
            gemsCollected = 0,
            endlessScore = 0,
            lives = 1
        )

        restartGameLoop()
    }

    fun pauseGame() {
        if (_uiState.value.status == GameStatus.PLAYING) {
            _uiState.value = _uiState.value.copy(status = GameStatus.PAUSED)
            audioEngine.currentSpeedFraction = 0f
        }
    }

    fun resumeGame() {
        if (_uiState.value.status == GameStatus.PAUSED) {
            _uiState.value = _uiState.value.copy(status = GameStatus.PLAYING)
        }
    }

    fun restartCurrentLevel() {
        if (_uiState.value.mode == GameMode.ENDLESS) {
            startEndlessMode()
        } else {
            startLevel(_uiState.value.currentLevelId)
        }
    }

    fun nextLevel() {
        val nextId = _uiState.value.currentLevelId + 1
        if (nextId <= 5) {
            startLevel(nextId)
        } else {
            // Completed all levels, go to level 1 or endless
            startLevel(1)
        }
    }

    /**
     * Invisible touch handling:
     * Player places thumb anywhere and moves around.
     * The delta from touch start or rolling vector smoothly accelerates the ball!
     */
    fun onTouchDown(x: Float, y: Float) {
        touchInput.value = TouchInputState(
            isTouching = true,
            touchStartX = x,
            touchStartY = y,
            touchCurrentX = x,
            touchCurrentY = y,
            forceX = 0f,
            forceZ = 0f
        )
    }

    fun onTouchMove(x: Float, y: Float) {
        val current = touchInput.value
        if (!current.isTouching) return

        val dx = x - current.touchStartX
        val dy = y - current.touchStartY
        val maxDragDist = 180f

        val dist = sqrt(dx * dx + dy * dy)
        val clampedDist = dist.coerceAtMost(maxDragDist)
        val ratio = if (dist > 0.001f) clampedDist / maxDragDist else 0f

        val normX = if (dist > 0.001f) (dx / dist) * ratio else 0f
        // Dragging UP (-dy) moves forward (+Z); dragging DOWN (+dy) pulls back/brakes (-Z)
        val normZ = if (dist > 0.001f) (-dy / dist) * ratio else 0f

        touchInput.value = current.copy(
            touchCurrentX = x,
            touchCurrentY = y,
            forceX = normX,
            forceZ = normZ
        )
    }

    fun onTouchUp() {
        touchInput.value = TouchInputState(isTouching = false)
    }

    private fun restartGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.nanoTime()

            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                if (_uiState.value.status == GameStatus.PLAYING) {
                    stepPhysicsAndGame(dt)
                }

                kotlinx.coroutines.delay(16) // ~60fps fixed physics tick
            }
        }
    }

    private fun stepPhysicsAndGame(dt: Float) {
        animTime += dt

        // Read thumb input force
        val input = touchInput.value
        val sensitivity = _uiState.value.sensitivity
        val inputForce = Vec3(input.forceX, 0f, input.forceZ)

        // Run ball physics update
        ballPhysics.update(
            dt = dt,
            inputForce = inputForce,
            sensitivity = sensitivity,
            elements = activeTrackElements
        ) { event ->
            handleCollisionEvent(event)
        }

        // Audio engine update
        val speedFraction = (ballPhysics.speed / 24f).coerceIn(0f, 1f)
        audioEngine.currentSpeedFraction = speedFraction

        // Spawn rolling dust particles
        if (ballPhysics.isGrounded && ballPhysics.horizontalSpeed > 5f) {
            // Renderer particle system will be accessed via UI state or event
        }

        // Camera follow logic
        val ballPos = ballPhysics.position
        val ballVel = ballPhysics.velocity

        // Dynamic camera distance increases with speed for velocity sensation
        val camDist = 7.0f + speedFraction * 2.0f
        val camHeight = 4.0f + speedFraction * 0.8f
        val lookAhead = ballVel * 0.25f

        val desiredCamPos = Vec3(
            ballPos.x * 0.85f,
            ballPos.y + camHeight,
            ballPos.z - camDist
        )
        val desiredTarget = ballPos + lookAhead + Vec3(0f, 0.8f, 0f)

        // Smooth camera lerp
        val camLerp = (dt * 8f).coerceIn(0.05f, 0.4f)
        camera.position = Vec3.lerp(camera.position, desiredCamPos, camLerp)
        camera.target = Vec3.lerp(camera.target, desiredTarget, camLerp)
        camera.updateOrientation()

        // Handle Endless chunk generation and pruning
        if (_uiState.value.mode == GameMode.ENDLESS) {
            if (ballPos.z + 60f > endlessNextSpawnZ) {
                val chunk = LevelFactory.generateEndlessChunk(endlessChunkCounter++, endlessNextSpawnZ)
                activeTrackElements.addAll(chunk)
                endlessNextSpawnZ += 40f

                // Prune elements far behind player
                activeTrackElements.removeAll { el -> el.position.z < ballPos.z - 40f }
            }

            val score = (ballPos.z.coerceAtLeast(0f) * 2f).toInt() + (_uiState.value.gemsCollected * 50)
            _uiState.value = _uiState.value.copy(
                endlessScore = score,
                speedKmh = ballPhysics.speed * 3.6f
            )
        } else {
            // Campaign Mode time & speed
            val newTime = _uiState.value.elapsedTimeSeconds + dt
            _uiState.value = _uiState.value.copy(
                elapsedTimeSeconds = newTime,
                speedKmh = ballPhysics.speed * 3.6f
            )
        }
    }

    private fun handleCollisionEvent(event: CollisionEvent) {
        when (event.type) {
            CollisionEvent.EventType.GROUND_HIT -> {
                audioEngine.playCollision(event.intensity)
                triggerHaptic(HapticEvent.LightTap)
            }
            CollisionEvent.EventType.WALL_BOUNCE -> {
                audioEngine.playCollision(0.8f)
                triggerHaptic(HapticEvent.MediumImpact)
            }
            CollisionEvent.EventType.BUMPER_HIT -> {
                audioEngine.playCollision(1.0f)
                triggerHaptic(HapticEvent.HeavyImpact)
            }
            CollisionEvent.EventType.SPEED_BOOST -> {
                audioEngine.playSpeedBoost()
                triggerHaptic(HapticEvent.MediumImpact)
            }
            CollisionEvent.EventType.JUMP_PAD -> {
                audioEngine.playJumpPad()
                triggerHaptic(HapticEvent.MediumImpact)
            }
            CollisionEvent.EventType.GEM_COLLECTED -> {
                val newGems = _uiState.value.gemsCollected + 1
                audioEngine.playGemPickup(newGems)
                triggerHaptic(HapticEvent.LightTap)
                _uiState.value = _uiState.value.copy(gemsCollected = newGems)
            }
            CollisionEvent.EventType.CHECKPOINT_REACHED -> {
                audioEngine.playGemPickup(5)
                triggerHaptic(HapticEvent.Success)
            }
            CollisionEvent.EventType.HAZARD_HIT -> {
                audioEngine.playCollision(1.0f)
                triggerHaptic(HapticEvent.HeavyImpact)
                onPlayerHitHazard()
            }
            CollisionEvent.EventType.FELL_OFF -> {
                audioEngine.playFall()
                triggerHaptic(HapticEvent.Warning)
                onPlayerFall()
            }
            CollisionEvent.EventType.GOAL_REACHED -> {
                onLevelWon()
            }
        }
    }

    private fun onPlayerHitHazard() {
        val currLives = _uiState.value.lives - 1
        if (currLives <= 0) {
            onGameOver()
        } else {
            _uiState.value = _uiState.value.copy(lives = currLives)
            ballPhysics.respawnAtCheckpoint()
        }
    }

    private fun onPlayerFall() {
        if (_uiState.value.mode == GameMode.ENDLESS) {
            onGameOver()
            return
        }

        val currLives = _uiState.value.lives - 1
        if (currLives <= 0) {
            onGameOver()
        } else {
            _uiState.value = _uiState.value.copy(lives = currLives)
            ballPhysics.respawnAtCheckpoint()
        }
    }

    private fun onGameOver() {
        _uiState.value = _uiState.value.copy(status = GameStatus.GAME_OVER)
        audioEngine.currentSpeedFraction = 0f

        if (_uiState.value.mode == GameMode.ENDLESS) {
            viewModelScope.launch {
                repository.saveEndlessScore(
                    score = _uiState.value.endlessScore,
                    gemsCollected = _uiState.value.gemsCollected
                )
            }
        }
    }

    private fun onLevelWon() {
        audioEngine.playVictory()
        triggerHaptic(HapticEvent.Success)
        audioEngine.currentSpeedFraction = 0f

        val time = _uiState.value.elapsedTimeSeconds
        val parTime = currentLevelConfig.parTimeSeconds
        val gems = _uiState.value.gemsCollected
        val totalGems = _uiState.value.totalGemsInLevel

        // Star calculation:
        // 1 Star: Course complete
        // 2 Stars: Completed under par time
        // 3 Stars: Completed under par time + collected at least 70% of gems
        var stars = 1
        if (time <= parTime) stars++
        if (totalGems > 0 && gems >= (totalGems * 0.7f).toInt()) stars++
        stars = stars.coerceIn(1, 3)

        _uiState.value = _uiState.value.copy(
            status = GameStatus.LEVEL_COMPLETE,
            starsEarned = stars
        )

        val timeMs = (time * 1000).toLong()
        viewModelScope.launch {
            repository.saveLevelResult(
                levelId = _uiState.value.currentLevelId,
                timeMs = timeMs,
                starsEarned = stars,
                gemsFound = gems,
                totalGemsAvailable = totalGems
            )
        }
    }

    private fun triggerHaptic(event: HapticEvent) {
        if (_uiState.value.hapticsEnabled) {
            _hapticEvents.tryEmit(event)
        }
    }

    fun unlockAndSelectSkin(skinId: String, cost: Int) {
        viewModelScope.launch {
            repository.unlockSkin(skinId, cost)
        }
    }

    fun selectSkin(skinId: String) {
        viewModelScope.launch {
            repository.selectSkin(skinId)
        }
    }

    fun updateSettings(sound: Boolean, haptics: Boolean, sensitivity: Float, showGuide: Boolean) {
        audioEngine.isSoundEnabled = sound
        viewModelScope.launch {
            repository.updateSettings(sound, haptics, sensitivity, showGuide)
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        audioEngine.release()
    }
}
