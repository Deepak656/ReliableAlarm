package com.reliablealarm.app.waketasks

sealed class TaskConfig {

    data class MathConfig(
        val problemCount: Int = 3,
        val difficulty: String = "MEDIUM"
    ) : TaskConfig()
    data class StepConfig(
        val stepsRequired: Int = 20
    ) : TaskConfig()

    data class ShakeConfig(
        val durationSeconds: Int = 10,
        val intensity: Int = 5
    ) : TaskConfig()

    data class QrConfig(
        val qrCodesRequired: Int = 1  // How many different QR codes to scan
    ) : TaskConfig()

    data class TypingConfig(
        val paragraphLength: Int = 2
    ) : TaskConfig()

    data class TapTaskConfig(
        val tapsRequired: Int = 30
    ) : TaskConfig()

    data class ColorBallsConfig(
        val numberOfround: Int = 5
    ) : TaskConfig()

    data class PopBalloonsConfig(
        val rounds: Int = 3,
        val balloonsPerRound: Int = 8
    ) : TaskConfig()

    data class TargetTapConfig(
        val rounds: Int = 3,
        val targetsPerRound: Int = 8
    ) : TaskConfig()

}