package com.example.foodieheal.view

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodieheal.R
import com.example.foodieheal.navigation.Screen
import com.example.foodieheal.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBodyStatusScreen(navController: NavController, fromRegister: Boolean = false) {
    // Shared ViewModel from Activity context to keep everything in sync
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = LocalContext.current as androidx.lifecycle.ViewModelStoreOwner)
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
            val category = when {
                score < 18.5 -> "Underweight"
                score < 25.0 -> "Normal"
                score < 30.0 -> "Overweight"
                else -> "Obese"
            }
            Pair(String.format("%.1f", score), category)
        } else {
            Pair("0.0", "")
        }
    }
    val bmiValue = bmiInfo.first
    val bmiCategory = bmiInfo.second

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Body Status", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    if (!fromRegister) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(painterResource(id = R.drawable.ic_arrowback), "Back", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8))
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "What’s your body status?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "This is for BMI calculation and meal planning purposes. You may skip first if you didn’t have any plans yet.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Gender Selection
            Text("Gender", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = gender == "Male",
                    onClick = { gender = "Male" },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text("Male", color = Color.Black, modifier = Modifier.padding(start = 4.dp, end = 24.dp))

                RadioButton(
                    selected = gender == "Female",
                    onClick = { gender = "Female" },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text("Female", color = Color.Black, modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weight Field
            StatusInputField(
                label = "Weight", 
                value = weight, 
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) weight = it }, 
                suffix = "kg"
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Height Field
            StatusInputField(
                label = "Height", 
                value = height, 
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) height = it }, 
                suffix = "cm"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Age Field
            StatusInputField(
                label = "Age", 
                value = age, 
                onValueChange = { if (it.all { char -> char.isDigit() }) age = it }, 
                suffix = "years"
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
                        Text("Calculated BMI", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = bmiValue, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            if (bmiCategory.isNotEmpty()) {
                                Text(
                                    text = " ($bmiCategory)", 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = when(bmiCategory) {
                                        "Normal" -> Color(0xFF4CAF50)
                                        "Overweight", "Underweight" -> Color(0xFFFF9800)
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

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (fromRegister) {
                    OutlinedButton(
                        onClick = {
                            // 🌟 FIX: Navigate to Home instead of Main
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("SKIP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Button(
                    onClick = {
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
                        )
                        if (fromRegister) {
                            // 🌟 FIX: Navigate to Home instead of Main
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = !authViewModel.isProcessing
                ) {
                    if (authViewModel.isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SUBMIT", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusInputField(label: String, value: String, onValueChange: (String) -> Unit, suffix: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Text(text = suffix, modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold, color = Color.Black)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFE8E8E8),
                unfocusedContainerColor = Color(0xFFE8E8E8),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
    }
}
