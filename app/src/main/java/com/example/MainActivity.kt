package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LanguagePreferences
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.stringsForLanguage
import com.example.data.LocalGuestTransactionRepository
import com.example.data.TransactionRecord
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.TransactionFormSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.state.HisabViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.CardStroke
import com.example.ui.theme.DailyHisabTheme
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class AppTab(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", Icons.Rounded.Home, Icons.Outlined.Home),
    REPORTS("reports", Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    HISTORY("history", Icons.Rounded.History, Icons.Outlined.History)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyHisabRoot()
        }
    }
}

@Composable
fun DailyHisabRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val languagePrefs = remember(context) { LanguagePreferences(context) }
    val language by languagePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)
    val isGuestMode by languagePrefs.isGuestModeFlow.collectAsStateWithLifecycle(initialValue = false)
    val strings = remember(language) { stringsForLanguage(language) }

    val guestRepository = remember(context) { LocalGuestTransactionRepository(context) }
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    CompositionLocalProvider(LocalAppStrings provides strings) {
        DailyHisabTheme(useBengaliFont = language == AppLanguage.BN) {
            val user = currentUser
            when {
                user != null -> {
                    var migrationToastMessage by remember(user.uid) { mutableStateOf<String?>(null) }
                    LaunchedEffect(user.uid) {
                        if (isGuestMode) {
                            languagePrefs.setGuestMode(false)
                        }
                        val pendingGuestRecords = guestRepository.getSnapshot()
                        if (pendingGuestRecords.isNotEmpty()) {
                            val db = FirebaseFirestore.getInstance(
                                context.applicationContext.getString(R.string.firestore_database_id)
                            )
                            val cloudRepo = TransactionRepository(db)
                            var allMigrated = true
                            for (record in pendingGuestRecords) {
                                val res = cloudRepo.addTransaction(
                                    userId = user.uid,
                                    type = record.parsedType,
                                    amount = record.amount,
                                    category = record.parsedCategory,
                                    description = record.description,
                                    transactionDateMillis = record.transactionDateMillis
                                )
                                if (res.isFailure) {
                                    allMigrated = false
                                }
                            }
                            if (allMigrated) {
                                guestRepository.clearAll()
                                migrationToastMessage = strings.guestMigratedSuccess
                            }
                        }
                    }
                    key(user.uid) {
                        DailyHisabAuthenticatedShell(
                            currentUser = user,
                            isGuest = false,
                            guestRepository = guestRepository,
                            initialNotice = migrationToastMessage,
                            onConsumeInitialNotice = { migrationToastMessage = null },
                            language = language,
                            onLanguageChange = { nextLang ->
                                scope.launch { languagePrefs.setLanguage(nextLang) }
                            },
                            onExitGuest = {}
                        )
                    }
                }

                isGuestMode -> {
                    key(LocalGuestTransactionRepository.GUEST_USER_ID) {
                        DailyHisabAuthenticatedShell(
                            currentUser = null,
                            isGuest = true,
                            guestRepository = guestRepository,
                            initialNotice = null,
                            onConsumeInitialNotice = {},
                            language = language,
                            onLanguageChange = { nextLang ->
                                scope.launch { languagePrefs.setLanguage(nextLang) }
                            },
                            onExitGuest = {
                                scope.launch { languagePrefs.setGuestMode(false) }
                            }
                        )
                    }
                }

                else -> {
                    AuthScreen(
                        language = language,
                        onLanguageChange = { nextLang ->
                            scope.launch { languagePrefs.setLanguage(nextLang) }
                        },
                        onAuthSuccess = {},
                        onContinueAsGuest = {
                            scope.launch { languagePrefs.setGuestMode(true) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyHisabAuthenticatedShell(
    currentUser: FirebaseUser?,
    isGuest: Boolean,
    guestRepository: LocalGuestTransactionRepository,
    initialNotice: String?,
    onConsumeInitialNotice: () -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onExitGuest: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialNotice) {
        if (!initialNotice.isNullOrBlank()) {
            snackbarHostState.showSnackbar(initialNotice)
            onConsumeInitialNotice()
        }
    }

    val sessionKey = if (isGuest) {
        LocalGuestTransactionRepository.GUEST_USER_ID
    } else {
        currentUser!!.uid
    }

    val viewModel: HisabViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                if (isGuest) {
                    HisabViewModel(
                        repository = guestRepository,
                        currentUserId = LocalGuestTransactionRepository.GUEST_USER_ID
                    )
                } else {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    HisabViewModel(
                        repository = TransactionRepository(db),
                        currentUserId = currentUser!!.uid
                    )
                }
            }
        }
    )

    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val selectedReportPeriod by viewModel.selectedReportPeriod.collectAsStateWithLifecycle()
    val reportOffset by viewModel.reportOffset.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }
    var showFormSheet by remember { mutableStateOf(false) }
    var formInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingRecord by remember { mutableStateOf<TransactionRecord?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }

    if (currentTab != AppTab.DASHBOARD) {
        BackHandler {
            currentTab = AppTab.DASHBOARD
        }
    }

    if (showAccountDialog) {
        AccountProfileDialog(
            user = currentUser,
            isGuest = isGuest,
            language = language,
            onLanguageChange = onLanguageChange,
            onSignOutConfirmed = {
                signOut(credentialManager = credentialManager, scope = scope)
            },
            onExitGuestConfirmed = onExitGuest,
            onDismiss = { showAccountDialog = false }
        )
    }

    if (showFormSheet) {
        TransactionFormSheet(
            initialType = formInitialType,
            editingRecord = editingRecord,
            language = language,
            isSaving = isMutating,
            onDismiss = {
                showFormSheet = false
                editingRecord = null
            },
            onSubmit = { existingId, type, amount, category, description, dateMillis ->
                viewModel.saveTransaction(
                    existingId = existingId,
                    type = type,
                    amount = amount,
                    category = category,
                    description = description,
                    transactionDateMillis = dateMillis
                ) { result ->
                    result.onSuccess {
                        showFormSheet = false
                        editingRecord = null
                        val msg = if (existingId == null) strings.savedSuccess else strings.updatedSuccess
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }.onFailure { err ->
                        scope.launch {
                            snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to save")
                        }
                    }
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = PureWhite,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editingRecord = null
                        formInitialType = TransactionType.EXPENSE
                        showFormSheet = true
                    },
                    containerColor = IncomeGreen,
                    contentColor = PureWhite,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = strings.addTransactionTitle
                    )
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = PureWhite,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val label = when (tab) {
                                AppTab.DASHBOARD -> strings.navDashboard
                                AppTab.REPORTS -> strings.navReports
                                AppTab.HISTORY -> strings.navHistory
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = IncomeGreen,
                                    selectedTextColor = IncomeGreen,
                                    indicatorColor = IncomeGreenSoft,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = PureWhite,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .padding(vertical = 12.dp)
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val label = when (tab) {
                                AppTab.DASHBOARD -> strings.navDashboard
                                AppTab.REPORTS -> strings.navReports
                                AppTab.HISTORY -> strings.navHistory
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = IncomeGreen,
                                    selectedTextColor = IncomeGreen,
                                    indicatorColor = IncomeGreenSoft,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (val state = transactionsState) {
                        is UiState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = IncomeGreen)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = strings.loadingData,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        is UiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { currentTab = AppTab.DASHBOARD },
                                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                                ) {
                                    Text(strings.retry)
                                }
                            }
                        }

                        is UiState.Success -> {
                            val transactions = state.data
                            when (currentTab) {
                                AppTab.DASHBOARD -> DashboardScreen(
                                    transactions = transactions,
                                    currentUser = currentUser,
                                    isGuest = isGuest,
                                    language = language,
                                    onLanguageChange = onLanguageChange,
                                    onOpenProfile = { showAccountDialog = true },
                                    onAddIncome = {
                                        editingRecord = null
                                        formInitialType = TransactionType.INCOME
                                        showFormSheet = true
                                    },
                                    onAddExpense = {
                                        editingRecord = null
                                        formInitialType = TransactionType.EXPENSE
                                        showFormSheet = true
                                    },
                                    onEditTransaction = { record ->
                                        editingRecord = record
                                        formInitialType = record.parsedType
                                        showFormSheet = true
                                    },
                                    onNavigateToReports = { currentTab = AppTab.REPORTS },
                                    onNavigateToHistory = { currentTab = AppTab.HISTORY }
                                )

                                AppTab.REPORTS -> ReportsScreen(
                                    transactions = transactions,
                                    selectedPeriod = selectedReportPeriod,
                                    periodOffset = reportOffset,
                                    language = language,
                                    onSelectPeriod = viewModel::selectReportPeriod,
                                    onStepOffset = viewModel::stepReportOffset
                                )

                                AppTab.HISTORY -> HistoryScreen(
                                    transactions = transactions,
                                    language = language,
                                    onEditTransaction = { record ->
                                        editingRecord = record
                                        formInitialType = record.parsedType
                                        showFormSheet = true
                                    },
                                    onDeleteTransaction = { record ->
                                        viewModel.deleteTransaction(record.id) { result ->
                                            result.onSuccess {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(strings.deletedSuccess)
                                                }
                                            }.onFailure { err ->
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        err.localizedMessage ?: "Delete failed"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
