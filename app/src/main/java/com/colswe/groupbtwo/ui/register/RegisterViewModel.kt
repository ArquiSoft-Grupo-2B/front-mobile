package com.colswe.groupbtwo.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colswe.groupbtwo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun register(email: String, password: String) {
        if (!validateEmail(email)) {
            _uiState.value = RegisterUiState(error = "Correo electrónico inválido")
            return
        }

        if (!validatePassword(password)) {
            _uiState.value = RegisterUiState(error = "La contraseña debe tener al menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val result = repo.register(email, password)
            _uiState.value = if (result.isSuccess) {
                RegisterUiState(success = true)
            } else {
                RegisterUiState(error = parseError(result.exceptionOrNull()?.message))
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }

    private fun parseError(message: String?): String {
        return when {
            message?.contains("email") == true -> "Correo electrónico inválido o ya registrado"
            message?.contains("password") == true -> "La contraseña es muy débil"
            message?.contains("network") == true -> "Error de conexión. Verifica tu internet"
            else -> message ?: "Error al crear la cuenta"
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class RegisterUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)