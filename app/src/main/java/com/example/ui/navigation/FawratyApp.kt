package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoiceDetailScreen
import com.example.ui.screens.InvoiceEditorScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.IcyBackground
import com.example.ui.theme.IosGlassBlue
import com.example.ui.theme.IosShadowColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.FawratyViewModel
import com.example.viewmodel.NavigationTab
import com.example.viewmodel.SubScreen

@Composable
fun FawratyApp(
    viewModel: FawratyViewModel = viewModel()
) {
    val currentTab by viewModel.selectedTab.collectAsState()
    val activeSubScreen by viewModel.activeSubScreen.collectAsState()
    val selectedInvoice by viewModel.selectedInvoice.collectAsState()

    // Handle back button
    BackHandler(enabled = activeSubScreen != SubScreen.NONE || currentTab != NavigationTab.DASHBOARD) {
        viewModel.navigateBack()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = IcyBackground,
            bottomBar = {
                if (activeSubScreen == SubScreen.NONE) {
                    IosFrostedBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.setTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF2F6FA),
                                Color(0xFFE8F0F7)
                            )
                        )
                    )
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = Pair(activeSubScreen, currentTab),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { (subScreen, tab) ->
                    when (subScreen) {
                        SubScreen.CREATE_INVOICE -> {
                            InvoiceEditorScreen(viewModel = viewModel)
                        }
                        SubScreen.INVOICE_DETAIL -> {
                            selectedInvoice?.let { inv ->
                                InvoiceDetailScreen(invoice = inv, viewModel = viewModel)
                            } ?: DashboardScreen(viewModel = viewModel)
                        }
                        SubScreen.NONE -> {
                            when (tab) {
                                NavigationTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                                NavigationTab.INVOICES -> DashboardScreen(viewModel = viewModel)
                                NavigationTab.CLIENTS -> ClientsScreen(viewModel = viewModel)
                                NavigationTab.CATALOG -> CatalogScreen(viewModel = viewModel)
                                NavigationTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                                NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * iOS Style Frosted Glass Floating Dock Bar
 */
@Composable
fun IosFrostedBottomNavigation(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavigationItem(NavigationTab.DASHBOARD, "الرئيسية", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavigationItem(NavigationTab.CLIENTS, "العملاء", Icons.Filled.Group, Icons.Outlined.Group),
        NavigationItem(NavigationTab.CATALOG, "الكتالوج", Icons.Outlined.Architecture, Icons.Outlined.Architecture),
        NavigationItem(NavigationTab.REPORTS, "التقارير", Icons.Filled.Assessment, Icons.Outlined.Assessment),
        NavigationItem(NavigationTab.SETTINGS, "المنشأة", Icons.Filled.Business, Icons.Outlined.Business)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = IosShadowColor,
                    spotColor = IosShadowColor
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xEEFFFFFF))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0x35007AFF),
                            Color(0x20007AFF)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(vertical = 6.dp, horizontal = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentTab == item.tab
                    val icon = if (isSelected) item.selectedIcon else item.unselectedIcon
                    val tintColor = if (isSelected) IosGlassBlue else TextSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0x18007AFF) else Color.Transparent)
                            .clickable { onTabSelected(item.tab) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("nav_${item.tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = item.label,
                                tint = tintColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                color = tintColor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NavigationItem(
    val tab: NavigationTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
