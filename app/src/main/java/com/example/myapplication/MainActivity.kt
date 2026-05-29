package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentScreen by remember { mutableStateOf("calculator") }
    val context = LocalContext.current
    val appName = stringResource(id = R.string.app_name)
    val aboutTitle = stringResource(id = R.string.about_title)
    val githubUrl = stringResource(id = R.string.github_url)
    val shareText = stringResource(id = R.string.share_text, githubUrl)

    val shareVia = stringResource(R.string.share_via)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentScreen == "calculator") appName else aboutTitle) },
                actions = {
                    if (currentScreen == "calculator") {
                        IconButton(onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, shareVia))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.desc_share))
                        }
                        IconButton(onClick = { currentScreen = "about" }) {
                            Icon(Icons.Default.Info, contentDescription = stringResource(R.string.desc_about))
                        }
                    } else {
                        TextButton(onClick = { currentScreen = "calculator" }) {
                            Text(stringResource(R.string.btn_back), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (currentScreen == "calculator") {
                ZakatCalculator()
            } else {
                AboutPage()
            }
        }
    }
}

@Composable
fun ZakatCalculator() {
    var weightInput by remember { mutableStateOf("") }
    var goldValueInput by remember { mutableStateOf("") }
    
    val optionKeep = stringResource(R.string.option_keep)
    val optionWear = stringResource(R.string.option_wear)
    val radioOptions = listOf(optionKeep, optionWear)
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }

    var totalValue by remember { mutableDoubleStateOf(0.0) }
    var weightMinusX by remember { mutableDoubleStateOf(0.0) }
    var zakatPayableValue by remember { mutableDoubleStateOf(0.0) }
    var totalZakat by remember { mutableDoubleStateOf(0.0) }
    var hasCalculated by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    val errorInput = stringResource(R.string.error_invalid_input)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.calc_header),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it; errorMsg = null },
            label = { Text(stringResource(R.string.label_weight)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = errorMsg != null && weightInput.isEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = goldValueInput,
            onValueChange = { goldValueInput = it; errorMsg = null },
            label = { Text(stringResource(R.string.label_value)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = errorMsg != null && goldValueInput.isEmpty()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(stringResource(R.string.label_usage_type), modifier = Modifier.align(Alignment.Start), fontWeight = FontWeight.SemiBold)
        Column(Modifier.selectableGroup()) {
            radioOptions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (text == selectedOption),
                        onClick = null
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val weight = weightInput.toDoubleOrNull()
                val value = goldValueInput.toDoubleOrNull()

                if (weight == null || value == null) {
                    errorMsg = errorInput
                    hasCalculated = false
                    return@Button
                }

                val thresholdX = if (selectedOption == optionKeep) 85.0 else 200.0
                
                totalValue = weight * value
                val diff = weight - thresholdX
                weightMinusX = if (diff > 0) diff else 0.0
                zakatPayableValue = weightMinusX * value
                totalZakat = zakatPayableValue * 0.025
                
                hasCalculated = true
                errorMsg = null
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(stringResource(R.string.btn_calculate), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (hasCalculated) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    ResultRow(stringResource(R.string.res_total_value), stringResource(R.string.format_currency, totalValue))
                    ResultRow(stringResource(R.string.res_weight_minus_x), stringResource(R.string.format_weight, weightMinusX))
                    ResultRow(stringResource(R.string.res_payable_value), stringResource(R.string.format_currency, zakatPayableValue))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ResultRow(stringResource(R.string.res_total_zakat), stringResource(R.string.format_currency, totalZakat), isBold = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.instructions),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun AboutPage() {
    val context = LocalContext.current
    val githubUrl = stringResource(id = R.string.github_url)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.dev_profile_header),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                AboutInfoRow(stringResource(R.string.label_name), stringResource(R.string.dev_name))
                AboutInfoRow(stringResource(R.string.label_student_id), stringResource(R.string.dev_id))
                AboutInfoRow(stringResource(R.string.label_programme), stringResource(R.string.dev_programme))
                AboutInfoRow(stringResource(R.string.label_group), stringResource(R.string.dev_group))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            stringResource(R.string.about_app_header),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            stringResource(R.string.about_description),
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(vertical = 12.dp),
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(stringResource(R.string.copyright), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.github_label), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            githubUrl,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                    context.startActivity(intent)
                }
        )
    }
}

@Composable
fun AboutInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(value)
    }
}
