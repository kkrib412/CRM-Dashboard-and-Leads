package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Interaction
import com.example.data.Lead
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CRMScreen(
    viewModel: CRMViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val isCommandPaletteOpen by viewModel.isCommandPaletteOpen.collectAsState()
    val selectLeadId by viewModel.selectedLeadId.collectAsState()
    val selectedLead by viewModel.selectedLead.collectAsState()
    val selectedLeadInteractions by viewModel.selectedLeadInteractions.collectAsState()
    val followUpLeadId by viewModel.followUpLeadId.collectAsState()
    val followUpLead by viewModel.followUpLead.collectAsState()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (currentUser == null) {
        AuthenticationScreen(viewModel = viewModel)
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceDark,
                drawerContentColor = Color.White,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .border(1.dp, SurfaceVariantDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(PrimaryDark, SecondaryDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AURA CRM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = currentUser?.email ?: "Workspace Admin",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Divider(color = SurfaceVariantDark, modifier = Modifier.padding(bottom = 24.dp))
                    
                    // Drawer Items
                    val items = listOf(
                        Triple("dashboard", "Dashboard", Icons.Default.Home),
                        Triple("kanban", "Pipeline Board", Icons.Default.Check),
                        Triple("list", "Lead Directory", Icons.Default.List),
                        Triple("settings", "Settings & Portability", Icons.Default.Settings)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        items.forEach { (route, label, icon) ->
                            val isSelected = currentTab == route
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.navigateTo(route)
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) PrimaryDark else Color.Gray
                                    )
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = SurfaceVariantDark.copy(alpha = 0.5f),
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("drawer_item_$route")
                            )
                        }
                    }
                    
                    // Footer
                    Divider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "AURA CRM v1.4.0",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        // System back-press handling for dialogs
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            },
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Drawer Menu",
                                tint = Color.White
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(PrimaryDark, SecondaryDark))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "AURA CRM",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 20.sp,
                                color = PrimaryDark
                            )
                        }
                    },
                actions = {
                    // Command Palette Shortcut trigger Button
                    IconButton(
                        onClick = { viewModel.toggleCommandPalette(true) },
                        modifier = Modifier
                            .testTag("command_palette_trigger")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search & Cmd Palette",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.openAddEdit(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Lead")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Lead")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Adaptive Navigation (Bottom Bar on compact mobile devices)
            CRMBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.navigateTo(it) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Main views based on tab state
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "MainViewTransition"
            ) { tab ->
                when (tab) {
                    "dashboard" -> DashboardView(
                        viewModel = viewModel,
                        onFollowUpClick = { viewModel.openFollowUp(it) },
                        onLeadClick = { viewModel.selectLead(it) }
                    )
                    "kanban" -> KanbanView(viewModel)
                    "list" -> LeadListView(viewModel)
                    "settings" -> SettingsView(viewModel)
                }
            }

            // Slide-out Detail Drawer (Slide animation overlay)
            // Beautiful Animated Slide-out Backdrop Scrim Overlay
            AnimatedVisibility(
                visible = selectLeadId != null && selectedLead != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.selectLead(null)
                        }
                )
            }

            // Beautiful Animated Slide-out Detail Drawer (Overlaid from CenterEnd edge)
            AnimatedVisibility(
                visible = selectLeadId != null && selectedLead != null,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
            ) {
                if (selectedLead != null) {
                    DetailDrawerContent(
                        lead = selectedLead!!,
                        interactions = selectedLeadInteractions,
                        onClose = { viewModel.selectLead(null) },
                        onEdit = { 
                            viewModel.openAddEdit(selectedLead!!.id)
                        },
                        onDelete = {
                            viewModel.deleteLead(selectedLead!!)
                            viewModel.selectLead(null)
                        },
                        onAddNote = { note ->
                            viewModel.addManualNote(selectedLead!!.id, note)
                        },
                        onEmailClick = { leadId ->
                            viewModel.openEmailComposer(leadId)
                        },
                        onUpdateStage = { stage ->
                            viewModel.updateLeadStage(selectedLead!!, stage)
                        },
                        onTriggerFollowUp = {
                            viewModel.openFollowUp(selectedLead!!.id)
                        }
                    )
                }
            }

            // Command Palette / Global Search Dialog overlay
            if (isCommandPaletteOpen) {
                CommandPaletteDialog(
                    viewModel = viewModel,
                    onClose = { viewModel.toggleCommandPalette(false) },
                    onSelectLead = { leadId ->
                        viewModel.toggleCommandPalette(false)
                        viewModel.selectLead(leadId)
                    },
                    onTriggerFollowUp = { leadId ->
                        viewModel.toggleCommandPalette(false)
                        viewModel.openFollowUp(leadId)
                    }
                )
            }

            // Follow Up Contextual Modal Dialog overlay
            if (followUpLeadId != null && followUpLead != null) {
                FollowUpModalDialog(
                    viewModel = viewModel,
                    lead = followUpLead!!,
                    onClose = { viewModel.closeFollowUpDetail() }
                )
            }

            // Add/Edit Lead Dialog overlay
            if (isAddEditOpen) {
                AddEditLeadDialog(
                    viewModel = viewModel,
                    onClose = { viewModel.closeAddEdit() }
                )
            }

            // In-app Email Composer Dialog
            val isEmailComposerOpen by viewModel.isEmailComposerOpen.collectAsState()
            val emailComposerLead by viewModel.emailComposerLead.collectAsState()
            if (isEmailComposerOpen && emailComposerLead != null) {
                EmailComposerDialog(
                    viewModel = viewModel,
                    lead = emailComposerLead!!,
                    onClose = { viewModel.closeEmailComposer() }
                )
            }
        }
    }
}
}

// Bottom Navigation component
@Composable
fun CRMBottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = SurfaceDark
    ) {
        NavigationBarItem(
            selected = currentTab == "dashboard",
            onClick = { onTabSelected("dashboard") },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryDark,
                selectedTextColor = PrimaryDark,
                indicatorColor = SurfaceVariantDark
            )
        )
        NavigationBarItem(
            selected = currentTab == "kanban",
            onClick = { onTabSelected("kanban") },
            icon = { Icon(imageVector = Icons.Default.Check, contentDescription = "Pipeline Board") },
            label = { Text("Pipeline") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryDark,
                selectedTextColor = PrimaryDark,
                indicatorColor = SurfaceVariantDark
            )
        )
        NavigationBarItem(
            selected = currentTab == "list",
            onClick = { onTabSelected("list") },
            icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Leads") },
            label = { Text("Leads") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryDark,
                selectedTextColor = PrimaryDark,
                indicatorColor = SurfaceVariantDark
            )
        )
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { onTabSelected("settings") },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryDark,
                selectedTextColor = PrimaryDark,
                indicatorColor = SurfaceVariantDark
            )
        )
    }
}

// KPIs, Queue Panel
@Composable
fun DashboardView(
    viewModel: CRMViewModel,
    onFollowUpClick: (Int) -> Unit,
    onLeadClick: (Int) -> Unit
) {
    val leads by viewModel.allLeads.collectAsState()
    val dueToday by viewModel.dueTodayLeads.collectAsState()
    val pipelineVal by viewModel.pipelineValue.collectAsState()
    val activeLeads by viewModel.activeLeadsCount.collectAsState()
    val conversion by viewModel.conversionRate.collectAsState()
    val salesCycle by viewModel.avgSalesCycle.collectAsState()

    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Performance Overview",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Visual KPIs Grid layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Total Pipeline Value
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Pipeline Value", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(SecondaryDark.copy(alpha = 0.15f), CircleShape)
                                .padding(5.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = SecondaryDark, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatter.format(pipelineVal),
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Visual Composition Progress Bar
                    val wonVal = leads.filter { it.pipelineStage == "Won" }.sumOf { it.dealValue }
                    val activeVal = leads.filter { it.pipelineStage != "Won" && it.pipelineStage != "Lost" }.sumOf { it.dealValue }
                    val totalVal = wonVal + activeVal
                    val wonFraction = if (totalVal > 0) (wonVal / totalVal).toFloat() else 0f
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfaceVariantDark)
                    ) {
                        if (wonFraction > 0f) {
                            Box(modifier = Modifier.fillMaxHeight().weight(wonFraction).background(ColorWon))
                        }
                        if (1f - wonFraction > 0f) {
                            Box(modifier = Modifier.fillMaxHeight().weight(1f - wonFraction).background(SecondaryDark))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Won: ${formatter.format(wonVal)}", fontSize = 9.sp, color = ColorWon)
                        Text(text = "Active: ${formatter.format(activeVal)}", fontSize = 9.sp, color = SecondaryDark)
                    }
                }
            }

            // Card 2: Conversion Rate with circular arc meter
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Win Rate", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(ColorWon.copy(alpha = 0.15f), CircleShape)
                                .padding(5.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ColorWon, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "%.1f%%".format(conversion),
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Conversion",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        
                        // Circular Progress Arc meter
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = SurfaceVariantDark,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 3.5.dp.toPx())
                                )
                                drawArc(
                                    color = ColorWon,
                                    startAngle = -90f,
                                    sweepAngle = (conversion.toFloat() * 3.6f).coerceIn(0f, 360f),
                                    useCenter = false,
                                    style = Stroke(width = 3.5.dp.toPx())
                                )
                            }
                            Text(
                                text = "${conversion.toInt()}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: Active Leads with Stacked Priority Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Active Leads", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(PrimaryDark.copy(alpha = 0.15f), CircleShape)
                                .padding(5.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PrimaryDark, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$activeLeads leads",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val highPriority = leads.count { it.pipelineStage != "Won" && it.pipelineStage != "Lost" && it.priority == "High" }.toFloat()
                    val medPriority = leads.count { it.pipelineStage != "Won" && it.pipelineStage != "Lost" && it.priority == "Medium" }.toFloat()
                    val lowPriority = leads.count { it.pipelineStage != "Won" && it.pipelineStage != "Lost" && it.priority == "Low" }.toFloat()
                    val totalPriority = highPriority + medPriority + lowPriority

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfaceVariantDark)
                    ) {
                        if (totalPriority > 0f) {
                            val highWeight = highPriority / totalPriority
                            val medWeight = medPriority / totalPriority
                            val lowWeight = lowPriority / totalPriority
                            if (highWeight > 0f) {
                                Box(modifier = Modifier.weight(highWeight).fillMaxHeight().background(PriorityHigh))
                            }
                            if (medWeight > 0f) {
                                Box(modifier = Modifier.weight(medWeight).fillMaxHeight().background(PriorityMedium))
                            }
                            if (lowWeight > 0f) {
                                Box(modifier = Modifier.weight(lowWeight).fillMaxHeight().background(PriorityLow))
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(SurfaceVariantDark))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Crit: ${highPriority.toInt()}", fontSize = 9.sp, color = PriorityHigh)
                        Text(text = "Med: ${medPriority.toInt()}", fontSize = 9.sp, color = PriorityMedium)
                        Text(text = "Low: ${lowPriority.toInt()}", fontSize = 9.sp, color = PriorityLow)
                    }
                }
            }

            // Card 4: Avg Sales Cycle with mini activity indicators bar chart
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Sales Cycle", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(ColorNewLead.copy(alpha = 0.15f), CircleShape)
                                .padding(5.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ColorNewLead, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "$salesCycle Days",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Avg closed cycle",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(
                            modifier = Modifier
                                .weight(0.7f)
                                .height(28.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val barHeights = listOf(0.40f, 0.75f, 0.50f, 0.85f, 0.60f, 0.95f)
                            barHeights.forEachIndexed { idx, heightVal ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(heightVal)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(ColorNewLead.copy(alpha = if (idx == barHeights.lastIndex) 0.9f else 0.45f))
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Intelligent Follow-Up: Due Today Queue
        Text(
            text = "Due Today Follow-Up Queue",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Immediate outreach targets based on next-action schedules",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (dueToday.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No tasks",
                        tint = PrimaryDark,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Follow-ups clear for today!",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Nice job! Create new leads to feed your active queue or review other pipeline cards.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dueToday.forEach { lead ->
                    DueTodayQueueItem(
                        lead = lead,
                        onFollowUp = { onFollowUpClick(lead.id) },
                        onViewDetails = { onLeadClick(lead.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Live Real-Time Activity Feed Component
        ActivityFeedCard(
            viewModel = viewModel,
            onLeadClick = onLeadClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Basic Info guide panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(SurfaceDark, SurfaceVariantDark)))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "💡 Local-First Operational Intelligence",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AURA CRM stores all data client-side in SQLite database for offline capability. Change channel preferences inside lead cards, trigger custom outreach sequences, and export files to preserve records.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ActivityFeedCard(
    viewModel: CRMViewModel,
    onLeadClick: (Int) -> Unit
) {
    val interactions by viewModel.allInteractions.collectAsState()
    val leads by viewModel.allLeads.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Stage Change", "Note", "Follow Up Executed", "Created")

    // Map lead entities for quick reference
    val leadMap = remember(leads) { leads.associateBy { it.id } }

    val filteredInteractions = remember(interactions, selectedFilter) {
        if (selectedFilter == "All") {
            interactions
        } else {
            interactions.filter { it.type == selectedFilter }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            .testTag("activity_feed_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Activity Feed",
                        tint = PrimaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "📈 Team Activity Feed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "${filteredInteractions.size} events",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Real-time updates of teammate tasks, deal stages, and outbound outreach records.",
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filtering chips list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val chipBg = if (isSelected) PrimaryDark.copy(alpha = 0.15f) else SurfaceVariantDark.copy(alpha = 0.5f)
                    val chipBorder = if (isSelected) PrimaryDark else SurfaceVariantDark
                    val chipText = if (isSelected) Color.White else Color.Gray

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val displayText = when (filter) {
                            "Follow Up Executed" -> "📞 Outreach"
                            "Stage Change" -> "🔄 Stage"
                            "Note" -> "📝 Notes"
                            "Created" -> "🆕 Added"
                            else -> "⚡ All"
                        }
                        Text(
                            text = displayText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = SurfaceVariantDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (filteredInteractions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activities logged for: $selectedFilter",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Display up to 10 events for performance and layout balance
                    filteredInteractions.take(10).forEach { interaction ->
                        val lead = leadMap[interaction.leadId]
                        val relativeTime = getRelativeTimeSpanString(interaction.timestamp)

                        val (icon, color) = when (interaction.type) {
                            "Created" -> Icons.Default.Add to ColorNewLead
                            "Stage Change" -> Icons.Default.Refresh to SecondaryDark
                            "Note" -> Icons.Default.Edit to PriorityMedium
                            "Follow Up Executed" -> Icons.Default.CheckCircle to ColorWon
                            else -> Icons.Default.Info to Color.LightGray
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark.copy(alpha = 0.4f))
                                .border(0.5.dp, SurfaceVariantDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { lead?.let { onLeadClick(it.id) } }
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Mini Badge
                            Box(
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(22.dp)
                                    .background(color.copy(alpha = 0.15f), CircleShape)
                                    .border(0.5.dp, color.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(11.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Activity content
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lead?.name ?: "System Process",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = relativeTime,
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                if (lead != null && lead.company.isNotBlank()) {
                                    Text(
                                        text = lead.company,
                                        fontSize = 9.sp,
                                        color = SecondaryDark,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                Text(
                                    text = interaction.description,
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    lineHeight = 14.sp
                                )

                                if (interaction.detail.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = interaction.detail,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        lineHeight = 13.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SurfaceVariantDark.copy(alpha = 0.4f))
                                            .padding(6.dp)
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

fun getRelativeTimeSpanString(time: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - time
    return when {
        diff < 0 -> "Future"
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 172800_000 -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(time))
    }
}

@Composable
fun KPICard(
    title: String,
    value: String,
    trendText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.15f), CircleShape)
                        .padding(6.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = trendText,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DueTodayQueueItem(
    lead: Lead,
    onFollowUp: () -> Unit,
    onViewDetails: () -> Unit
) {
    val formatterCur = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    val formatterDate = SimpleDateFormat("MMM d, yyyy", Locale.US)
    val isOverdue = lead.nextFollowUpDate < System.currentTimeMillis()

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isOverdue) PriorityHigh.copy(alpha = 0.8f) else SurfaceVariantDark,
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lead.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Priority Indicator Badge
                    val pColor = when (lead.priority) {
                        "High" -> PriorityHigh
                        "Medium" -> PriorityMedium
                        else -> PriorityLow
                    }
                    Box(
                        modifier = Modifier
                            .background(pColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(lead.priority, fontSize = 10.sp, color = pColor, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${lead.company} • ${formatterCur.format(lead.dealValue)}",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconChan = when (lead.channelPreference) {
                        "Email" -> Icons.Default.Email
                        "Phone" -> Icons.Default.Phone
                        "SMS" -> Icons.Default.MailOutline
                        else -> Icons.Default.Share
                    }
                    Icon(
                        imageVector = iconChan,
                        contentDescription = "Channel",
                        tint = PrimaryDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Preferred Channel: ${lead.channelPreference}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (isOverdue) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Overdue",
                            tint = PriorityHigh,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Overdue! Priority Task",
                            fontSize = 11.sp,
                            color = PriorityHigh,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CTA Follow Up Buttons
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onFollowUp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverdue) PriorityHigh else PrimaryDark
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Campaign, contentDescription = "Outreach", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Outreach", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "View details",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onViewDetails() }
                        .padding(4.dp)
                )
            }
        }
    }
}

// Kanban View
@Composable
fun KanbanView(viewModel: CRMViewModel) {
    val leads by viewModel.allLeads.collectAsState()
    val columns = listOf("New Lead", "Contacted", "Proposal Sent", "In Negotiation", "Won", "Lost")
    
    val scrollState = rememberScrollState()

    // Kanban Interactive Filter Bar States
    var searchQuery by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("All") }
    var selectedValueMin by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Pipeline Tracking Board",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "Horizontally scroll columns. Drag, drop or update status on leads",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )

        // Kanban Interactive Filter Bar Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search board...", fontSize = 12.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("kanban_search_input")
                )

                if (searchQuery.isNotEmpty() || selectedPriority != "All" || selectedValueMin > 0.0) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedPriority = "All"
                            selectedValueMin = 0.0
                        },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("kanban_clear_filters")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(14.dp), tint = PriorityHigh)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = PriorityHigh, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Filter Chips Rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority:",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
                
                val priorities = listOf("All", "High", "Medium", "Low")
                priorities.forEach { p ->
                    val isSelected = selectedPriority == p
                    val pColor = when (p) {
                        "High" -> PriorityHigh
                        "Medium" -> PriorityMedium
                        "Low" -> PriorityLow
                        else -> PrimaryDark
                    }
                    val bg = if (isSelected) pColor.copy(alpha = 0.2f) else Color.Transparent
                    val borderCol = if (isSelected) pColor else SurfaceVariantDark
                    val textCol = if (isSelected) pColor else Color.LightGray

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg)
                            .border(0.5.dp, borderCol, RoundedCornerShape(6.dp))
                            .clickable { selectedPriority = p }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("kanban_priority_$p")
                    ) {
                        Text(text = p, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textCol)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Divider(
                    color = SurfaceVariantDark,
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Min Value:",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )

                val valPresets = listOf(
                    0.0 to "All",
                    10000.0 to "> $10K",
                    50000.0 to "> $50K",
                    100000.0 to "> $100K"
                )
                valPresets.forEach { (v, label) ->
                    val isSelected = selectedValueMin == v
                    val bg = if (isSelected) PrimaryDark.copy(alpha = 0.2f) else Color.Transparent
                    val borderCol = if (isSelected) PrimaryDark else SurfaceVariantDark
                    val textCol = if (isSelected) PrimaryDark else Color.LightGray

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg)
                            .border(0.5.dp, borderCol, RoundedCornerShape(6.dp))
                            .clickable { selectedValueMin = v }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("kanban_value_$label")
                    ) {
                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textCol)
                    }
                }
            }
        }

        Divider(color = SurfaceVariantDark, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))

        // Kanban Board columns container (Horizontal Scroll)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            columns.forEach { stage ->
                val stageLeads = leads.filter { lead ->
                    val matchesStage = lead.pipelineStage == stage
                    val matchesSearch = searchQuery.isBlank() || 
                        lead.name.contains(searchQuery, ignoreCase = true) ||
                        lead.company.contains(searchQuery, ignoreCase = true) ||
                        lead.notes.contains(searchQuery, ignoreCase = true)
                    val matchesPriority = selectedPriority == "All" || lead.priority.equals(selectedPriority, ignoreCase = true)
                    val matchesValue = lead.dealValue >= selectedValueMin
                    matchesStage && matchesSearch && matchesPriority && matchesValue
                }
                KanbanColumn(
                    stage = stage,
                    leads = stageLeads,
                    onMoveStage = { lead, nextStage ->
                        viewModel.updateLeadStage(lead, nextStage)
                    },
                    onSelect = { leadId ->
                        viewModel.selectLead(leadId)
                    }
                )
            }
        }
    }
}

@Composable
fun KanbanColumn(
    stage: String,
    leads: List<Lead>,
    onMoveStage: (Lead, String) -> Unit,
    onSelect: (Int) -> Unit
) {
    val totalValue = leads.sumOf { it.dealValue }
    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    val stageColor = when (stage) {
        "New Lead" -> ColorNewLead
        "Contacted" -> ColorContacted
        "Proposal Sent" -> ColorProposalSent
        "In Negotiation" -> ColorInNegotiation
        "Won" -> ColorWon
        else -> ColorLost
    }

    Box(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariantDark.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(stageColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stage,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(stageColor.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = leads.size.toString(),
                        fontSize = 11.sp,
                        color = stageColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Total deal value info line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Column Value:", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = formatter.format(totalValue),
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = SurfaceVariantDark, thickness = 1.dp)

            if (leads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No prospects here\nDrag files or change state.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(leads) { lead ->
                        KanbanCard(
                            lead = lead,
                            onMoveStage = { onMoveStage(lead, it) },
                            onSelect = { onSelect(lead.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KanbanCard(
    lead: Lead,
    onMoveStage: (String) -> Unit,
    onSelect: () -> Unit
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    
    // Check if overdue follow-up
    val now = System.currentTimeMillis()
    val isOverdue = lead.nextFollowUpDate > 0 && lead.nextFollowUpDate <= now
    
    // Calculate Days Stagnant
    val diffMs = System.currentTimeMillis() - lead.stageChangedDate
    val daysStagnant = (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

    val borderStrokeColor = if (isOverdue) PriorityHigh.copy(alpha = 0.8f) else SurfaceVariantDark

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isOverdue) 1.5.dp else 0.5.dp,
                color = borderStrokeColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = lead.company,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Stage Quick Mover Picker Trigger
                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Move Lead",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        val stages = listOf("New Lead", "Contacted", "Proposal Sent", "In Negotiation", "Won", "Lost")
                        stages.filter { it != lead.pipelineStage }.forEach { targetStage ->
                            DropdownMenuItem(
                                text = { Text(targetStage, fontSize = 12.sp, color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onMoveStage(targetStage)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deal value
                Text(
                    text = formatter.format(lead.dealValue),
                    fontSize = 14.sp,
                    color = PrimaryDark,
                    fontWeight = FontWeight.Bold
                )

                // Stagnancy count
                Text(
                    text = "$daysStagnant days stagnant",
                    fontSize = 10.sp,
                    color = if (daysStagnant >= 7 && lead.pipelineStage != "Won" && lead.pipelineStage != "Lost") PriorityMedium else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pColor = when (lead.priority) {
                    "High" -> PriorityHigh
                    "Medium" -> PriorityMedium
                    else -> PriorityLow
                }
                Box(
                    modifier = Modifier
                        .background(pColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(lead.priority, fontSize = 9.sp, color = pColor, fontWeight = FontWeight.Bold)
                }

                if (lead.nextFollowUpDate > 0) {
                    val sdf = SimpleDateFormat("M/d", Locale.US)
                    val dateStr = sdf.format(Date(lead.nextFollowUpDate))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Follow Up Date",
                            tint = if (isOverdue) PriorityHigh else Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "F/U: $dateStr",
                            fontSize = 9.sp,
                            color = if (isOverdue) PriorityHigh else Color.Gray,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// Leads list view
@Composable
fun LeadListView(viewModel: CRMViewModel) {
    val leads by viewModel.filteredLeads.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectStageFilter by viewModel.filterStage.collectAsState()
    val selectPriorityFilter by viewModel.filterPriority.collectAsState()
    val selectedIds by viewModel.selectedLeadIds.collectAsState()

    var showStageFilterMenu by remember { mutableStateOf(false) }
    var showPriorityFilterMenu by remember { mutableStateOf(false) }
    var showBulkActionMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar & filtering controls
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by name, company, email...", color = Color.Gray) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search", tint = Color.LightGray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = PrimaryDark,
                unfocusedBorderColor = SurfaceVariantDark
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter badges row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stage filter badge
            Box {
                Button(
                    onClick = { showStageFilterMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectStageFilter != null) PrimaryDark else SurfaceDark
                    ),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = selectStageFilter ?: "Stage: All",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "", modifier = Modifier.size(12.dp))
                }

                DropdownMenu(
                    expanded = showStageFilterMenu,
                    onDismissRequest = { showStageFilterMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    DropdownMenuItem(text = { Text("All Stages", color = Color.White) }, onClick = {
                        viewModel.setStageFilter(null)
                        showStageFilterMenu = false
                    })
                    val stages = listOf("New Lead", "Contacted", "Proposal Sent", "In Negotiation", "Won", "Lost")
                    stages.forEach { stage ->
                        DropdownMenuItem(text = { Text(stage, color = Color.White) }, onClick = {
                            viewModel.setStageFilter(stage)
                            showStageFilterMenu = false
                        })
                    }
                }
            }

            // Priority filter badge
            Box {
                Button(
                    onClick = { showPriorityFilterMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectPriorityFilter != null) PrimaryDark else SurfaceDark
                    ),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = selectPriorityFilter ?: "Priority: All",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "", modifier = Modifier.size(12.dp))
                }

                DropdownMenu(
                    expanded = showPriorityFilterMenu,
                    onDismissRequest = { showPriorityFilterMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    DropdownMenuItem(text = { Text("All Priorities", color = Color.White) }, onClick = {
                        viewModel.setPriorityFilter(null)
                        showPriorityFilterMenu = false
                    })
                    val priorities = listOf("High", "Medium", "Low")
                    priorities.forEach { priority ->
                        DropdownMenuItem(text = { Text(priority, color = Color.White) }, onClick = {
                            viewModel.setPriorityFilter(priority)
                            showPriorityFilterMenu = false
                        })
                    }
                }
            }

            // Counter info
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${leads.size} matches",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bulk operations bar
        if (selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryDark.copy(alpha = 0.15f))
                    .border(1.dp, PrimaryDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${selectedIds.size} selected for bulk action",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    Box {
                        Button(
                            onClick = { showBulkActionMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Change Stage", fontSize = 10.sp)
                        }

                        DropdownMenu(
                            expanded = showBulkActionMenu,
                            onDismissRequest = { showBulkActionMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            val stages = listOf("New Lead", "Contacted", "Proposal Sent", "In Negotiation", "Won", "Lost")
                            stages.forEach { stage ->
                                DropdownMenuItem(text = { Text(stage, color = Color.White) }, onClick = {
                                    viewModel.bulkChangeSelectedStage(stage)
                                    showBulkActionMenu = false
                                })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.bulkDeleteSelected() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Bulk Delete", tint = PriorityHigh)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.clearSelections() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Selections", tint = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Table List
        if (leads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.List, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No match found", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(leads) { lead ->
                    val isChecked = selectedIds.contains(lead.id)
                    LeadRowItem(
                        lead = lead,
                        isSelected = isChecked,
                        onCheckboxChange = { viewModel.toggleSelectLead(lead.id) },
                        onCellClick = { viewModel.selectLead(lead.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun LeadRowItem(
    lead: Lead,
    isSelected: Boolean,
    onCheckboxChange: () -> Unit,
    onCellClick: () -> Unit
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .clickable { onCellClick() }
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onCheckboxChange() },
                colors = CheckboxDefaults.colors(checkedColor = PrimaryDark, uncheckedColor = Color.Gray)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lead.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = lead.company,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.End) {
                val stageColor = when (lead.pipelineStage) {
                    "New Lead" -> ColorNewLead
                    "Contacted" -> ColorContacted
                    "Proposal Sent" -> ColorProposalSent
                    "In Negotiation" -> ColorInNegotiation
                    "Won" -> ColorWon
                    else -> ColorLost
                }
                Box(
                    modifier = Modifier
                        .background(stageColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = lead.pipelineStage,
                        fontSize = 10.sp,
                        color = stageColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatter.format(lead.dealValue),
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Detail Drawer Panel Overlay Sheet (Direct Slide Out Drawer Content)
@Composable
fun DetailDrawerContent(
    lead: Lead,
    interactions: List<Interaction>,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddNote: (String) -> Unit,
    onEmailClick: (Int) -> Unit,
    onUpdateStage: (String) -> Unit,
    onTriggerFollowUp: () -> Unit
) {
    val formatterCur = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    
    val dateAddedStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(lead.addedDate))
    val lastInterStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(lead.lastInteractionDate))
    
    var manualNoteText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Surface(
        color = SurfaceDark,
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth(0.92f)
            .fillMaxHeight()
            .border(1.dp, SurfaceVariantDark.copy(alpha = 0.8f), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .testTag("detail_drawer_surface")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${lead.company} • Added $dateAddedStr",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("detail_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Lead",
                            tint = PrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Lead",
                            tint = PriorityHigh,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("detail_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Divider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 12.dp))

            // Scrollable Content area to prevent any overflow on smaller devices
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // High contrast priority badge and health line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val priorityColor = when (lead.priority) {
                        "High" -> PriorityHigh
                        "Medium" -> PriorityMedium
                        else -> PriorityLow
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .border(0.5.dp, priorityColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${lead.priority} Priority",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                    }

                    // Quality Badge
                    val healthLabel = when {
                        lead.confidence >= 70 -> "High Quality Lead"
                        lead.confidence <= 30 -> "At-Risk Lead"
                        else -> "Standard Lead"
                    }
                    val healthColor = when {
                        lead.confidence >= 70 -> ColorWon
                        lead.confidence <= 30 -> ColorLost
                        else -> ColorProposalSent
                    }

                    Text(
                        text = healthLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Pipeline Stage Tracker Bar
                Text(
                    text = "🔄 Pipeline Deal Stage Tracker (Click to transition)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundDark.copy(alpha = 0.5f))
                        .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stages = listOf("New Lead", "Contacted", "Proposal Sent", "In Negotiation", "Won", "Lost")
                    stages.forEach { stage ->
                        val isCurrent = lead.pipelineStage == stage
                        val stageColor = when (stage) {
                            "New Lead" -> ColorNewLead
                            "Contacted" -> ColorContacted
                            "Proposal Sent" -> ColorProposalSent
                            "In Negotiation" -> ColorInNegotiation
                            "Won" -> ColorWon
                            "Lost" -> ColorLost
                            else -> Color.Gray
                        }

                        val bg = if (isCurrent) stageColor.copy(alpha = 0.2f) else Color.Transparent
                        val borderCol = if (isCurrent) stageColor else Color.Transparent
                        val textCol = if (isCurrent) stageColor else Color.LightGray.copy(alpha = 0.6f)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg)
                                .border(0.5.dp, borderCol, RoundedCornerShape(6.dp))
                                .clickable { onUpdateStage(stage) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active Stage",
                                    tint = stageColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = stage,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Grid layout for contacting & metric info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Contact Info card
                    DetailSectionCard(
                        title = "Contact Channels",
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ContactLine(icon = Icons.Default.Email, label = lead.email, onClick = { onEmailClick(lead.id) })
                            ContactLine(icon = Icons.Default.Phone, label = lead.phone)
                            ContactLine(icon = Icons.Default.Share, label = "Pref: ${lead.channelPreference}")
                        }
                    }

                    // Deal Value Metrics card
                    DetailSectionCard(
                        title = "Deal Valuation Metrics",
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetricLine(label = "Deal Value", value = formatterCur.format(lead.dealValue), valueColor = PrimaryDark)
                            MetricLine(label = "Confidence", value = "${lead.confidence}%", valueColor = if (lead.confidence >= 60) ColorWon else if (lead.confidence <= 30) ColorLost else ColorProposalSent)
                            MetricLine(label = "Last Active", value = lastInterStr)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sales quality visual meter indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = BackgroundDark.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, SurfaceVariantDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📈 Deal Probability Quality Index",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${lead.confidence}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryDark
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Quality track line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(SurfaceVariantDark)
                        ) {
                            val meterColor = if (lead.confidence >= 70) ColorWon else if (lead.confidence <= 30) ColorLost else ColorProposalSent
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(lead.confidence / 100f)
                                    .fillMaxHeight()
                                    .background(meterColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Outbound Context Action Banner (Trigger follow up modal instantly)
                Button(
                    onClick = onTriggerFollowUp,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("drawer_follow_up_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Trigger Followup",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Schedule/Run Follow-Up Playbook",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes input & description
                DetailSectionCard(title = "Primary Description / General Notes") {
                    Column {
                        Text(
                            text = lead.notes.ifEmpty { "No core description provided. Click edit to add notes." },
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom immediate interaction logger
                        Row {
                            OutlinedTextField(
                                value = manualNoteText,
                                onValueChange = { manualNoteText = it },
                                placeholder = { Text("Log instant interaction note...", fontSize = 11.sp, color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = BackgroundDark,
                                    unfocusedContainerColor = BackgroundDark,
                                    focusedBorderColor = PrimaryDark,
                                    unfocusedBorderColor = SurfaceVariantDark
                                ),
                                textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("instant_note_input")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (manualNoteText.isNotBlank()) {
                                        onAddNote(manualNoteText)
                                        manualNoteText = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("instant_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Log Note",
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chronological interaction timeline history
                Text(
                    text = "⏱ Chronological Interaction Timeline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (interactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No timeline history present", color = Color.Gray, fontSize = 11.sp, fontStyle = FontStyle.Italic)
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        interactions.forEach { inter ->
                            TimelineCard(interaction = inter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            content()
        }
    }
}

@Composable
fun ContactLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Icon(imageVector = icon, contentDescription = "", tint = PrimaryDark, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label.ifEmpty { "Not set" },
            fontSize = 12.sp,
            color = if (onClick != null) PrimaryDark else Color.LightGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (onClick != null) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun MetricLine(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimelineCard(interaction: Interaction) {
    val formatter = SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.US)
    val dateStr = formatter.format(Date(interaction.timestamp))
    
    val badgeColor = when (interaction.type) {
        "Created" -> ColorContacted
        "Stage Change" -> ColorProposalSent
        "Follow Up Executed" -> ColorWon
        else -> ColorNewLead
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = interaction.type,
                        fontSize = 9.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(text = dateStr, fontSize = 10.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = interaction.description, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            
            if (interaction.detail.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = interaction.detail,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Global Command Palette Overlay Dialog (Triggered with Cmd+K UI button)
@Composable
fun CommandPaletteDialog(
    viewModel: CRMViewModel,
    onClose: () -> Unit,
    onSelectLead: (Int) -> Unit,
    onTriggerFollowUp: (Int) -> Unit
) {
    var searchVal by remember { mutableStateOf("") }
    val allLeads by viewModel.allLeads.collectAsState()
    
    val matches = if (searchVal.isEmpty()) {
        allLeads.take(4) // show recent directly
    } else {
        allLeads.filter { lead ->
            lead.name.contains(searchVal, ignoreCase = true) ||
                    lead.company.contains(searchVal, ignoreCase = true)
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = SurfaceDark,
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, PrimaryDark, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡️ AURA Commands Center",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PrimaryDark
                    )
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchVal,
                    onValueChange = { searchVal = it },
                    placeholder = { Text("Search matches ... try 'Alice'", color = Color.Gray, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Commands / Results", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                if (matches.isEmpty()) {
                    Text(
                        text = "No prospects found matching query",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        matches.forEach { lead ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceVariantDark.copy(alpha = 0.5f))
                                    .clickable { onSelectLead(lead.id) }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = lead.name, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(text = "${lead.company} • ${lead.pipelineStage}", fontSize = 11.sp, color = Color.Gray)
                                }

                                Row {
                                    IconButton(
                                        onClick = { onTriggerFollowUp(lead.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = "Follow Up",
                                            tint = PrimaryDark,
                                            modifier = Modifier.size(16.dp)
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

// Dialog for follow up sequence (Email / Call / SMS pre-filled action templates)
@Composable
fun FollowUpModalDialog(
    viewModel: CRMViewModel,
    lead: Lead,
    onClose: () -> Unit
) {
    val templateId by viewModel.activeTemplateId.collectAsState()
    val messageText by viewModel.generatedMessageText.collectAsState()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Intelligent Follow Up: ${lead.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Generate and trigger professional client messages",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Template selector tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple(1, "Warm Intro", "Warm Check-In"),
                        Triple(2, "Nudge", "Proposal Nudge"),
                        Triple(3, "Breakup", "Breakup")
                    ).forEach { (id, short, label) ->
                        val isSelected = id == templateId
                        Button(
                            onClick = { viewModel.setTemplateId(id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PrimaryDark else SurfaceVariantDark
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text(short, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Editable text field showing generated messaging draft
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { viewModel.updateGeneratedMessage(it) },
                    placeholder = { Text("Drafting Message ...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White, lineHeight = 18.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // One-click action triggers using device Intents
                Text("💥 Interactive Protocol Launchers", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Copy
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("CRM Followup Message", messageText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied template to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }

                    // Email Intent trigger - Opens in-app email Composer
                    Button(
                        onClick = {
                            viewModel.openEmailComposer(lead.id)
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Email", fontSize = 11.sp)
                    }

                    // Dial Dialer Intent trigger
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${lead.phone}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No dialer application found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary CTA finalize follow-up logging
                Button(
                    onClick = {
                        viewModel.executeFollowUpAction(lead)
                        Toast.makeText(context, "Follow up logged to lead history!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✅ Permanently Log Follow-Up Executed", fontSize = 13.sp)
                }
            }
        }
    }
}

// Add or edit lead dialog drawer panel
@Composable
fun AddEditLeadDialog(
    viewModel: CRMViewModel,
    onClose: () -> Unit
) {
    val editingLeadId by viewModel.editingLeadId.collectAsState()
    
    val name by viewModel.formName.collectAsState()
    val company by viewModel.formCompany.collectAsState()
    val email by viewModel.formEmail.collectAsState()
    val phone by viewModel.formPhone.collectAsState()
    val dealVal by viewModel.formDealValue.collectAsState()
    val confidence by viewModel.formConfidence.collectAsState()
    val stage by viewModel.formStage.collectAsState()
    val priority by viewModel.formPriority.collectAsState()
    val notes by viewModel.formNotes.collectAsState()
    val nextDate by viewModel.formNextFollowUp.collectAsState()
    val channelPref by viewModel.formChannelPref.collectAsState()

    // Error states
    val errName by viewModel.errorName.collectAsState()
    val errEmail by viewModel.errorEmail.collectAsState()
    val errVal by viewModel.errorDealValue.collectAsState()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = BackgroundDark,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingLeadId == null) "Add Lead Prospects" else "Edit Prospect Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Divider(color = SurfaceVariantDark, modifier = Modifier.padding(vertical = 10.dp))

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.formName.value = it },
                    label = { Text("Client/Full Name (Required)", color = Color.Gray) },
                    isError = errName != null,
                    supportingText = { if (errName != null) Text(errName!!, color = PriorityHigh) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Company input
                OutlinedTextField(
                    value = company,
                    onValueChange = { viewModel.formCompany.value = it },
                    label = { Text("Company Name", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.formEmail.value = it },
                    label = { Text("Primary Email Address", color = Color.Gray) },
                    isError = errEmail != null,
                    supportingText = { if (errEmail != null) Text(errEmail!!, color = PriorityHigh) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.formPhone.value = it },
                    label = { Text("Mobile/Phone Number", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Value and confidence sliders row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dealVal,
                        onValueChange = { viewModel.formDealValue.value = it },
                        label = { Text("Deal Value ($)", color = Color.Gray) },
                        isError = errVal != null,
                        supportingText = { if (errVal != null) Text(errVal!!, color = PriorityHigh) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = PrimaryDark,
                            unfocusedBorderColor = SurfaceVariantDark
                        ),
                        textStyle = TextStyle(color = Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Confidence: $confidence%", fontSize = 11.sp, color = Color.Gray)
                        Slider(
                            value = confidence.toFloatOrNull() ?: 50f,
                            onValueChange = { viewModel.formConfidence.value = it.toInt().toString() },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryDark, activeTrackColor = PrimaryDark)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select stage & priority dropdown togglers
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    var expStage by remember { mutableStateOf(false) }
                    var expPriority by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expStage = true },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 0.5.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark, contentColor = Color.White)
                        ) {
                            Text("Stage: $stage", fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = expStage,
                            onDismissRequest = { expStage = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            val stages = listOf("New Lead", "Contacted", "Proposal Sent", "In Negotiation", "Won", "Lost")
                            stages.forEach { s ->
                                DropdownMenuItem(text = { Text(s, color = Color.White) }, onClick = {
                                    viewModel.formStage.value = s
                                    expStage = false
                                })
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expPriority = true },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 0.5.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark, contentColor = Color.White)
                        ) {
                            Text("Priority: $priority", fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = expPriority,
                            onDismissRequest = { expPriority = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            val priorities = listOf("High", "Medium", "Low")
                            priorities.forEach { p ->
                                DropdownMenuItem(text = { Text(p, color = Color.White) }, onClick = {
                                    viewModel.formPriority.value = p
                                    expPriority = false
                                })
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Next follow up scheduler & channel preference
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    var expChannel by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.weight(1.0f)) {
                        OutlinedButton(
                            onClick = { expChannel = true },
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 0.5.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceDark, contentColor = Color.White)
                        ) {
                            Text("Channel Pref: $channelPref", fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = expChannel,
                            onDismissRequest = { expChannel = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            listOf("Email", "Phone", "SMS", "LinkedIn").forEach { ch ->
                                DropdownMenuItem(text = { Text(ch, color = Color.White) }, onClick = {
                                    viewModel.formChannelPref.value = ch
                                    expChannel = false
                                })
                            }
                        }
                    }

                    // Schedule follow up action date selector
                    Column(modifier = Modifier.weight(1.0f)) {
                        var isScheduled by remember { mutableStateOf(nextDate > 0) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isScheduled,
                                onCheckedChange = { checked ->
                                    isScheduled = checked
                                    viewModel.formNextFollowUp.value = if (checked) {
                                        // Set to today plus 1 day as default
                                        System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                                    } else 0L
                                },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryDark)
                            )
                            Text("Schedule Outreach?", fontSize = 10.sp, color = Color.White)
                        }

                        // Date Preview
                        if (isScheduled && nextDate > 0) {
                            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
                            Text(
                                "Target: ${sdf.format(Date(nextDate))}",
                                fontSize = 10.sp,
                                color = PrimaryDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    // Simulated simple toggling of follow up calendar offsets: e.g. delay by 1 more day or select overdue
                                    viewModel.formNextFollowUp.value = nextDate + 24 * 60 * 60 * 1000L
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes input field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.formNotes.value = it },
                    label = { Text("Primary Prospect Notes & Context", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (viewModel.saveLead()) {
                                // successfully saved
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}

// SettingsView to Import/Export JSON database
@Composable
fun SettingsView(viewModel: CRMViewModel) {
    val context = LocalContext.current
    val settingsMessage by viewModel.settingsMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val optInNotifications by viewModel.optInNotifications.collectAsState()
    var rawTextImport by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "System Settings & Portability",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Back up and import client and pipeline database files locally",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Account Profile & Session Control
        currentUser?.let { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "👤 Active Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = user.email,
                                fontSize = 13.sp,
                                color = PrimaryDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = { viewModel.handleLogout() },
                            colors = ButtonDefaults.buttonColors(containerColor = PriorityHigh),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Log Out", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    if (user.businessName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = SurfaceVariantDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Business Name:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = user.businessName,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Notification preferences
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔔 Push Alerts & Reminders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Get real-time push alerts for newly assigned lead deals and critical overdue outreach events.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Switch(
                        checked = optInNotifications,
                        onCheckedChange = { isEnabled ->
                            viewModel.toggleNotificationOptIn(isEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryDark,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = SurfaceVariantDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            WebhooksAndIntegrationsCard(viewModel = viewModel)
        }

        // Snack/Message prompt
        if (settingsMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryDark.copy(alpha = 0.2f))
                    .border(1.dp, PrimaryDark, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .padding(bottom = 14.dp)
            ) {
                Text(
                    text = settingsMessage!!,
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Export card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📥 Backup Lead Records (Export)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Extracts all client contacts, details, evaluations, and chronological interactions logs. Formats database into portability standard JSON.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.exportData { json ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("CRM JSON BACKUP", json)
                            clipboard.setPrimaryClip(clip)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export & Copy JSON Database to Clipboard")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Import card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📤 Restore Lead Records (Import)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Paste standard exported Aura CRM database JSON payload in text area box below to restore pipeline items instantly.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rawTextImport,
                    onValueChange = { rawTextImport = it },
                    placeholder = { Text("Paste database JSON array object here ...", color = Color.Gray, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(fontSize = 11.sp, color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.importData(rawTextImport) { success ->
                            if (success) {
                                rawTextImport = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Import")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Validate & Load JSON Dataset into CRM")
                }
            }
        }
    }
}

@Composable
fun AuthenticationScreen(viewModel: CRMViewModel) {
    val email by viewModel.authEmail.collectAsState()
    val password by viewModel.authPassword.collectAsState()
    val businessName by viewModel.authBusinessName.collectAsState()
    val isSignUp by viewModel.isSignUpMode.collectAsState()
    val error by viewModel.authError.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Visual Icon and logo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(PrimaryDark, SecondaryDark))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AURA SALES CRM",
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 24.sp,
                color = PrimaryDark,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isSignUp) "Setup your contractor/business pipeline" else "Access your business deals instantly",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Auth Dialog Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isSignUp) "Register Business Account" else "Sign In Securely",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    // Error Feedbacks
                    error?.let { errMsg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ColorLost.copy(alpha = 0.15f))
                                .border(1.dp, ColorLost, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "⚠️ $errMsg",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Email field
                    Column {
                        Text("Email Address:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { viewModel.authEmail.value = it },
                            placeholder = { Text("you@example.com", color = Color.Gray, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceVariantDark,
                                unfocusedContainerColor = SurfaceVariantDark,
                                focusedBorderColor = PrimaryDark,
                                unfocusedBorderColor = SurfaceVariantDark
                            ),
                            textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Email, contentDescription = "", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    // Password field
                    Column {
                        Text("Password:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { viewModel.authPassword.value = it },
                            placeholder = { Text("••••••", color = Color.Gray, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceVariantDark,
                                unfocusedContainerColor = SurfaceVariantDark,
                                focusedBorderColor = PrimaryDark,
                                unfocusedBorderColor = SurfaceVariantDark
                            ),
                            textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input"),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.CheckCircle else Icons.Default.Face,
                                        contentDescription = "Toggle Visibility",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }

                    // Business profile field (shown only in register mode)
                    if (isSignUp) {
                        Column {
                            Text("Business/Contractor Name:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { viewModel.authBusinessName.value = it },
                                placeholder = { Text("e.g. Apex Marketing Group", color = Color.Gray, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceVariantDark,
                                    unfocusedContainerColor = SurfaceVariantDark,
                                    focusedBorderColor = PrimaryDark,
                                    unfocusedBorderColor = SurfaceVariantDark
                                ),
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_business_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Build, contentDescription = "", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary submit button
                    Button(
                        onClick = { viewModel.handleAuthentication() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isSignUp) "Create My Workspace" else "Sign In Securely",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text toggle button
            TextButton(
                onClick = { viewModel.toggleAuthMode() },
                modifier = Modifier.testTag("auth_toggle_mode")
            ) {
                Text(
                    text = if (isSignUp) "Already have a company pipeline? Log In" else "New contractor? Register your business",
                    color = SecondaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmailComposerDialog(
    viewModel: CRMViewModel,
    lead: Lead,
    onClose: () -> Unit
) {
    val to by viewModel.emailToField.collectAsState()
    val subject by viewModel.emailSubjectField.collectAsState()
    val body by viewModel.emailBodyField.collectAsState()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📨 Send Email Outreach",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Composing a direct follow-up message for ${lead.company}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recipient field
                Text("To:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = to,
                    onValueChange = { viewModel.emailToField.value = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("email_to_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subject field
                Text("Subject:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { viewModel.emailSubjectField.value = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("email_sub_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Body field
                Text("Message Body:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { viewModel.emailBodyField.value = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariantDark,
                        unfocusedContainerColor = SurfaceVariantDark,
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    textStyle = TextStyle(fontSize = 12.sp, color = Color.White, lineHeight = 18.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("email_body_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (to.isBlank()) {
                                Toast.makeText(context, "Recipient email cannot be empty", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.sendEmail { recipient, sub, text ->
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                                        putExtra(Intent.EXTRA_SUBJECT, sub)
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Opening mail client...", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open mail app.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        modifier = Modifier.weight(1.5f).testTag("email_send_and_log_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send & Log Action", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WebhooksAndIntegrationsCard(viewModel: CRMViewModel) {
    val context = LocalContext.current
    val apiKey by viewModel.apiKey.collectAsState()
    
    var selectedPreset by remember { mutableStateOf("twilio_missed_call") }
    
    val twilioPreset = """{
  "event": "missed_call",
  "from": "+15553289012",
  "caller_name": "Alexander Graham",
  "transcription": "Missed Call: I need a rapid price quotation on a roof restoration project. Call my cell.",
  "duration_seconds": 32
}"""

    val contactPreset = """{
  "event": "contact_form",
  "name": "Evelyn Sterling",
  "email": "evelyn@sterlingdesigns.co",
  "phone": "+15558913344",
  "company": "Sterling Designs",
  "message": "Website Contact Form: Asking about custom patio extensions. Follow up urgently."
}"""

    val fbPreset = """{
  "event": "facebook_lead",
  "name": "Devin O'Connor",
  "email": "devin.oc@outlook.com",
  "phone": "+15557721199",
  "company": "O'Connor Solar",
  "deal_value": 38000.0,
  "notes": "Facebook Leads Ad: Inquiry for standard residential battery backups quotation."
}"""

    // Simulated payload state
    var payloadText by remember { mutableStateOf(twilioPreset) }
    var apiTokenInput by remember { mutableStateOf(apiKey) }
    
    // Sync active api key automatically if it changes/regenerates
    LaunchedEffect(apiKey) {
        apiTokenInput = apiKey
    }
    
    var consoleOutput by remember { mutableStateOf("🟢 Sandbox Simulator Listening - Ready for dispatch trigger...") }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share, 
                    contentDescription = "Integrations", 
                    tint = PrimaryDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🔌 Webhooks & Integrations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Automate client intake! Map Twilio missed calls, contact forms, or third-party web apps (such as Zapier / Make.com) to route leads into your CRM pipeline.",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = SurfaceVariantDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // API Section
            Text(
                text = "🔑 Your Unique CRM Access Token",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .border(0.5.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = apiKey.ifBlank { "Token Not Generated" },
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = PrimaryDark,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("CRM API KEY", apiKey)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "API Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Copy Token", 
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = { viewModel.regenerateApiKey() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh, 
                            contentDescription = "Regenerate Token", 
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Integration endpoint details
            Text(
                text = "📁 Active Webhook Endpoints",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Twilio Card details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
                    .padding(10.dp)
            ) {
                Text(
                    text = "Twilio Missed Calls & Voicemail mapping:",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "https://api.auracrm.live/v1/incoming/twilio?apiKey=$apiKey",
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = SecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("TWILIO ENDPOINT", "https://api.auracrm.live/v1/incoming/twilio?apiKey=$apiKey")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Twilio Endpoint copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Copy Twilio Endpoint", 
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Zapier / Make.com Webhook Catch hook:",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "https://api.auracrm.live/v1/incoming/zapier?apiKey=$apiKey",
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = SecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("ZAPIER ENDPOINT", "https://api.auracrm.live/v1/incoming/zapier?apiKey=$apiKey")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Zapier Endpoint copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Copy Zapier Endpoint", 
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = SurfaceVariantDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Simulator Section header
            Text(
                text = "⚡ Real-time Webhook Receiver Sandbox",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = PrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Test incoming automated payloads. Pick a workflow template below, then simulate a real-time incoming webhook forward action.",
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Presets selector chips Row
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preset 1: Twilio Missed Call
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPreset == "twilio_missed_call") PrimaryDark.copy(alpha = 0.15f) else SurfaceVariantDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable {
                            selectedPreset = "twilio_missed_call"
                            payloadText = twilioPreset
                        }
                        .border(
                            1.dp,
                            if (selectedPreset == "twilio_missed_call") PrimaryDark else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Text(
                        text = "📞 Twilio Missed Call",
                        fontSize = 11.sp,
                        color = if (selectedPreset == "twilio_missed_call") Color.White else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Preset 2: Zapier Website Form
                Card(
                    colors = CardColors(
                        containerColor = if (selectedPreset == "zapier_contact_form") PrimaryDark.copy(alpha = 0.15f) else SurfaceVariantDark,
                        contentColor = Color.White,
                        disabledContainerColor = SurfaceVariantDark,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable {
                            selectedPreset = "zapier_contact_form"
                            payloadText = contactPreset
                        }
                        .border(
                            1.dp,
                            if (selectedPreset == "zapier_contact_form") PrimaryDark else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Text(
                        text = "📬 Website Form Intake",
                        fontSize = 11.sp,
                        color = if (selectedPreset == "zapier_contact_form") Color.White else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Preset 3: Zapier FB Lead Ads
                Card(
                    colors = CardColors(
                        containerColor = if (selectedPreset == "zapier_facebook_lead") PrimaryDark.copy(alpha = 0.15f) else SurfaceVariantDark,
                        contentColor = Color.White,
                        disabledContainerColor = SurfaceVariantDark,
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable {
                            selectedPreset = "zapier_facebook_lead"
                            payloadText = fbPreset
                        }
                        .border(
                            1.dp,
                            if (selectedPreset == "zapier_facebook_lead") PrimaryDark else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Text(
                        text = "⚡ FB Ads Social Lead",
                        fontSize = 11.sp,
                        color = if (selectedPreset == "zapier_facebook_lead") Color.White else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Payload text area
            Text("Webhook JSON Body Payload:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = payloadText,
                onValueChange = { payloadText = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BackgroundDark,
                    unfocusedContainerColor = BackgroundDark,
                    focusedBorderColor = PrimaryDark,
                    unfocusedBorderColor = SurfaceVariantDark
                ),
                textStyle = TextStyle(
                    fontSize = 11.sp, 
                    color = Color.White, 
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    lineHeight = 15.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("webhook_simulator_payload_field"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Simulated Header Key Field
            Text("Request Header: X-CRM-API-Key", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = apiTokenInput,
                onValueChange = { apiTokenInput = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BackgroundDark,
                    unfocusedContainerColor = BackgroundDark,
                    focusedBorderColor = PrimaryDark,
                    unfocusedBorderColor = SurfaceVariantDark
                ),
                textStyle = TextStyle(
                    fontSize = 11.sp, 
                    color = Color.White, 
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                placeholder = { Text("Paste active API key to authorize...", color = Color.Gray, fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("webhook_simulator_token_field"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dispatch Button
            Button(
                onClick = {
                    consoleOutput = "🟡 Processing webhook ingestion request..."
                    val outcome = viewModel.triggerSimulatedWebhook(
                        userKey = apiTokenInput,
                        payloadJson = payloadText
                    )
                    consoleOutput = outcome
                },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("webhook_simulate_dispatch_btn"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simulate Inbound Webhook Event", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sandbox Console Screen
            Text("Sandbox Output Log Console:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = consoleOutput,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (consoleOutput.contains("❌")) ColorLost else if (consoleOutput.contains("✅")) ColorWon else Color.LightGray,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = SurfaceVariantDark, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Expandable Documentation Section
            var isDocExpanded by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDocExpanded = !isDocExpanded }
                    .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Documentation",
                                tint = PrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "📖 Zapier Integration Guide & Schema",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = if (isDocExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Documentation",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isDocExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = SurfaceVariantDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Zapier instructions step by step
                        Text(
                            text = "⚡ Zapier Setup Instructions:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val bulletPoints = listOf(
                            "1. Create a premium Zap and select 'Webhooks by Zapier' as the Trigger App, selecting 'Catch Hook' as the Event.",
                            "2. In the Set-up Trigger section, copy your unique API endpoint from above (e.g., /v1/incoming/zapier?apiKey=YOUR_TOKEN).",
                            "3. Set up the Action step in your Zap (e.g., Twilio Missed Calls, Facebook Lead Ads, Gmail, Typeform).",
                            "4. Add standard Request Headers to pass authorization safely:\n   - Set Custom Header Key: X-CRM-API-Key\n   - Set Header Value: (your Access Token from above)",
                            "5. Map the fields from your trigger app to the matching JSON body schema below. Once configured, leads will route in real-time."
                        )

                        bulletPoints.forEach { point ->
                            Text(
                                text = point,
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = SurfaceVariantDark, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Schema header and copy button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 Required JSON Schema Mapping",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            
                            val schemaJson = """{
  "event": "contact_form",
  "name": "Jane Doe",
  "email": "jane@example.com",
  "phone": "+15551234567",
  "company": "Acme Corporation",
  "notes": "Interested in premium tier custom contractor automation software.",
  "deal_value": 25000.0
}"""
                            
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("CRM Webhook JSON Schema", schemaJson)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Schema JSON copied!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = "", modifier = Modifier.size(10.dp), tint = SecondaryDark)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy JSON", fontSize = 10.sp, color = SecondaryDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Scrollable/styled code block container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(0.5.dp, SurfaceVariantDark, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = """{
  "event": "contact_form",
  "name": "Jane Doe",
  "email": "jane@example.com",
  "phone": "+15551234567",
  "company": "Acme Corporation",
  "notes": "Query details or voicemail logs here.",
  "deal_value": 25000.0
}""",
                                fontSize = 10.5.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = ColorWon,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Payload Fields Legend Table
                        Text(
                            text = "Schema Parameters Field Legend:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val fieldsList = listOf(
                            "event: 'missed_call', 'contact_form', 'facebook_lead' (string)" to "Determines automatic categorization, badge graphics & custom workflow templates in CRM.",
                            "name / caller_name: (Required string)" to "The incoming client's full contact name. Creates the primary pipeline card.",
                            "phone / from: (Required string)" to "The client's telephone contact. Essential for direct outreach call-back logic.",
                            "email: (Optional string)" to "Target email address for logging automatic follow-up and tracking.",
                            "company: (Optional string)" to "Associated contractor, brand, or lead organization.",
                            "notes / transcription: (Optional string)" to "Detail logs, voicemail transcription summaries, or website contact details.",
                            "deal_value: (Optional decimal number)" to "Estimated sales deal value. Defaults automatically to 25000.0 if omitted."
                        )

                        fieldsList.forEach { (field, desc) ->
                            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                Text(
                                    text = "• $field",
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = desc,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    lineHeight = 13.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
