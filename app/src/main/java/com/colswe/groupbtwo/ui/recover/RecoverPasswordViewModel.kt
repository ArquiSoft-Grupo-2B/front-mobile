package com.colswe.groupbtwo.ui.recover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colswe.groupbtwo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecoverPasswordViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoverPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun recoverPassword(email: String) {
        if (!validateEmail(email)) {
            _uiState.value = RecoverPasswordUiState(error = "Correo electrónico inválido")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = repo.recoverPassword(email)
            _uiState.value = if (result.isSuccess) {
                RecoverPasswordUiState(success = true)
            } else {
                RecoverPasswordUiState(error = parseError(result.exceptionOrNull()?.message))
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun parseError(message: String?): String {
        return when {
            message?.contains("user-not-found") == true -> "No existe una cuenta con este correo electrónico"
            message?.contains("invalid-email") == true -> "Correo electrónico inválido"
            message?.contains("network") == true -> "Error de conexión. Verifica tu internet"
            message?.contains("too-many-requests") == true -> "Demasiados intentos. Intenta más tarde"
            else -> message ?: "Error al enviar el correo de recuperación"
        }
    }

    fun resetState() {
        _uiState.value = RecoverPasswordUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class RecoverPasswordUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)