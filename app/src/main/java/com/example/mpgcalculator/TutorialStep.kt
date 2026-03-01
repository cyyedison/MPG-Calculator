package com.example.mpgcalculator

import android.view.View

data class TutorialStep(
    val title: String,
    val message: String,
    val getTargetView: (() -> View?)? = null,
    val preAction: (() -> Unit)? = null
)
