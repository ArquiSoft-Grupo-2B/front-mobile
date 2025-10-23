package com.colswe.groupbtwo.ui.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.colswe.groupbtwo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val primaryColor = Color(0xFF003F55)
    val secondaryColor = Color(0xFF4CAF50)
    val textColor = Color(0xFF000000)

    LaunchedEffect(state.success) {
        if (state.success) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8EAF6),
                            Color(0xFFF5F7FA),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.iconr),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = "Crear cuenta",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 28.sp
                )

                Text(
                    text = "Únete y comienza tu experiencia",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF636E72),
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Correo electrónico",
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("ejemplo@correo.com", color = Color(0xFF999999)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = primaryColor
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFDFE6E9),
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                focusedTextColor = Color(0xFF000000),
                                unfocusedTextColor = Color(0xFF000000)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = state.error != null && email.isNotEmpty()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Contraseña",
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Mínimo 6 caracteres", color = Color(0xFF999999)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Password",
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = {
                                TextButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = primaryColor
                                    )
                                ) {
                                    Text(
                                        text = if (passwordVisible) "Ocultar" else "Mostrar",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (password.isNotEmpty() && password.length < 6)
                                    Color(0xFFFF5252)
                                else
                                    primaryColor,
                                unfocusedBorderColor = Color(0xFFDFE6E9),
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                focusedTextColor = Color(0xFF000000),
                                unfocusedTextColor = Color(0xFF000000)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            isError = password.isNotEmpty() && password.length < 6
                        )

                        if (password.isNotEmpty() && password.length < 6) {
                            Text(
                                text = "Mínimo 6 caracteres",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF5252),
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Confirmar contraseña",
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = { Text("Repite tu contraseña", color = Color(0xFF999999)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Confirm Password",
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = {
                                TextButton(
                                    onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = primaryColor
                                    )
                                ) {
                                    Text(
                                        text = if (confirmPasswordVisible) "Ocultar" else "Mostrar",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (confirmPassword.isNotEmpty() && password != confirmPassword)
                                    Color(0xFFFF5252)
                                else
                                    primaryColor,
                                unfocusedBorderColor = Color(0xFFDFE6E9),
                                focusedContainerColor = Color(0xFFF8F9FA),
                                unfocusedContainerColor = Color(0xFFF8F9FA),
                                focusedTextColor = Color(0xFF000000),
                                unfocusedTextColor = Color(0xFF000000)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (validateForm(email, password, confirmPassword)) {
                                        viewModel.register(email, password)
                                    }
                                }
                            ),
                            isError = confirmPassword.isNotEmpty() && password != confirmPassword
                        )

                        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                            Text(
                                text = "Las contraseñas no coinciden",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF5252),
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        if (state.error != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEBEE)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = state.error ?: "",
                                    color = Color(0xFFD32F2F),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (validateForm(email, password, confirmPassword)) {
                                    viewModel.register(email, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !state.loading && validateForm(email, password, confirmPassword),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFB2BAC2),
                                disabledContentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            if (state.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Crear cuenta",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Al registrarte, aceptas nuestros Términos de Servicio y Política de Privacidad",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF636E72),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Ya tienes cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF636E72)
                    )
                    TextButton(
                        onClick = onBackToLogin,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = primaryColor
                        )
                    ) {
                        Text(
                            "Iniciar sesión",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun validateForm(
    email: String,
    password: String,
    confirmPassword: String
): Boolean {
    return email.isNotEmpty() &&
            email.contains("@") &&
            password.length >= 6 &&
            password == confirmPassword
}