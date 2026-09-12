package com.example.veiltalk.feature.story.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.util.ApiResult
import com.example.veiltalk.feature.story.data.StoryRepository
import com.example.veiltalk.feature.story.data.dto.StoryResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryUiState(
    val stories: Map<String, List<StoryResponseDto>> = emptyMap(),
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val repository: StoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        loadStories()
    }

    fun loadStories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getStories()) {
                is ApiResult.Success -> {
                    // Group stories by creator username
                    val grouped = result.data.groupBy { it.creatorUsername }
                    _uiState.value = _uiState.value.copy(stories = grouped, isLoading = false)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun postStory(uri: Uri, caption: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPosting = true)
            when (val result = repository.postStory(uri, caption)) {
                is ApiResult.Success -> {
                    _events.emit("استوری با موفقیت منتشر شد.")
                    loadStories()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
            _uiState.value = _uiState.value.copy(isPosting = false)
        }
    }

    fun viewStory(storyId: Long) {
        viewModelScope.launch {
            repository.viewStory(storyId)
        }
    }

    fun reactStory(storyId: Long, emoji: String) {
        viewModelScope.launch {
            repository.reactStory(storyId, emoji)
        }
    }

    fun getStoryViewers(storyId: Long, onResult: (List<com.example.veiltalk.feature.story.data.dto.StoryViewerInfoDto>) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.getStoryViewers(storyId)) {
                is ApiResult.Success -> onResult(result.data)
                else -> onResult(emptyList())
            }
        }
    }
}
