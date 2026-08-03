package com.example.veiltalk.feature.user.ui

import androidx.lifecycle.ViewModel
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserDirectoryEntryPointViewModel @Inject constructor(
    val repository: UserDirectoryRepository
) : ViewModel()