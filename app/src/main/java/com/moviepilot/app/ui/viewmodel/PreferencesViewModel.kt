package com.moviepilot.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.moviepilot.app.data.local.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel()
