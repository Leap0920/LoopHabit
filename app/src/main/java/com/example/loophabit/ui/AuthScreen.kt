package com.example.loophabit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: HabitViewModel) {
    var mode by remember { mutableStateOf("LOGIN") } // LOGIN, REGISTER, FORGOT

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("What was the name of your first pet?") }
    var securityAnswer by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf("") }

    // Forgot Password Flow State
    var forgotStep by remember { mutableStateOf(1) } // 1: find email, 2: answer & reset
    var resolvedSecurityQuestion by remember { mutableStateOf("") }

    val securityQuestions = listOf(
        "What was the name of your first pet?",
        "What is your mother's maiden name?",
        "In what city were you born?",
        "What was the make of your first car?",
        "What is your favorite book?"
    )

    var showQuestionDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing background circles matching theme colors
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), CircleShape)
        )

        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "♾️",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = when (mode) {
                        "LOGIN" -> "Welcome Back"
                        "REGISTER" -> "Create Account"
                        else -> "Reset Password"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (mode) {
                        "LOGIN" -> "Sign in to track your habits"
                        "REGISTER" -> "Join LoopHabit today"
                        else -> "Recover your account credentials"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (successMsg.isNotEmpty()) {
                    Text(
                        text = successMsg,
                        color = Color(0xFF06D6A0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )

                when (mode) {
                    "LOGIN" -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMsg = ""; successMsg = "" },
                            label = { Text("Username or Email") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") },
                            singleLine = true,
                            shape = CircleShape,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = "" },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                            singleLine = true,
                            shape = CircleShape,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        GradientButton(
                            text = "Login",
                            onClick = {
                                viewModel.login(email, password,
                                    onSuccess = { errorMsg = "" },
                                    onError = { errorMsg = it }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Create Account",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    mode = "REGISTER"
                                    errorMsg = ""
                                    successMsg = ""
                                    password = ""
                                }
                            )
                            Text(
                                text = "Forgot Password?",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    mode = "FORGOT"
                                    forgotStep = 1
                                    errorMsg = ""
                                    successMsg = ""
                                    password = ""
                                }
                            )
                        }
                    }

                    "REGISTER" -> {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; errorMsg = "" },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User") },
                            singleLine = true,
                            shape = CircleShape,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMsg = "" },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                            singleLine = true,
                            shape = CircleShape,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = "" },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                            singleLine = true,
                            shape = CircleShape,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = securityQuestion,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Security Question") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Question") },
                                shape = CircleShape,
                                colors = textFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showQuestionDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showQuestionDropdown,
                                onDismissRequest = { showQuestionDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                securityQuestions.forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                                        onClick = {
                                            securityQuestion = q
                                            showQuestionDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = securityAnswer,
                            onValueChange = { securityAnswer = it; errorMsg = "" },
                            label = { Text("Your Answer") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Answer") },
                            singleLine = true,
                            shape = CircleShape,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        GradientButton(
                            text = "Create Account",
                            colors = listOf(Color(0xFF06D6A0), Color(0xFF118AB2)),
                            onClick = {
                                viewModel.register(username, email, password, securityQuestion, securityAnswer,
                                    onSuccess = {
                                        errorMsg = ""
                                        successMsg = ""
                                    },
                                    onError = {
                                        errorMsg = it
                                    }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Already have an account? Sign In",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                mode = "LOGIN"
                                errorMsg = ""
                                successMsg = ""
                            }
                        )
                    }

                    "FORGOT" -> {
                        if (forgotStep == 1) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; errorMsg = "" },
                                label = { Text("Registered Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                                singleLine = true,
                                shape = CircleShape,
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            GradientButton(
                                text = "Find Account",
                                onClick = {
                                    viewModel.getSecurityQuestion(email,
                                        onSuccess = { q ->
                                            resolvedSecurityQuestion = q
                                            forgotStep = 2
                                            errorMsg = ""
                                        },
                                        onError = { errorMsg = it }
                                    )
                                }
                            )
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = resolvedSecurityQuestion,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 14.sp
                                )
                            }

                            OutlinedTextField(
                                value = securityAnswer,
                                onValueChange = { securityAnswer = it; errorMsg = "" },
                                label = { Text("Your Answer") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = "Answer") },
                                singleLine = true,
                                shape = CircleShape,
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; errorMsg = "" },
                                label = { Text("New Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                                singleLine = true,
                                shape = CircleShape,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMsg = "" },
                                label = { Text("Confirm New Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                                singleLine = true,
                                shape = CircleShape,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = textFieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            GradientButton(
                                text = "Reset Password",
                                onClick = {
                                    if (password != confirmPassword) {
                                        errorMsg = "Passwords do not match"
                                        return@GradientButton
                                    }
                                    viewModel.resetPassword(email, securityAnswer, password,
                                        onSuccess = {
                                            successMsg = "Password reset successfully! Log in."
                                            mode = "LOGIN"
                                            forgotStep = 1
                                            errorMsg = ""
                                            securityAnswer = ""
                                            password = ""
                                            confirmPassword = ""
                                        },
                                        onError = { errorMsg = it }
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Back to Sign In",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                mode = "LOGIN"
                                errorMsg = ""
                                successMsg = ""
                            }
                        )
                    }
                }
            }
        }
    }
}
