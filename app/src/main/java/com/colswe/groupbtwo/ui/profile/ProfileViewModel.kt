package com.colswe.groupbtwo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colswe.groupbtwo.data.model.UserProfile
import com.colswe.groupbtwo.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                val result = repository.getUserProfile()

                _uiState.value = if (result.isSuccess) {
                    ProfileUiState(
                        profile = result.getOrNull(),
                        loading = false
                    )
                } else {
                    ProfileUiState(
                        error = result.exceptionOrNull()?.message ?: "Error al cargar perfil",
                        loading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(
                    error = "Error inesperado: ${e.message}",
                    loading = false
                )
            }
        }
    }

    fun updateProfile(alias: String) {
        if (alias.trim().isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "El alias no puede estar vacío")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                updating = true,
                error = null,
                successMessage = null
            )

            try {
                val currentProfile = _uiState.value.profile ?: UserProfile()

                val updatedProfile = currentProfile.copy(
                    alias = alias.trim()
                )

                val result = repository.updateUserProfile(updatedProfile)

                if (result.isSuccess) {
                    _uiState.value = ProfileUiState(
                        profile = updatedProfile,
                        updating = false,
                        successMessage = "Perfil actualizado correctamente"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.exceptionOrNull()?.message ?: "Error al actualizar perfil",
                        updating = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error inesperado: ${e.message}",
                    updating = false
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}

data class ProfileUiState(
    val profile: UserProfile? = null,
    val loading: Boolean = false,
    val updating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)