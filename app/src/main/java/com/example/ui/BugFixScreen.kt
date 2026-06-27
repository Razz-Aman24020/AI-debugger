package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.BugFixSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugFixScreen(
    viewModel: BugFixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // UI State
    val buggyCode by viewModel.buggyCode.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val apiError by viewModel.apiError.collectAsStateWithLifecycle()
    val selectedSession by viewModel.selectedSession.collectAsStateWithLifecycle()
    val savedSessions by viewModel.savedSessions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterLanguage by viewModel.filterLanguage.collectAsStateWithLifecycle()

    // Tab control: "Workspace" vs "History Library"
    var currentTab by remember { mutableStateOf("Workspace") }
    
    // Dropdown list control for language picker
    var isLangDropdownExpanded by remember { mutableStateOf(false) }

    // Popular languages for quick chip selection
    val quickLanguages = listOf("Auto-detect", "Python", "Java", "JavaScript", "Kotlin", "C++", "Rust", "Go")
    val allLanguages = listOf("Auto-detect", "Python", "Java", "JavaScript", "TypeScript", "Kotlin", "C++", "Rust", "Go", "HTML/CSS", "SQL", "PHP", "Ruby", "Other")

    // Preset examples of bugs for users to test
    val presetBugs = listOf(
        PresetBug(
            title = "Python Index Error",
            language = "Python",
            code = """def calculate_average(numbers):
    total = 0
    # BUG: range(len + 1) will cause IndexError
    for i in range(len(numbers) + 1):
        total += numbers[i]
    return total / len(numbers)""",
            error = "IndexError: list index out of range"
        ),
        PresetBug(
            title = "Java NullPointer",
            language = "Java",
            code = """public class ProfileService {
    private String userEmail; // Uninitialized
    
    public int getLength() {
        // BUG: userEmail is null
        return userEmail.length();
    }
}""",
            error = "java.lang.NullPointerException: Cannot invoke \"String.length()\""
        ),
        PresetBug(
            title = "JS Scope Closure",
            language = "JavaScript",
            code = """function registerHandlers() {
    // BUG: var has function scope, closure captures final index '5'
    for (var i = 0; i < 5; i++) {
        setTimeout(function() {
            console.log("Item registered: " + i);
        }, 100);
    }
}""",
            error = "Prints 'Item registered: 5' five times instead of 0,1,2,3,4"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.example.ui.theme.Indigo600),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Fixify AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Text(
                                text = "Zero-effort AI debugger",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.clearInputs()
                            Toast.makeText(context, "Workspace cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("clear_workspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear all inputs",
                            tint = com.example.ui.theme.Indigo600
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(com.example.ui.theme.Slate200)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Profile",
                            tint = com.example.ui.theme.Slate600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = if (currentTab == "Workspace") 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = currentTab == "Workspace",
                    onClick = { currentTab = "Workspace" },
                    text = { Text("Workspace", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Workspace") },
                    modifier = Modifier.testTag("tab_workspace")
                )
                Tab(
                    selected = currentTab == "History",
                    onClick = { currentTab = "History" },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("History Library", fontWeight = FontWeight.SemiBold)
                            if (savedSessions.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(savedSessions.size.toString())
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "History") },
                    modifier = Modifier.testTag("tab_history")
                )
            }

            // Main Contents switching between tabs
            if (currentTab == "Workspace") {
                Box(modifier = Modifier.fillMaxSize()) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Beautiful Interactive Bento Grid Dashboard
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Tile 1: Active Session Block (Indigo)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.Indigo600)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ACTIVE SESSION",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = com.example.ui.theme.Indigo100,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                            Text(
                                                text = if (buggyCode.isNotEmpty()) "Repair Workspace" else "Ready to Scan",
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.2f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = selectedLanguage,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Live Status Console
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.Black.copy(alpha = 0.15f))
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isProcessing) Color(0xFF10B981) else Color(0xFFF59E0B))
                                                )
                                                Text(
                                                    text = if (isProcessing) "analyzing_stack_trace..." else if (buggyCode.isNotEmpty()) "code_ready_for_repair" else "waiting_for_input...",
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.8f)
                                                    )
                                                )
                                            }
                                            // Progress bar
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(if (isProcessing) 0.75f else if (buggyCode.isNotEmpty()) 0.4f else 0.1f)
                                                        .fillMaxHeight()
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF10B981))
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Row of Tiles 2 & 3
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Tile 2: Languages Supported (White with Orange accent)
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, com.example.ui.theme.Slate200)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(com.example.ui.theme.Orange50),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = com.example.ui.theme.Orange500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "42+",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    color = com.example.ui.theme.Slate900,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "Languages Supported",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = com.example.ui.theme.Slate500,
                                                    fontWeight = FontWeight.Medium,
                                                    lineHeight = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                // Tile 3: Success Rate (Emerald)
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.Emerald50),
                                    border = BorderStroke(1.dp, com.example.ui.theme.Emerald100)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(com.example.ui.theme.Emerald100),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = com.example.ui.theme.Emerald600,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "98%",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    color = com.example.ui.theme.Emerald900,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = "Success Rate",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = com.example.ui.theme.Emerald700,
                                                    fontWeight = FontWeight.Medium,
                                                    lineHeight = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Tile 4: Last Scanned (Dark Slate)
                            val lastSession = savedSessions.firstOrNull()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = lastSession != null) {
                                        if (lastSession != null) {
                                            viewModel.selectSession(lastSession)
                                        }
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.Slate900)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "LAST SCANNED",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = com.example.ui.theme.Slate400,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                        Text(
                                            text = lastSession?.title ?: "No recent scans",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (lastSession != null) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("3 Critical", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("1 Warning", color = Color(0xFFFCD34D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Scan a bug to see severity report",
                                                style = MaterialTheme.typography.bodySmall.copy(color = com.example.ui.theme.Slate400)
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Load Session",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Preset Bugs Section (The Playground)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Try A Preset Bug (Sandbox Playground)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                presetBugs.forEach { bug ->
                                    Card(
                                        modifier = Modifier
                                            .width(220.dp)
                                            .clickable {
                                                viewModel.updateBuggyCode(bug.code)
                                                viewModel.updateErrorMessage(bug.error)
                                                viewModel.updateSelectedLanguage(bug.language)
                                                viewModel.selectSession(null) // Reset active viewing session
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Loaded ${bug.title}",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                                Text(
                                                    text = bug.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Simulated error: ${bug.error}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Selection & Code Input Block
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Configuration line: Language and Model Picker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Language selection chip triggering dropdown
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedCard(
                                        onClick = { isLangDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Language: $selectedLanguage",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand list"
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = isLangDropdownExpanded,
                                        onDismissRequest = { isLangDropdownExpanded = false }
                                    ) {
                                        allLanguages.forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text(lang) },
                                                onClick = {
                                                    viewModel.updateSelectedLanguage(lang)
                                                    isLangDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Model picker choice
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (selectedModel == "gemini-3.1-pro-preview") "Model: 3.1 Pro 💎" else "Model: 3.5 Flash ⚡",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Switch(
                                            checked = selectedModel == "gemini-3.1-pro-preview",
                                            onCheckedChange = { isPro ->
                                                viewModel.updateSelectedModel(
                                                    if (isPro) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
                                                )
                                            },
                                            thumbContent = {
                                                Icon(
                                                    imageVector = if (selectedModel == "gemini-3.1-pro-preview") Icons.Default.Star else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                }
                            }

                            // Horizontal Quick Language Picker Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickLanguages.forEach { lang ->
                                    FilterChip(
                                        selected = selectedLanguage == lang,
                                        onClick = { viewModel.updateSelectedLanguage(lang) },
                                        label = { Text(lang) }
                                    )
                                }
                            }
                        }

                        // 4. Code input pane styled as a terminal
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Buggy Code Terminal Source",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (buggyCode.isNotEmpty()) {
                                    Text(
                                        text = "${buggyCode.lines().size} Lines",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E24))
                                    .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(12.dp))
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    // Simulated Line Numbers column
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(36.dp)
                                            .background(Color(0xFF151518))
                                            .padding(top = 16.dp, bottom = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val lineCount = maxOf(1, buggyCode.lines().size)
                                        for (i in 1..minOf(10, lineCount)) {
                                            Text(
                                                text = i.toString(),
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF52525B)
                                                )
                                            )
                                        }
                                        if (lineCount > 10) {
                                            Text(
                                                text = "..",
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF52525B)
                                                )
                                            )
                                        }
                                    }

                                    // Real Code Editor TextField
                                    TextField(
                                        value = buggyCode,
                                        onValueChange = { viewModel.updateBuggyCode(it) },
                                        placeholder = {
                                            Text(
                                                text = "Paste or type your buggy code function here...\n\nExample:\ndef calculate(x):\n   return x / 0",
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF71717A)
                                                )
                                            )
                                        },
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = Color(0xFFE4E4E7)
                                        ),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("buggy_code_input")
                                    )
                                }
                            }
                        }

                        // 5. Secondary Error Logs/Console inputs
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Compiler Error / Stack Trace (Optional Context)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            OutlinedTextField(
                                value = errorMessage,
                                onValueChange = { viewModel.updateErrorMessage(it) },
                                placeholder = {
                                    Text(
                                        text = "Paste compiler error logs, unexpected return values, or infinite loop details here to guide the AI repair...",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    )
                                },
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("error_message_input")
                            )
                        }

                        // 6. Primary Repair Button
                        Button(
                            onClick = {
                                viewModel.fixBug(BuildConfig.GEMINI_API_KEY)
                            },
                            enabled = buggyCode.trim().isNotEmpty() && !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("fix_bug_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null
                                )
                                Text(
                                    text = if (isProcessing) "AI Analyzing Code..." else "Generate Code Fix",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }

                        // Displaying Error Statuses
                        AnimatedVisibility(visible = apiError != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error icon",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Column {
                                        Text(
                                            text = "Repair Failed",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = apiError ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom sheet elements
                    }

                    // 7. Processing Animation overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isProcessing,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.75f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(32.dp)
                                    .fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 4.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Consulting AI Repair Engine...",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Analyzing syntax models, correcting logical traps, and drafting explanations",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 16.sp
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 8. Sliding up Bottom Sheet style display of active Session Fixes!
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedSession != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        selectedSession?.let { session ->
                            CodeRepairBottomSheet(
                                session = session,
                                onClose = { viewModel.selectSession(null) },
                                onCopy = { code ->
                                    clipboardManager.setText(AnnotatedString(code))
                                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // History tab view
                HistoryLibraryScreen(
                    sessions = savedSessions,
                    searchQuery = searchQuery,
                    filterLanguage = filterLanguage,
                    languages = allLanguages,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onLanguageSelect = { viewModel.setFilterLanguage(it) },
                    onSelectSession = { session ->
                        viewModel.selectSession(session)
                        currentTab = "Workspace" // Switch back to view details!
                    },
                    onDeleteSession = { id ->
                        viewModel.deleteSession(id)
                        Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                    },
                    onClearHistory = {
                        viewModel.clearHistory()
                        Toast.makeText(context, "History cleared completely", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun CodeRepairBottomSheet(
    session: BugFixSession,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .testTag("repair_bottom_sheet"),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Repaired: ${session.language}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_sheet_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close sheet")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Body content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fixed Code Block Header + Copy action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Corrected Code",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Button(
                        onClick = { onCopy(session.fixedCode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("copy_code_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share, // Represents copying/sharing
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Fixed Code Block Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E24))
                        .border(1.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = session.fixedCode,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF) // Beautiful neon repair color
                        )
                    )
                }

                // Explanation Block
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI Bug Analysis & Explanation",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Simple markdown rendering for bullets and lines
                            val formattedText = session.explanation.split("\n")
                            formattedText.forEach { line ->
                                if (line.trim().startsWith("*") || line.trim().startsWith("-")) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Text(
                                            text = line.removePrefix("*").removePrefix("-").trim(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else if (line.trim().startsWith("#")) {
                                    // Header lines
                                    val cleanLine = line.replace("#", "").trim()
                                    Text(
                                        text = cleanLine,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                } else {
                                    if (line.isNotEmpty()) {
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryLibraryScreen(
    sessions: List<BugFixSession>,
    searchQuery: String,
    filterLanguage: String,
    languages: List<String>,
    onSearchChange: (String) -> Unit,
    onLanguageSelect: (String) -> Unit,
    onSelectSession: (BugFixSession) -> Unit,
    onDeleteSession: (Int) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLangFilterExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Controls line: Search + Filter Language
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search historical fixes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_input")
            )

            Box {
                Button(
                    onClick = { isLangFilterExpanded = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = if (filterLanguage == "All") "Lang" else filterLanguage)
                    }
                }
                DropdownMenu(
                    expanded = isLangFilterExpanded,
                    onDismissRequest = { isLangFilterExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Languages") },
                        onClick = {
                            onLanguageSelect("All")
                            isLangFilterExpanded = false
                        }
                    )
                    languages.filter { it != "Auto-detect" }.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang) },
                            onClick = {
                                onLanguageSelect(lang)
                                isLangFilterExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Header section of historical list with delete all
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (filterLanguage == "All") "Saved Fix Sessions" else "$filterLanguage Fixes",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (sessions.isNotEmpty()) {
                TextButton(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp)
                }
            }
        }

        // Sessions list or empty state display
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No fixes matching your search" else "No historical code repairs yet.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Bugs analyzed and repaired in the workspace will automatically save to your history library.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSession(session) }
                            .testTag("history_item_${session.id}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) {
                                        Text(session.language, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                    Text(
                                        text = formatTime(session.timestamp),
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = session.explanation,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSession(session.id) },
                                modifier = Modifier.testTag("delete_session_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete from history",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Utility class for playground sandbox
data class PresetBug(
    val title: String,
    val language: String,
    val code: String,
    val error: String
)
