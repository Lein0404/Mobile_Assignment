package com.example.foodieheal.User.View

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.User.viewModel.AuthViewModel
import com.example.foodieheal.meal_planner.screen.OfflinePlaceholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBodyStatusScreen(navController: NavController, fromRegister: Boolean = false) {
    // Shared ViewModel from Activity context to keep everything in sync
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val user = authViewModel.currentUser

    var weight by remember { mutableStateOf(user?.weight?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var height by remember { mutableStateOf(user?.height?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var age by remember { mutableStateOf(user?.age?.let { if (it == 0) "" else it.toString() } ?: "") }
    var gender by remember { mutableStateOf(user?.gender ?: "Male") }

    // Optimization: Calculate BMI only when needed to prevent "Davey" hangs
    val bmiInfo = remember(weight, height) {
        val w = weight.toDoubleOrNull() ?: 0.0
        val h = (height.toDoubleOrNull() ?: 0.0) / 100.0
        if (h > 0) {
            val score = w / (h * h)
            val categoryResId = when {
                score < 18.5 -> R.string.body_status_bmi_underweight
                score < 25.0 -> R.string.body_status_bmi_normal
                score < 30.0 -> R.string.body_status_bmi_overweight
                else -> R.string.body_status_bmi_obese
            }
            Pair(String.format("%.1f", score), categoryResId)
        } else {
            Pair("0.0", null)
        }
    }
    val bmiValue = bmiInfo.first
    val bmiCategoryResId = bmiInfo.second

    // 🌟 No more collection logic here, avoids the "kick back" bug entirely
    DisposableEffect(Unit) {
        authViewModel.clearProfileEvents()
        onDispose { }
    }

    // 🌟 Handle navigation after successful registration
    LaunchedEffect(authViewModel.loginSuccess) {
        if (authViewModel.loginSuccess && fromRegister) {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding().navigationBarsPadding(), // 🌟 Added IME and Navigation padding
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.body_status_title), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (fromRegister) {
                            // 🌟 If we just registered, explicitly go back to Register screen
                            navController.navigate(Screen.Register.route) {
                                popUpTo(Screen.Register.route) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack() 
                        }
                    }) {
                        Icon(painterResource(id = R.drawable.ic_arrowback), stringResource(R.string.back_button), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (!authViewModel.isNetworkAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                OfflinePlaceholder(message = stringResource(R.string.desc_connect_internet_profile))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
            Text(
                text = stringResource(R.string.body_status_question),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.body_status_desc),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Gender Selection
            Text(stringResource(R.string.gender), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = gender == "Male",
                    onClick = { gender = "Male" },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text(stringResource(R.string.gender_male), color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, end = 24.dp))

                RadioButton(
                    selected = gender == "Female",
                    onClick = { gender = "Female" },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text(stringResource(R.string.gender_female), color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weight Field
            StatusInputField(
                label = stringResource(R.string.body_status_weight), 
                value = weight, 
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) weight = it }, 
                suffix = "kg"
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Height Field
            StatusInputField(
                label = stringResource(R.string.body_status_height), 
                value = height, 
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) height = it }, 
                suffix = "cm"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Age Field
            StatusInputField(
                label = stringResource(R.string.body_status_age), 
                value = age, 
                onValueChange = { if (it.all { char -> char.isDigit() }) age = it }, 
                suffix = stringResource(R.string.body_status_years)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // BMI Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.body_status_calculated_bmi), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = bmiValue, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                            if (bmiCategoryResId != null) {
                                val category = stringResource(bmiCategoryResId)
                                Text(
                                    text = " ($category)", 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = when(bmiCategoryResId) {
                                        R.string.body_status_bmi_normal -> Color(0xFF4CAF50)
                                        R.string.body_status_bmi_overweight, R.string.body_status_bmi_underweight -> Color(0xFFFF9800)
                                        else -> Color(0xFFF44336)
                                    },
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fire),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (authViewModel.errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = authViewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (fromRegister) {
                    OutlinedButton(
                        onClick = {
                            // 🌟 SKIP during registration: Create account with empty body status
                            authViewModel.registerWithProfile(
                                weight = null, height = null, age = null, gender = "Male", bmi = null
                            )
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        enabled = !authViewModel.isProcessing
                    ) {
                        Text(stringResource(R.string.body_status_skip), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Button(
                    onClick = {
                        if (fromRegister) {
                            // 🌟 SUBMIT during registration: Create account with full body status
                            authViewModel.registerWithProfile(
                                weight = weight.toDoubleOrNull(),
                                height = height.toDoubleOrNull(),
                                age = age.toIntOrNull(),
                                gender = gender,
                                bmi = bmiValue.toDoubleOrNull()
                            )
                        } else {
                            // Regular update for existing users
                            authViewModel.updateProfile(
                                name = user?.name ?: "",
                                email = user?.email ?: "",
                                profilePicUrl = user?.profilePicUrl ?: "",
                                description = user?.description ?: "",
                                weight = weight.toDoubleOrNull(),
                                height = height.toDoubleOrNull(),
                                age = age.toIntOrNull(),
                                gender = gender,
                                bmi = bmiValue.toDoubleOrNull()
                            ) {
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !authViewModel.isProcessing
                ) {
                    if (authViewModel.isProcessing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (fromRegister) stringResource(R.string.body_status_register) else stringResource(R.string.body_status_submit), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
}

@Composable
fun StatusInputField(label: String, value: String, onValueChange: (String) -> Unit, suffix: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Text(text = suffix, modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
