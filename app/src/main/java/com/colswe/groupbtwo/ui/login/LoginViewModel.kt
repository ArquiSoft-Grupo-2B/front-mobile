package com.colswe.groupbtwo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colswe.groupbtwo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = LoginUiState(error = "Por favor completa todos los campos")
            return
        }

        if (!validateEmail(email)) {
            _uiState.value = LoginUiState(error = "Correo electrónico inválido")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)

            try {
                val result = repo.login(email, password)

                _uiState.value = if (result.isSuccess) {
                    LoginUiState(success = true, loading = false)
                } else {
                    LoginUiState(
                        error = parseError(result.exceptionOrNull()?.message),
                        loading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState(
                    error = "Error inesperado: ${e.message}",
                    loading = false
                )
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun parseError(message: String?): String {
        return when {
            message?.contains("password", ignoreCase = true) == true -> "Contraseña incorrecta"
            message?.contains("user-not-found", ignoreCase = true) == true -> "Usuario no encontrado"
            message?.contains("user-disabled", ignoreCase = true) == true -> "Usuario deshabilitado"
            message?.contains("invalid-email", ignoreCase = true) == true -> "Correo electrónico inválido"
            message?.contains("invalid-credential", ignoreCase = true) == true -> "Credenciales inválidas"
            message?.contains("network", ignoreCase = true) == true -> "Error de conexión. Verifica tu internet"
            message?.contains("too-many-requests", ignoreCase = true) == true -> "Demasiados intentos. Intenta más tarde"
            else -> message ?: "Error al iniciar sesión"
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}

data class LoginUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null
)