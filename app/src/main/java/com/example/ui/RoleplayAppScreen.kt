package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImageContent
import com.example.data.database.*
import com.example.ui.viewmodel.RoleplayViewModel
import com.example.utils.FileUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.net.Uri
import android.content.Intent
import android.content.Context
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

class GetMultipleContents : ActivityResultContract<String, List<Uri>>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return emptyList()
        }
        val uris = mutableListOf<Uri>()
        intent.data?.let { uris.add(it) }
        intent.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                uris.add(clipData.getItemAt(i).uri)
            }
        }
        return uris.distinct()
    }
}

enum class RoleplayTab {
    LIBRARY, CHAT, DREAM_ART, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleplayAppScreen(
    viewModel: RoleplayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    // UI states from Flow
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val userApiKey by viewModel.currentApiKey.collectAsStateWithLifecycle()
    val savedGeminiRoomKeys by viewModel.savedGeminiApiKeys.collectAsStateWithLifecycle()
    val groqApiKey by viewModel.groqApiKey.collectAsStateWithLifecycle()
    val savedGroqRoomKeys by viewModel.savedGroqApiKeys.collectAsStateWithLifecycle()
    val zaiApiKey by viewModel.zaiApiKey.collectAsStateWithLifecycle()
    val savedZaiRoomKeys by viewModel.savedZaiApiKeys.collectAsStateWithLifecycle()
    val activeModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val customInstruction by viewModel.customInstruction.collectAsStateWithLifecycle()
    val videoMaxFrames by viewModel.videoMaxFrames.collectAsStateWithLifecycle()
    val videoFrameIntervalMs by viewModel.videoFrameIntervalMs.collectAsStateWithLifecycle()
    val selectedCharId by viewModel.selectedCharacterId.collectAsStateWithLifecycle()
    val dailyUsage by viewModel.dailyUsageCount.collectAsStateWithLifecycle()
    val weeklyUsage by viewModel.weeklyUsageCount.collectAsStateWithLifecycle()
    val camouflageEnabled by viewModel.camouflageEnabled.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(RoleplayTab.LIBRARY) }
    var selectedCharacterForDetail by remember { mutableStateOf<CharacterEntity?>(null) }
    var isShowCreationDialog by remember { mutableStateOf(false) }
    var isShowGroupCreationDialog by remember { mutableStateOf(false) }

    // Sophisticated Dark Style Palette
    val slateBg = Color(0xFF1C1B1F)       // Deep charcoal canvas background
    val cardDark = Color(0xFF2B2930)      // Slate container dark purple/grey
    val accentNeonColor = Color(0xFFD0BCFF) // Elegant Lavender pastel accent
    val borderDark = Color(0xFF49454F)    // Tech borderline

    // Screen-width configuration check for split pane
    val screenWidth = LocalContext.current.resources.configuration.screenWidthDp
    val isTablet = screenWidth > 640

    // Toast triggers on error
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Auto navigate to CHAT tab once a session becomes active
    LaunchedEffect(activeSession?.id) {
        if (activeSession != null) {
            activeTab = RoleplayTab.CHAT
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1C1B1F),
                drawerContentColor = Color.White,
                modifier = Modifier.width(290.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Logo & Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Face,
                        contentDescription = "OurDream AI",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "OURDREAM AI",
                            color = Color(0xFFD0BCFF),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "ROLEPLAY PRO ENGINE 3.0",
                            color = Color(0xFFFF79C6),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFF49454F).copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                )

                // Menu items
                val menuItems = listOf(
                    Triple(RoleplayTab.LIBRARY, "Explore (Karakter)", Icons.Filled.Explore),
                    Triple(RoleplayTab.CHAT, "Chat (Riwayat/Aktif)", Icons.AutoMirrored.Filled.Chat),
                    Triple(RoleplayTab.DREAM_ART, "Dream Art (Seni AI)", Icons.Filled.Brush),
                    Triple(RoleplayTab.SETTINGS, "Setelan (Aplikasi)", Icons.Filled.Settings)
                )

                menuItems.forEach { (tab, label, icon) ->
                    val isSelected = activeTab == tab
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selected = isSelected,
                        onClick = {
                            activeTab = tab
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF2B2930),
                            selectedIconColor = Color(0xFFD0BCFF),
                            selectedTextColor = Color(0xFFD0BCFF),
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = Color(0xFF938F99),
                            unselectedTextColor = Color(0xFF938F99)
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(50.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Footer section with online state indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF50FA7B))
                    )
                    Text(
                        text = "Sesi Aktif Anda Terkoneksi",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.background(slateBg)
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = slateBg
            ) {
                when (activeTab) {
                    RoleplayTab.LIBRARY -> {
                        LibraryTab(
                            viewModel = viewModel,
                            characters = characters,
                            onCharacterSelect = { viewModel.selectCharacter(it.id) },
                            onCharacterDelete = { viewModel.deleteCharacter(it) },
                            onCharacterDetail = { selectedCharacterForDetail = it },
                            onCreateNewClick = { isShowCreationDialog = true },
                            onShowGroupCreation = { isShowGroupCreationDialog = true },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                    RoleplayTab.CHAT -> {
                        if (activeSession != null) {
                            ChatPane(
                                viewModel = viewModel,
                                messages = messages,
                                isGenerating = isGenerating,
                                character = characters.find { it.id == activeSession?.characterId },
                                activeModel = activeModel,
                                onBackClick = { viewModel.closeActiveSession() }
                            )
                        } else {
                            RecentChatsPane(
                                viewModel = viewModel,
                                sessions = sessions,
                                characters = characters,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                    RoleplayTab.DREAM_ART -> {
                        DreamArtTab(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                    RoleplayTab.SETTINGS -> {
                        SettingsTab(
                            viewModel = viewModel,
                            userApiKey = userApiKey,
                            savedApiKeys = savedGeminiRoomKeys,
                            groqApiKey = groqApiKey,
                            savedGroqApiKeys = savedGroqRoomKeys,
                            zaiApiKey = zaiApiKey,
                            savedZaiApiKeys = savedZaiRoomKeys,
                            activeModel = activeModel,
                            customInstruction = customInstruction,
                            dailyUsage = dailyUsage,
                            weeklyUsage = weeklyUsage,
                            videoMaxFrames = videoMaxFrames,
                            videoFrameIntervalMs = videoFrameIntervalMs,
                            camouflageEnabled = camouflageEnabled,
                            onCamouflageChanged = { viewModel.setCamouflageEnabled(it) },
                            onApiKeySaved = { viewModel.saveApiKey(it) },
                            onApiKeySelected = { viewModel.selectActiveApiKey(it) },
                            onApiKeyDeleted = { viewModel.deleteApiKey(it) },
                            onGroqApiKeySaved = { viewModel.saveGroqApiKey(it) },
                            onGroqApiKeySelected = { viewModel.selectActiveGroqApiKey(it) },
                            onGroqApiKeyDeleted = { viewModel.deleteGroqApiKey(it) },
                            onZaiApiKeySaved = { viewModel.saveZaiApiKey(it) },
                            onZaiApiKeySelected = { viewModel.selectActiveZaiApiKey(it) },
                            onZaiApiKeyDeleted = { viewModel.deleteZaiApiKey(it) },
                            onModelChanged = { viewModel.saveSelectedModel(it) },
                            onCustomInstructionSaved = { viewModel.saveCustomInstruction(it) },
                            onVideoMaxFramesChanged = { viewModel.setVideoMaxFrames(it) },
                            onVideoFrameIntervalMsChanged = { viewModel.setVideoFrameIntervalMs(it) },
                            onCheckUsage = { viewModel.checkUsageLimits() },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                }

            // Character Detail bio (Lore Reader Sheet)
            selectedCharacterForDetail?.let { char ->
                CharacterDetailDialog(
                    character = char,
                    onDismiss = { selectedCharacterForDetail = null },
                    onStartChat = {
                        viewModel.selectCharacter(char.id)
                        selectedCharacterForDetail = null
                    }
                )
            }

            // Create Character Fullscreen-like Dialog Screen
            if (isShowCreationDialog) {
                CharacterCreationDialog(
                    viewModel = viewModel,
                    onDismiss = { isShowCreationDialog = false },
                    onCreate = { name, app, pers, bg, greet, tags, avatar ->
                        viewModel.createCharacter(name, app, pers, bg, greet, tags, avatar)
                        isShowCreationDialog = false
                    }
                )
            }

            // Create Group Chat Dialog Screen
            if (isShowGroupCreationDialog) {
                GroupCreationDialog(
                    onDismiss = { isShowGroupCreationDialog = false },
                    onCreate = { name, participantIds ->
                        viewModel.createGroupChat(name, participantIds)
                        isShowGroupCreationDialog = false
                    },
                    characters = characters
                )
            }
        }
    }
}
}

// ------ LIBRARY INTERFACE COMPONENT ------

@Composable
fun LibraryTab(
    viewModel: RoleplayViewModel,
    characters: List<CharacterEntity>,
    onCharacterSelect: (CharacterEntity) -> Unit,
    onCharacterDelete: (CharacterEntity) -> Unit,
    onCharacterDetail: (CharacterEntity) -> Unit,
    onCreateNewClick: () -> Unit,
    onShowGroupCreation: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Grup Chat")

    val filteredCharacters = remember(characters, selectedCategory) {
        if (selectedCategory == "Semua") {
            characters
        } else {
            characters.filter { it.tags.contains(selectedCategory, ignoreCase = true) }
        }
    }

    val groupSessions by viewModel.groupSessionsList.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Buka Menu",
                            tint = Color(0xFFD0BCFF)
                        )
                    }
                    Column {
                        Text(
                            text = "Arsip Persona",
                            color = Color(0xFFD0BCFF),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "OURDREAM AI PRO 3.0",
                            color = Color(0xFFFF79C6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
                
                // Styled Add button header
                IconButton(
                    onClick = {
                        if (selectedCategory == "Grup Chat") {
                            onShowGroupCreation()
                        } else {
                            onCreateNewClick()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFD0BCFF).copy(alpha = 0.15f),
                        contentColor = Color(0xFFD0BCFF)
                    ),
                    modifier = Modifier.border(1.dp, Color(0xFF49454F), CircleShape)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Buat Item Baru")
                }
            }

            // Horizontal Category Selector Chips Row
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Color(0xFFD0BCFF) else Color(0xFF2B2930)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color(0xFF381E72) else Color(0xFFE6E1E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (selectedCategory == "Grup Chat") {
                if (groupSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forum,
                                contentDescription = "Grup Chat Kosong",
                                tint = Color(0xFF938F99).copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Grup chat multi-agent belum dibuat.",
                                color = Color(0xFF938F99),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onShowGroupCreation,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD0BCFF),
                                    contentColor = Color(0xFF381E72)
                                )
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Tambah")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buat Sesi Grup Chat")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groupSessions) { group ->
                            GroupCard(
                                group = group,
                                characters = characters,
                                onClick = {
                                    viewModel.selectSession(group)
                                },
                                onDelete = { viewModel.deleteSession(group) }
                            )
                        }
                    }
                }
            } else {
                if (characters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF).copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Perpustakaan Kosong",
                                color = Color(0xFFE6E1E5),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Karakter awal berhasil dihapus. Ketuk tombol '+' di sudut kanan bawah untuk mulai membuat karakter kustom impian Anda!",
                                color = Color(0xFF938F99),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else if (filteredCharacters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada persona dengan tag '$selectedCategory'",
                            color = Color(0xFF938F99),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCharacters) { char ->
                            CharacterCard(
                                character = char,
                                onClickChat = { onCharacterSelect(char) },
                                onClickInfo = { onCharacterDetail(char) },
                                onClickDelete = { onCharacterDelete(char) }
                            )
                        }
                    }
                }
            }
        }

        // Sophisticated Dark Floating Action Button
        FloatingActionButton(
            onClick = {
                if (selectedCategory == "Grup Chat") {
                    onShowGroupCreation()
                } else {
                    onCreateNewClick()
                }
            },
            containerColor = Color(0xFFD0BCFF),
            contentColor = Color(0xFF381E72),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(56.dp)
                .border(2.dp, Color(0xFF1C1B1F), CircleShape)
                .testTag("fab_create_character")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Buat Karakter Kustom", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun CharacterCard(
    character: CharacterEntity,
    onClickChat: () -> Unit,
    onClickInfo: () -> Unit,
    onClickDelete: () -> Unit
) {
    val fallbackImage = remember(character.name, character.avatarUri) {
        if (!character.avatarUri.isNullOrBlank() && 
            !character.avatarUri.startsWith("gradient_") && 
            (character.avatarUri.startsWith("http") || character.avatarUri.contains(".") || character.avatarUri.contains("/"))) {
            character.avatarUri
        } else {
            val fallbackUrls = listOf(
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=500",
                "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&q=80&w=500",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=500",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=500",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=500",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=500",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=500",
                "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&q=80&w=500"
            )
            val index = Math.abs(character.name.hashCode()) % fallbackUrls.size
            fallbackUrls[index]
        }
    }

    val stableAge = remember(character.name) {
        val hash = Math.abs(character.name.hashCode())
        (hash % 7) + 19
    }
    val stableLikes = remember(character.name) {
        val hash = Math.abs(character.name.hashCode())
        val likes = hash % 850 + 95
        if (likes > 1000) "%.1fk".format(likes / 1000f) else "$likes"
    }
    val stableChatCount = remember(character.name) {
        val hash = Math.abs(character.name.hashCode())
        val count = hash % 180 + 20
        "${count}k"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
            .clickable { onClickChat() }
            .testTag("character_card_${character.name}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = fallbackImage,
                contentDescription = character.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.92f)
                            ),
                            startY = 180f
                        )
                    )
            )

            if (!character.isPredefined) {
                IconButton(
                    onClick = onClickDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Hapus Karakter",
                        tint = Color(0xFFF2B8B5),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            IconButton(
                onClick = onClickInfo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Detail",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = character.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$stableAge",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val descText = if (character.tags.isNotBlank()) character.tags else character.personality
                Text(
                    text = descText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Likes",
                            tint = Color(0xFFF2B8B5),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stableLikes,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubble,
                            contentDescription = "Chats",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = stableChatCount,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (character.isPredefined) "@ourdream" else "@user",
                        color = Color(0xFFFF79C6).copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ------ ACTIVE CHAT CONVERSATION SCREEN COMPONENT ------

@Composable
fun ChatPane(
    viewModel: RoleplayViewModel,
    messages: List<ChatMessageEntity>,
    isGenerating: Boolean,
    character: CharacterEntity?,
    activeModel: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val videoMaxFrames by viewModel.videoMaxFrames.collectAsStateWithLifecycle()
    val videoFrameIntervalMs by viewModel.videoFrameIntervalMs.collectAsStateWithLifecycle()

    val colorDialogue by viewModel.colorDialogue.collectAsStateWithLifecycle()
    val colorThought by viewModel.colorThought.collectAsStateWithLifecycle()
    val colorNarration by viewModel.colorNarration.collectAsStateWithLifecycle()
    val colorAiBg by viewModel.colorAiBackground.collectAsStateWithLifecycle()
    val colorUserBg by viewModel.colorUserBackground.collectAsStateWithLifecycle()
    val colorGeneralBg by viewModel.colorGeneralBackground.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    
    // Search feature state variables
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableStateOf(0) }

    val searchMatches = remember(searchQuery, messages) {
        if (searchQuery.isBlank()) {
            emptyList<Int>()
        } else {
            messages.mapIndexedNotNull { index, msg ->
                if (msg.text.contains(searchQuery, ignoreCase = true)) index else null
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty() && searchMatches.isNotEmpty()) {
            currentMatchIndex = 0
            listState.animateScrollToItem(searchMatches[0])
        }
    }
    
    // Custom controls settings/options modal trigger state
    var isShowOptionsDialog by remember { mutableStateOf(false) }
    var viewFileContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Attachment states
    var attachedBase64 by remember { mutableStateOf<String?>(null) }
    var attachedType by remember { mutableStateOf<String?>(null) }
    var attachedName by remember { mutableStateOf<String?>(null) }

    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val isChatSceneGenerating by viewModel.chatSceneImageGenerating.collectAsStateWithLifecycle()

    val animatedMessageIds = remember { mutableStateOf(emptySet<Int>()) }

    val showScrollToBottomButton by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            
            val isScrolledDown = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 150
            val isAtBottom = lastVisibleItem != null && lastVisibleItem.index >= totalItems - 1
            
            isScrolledDown && !isAtBottom
        }
    }

    LaunchedEffect(activeSession?.id) {
        animatedMessageIds.value = emptySet()
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty() && animatedMessageIds.value.isEmpty()) {
            animatedMessageIds.value = messages.map { it.id }.toSet()
        }
    }

    // Launchers for Picking Files
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val name = FileUtils.getUriFileName(context, uri)
                val isVideo = FileUtils.isVideoFile(context, uri, name)
                if (isVideo) {
                    Toast.makeText(context, "Menyiapkan file video...", Toast.LENGTH_SHORT).show()
                    val cachedPath = FileUtils.saveUriToCacheFile(context, uri, name)
                    if (cachedPath != null) {
                        attachedBase64 = cachedPath
                        attachedType = "video/mp4"
                        attachedName = name
                        Toast.makeText(context, "Video berhasil dilampirkan.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal melampirkan file video.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val result = FileUtils.compressUriToJpegBase64(context, uri)
                    if (result != null) {
                        attachedBase64 = result.first
                        attachedType = result.second
                        attachedName = name
                    } else {
                        Toast.makeText(context, "Gagal memproses gambar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                if (uris.size == 1) {
                    val uri = uris.first()
                    val name = FileUtils.getUriFileName(context, uri)
                    val isImage = name.endsWith(".jpg", true) || 
                                  name.endsWith(".jpeg", true) || 
                                  name.endsWith(".png", true) || 
                                  name.endsWith(".webp", true)
                    val isVideo = FileUtils.isVideoFile(context, uri, name)
                    if (isImage) {
                        val result = FileUtils.compressUriToJpegBase64(context, uri)
                        if (result != null) {
                            attachedBase64 = result.first
                            attachedType = result.second
                            attachedName = name
                        }
                    } else if (isVideo) {
                        Toast.makeText(context, "Menyiapkan file video...", Toast.LENGTH_SHORT).show()
                        val cachedPath = FileUtils.saveUriToCacheFile(context, uri, name)
                        if (cachedPath != null) {
                            attachedBase64 = cachedPath
                            attachedType = "video/mp4"
                            attachedName = name
                            Toast.makeText(context, "Video berhasil dilampirkan.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal melampirkan file video.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val fileSize = FileUtils.getUriFileSize(context, uri)
                        if (fileSize > 2 * 1024 * 1024) {
                            Toast.makeText(context, "File terlalu besar! Maksimal ukuran file teks adalah 2MB.", Toast.LENGTH_LONG).show()
                        } else {
                            val text = FileUtils.readUriAsText(context, uri)
                            if (text != null) {
                                attachedBase64 = text
                                attachedType = if (name.endsWith(".md", true)) "text/markdown" else "text/plain"
                                attachedName = name
                            } else {
                                Toast.makeText(context, "Dokumen kosong atau format tidak didukung (Gunakan file teks/plain atau markdown).", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    val combinedTextBuilder = StringBuilder()
                    val fileNames = mutableListOf<String>()
                    var successCount = 0
                    var hasTooLargeFile = false
                    
                    for (uri in uris) {
                        val name = FileUtils.getUriFileName(context, uri)
                        val fileSize = FileUtils.getUriFileSize(context, uri)
                        if (fileSize > 2 * 1024 * 1024) {
                            hasTooLargeFile = true
                            continue
                        }
                        val text = FileUtils.readUriAsText(context, uri)
                        if (text != null) {
                            combinedTextBuilder.append("---START_FILE: $name---\n")
                            combinedTextBuilder.append(text)
                            combinedTextBuilder.append("\n---END_FILE: $name---\n")
                            fileNames.add(name)
                            successCount++
                        }
                    }
                    
                    if (successCount > 0) {
                        attachedBase64 = combinedTextBuilder.toString()
                        attachedType = "text/plain"
                        attachedName = fileNames.joinToString(", ")
                    } else {
                        Toast.makeText(context, "Tidak ada file teks yang berhasil dibaca.", Toast.LENGTH_SHORT).show()
                    }
                    if (hasTooLargeFile) {
                        Toast.makeText(context, "Beberapa file dilewati karena ukurannya melebihi 2MB.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    var hasDoneInitialScroll by remember(activeSession?.id) { mutableStateOf(false) }

    // Auto scroll chat list down (jump immediately on initial load, animate on new messages)
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            if (!hasDoneInitialScroll) {
                listState.scrollToItem(messages.size - 1)
                hasDoneInitialScroll = true
            } else {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    if (activeSession == null) {
        EmptyState("Pilih atau buat karakter terlebih dahulu!")
        return
    }

    val isGroup = activeSession?.isGroup == true
    val headerTitle = if (isGroup) activeSession?.groupName ?: activeSession?.sessionName ?: "Grup Obrolan" else character?.name ?: activeSession?.sessionName ?: "Persona"
    val headerSubtitle = if (isGroup) "Grup Multi-Agent • Aktif" else "Karakter Persona • Aktif"
    val avatarUriString = if (isGroup) "gradient_group" else character?.avatarUri ?: "gradient_custom_1"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(parseHexColor(colorGeneralBg, Color(0xFF1C1B1F)))
            .imePadding()
    ) {
        // Floating Room Header - Matching "Sophisticated Dark" Title header Spec
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = Color(0xFF49454F),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Riwayat",
                        tint = Color(0xFFD0BCFF)
                    )
                }

                CharacterAvatar(
                    avatarUri = avatarUriString,
                    name = headerTitle,
                    size = 42.dp,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        color = Color(0xFFD0BCFF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isGenerating) Color(0xFFD0BCFF) else Color(0xFFFF79C6))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGenerating) "sedang mengetik..." else headerSubtitle,
                            color = Color(0xFF938F99),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // NEW: Open Options Gear Dialog Icon (Sesi & Memori Integration)
                IconButton(
                    onClick = { isShowOptionsDialog = true },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.4f), CircleShape)
                        .testTag("btn_options_gear")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Pengaturan Sesi & Memori",
                        tint = Color(0xFFD0BCFF)
                    )
                }

                // Clear state & delete memory action icons
                IconButton(
                    onClick = { viewModel.clearActiveChat() },
                    modifier = Modifier.border(1.dp, Color(0xFF49454F).copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Mula Ulang Chat",
                        tint = Color(0xFF938F99)
                    )
                }
            }
        }

        // Search bar overlay
        AnimatedVisibility(
            visible = isSearchMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = Color(0xFF231F2A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f)),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Cari",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_text_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFD0BCFF)),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Cari kata atau respon...",
                                    color = Color(0xFF938F99),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    
                    if (searchQuery.isNotEmpty()) {
                        val matchesCount = searchMatches.size
                        Text(
                            text = if (matchesCount > 0) "${currentMatchIndex + 1}/$matchesCount" else "0",
                            color = if (matchesCount > 0) Color(0xFFD0BCFF) else Color(0xFFFFB4AB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        
                        IconButton(
                            onClick = {
                                if (searchMatches.isNotEmpty()) {
                                    currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size) % searchMatches.size
                                    scope.launch {
                                        listState.animateScrollToItem(searchMatches[currentMatchIndex])
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp),
                            enabled = searchMatches.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Sebelumnya",
                                tint = if (searchMatches.isNotEmpty()) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                if (searchMatches.isNotEmpty()) {
                                    currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
                                    scope.launch {
                                        listState.animateScrollToItem(searchMatches[currentMatchIndex])
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp),
                            enabled = searchMatches.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Berikutnya",
                                tint = if (searchMatches.isNotEmpty()) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                searchQuery = ""
                                currentMatchIndex = 0
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Bersihkan",
                                tint = Color(0xFF938F99),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { 
                            isSearchMode = false
                            searchQuery = ""
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "Tutup Cari",
                            tint = Color(0xFFFFB4AB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // List bubbles wrapper for Scroll-to-Bottom overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val shouldAnimate = remember(msg.id, animatedMessageIds.value) {
                        msg.role == "model" && !animatedMessageIds.value.contains(msg.id)
                    }

                    ChatBubble(
                        message = msg,
                        character = character,
                        viewModel = viewModel,
                        shouldAnimateText = shouldAnimate,
                        onAnimationComplete = {
                            animatedMessageIds.value = animatedMessageIds.value + msg.id
                        },
                        allMessages = messages,
                        onViewFile = { name, content ->
                            viewFileContent = Pair(name, content)
                        },
                        searchQuery = searchQuery
                    )
                }
                if (isGenerating) {
                    item {
                        TypingIndicatorRow(character = character ?: CharacterEntity(name = headerTitle, appearance="", personality="", background="", greeting=""))
                    }
                }
            }

            // Scroll-to-Bottom Floating Button with animation
            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToBottomButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        scope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("scroll_to_bottom_button")
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .border(1.dp, Color(0xFF938F99).copy(alpha = 0.3f), CircleShape),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF1D1B20).copy(alpha = 0.9f),
                        contentColor = Color(0xFF938F99)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "Scroll ke bawah",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Options Gear Dialog overlay trigger (Sesi / Memori / Edit Persona)
        if (isShowOptionsDialog && activeSession != null) {
            ChatOptionsDialog(
                viewModel = viewModel,
                activeSession = activeSession!!,
                character = character,
                onDismiss = { isShowOptionsDialog = false }
            )
        }

        // Reference File Viewer Dialog
        viewFileContent?.let { (fileName, fileText) ->
            val isImg = fileName.endsWith(".jpg", true) ||
                        fileName.endsWith(".jpeg", true) ||
                        fileName.endsWith(".png", true) ||
                        fileName.endsWith(".webp", true) ||
                        fileName.contains("Foto", ignoreCase = true) ||
                        fileName.contains("Image", ignoreCase = true) ||
                        fileName.contains("Gambar", ignoreCase = true) ||
                        fileName.contains("Pic", ignoreCase = true)
            val isVid = fileName.endsWith(".mp4", true) ||
                        fileName.endsWith(".mkv", true) ||
                        fileName.endsWith(".avi", true) ||
                        fileName.endsWith(".webm", true) ||
                        fileName.endsWith(".3gp", true) ||
                        fileName.endsWith(".mov", true) ||
                        fileName.endsWith(".flv", true) ||
                        fileName.contains("Video", ignoreCase = true)
            AlertDialog(
                onDismissRequest = { viewFileContent = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isImg) Icons.Filled.Image else if (isVid) Icons.Filled.Movie else Icons.Filled.AttachFile,
                            contentDescription = "File Icon",
                            tint = Color(0xFFF48FB1),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .then(if (isVid) Modifier else Modifier.verticalScroll(rememberScrollState())),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImg) {
                            val cleanBase64 = if (fileText.contains(",")) fileText.substringAfter(",") else fileText
                            val isUrlOrPath = (cleanBase64.startsWith("http://") || cleanBase64.startsWith("https://") || (cleanBase64.startsWith("/") && cleanBase64.length < 1024 && !cleanBase64.startsWith("/9j/")))
                            if (isUrlOrPath) {
                                ZoomableImage(
                                    model = cleanBase64,
                                    contentDescription = "Image View",
                                    modifier = Modifier.fillMaxWidth().height(300.dp)
                                )
                            } else {
                                val decodedBytes = remember(cleanBase64) {
                                    try {
                                        android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (decodedBytes != null) {
                                    ZoomableImage(
                                        model = decodedBytes,
                                        contentDescription = "Image View",
                                        modifier = Modifier.fillMaxWidth().height(300.dp)
                                    )
                                } else {
                                    Text(
                                        text = "[Gambar tidak valid atau rusak]",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Red)
                                    )
                                }
                            }
                        } else if (isVid) {
                            val cleanVideoUrl = if (fileText.contains("|||VIDEO_FILE_PATH|||")) {
                                fileText.substringBefore("|||VIDEO_FILE_PATH|||")
                            } else {
                                fileText
                            }
                            VideoPlayer(
                                videoUrl = cleanVideoUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Text(
                                text = fileText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFE6E1E5),
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewFileContent = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF48FB1))
                    ) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF2B2930),
                tonalElevation = 6.dp
            )
        }

        // Sophisticated Dark Rounded Action Input Panel [211F26] with top border
        Surface(
            color = Color(0xFF211F26),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = Color(0xFF49454F),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = strokeWidth
                        )
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            ) {
                
                // Attached File Indicator Preview
                attachedBase64?.let { base64Data ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color(0xFF2B2930), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = if (attachedType?.startsWith("image/") == true) Icons.Filled.Image else Icons.Filled.Description
                            Icon(icon, contentDescription = "File", tint = Color(0xFFD0BCFF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = attachedName ?: "File Lampiran",
                                color = Color(0xFFE6E1E5),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                attachedBase64 = null
                                attachedType = null
                                attachedName = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Batalkan", tint = Color(0xFF938F99), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Keyboard action bar with styled inner background card container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2B2930), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ketik cerita atau respon...", color = Color(0xFF938F99), fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp, max = 150.dp)
                            .testTag("chat_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5)
                        ),
                        singleLine = false,
                        maxLines = 6
                    )

                    HorizontalDivider(
                        color = Color(0xFF49454F).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Trigger Image Gen of current roleplayed Scene or close-up character Selfie
                            IconButton(
                                onClick = {
                                    viewModel.generateChatSceneImage()
                                },
                                enabled = !isChatSceneGenerating,
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isChatSceneGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color(0xFFD0BCFF),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Lukis Adegan / Selfie",
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Trigger Document Plaintext Pick
                            IconButton(
                                onClick = {
                                    filePickerLauncher.launch("*/*")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = "Unggah Dokumen",
                                    tint = Color(0xFF938F99),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Trigger Keyword Search
                            IconButton(
                                onClick = {
                                    isSearchMode = !isSearchMode
                                    if (!isSearchMode) {
                                        searchQuery = ""
                                    }
                                },
                                modifier = Modifier.size(36.dp).testTag("search_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Cari Kata",
                                    tint = if (isSearchMode) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (attachedName != null) {
                            Text(
                                text = "📎 $attachedName",
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // BULAT KUNING: Platform Selector (Gemini / Groq)
                        var isPlatformMenuExpanded by remember { mutableStateOf(false) }
                        val activePlatform = remember(activeModel) {
                            if (activeModel.startsWith("gemini-")) "gemini" else "groq"
                        }
                        
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B2930))
                                    .border(1.dp, Color(0xFF49454F), CircleShape)
                                    .clickable { isPlatformMenuExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                val platformIcon = if (activePlatform == "gemini") Icons.Filled.AutoAwesome else Icons.Filled.Bolt
                                val platformIconColor = if (activePlatform == "gemini") Color(0xFFD0BCFF) else Color(0xFFFFB74D)
                                Icon(
                                    imageVector = platformIcon,
                                    contentDescription = "Platform Selector",
                                    tint = platformIconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = isPlatformMenuExpanded,
                                onDismissRequest = { isPlatformMenuExpanded = false },
                                modifier = Modifier
                                    .background(Color(0xFF2B2930))
                                    .border(1.dp, Color(0xFF49454F))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Google Gemini", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                                        }
                                    },
                                    onClick = {
                                        isPlatformMenuExpanded = false
                                        viewModel.saveSelectedModel("gemini-3.5-flash")
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Groq Cloud", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                                        }
                                    },
                                    onClick = {
                                        isPlatformMenuExpanded = false
                                        viewModel.saveSelectedModel("llama-3.3-70b-versatile")
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // KOTAK MERAH: Model Selector according to selected platform
                        var isModelMenuExpanded by remember { mutableStateOf(false) }
                        val activePlatformModels = if (activePlatform == "gemini") {
                            listOf(
                                "gemini-3.5-flash" to "Dreamini 3 Pro",
                                "gemini-3.1-pro-preview" to "Dreamini 3.1 Pro",
                                "gemini-3-flash-preview" to "Dreamini 3 Flash",
                                "gemini-3.1-flash-lite" to "Dreamini 3.1 Flash Lite",
                                "gemini-2.5-flash" to "Dreamini Pro"
                            )
                        } else {
                            listOf(
                                "llama-3.3-70b-versatile" to "Llama 3.3 70B",
                                "llama-3.1-8b-instant" to "Llama 3.1 8B",
                                "meta-llama/llama-4-scout-17b-16e-instruct" to "Llama 4 Scout 17B",
                                "groq/compound" to "Compound",
                                "groq/compound-mini" to "Compound Mini",
                                "qwen/qwen3-32b" to "Qwen3 32B",
                                "qwen/qwen3.6-27b" to "Qwen 3.6 27B"
                            )
                        }
                        
                        val activeModelDisplayNameShort = when (activeModel) {
                            "gemini-3.5-flash" -> "Dreamini 3 Pro"
                            "gemini-3.1-pro", "gemini-3.1-pro-preview" -> "Dreamini 3.1 Pro"
                            "gemini-3-flash-preview" -> "Dreamini 3 Flash"
                            "gemini-3.1-flash-lite" -> "Dreamini 3.1 Flash"
                            "gemini-2.5-flash" -> "Dreamini Pro"
                            "llama-3.3-70b-versatile" -> "Llama 3.3 70B"
                            "llama-3.1-8b-instant" -> "Llama 3.1 8B"
                            "meta-llama/llama-4-scout-17b-16e-instruct" -> "Llama 4 Scout"
                            "groq/compound" -> "Compound"
                            "groq/compound-mini" -> "Compound Mini"
                            "qwen/qwen3-32b" -> "Qwen3 32B"
                            "qwen/qwen3.6-27b" -> "Qwen 3.6 27B"
                            else -> activeModel
                        }
                        
                        Box {
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .background(Color(0xFF381E72).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { isModelMenuExpanded = true }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = activeModelDisplayNameShort,
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select Model",
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            
                            DropdownMenu(
                                expanded = isModelMenuExpanded,
                                onDismissRequest = { isModelMenuExpanded = false },
                                modifier = Modifier
                                    .background(Color(0xFF2B2930))
                                    .border(1.dp, Color(0xFF49454F))
                            ) {
                                activePlatformModels.forEach { (key, name) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = name,
                                                color = if (key == activeModel) Color(0xFFD0BCFF) else Color(0xFFE6E1E5),
                                                fontSize = 13.sp,
                                                fontWeight = if (key == activeModel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            isModelMenuExpanded = false
                                            viewModel.saveSelectedModel(key)
                                            Toast.makeText(context, "Model beralih ke $name", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Sophisticated Dark Circle Send/Stop Trigger Button
                        if (isGenerating) {
                            IconButton(
                                onClick = {
                                    viewModel.stopGenerating()
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFFF48FB1), // Soft Red/Pink for Stop
                                    contentColor = Color(0xFF211F26)
                                ),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("chat_stop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = "Batal",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank() || attachedBase64 != null) {
                                        viewModel.sendMessage(
                                            text = inputText,
                                            attachmentName = attachedName,
                                            attachmentType = attachedType,
                                            attachmentBase64 = attachedBase64
                                        )
                                        inputText = ""
                                        attachedBase64 = null
                                        attachedType = null
                                        attachedName = null
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color(0xFFD0BCFF),
                                    contentColor = Color(0xFF381E72)
                                ),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Kirim",
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

fun formatRoleplayText(
    text: String,
    dialogueColor: Color,
    thoughtColor: Color,
    narrationColor: Color
): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    
    // Pattern matches:
    // Group 1: **"dialogue"**
    // Group 2: inner dialogue
    // Group 3: *thoughts*
    // Group 4: inner thoughts
    val pattern = Regex("""(\*\*"(.*?)"\*\*)|(\*([^*]+?)\*)""", RegexOption.DOT_MATCHES_ALL)
    
    var lastIndex = 0
    pattern.findAll(text).forEach { matchResult ->
        // Append plain text (Narasi) before match
        if (matchResult.range.first > lastIndex) {
            val plain = text.substring(lastIndex, matchResult.range.first)
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(color = narrationColor))
            builder.append(plain)
            builder.pop()
        }
        
        val dialogueMatch = matchResult.groups[1]
        val thoughtMatch = matchResult.groups[3]
        
        if (dialogueMatch != null) {
            val content = matchResult.groups[2]?.value ?: ""
            builder.pushStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = dialogueColor,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            )
            builder.append("\"$content\"") // Kept quotes cleanly, removed ** asterisks
            builder.pop()
        } else if (thoughtMatch != null) {
            val content = matchResult.groups[4]?.value ?: ""
            builder.pushStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = thoughtColor,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            )
            builder.append(content) // Kept inner thoughts, removed * asterisks
            builder.pop()
        }
        
        lastIndex = matchResult.range.last + 1
    }
    
    // Append remaining plain text (Narasi)
    if (lastIndex < text.length) {
        val plain = text.substring(lastIndex)
        builder.pushStyle(androidx.compose.ui.text.SpanStyle(color = narrationColor))
        builder.append(plain)
        builder.pop()
    }
    
    return builder.toAnnotatedString()
}

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length == 8) {
            Color(cleaned.toLong(16))
        } else if (cleaned.length == 6) {
            Color((0xFF000000 or cleaned.toLong(16)).toInt())
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

fun highlightAnnotatedString(
    annotated: androidx.compose.ui.text.AnnotatedString,
    query: String
): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return annotated
    val text = annotated.text
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(annotated)
    
    var index = text.indexOf(query, ignoreCase = true)
    while (index != -1) {
        builder.addStyle(
            style = androidx.compose.ui.text.SpanStyle(
                background = Color(0xFFFFEB3B), // Bright Yellow highlight
                color = Color.Black
            ),
            start = index,
            end = index + query.length
        )
        index = text.indexOf(query, index + query.length, ignoreCase = true)
    }
    return builder.toAnnotatedString()
}

fun splitHeaderAndStory(storyContent: String): Pair<String?, String> {
    val lines = storyContent.lines()
    val firstLine = lines.firstOrNull()?.trim() ?: ""
    if (firstLine.startsWith("Day", ignoreCase = true) && firstLine.contains("(") && (firstLine.contains("#Respon") || firstLine.contains("Respon"))) {
        val remainingStory = lines.drop(1).joinToString("\n").trim()
        return Pair(firstLine, remainingStory)
    }
    return Pair(null, storyContent)
}

fun extractFileFromConcatenatedContent(concatenated: String, fileName: String): String {
    val startTag = "---START_FILE: $fileName---"
    val endTag = "---END_FILE: $fileName---"
    if (concatenated.contains(startTag, ignoreCase = true) && concatenated.contains(endTag, ignoreCase = true)) {
        val startIndex = concatenated.indexOf(startTag, ignoreCase = true) + startTag.length
        val endIndex = concatenated.indexOf(endTag, ignoreCase = true)
        if (endIndex > startIndex) {
            return concatenated.substring(startIndex, endIndex).trim()
        }
    }
    return concatenated
}

data class ParsedResponse(
    val storyContent: String,
    val options: List<String>,
    val trackerRawText: String
)

fun String.parseAiResponse(): ParsedResponse {
    var story = this
    var tracker = ""
    var optionsList = emptyList<String>()

    val startTag = "===START_TRACKER==="
    val endTag = "===END_TRACKER==="

    // 1. Extract Tracker part
    if (this.contains(startTag, ignoreCase = true) && this.contains(endTag, ignoreCase = true)) {
        val startIndex = this.indexOf(startTag, ignoreCase = true)
        val endIndex = this.indexOf(endTag, ignoreCase = true)
        if (endIndex > startIndex) {
            val beforeTracker = this.substring(0, startIndex).trim()
            val trackerContent = this.substring(startIndex + startTag.length, endIndex).trim()
            val afterTracker = this.substring(endIndex + endTag.length).trim()
            
            tracker = trackerContent
            story = if (beforeTracker.isNotEmpty() && afterTracker.isNotEmpty()) {
                beforeTracker + "\n\n" + afterTracker
            } else if (beforeTracker.isNotEmpty()) {
                beforeTracker
            } else {
                afterTracker
            }
        } else {
            val parts = this.split(Regex(startTag, RegexOption.IGNORE_CASE))
            story = parts[0].trim()
            tracker = parts.getOrNull(1)?.trim() ?: ""
        }
    } else if (this.contains(startTag, ignoreCase = true)) {
        val parts = this.split(Regex(startTag, RegexOption.IGNORE_CASE))
        story = parts[0].trim()
        tracker = parts.getOrNull(1)?.trim() ?: ""
    } else if (this.contains("STORY STATE TRACKER:", ignoreCase = true)) {
        val parts = this.split(Regex("STORY STATE TRACKER:", RegexOption.IGNORE_CASE))
        story = parts[0].trim()
        tracker = "STORY STATE TRACKER:\n" + (parts.getOrNull(1)?.trim() ?: "")
    }

    // Clean up any remaining tag artifacts from story or tracker
    story = story.replace(startTag, "", ignoreCase = true)
                 .replace(endTag, "", ignoreCase = true)
                 .trim()
    tracker = tracker.replace(startTag, "", ignoreCase = true)
                     .replace(endTag, "", ignoreCase = true)
                     .trim()

    // 2. Extract Options part from story
    if (story.contains("===NEXT_OPTION===", ignoreCase = true)) {
        val parts = story.split(Regex("===NEXT_OPTION===", RegexOption.IGNORE_CASE))
        story = parts[0].trim()
        val optionsRaw = parts.getOrNull(1)?.trim() ?: ""
        
        optionsList = optionsRaw.lines()
            .filter { line ->
                val trimmedLine = line.trim()
                trimmedLine.firstOrNull()?.isDigit() == true && 
                (trimmedLine.contains(".") || trimmedLine.contains(")"))
            }
            .map { line ->
                val trimmedLine = line.trim()
                val delimiter = if (trimmedLine.contains(".")) "." else ")"
                trimmedLine.substringAfter(delimiter).trim().removeSurrounding("[", "]")
            }
            .filter { it.isNotEmpty() }
    } else if (story.contains("Pilihan Lanjutan Cerita:", ignoreCase = true)) {
        val parts = story.split(Regex("Pilihan Lanjutan Cerita:", RegexOption.IGNORE_CASE))
        story = parts[0].trim()
        val optionsRaw = parts.getOrNull(1)?.trim() ?: ""
        optionsList = optionsRaw.lines()
            .filter { line ->
                val trimmedLine = line.trim()
                trimmedLine.firstOrNull()?.isDigit() == true && 
                (trimmedLine.contains(".") || trimmedLine.contains(")"))
            }
            .map { line ->
                val trimmedLine = line.trim()
                val delimiter = if (trimmedLine.contains(".")) "." else ")"
                trimmedLine.substringAfter(delimiter).trim().removeSurrounding("[", "]")
            }
            .filter { it.isNotEmpty() }
    }

    return ParsedResponse(
        storyContent = story.trim(),
        options = optionsList,
        trackerRawText = tracker.trim()
    )
}

@Composable
fun StoryTrackerAndOptionsWidget(
    parsedData: ParsedResponse,
    onOptionSelected: (String) -> Unit,
    onFileClicked: (String) -> Unit,
    showOptions: Boolean = true,
    allMessages: List<ChatMessageEntity> = emptyList()
) {
    var isTrackerExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        // --- 1. TAMPILAN PILIHAN LANJUTAN CERITA ---
        if (showOptions && parsedData.options.isNotEmpty()) {
            Text(
                text = "Pilihan Lanjutan Cerita:",
                style = androidx.compose.ui.text.TextStyle(
                    color = Color(0xFFF48FB1),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
            )
            
            parsedData.options.forEachIndexed { index, optionText ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onOptionSelected(optionText) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF312E3B)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFF48FB1).copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "${index + 1}. $optionText",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // --- 2. DROPDOWN BOX UNTUK STORY STATE TRACKER (COLLAPSIBLE) ---
        if (parsedData.trackerRawText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B24)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, Color(0xFF49454F).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTrackerExpanded = !isTrackerExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Tracker Book",
                                tint = Color(0xFFF48FB1),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📖 STORY STATE TRACKER",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF48FB1),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Icon(
                            imageVector = if (isTrackerExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Toggle Tracker",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isTrackerExpanded) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFF49454F).copy(alpha = 0.5f)
                        )
                        
                        val cleanTrackerText = parsedData.trackerRawText
                            .replace("**", "")
                            .replace("*", "")
                            .trim()
                        
                        val fileRegex = Regex(
                            "([a-zA-Z0-9_\\-\\(\\)]+\\.(?:txt|jpg|png|jpeg|webp))|" +
                            "((?:Foto\\s+Referensi|Foto|Gambar)\\s+[a-zA-Z0-9_\\-\\(\\)]{2,20}(?:\\s+[a-zA-Z0-9_\\-\\(\\)]{2,20}){0,2})",
                            RegexOption.IGNORE_CASE
                        )
                        val matches = fileRegex.findAll(cleanTrackerText)
                            .filter { matchResult ->
                                val value = matchResult.value.lowercase()
                                !value.contains(" yang ") &&
                                !value.contains(" dan ") &&
                                !value.contains(" dengan ") &&
                                !value.contains(" untuk ") &&
                                !value.contains(" bener ") &&
                                !value.contains(" cantik ") &&
                                !value.contains(" kamu ") &&
                                !value.contains(" saya ") &&
                                !value.contains(" aku ") &&
                                value.length < 50
                            }
                            .toList()

                        val actualAttachments = allMessages.mapNotNull { it.attachmentName }.distinct()

                        val annotatedTrackerString = androidx.compose.ui.text.buildAnnotatedString {
                            var lastIndex = 0
                            for (match in matches) {
                                append(cleanTrackerText.substring(lastIndex, match.range.first))
                                
                                val matchedName = match.value
                                val cleanMatchedName = matchedName.substringBeforeLast(".")
                                    .replace("Foto Referensi", "", ignoreCase = true)
                                    .replace("Foto", "", ignoreCase = true)
                                    .replace("Gambar", "", ignoreCase = true)
                                    .trim()
                                
                                val exactMatch = actualAttachments.find { it.equals(matchedName, ignoreCase = true) }
                                val mappedName = when {
                                    exactMatch != null -> exactMatch
                                    else -> {
                                        val fuzzyMatch = actualAttachments.find { actualName ->
                                            val cleanActual = actualName.substringBeforeLast(".").trim()
                                            cleanActual.isNotEmpty() && cleanMatchedName.isNotEmpty() && (
                                                cleanActual.contains(cleanMatchedName, ignoreCase = true) ||
                                                cleanMatchedName.contains(cleanActual, ignoreCase = true)
                                            )
                                        }
                                        if (fuzzyMatch != null) fuzzyMatch else {
                                            val isImageWord = matchedName.contains("foto", ignoreCase = true) ||
                                                              matchedName.contains("baru", ignoreCase = true) ||
                                                              matchedName.contains("referensi", ignoreCase = true) ||
                                                              matchedName.contains("gambar", ignoreCase = true) ||
                                                              matchedName.contains("image", ignoreCase = true) ||
                                                              matchedName.contains("pic", ignoreCase = true)
                                            if (isImageWord) {
                                                val lastImageAttachment = actualAttachments.findLast { actualName ->
                                                    actualName.endsWith(".jpg", true) ||
                                                    actualName.endsWith(".jpeg", true) ||
                                                    actualName.endsWith(".png", true) ||
                                                    actualName.endsWith(".webp", true)
                                                }
                                                lastImageAttachment ?: matchedName
                                            } else {
                                                matchedName
                                            }
                                        }
                                    }
                                }
                                
                                pushStringAnnotation(tag = "FILE_CLICK", annotation = mappedName)
                                withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFFF48FB1), fontWeight = FontWeight.Bold)) {
                                    append(match.value)
                                }
                                pop()
                                lastIndex = match.range.last + 1
                            }
                            if (lastIndex < cleanTrackerText.length) {
                                append(cleanTrackerText.substring(lastIndex))
                            }
                        }

                        ClickableText(
                            text = annotatedTrackerString,
                            style = androidx.compose.ui.text.TextStyle(
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            ),
                            onClick = { offset ->
                                annotatedTrackerString.getStringAnnotations(tag = "FILE_CLICK", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        onFileClicked(annotation.item)
                                    }
                            }
                        )

                        // --- 3. FITUR REFERENCES (Pills Row) ---
                        val matchFilesRaw = fileRegex.findAll(parsedData.trackerRawText)
                            .map { it.value }
                            .distinct()
                            .filter { value ->
                                val lower = value.lowercase()
                                !lower.contains(" yang ") &&
                                !lower.contains(" dan ") &&
                                !lower.contains(" dengan ") &&
                                !lower.contains(" untuk ") &&
                                !lower.contains(" bener ") &&
                                !lower.contains(" cantik ") &&
                                !lower.contains(" kamu ") &&
                                !lower.contains(" saya ") &&
                                !lower.contains(" aku ") &&
                                value.length < 50
                            }
                            .toList()

                        val matchFiles = matchFilesRaw.map { fileName ->
                            val cleanFileName = fileName.substringBeforeLast(".")
                                .replace("Foto Referensi", "", ignoreCase = true)
                                .replace("Foto", "", ignoreCase = true)
                                .replace("Gambar", "", ignoreCase = true)
                                .trim()
                            
                            val exactMatch = actualAttachments.find { it.equals(fileName, ignoreCase = true) }
                            if (exactMatch != null) return@map exactMatch
                            
                            val fuzzyMatch = actualAttachments.find { actualName ->
                                val cleanActual = actualName.substringBeforeLast(".").trim()
                                cleanActual.isNotEmpty() && cleanFileName.isNotEmpty() && (
                                    cleanActual.contains(cleanFileName, ignoreCase = true) ||
                                    cleanFileName.contains(cleanActual, ignoreCase = true)
                                )
                            }
                            if (fuzzyMatch != null) return@map fuzzyMatch
                            
                            val isImageWord = fileName.contains("foto", ignoreCase = true) ||
                                              fileName.contains("baru", ignoreCase = true) ||
                                              fileName.contains("referensi", ignoreCase = true) ||
                                              fileName.contains("gambar", ignoreCase = true) ||
                                              fileName.contains("image", ignoreCase = true) ||
                                              fileName.contains("pic", ignoreCase = true)
                            
                            if (isImageWord) {
                                val lastImageAttachment = actualAttachments.findLast { actualName ->
                                    actualName.endsWith(".jpg", true) ||
                                    actualName.endsWith(".jpeg", true) ||
                                    actualName.endsWith(".png", true) ||
                                    actualName.endsWith(".webp", true)
                                }
                                if (lastImageAttachment != null) return@map lastImageAttachment
                            }
                            
                            fileName
                        }.distinct()

                        if (matchFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Referensi Lampiran (Klik untuk buka):",
                                style = androidx.compose.ui.text.TextStyle(
                                    color = Color.LightGray.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                matchFiles.forEach { fileName ->
                                    Surface(
                                        color = Color(0xFF2D2533),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFFF48FB1).copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { onFileClicked(fileName) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val isImg = fileName.endsWith(".jpg", true) ||
                                                        fileName.endsWith(".jpeg", true) ||
                                                        fileName.endsWith(".png", true) ||
                                                        fileName.endsWith(".webp", true)
                                            Icon(
                                                imageVector = if (isImg) Icons.Filled.Image else Icons.Filled.AttachFile,
                                                contentDescription = "File Reference",
                                                tint = Color(0xFFF48FB1),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = fileName,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    color = Color(0xFFF48FB1),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
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
}

@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    character: CharacterEntity?,
    viewModel: RoleplayViewModel,
    shouldAnimateText: Boolean = false,
    onAnimationComplete: () -> Unit = {},
    allMessages: List<ChatMessageEntity> = emptyList(),
    onViewFile: (String, String) -> Unit = { _, _ -> },
    searchQuery: String = ""
) {
    val isUser = message.role == "user"
    val senderNameText = if (isUser) "Anda" else message.senderName ?: character?.name ?: "Asisten"
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val colorDialogue by viewModel.colorDialogue.collectAsStateWithLifecycle()
    val colorThought by viewModel.colorThought.collectAsStateWithLifecycle()
    val colorNarration by viewModel.colorNarration.collectAsStateWithLifecycle()
    val colorAiBg by viewModel.colorAiBackground.collectAsStateWithLifecycle()
    val colorUserBg by viewModel.colorUserBackground.collectAsStateWithLifecycle()
    
    var displayedText by remember(message.text, shouldAnimateText) {
        mutableStateOf(if (shouldAnimateText) "" else message.text)
    }
    val parsedData = remember(displayedText) { displayedText.parseAiResponse() }
    
    var isEditing by remember(message.id) { mutableStateOf(false) }
    var editText by remember(message.id, message.text) { 
        mutableStateOf(if (isUser) message.text else parsedData.storyContent) 
    }

    if (shouldAnimateText) {
        LaunchedEffect(message.text) {
            val totalLength = message.text.length
            if (totalLength == 0) {
                displayedText = ""
                onAnimationComplete()
                return@LaunchedEffect
            }

            // Dynamic chunking for a fast Gemini-like progressive display
            // Ensures the entire animation runs fast, completing in ~400ms max regardless of length
            val stepsCount = 35
            val calculatedChunkSize = (totalLength / stepsCount).coerceAtLeast(3)
            val delayMillis = 12L
            var currentIndex = 0
            while (currentIndex < totalLength) {
                currentIndex = (currentIndex + calculatedChunkSize).coerceAtMost(totalLength)
                displayedText = message.text.substring(0, currentIndex)
                delay(delayMillis)
            }
            onAnimationComplete()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // App guidelines met: NO visual avatar icon or circle bullet inside chat flow bubbles anymore.

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (isUser) {
                // User Bubble: dynamic background container with dynamic text colors
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = parseHexColor(colorUserBg, Color(0xFFD0BCFF))
                    ),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 0.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (message.isPinned) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = "Pinned",
                                    tint = Color(0xFF381E72),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ter-pin (Memori Kunci)",
                                    color = Color(0xFF381E72),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Render image attachments if embedded
                        if (message.attachmentType?.startsWith("image/") == true && message.attachmentBase64 != null) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .sizeIn(maxHeight = 200.dp, maxWidth = 200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                val decodedBytes = remember(message.attachmentBase64) {
                                    android.util.Base64.decode(message.attachmentBase64, android.util.Base64.DEFAULT)
                                }
                                AsyncImage(
                                    model = decodedBytes,
                                    contentDescription = "Attached Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Render plain text or markdown file visual badge in chat bubble
                        if ((message.attachmentType == "text/plain" || message.attachmentType == "text/markdown") && message.attachmentBase64 != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF381E72).copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (message.attachmentType == "text/markdown") Icons.Filled.Notes else Icons.Filled.Description,
                                            contentDescription = "Doc",
                                            tint = Color(0xFF381E72),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = message.attachmentName ?: "dokumen.txt",
                                            color = Color(0xFF381E72),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.attachmentBase64.take(120) + "...",
                                        color = Color(0xFF381E72).copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Render video file visual badge with extracted frame thumbnails in chat bubble
                        if (message.attachmentType?.startsWith("video/") == true && message.attachmentBase64 != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF381E72).copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Movie,
                                            contentDescription = "Video",
                                            tint = Color(0xFF381E72),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = message.attachmentName ?: "video.mp4",
                                                color = Color(0xFF381E72),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Video terlampir (Native Gemini Analysis)",
                                                color = Color(0xFF381E72).copy(alpha = 0.7f),
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                    
                                    val frames = remember(message.attachmentBase64) {
                                        val data = message.attachmentBase64 ?: ""
                                        if (data.contains("|||VIDEO_FILE_PATH|||")) {
                                            data.substringAfter("|||VIDEO_FILE_PATH|||").split("|||VIDEO_FRAME|||").filter { it.isNotBlank() }
                                        } else {
                                            data.split("|||VIDEO_FRAME|||").filter { it.isNotBlank() }
                                        }
                                    }
                                    if (frames.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(frames) { frameBase64 ->
                                                val decodedFrame = remember(frameBase64) {
                                                    try {
                                                        android.util.Base64.decode(frameBase64, android.util.Base64.DEFAULT)
                                                    } catch (e: Exception) {
                                                        null
                                                    }
                                                }
                                                if (decodedFrame != null) {
                                                    AsyncImage(
                                                        model = decodedFrame,
                                                        contentDescription = "Frame Preview",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(45.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isEditing) {
                            OutlinedTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_user_message_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF381E72),
                                    unfocusedTextColor = Color(0xFF381E72),
                                    focusedContainerColor = Color.White.copy(alpha = 0.95f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.95f),
                                    focusedBorderColor = Color(0xFF381E72),
                                    unfocusedBorderColor = Color(0xFF381E72).copy(alpha = 0.5f)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        editText = message.text
                                        isEditing = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF381E72))
                                ) {
                                    Text("Batal", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        if (editText.isNotBlank()) {
                                            focusManager.clearFocus()
                                            viewModel.editUserMessage(message, editText)
                                            isEditing = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF381E72),
                                        contentColor = Color(0xFFD0BCFF)
                                    )
                                ) {
                                    Text("Simpan", fontSize = 11.sp)
                                }
                            }
                        } else {
                            // Main user chat text
                            val parsedUserBg = parseHexColor(colorUserBg, Color(0xFFD0BCFF))
                            val isUserBgDark = (parsedUserBg.red * 0.299f + parsedUserBg.green * 0.587f + parsedUserBg.blue * 0.114f) < 0.5f
                            val defaultUserTextColor = if (isUserBgDark) Color(0xFFE6E1E5) else Color(0xFF381E72)
                            val defaultUserActionColor = if (isUserBgDark) Color(0xFFFF79C6) else Color(0xFF381E72).copy(alpha = 0.8f)
                            val defaultUserThoughtColor = if (isUserBgDark) Color(0xFFD0BCFF) else Color(0xFF6200EE)

                            Text(
                                text = highlightAnnotatedString(
                                    annotated = formatRoleplayText(
                                        text = message.text,
                                        dialogueColor = parseHexColor(colorDialogue, defaultUserActionColor),
                                        thoughtColor = parseHexColor(colorThought, defaultUserThoughtColor),
                                        narrationColor = parseHexColor(colorNarration, defaultUserTextColor)
                                    ),
                                    query = searchQuery
                                ),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // Render inline video player if video links are present in user message text
                            val userVideoUrls = remember(message.text) { extractVideoUrls(message.text) }
                            userVideoUrls.forEach { videoUrl ->
                                Spacer(modifier = Modifier.height(8.dp))
                                VideoPlayer(
                                    videoUrl = videoUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }

                            // Render general web link sniffer if generic URLs are present
                            val userWebUrls = remember(message.text) { extractGenericWebUrls(message.text) }
                            userWebUrls.forEach { webUrl ->
                                Spacer(modifier = Modifier.height(8.dp))
                                LinkSnifferWidget(
                                    webUrl = webUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Inline click to edit response text trigger overlay
                if (!isEditing) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { isEditing = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Respon",
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF938F99)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Respon", color = Color(0xFF938F99), fontSize = 10.sp)
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { viewModel.toggleMessagePin(message) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = if (message.isPinned) "Lepas Pin" else "Pin Memori",
                                modifier = Modifier.size(12.dp),
                                tint = if (message.isPinned) Color(0xFFFFD54F) else Color(0xFF938F99)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { viewModel.deleteMessageAndSubsequent(message) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Hapus Pesan",
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFF48FB1)
                            )
                        }
                    }
                }
            } else {
                // AI Bubble: dynamic container background with dynamic accent colors
                Column {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = parseHexColor(colorAiBg, Color(0xFF2B2930))
                        ),
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        border = BorderStroke(0.5.dp, Color(0xFF49454F)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    val strokeWidth = 3.dp.toPx()
                                    drawLine(
                                        color = parseHexColor(colorDialogue, Color(0xFFD0BCFF)),
                                        start = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, size.height),
                                        strokeWidth = strokeWidth
                                    )
                                }
                                .padding(start = 14.dp)
                                .padding(top = 14.dp, bottom = 14.dp, end = 14.dp)
                        ) {
                            // AI Character Title badge header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = "$senderNameText • ${character?.personality ?: "Grup Persona"}",
                                        color = parseHexColor(colorDialogue, Color(0xFFD0BCFF)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (message.isPinned) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = Color(0xFFFFD54F).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, Color(0xFFFFD54F))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = Color(0xFFFFD54F),
                                                    modifier = Modifier.size(9.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "PINNED",
                                                    color = Color(0xFFFFD54F),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }

                                val (header, actualStory) = remember(parsedData.storyContent) {
                                    splitHeaderAndStory(parsedData.storyContent)
                                }

                                if (header != null) {
                                    Surface(
                                        color = Color(0xFF381E72).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f)),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AccessTime,
                                                contentDescription = "Day Icon",
                                                tint = Color(0xFFF48FB1),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = header,
                                                color = Color(0xFFE6E1E5),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editText,
                                        onValueChange = { editText = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("edit_ai_message_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFFE6E1E5),
                                            unfocusedTextColor = Color(0xFFE6E1E5),
                                            focusedContainerColor = Color(0xFF1D1B20),
                                            unfocusedContainerColor = Color(0xFF1D1B20),
                                            focusedBorderColor = Color(0xFFD0BCFF),
                                            unfocusedBorderColor = Color(0xFFD0BCFF).copy(alpha = 0.5f)
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = {
                                                focusManager.clearFocus()
                                                editText = parsedData.storyContent
                                                isEditing = false
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD0BCFF))
                                        ) {
                                            Text("Batal", fontSize = 11.sp)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Button(
                                            onClick = {
                                                if (editText.isNotBlank()) {
                                                    focusManager.clearFocus()
                                                    val originalRaw = message.text
                                                    val startTag = "===START_TRACKER==="
                                                    val endTag = "===END_TRACKER==="
                                                    
                                                    val trackerPart = if (originalRaw.contains(startTag, ignoreCase = true)) {
                                                        val start = originalRaw.indexOf(startTag, ignoreCase = true)
                                                        val end = originalRaw.indexOf(endTag, ignoreCase = true)
                                                        if (end > start) {
                                                            originalRaw.substring(start, end + endTag.length)
                                                        } else {
                                                            originalRaw.substring(start)
                                                        }
                                                    } else ""
                                                    
                                                    val optionsPart = when {
                                                        originalRaw.contains("===NEXT_OPTION===", ignoreCase = true) -> {
                                                            val idx = originalRaw.indexOf("===NEXT_OPTION===", ignoreCase = true)
                                                            originalRaw.substring(idx)
                                                        }
                                                        originalRaw.contains("Pilihan Lanjutan Cerita:", ignoreCase = true) -> {
                                                            val idx = originalRaw.indexOf("Pilihan Lanjutan Cerita:", ignoreCase = true)
                                                            originalRaw.substring(idx)
                                                        }
                                                        else -> ""
                                                     }
                                                     
                                                    var finalRawText = editText.trim()
                                                    if (trackerPart.isNotEmpty()) {
                                                        finalRawText += "\n\n" + trackerPart
                                                    }
                                                    if (optionsPart.isNotEmpty()) {
                                                        finalRawText += "\n\n" + optionsPart
                                                    }
                                                    
                                                    viewModel.editModelMessage(message, finalRawText)
                                                    isEditing = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD0BCFF),
                                                contentColor = Color(0xFF381E72)
                                            )
                                        ) {
                                            Text("Simpan", fontSize = 11.sp)
                                        }
                                    }
                                } else {
                                    // Main chat text inside AI bubble formatted for roleplay asterisks!
                                    val parsedAiBg = parseHexColor(colorAiBg, Color(0xFF2B2930))
                                    val isAiBgDark = (parsedAiBg.red * 0.299f + parsedAiBg.green * 0.587f + parsedAiBg.blue * 0.114f) < 0.5f
                                    val defaultAiTextColor = if (isAiBgDark) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
                                    val defaultAiActionColor = if (isAiBgDark) Color(0xFFFF79C6) else Color(0xFF8E24AA)
                                    val defaultAiThoughtColor = if (isAiBgDark) Color(0xFFD0BCFF) else Color(0xFF6200EE)

                                    Text(
                                        text = highlightAnnotatedString(
                                            annotated = formatRoleplayText(
                                                text = header?.let { actualStory } ?: parsedData.storyContent,
                                                dialogueColor = parseHexColor(colorDialogue, defaultAiActionColor),
                                                thoughtColor = parseHexColor(colorThought, defaultAiThoughtColor),
                                                narrationColor = parseHexColor(colorNarration, defaultAiTextColor)
                                            ),
                                            query = searchQuery
                                        ),
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp
                                    )

                                    // Render inline video player if video links are present in AI response text
                                    val aiVideoUrls = remember(message.text) { extractVideoUrls(message.text) }
                                    aiVideoUrls.forEach { videoUrl ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        VideoPlayer(
                                            videoUrl = videoUrl,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        )
                                    }

                                    // Render general web link sniffer if generic URLs are present
                                    val aiWebUrls = remember(message.text) { extractGenericWebUrls(message.text) }
                                    aiWebUrls.forEach { webUrl ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinkSnifferWidget(
                                            webUrl = webUrl,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        )
                                    }
                                }

                                // Render AI generated image attachments if embedded
                                if (message.attachmentType?.startsWith("image/") == true && message.attachmentBase64 != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .sizeIn(maxHeight = 320.dp, maxWidth = 320.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    ) {
                                        val base64 = message.attachmentBase64
                                        if (base64.startsWith("/") || base64.startsWith("http")) {
                                            SubcomposeAsyncImage(
                                                model = base64,
                                                contentDescription = "AI Generated Image",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                val state = painter.state
                                                if (state is AsyncImagePainter.State.Loading) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(180.dp),
                                                        contentAlignment = Alignment.Center
                                                     ) {
                                                        CircularProgressIndicator(
                                                            color = Color(0xFFD0BCFF),
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                     }
                                                } else if (state is AsyncImagePainter.State.Error) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(100.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "Gagal memuat visual",
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                } else {
                                                    SubcomposeAsyncImageContent()
                                                }
                                            }
                                        } else {
                                            val decodedBytes = remember(base64) {
                                                try {
                                                     android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                                } catch (e: Exception) {
                                                     null
                                                }
                                            }
                                            if (decodedBytes != null) {
                                                AsyncImage(
                                                    model = decodedBytes,
                                                    contentDescription = "AI Generated Image",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Options and Tracker dropdown widget below the card
                    val fullParsedData = remember(message.text) { message.text.parseAiResponse() }
                    val animationDone = !shouldAnimateText || displayedText.length >= message.text.length

                    val isLastModelMessage = remember(allMessages, message) {
                        allMessages.lastOrNull { it.role == "model" }?.id == message.id
                    }
                    var hasClickedOption by remember(message.id) { mutableStateOf(false) }

                    if (animationDone && (fullParsedData.options.isNotEmpty() || fullParsedData.trackerRawText.isNotEmpty())) {
                        StoryTrackerAndOptionsWidget(
                            parsedData = fullParsedData,
                            showOptions = isLastModelMessage && !hasClickedOption,
                            onOptionSelected = { optionText ->
                                hasClickedOption = true
                                if (optionText.contains("Kustom", ignoreCase = true) || optionText.contains("Ketik sendiri", ignoreCase = true)) {
                                    // Let them type in the input bar
                                } else {
                                    viewModel.sendMessage(optionText)
                                }
                            },
                            onFileClicked = { fileName ->
                                var match = allMessages.findLast { msg ->
                                    val cleanMsgName = msg.attachmentName?.substringBeforeLast(".")?.trim() ?: ""
                                    val cleanFileName = fileName.substringBeforeLast(".")
                                        .replace("Foto Referensi", "", ignoreCase = true)
                                        .replace("Foto", "", ignoreCase = true)
                                        .replace("Gambar", "", ignoreCase = true)
                                        .trim()
                                    
                                    val isExactMatch = msg.attachmentName?.equals(fileName, ignoreCase = true) == true
                                    val isFuzzyMatch = cleanMsgName.isNotEmpty() && cleanFileName.isNotEmpty() && (
                                        cleanMsgName.contains(cleanFileName, ignoreCase = true) ||
                                        cleanFileName.contains(cleanMsgName, ignoreCase = true)
                                    )
                                    (isExactMatch || isFuzzyMatch) && msg.attachmentBase64 != null
                                }
                                if (match == null && (
                                    fileName.endsWith(".jpg", true) ||
                                    fileName.endsWith(".jpeg", true) ||
                                    fileName.endsWith(".png", true) ||
                                    fileName.endsWith(".webp", true) ||
                                    fileName.contains("foto", ignoreCase = true) ||
                                    fileName.contains("baru", ignoreCase = true) ||
                                    fileName.contains("referensi", ignoreCase = true) ||
                                    fileName.contains("gambar", ignoreCase = true) ||
                                    fileName.contains("image", ignoreCase = true) ||
                                    fileName.contains("pic", ignoreCase = true)
                                )) {
                                    // Try last user uploaded image first
                                    match = allMessages.lastOrNull { msg ->
                                        msg.role == "user" &&
                                        msg.attachmentType?.startsWith("image/") == true &&
                                        msg.attachmentBase64 != null
                                    }
                                    // Fallback to any image in chat
                                    if (match == null) {
                                        match = allMessages.lastOrNull { msg ->
                                            msg.attachmentType?.startsWith("image/") == true &&
                                            msg.attachmentBase64 != null
                                        }
                                    }
                                }
                                if (match != null) {
                                    onViewFile(fileName, match.attachmentBase64 ?: "")
                                } else {
                                    val isImgFile = fileName.endsWith(".jpg", true) ||
                                                    fileName.endsWith(".jpeg", true) ||
                                                    fileName.endsWith(".png", true) ||
                                                    fileName.endsWith(".webp", true)
                                    val multiMatch = allMessages.findLast { msg ->
                                        (msg.attachmentType == "text/plain" || msg.attachmentType == "text/markdown") &&
                                        msg.attachmentBase64 != null &&
                                        (
                                            (!isImgFile && msg.attachmentName?.contains(fileName, ignoreCase = true) == true) ||
                                            msg.attachmentBase64.contains("---START_FILE: $fileName---", ignoreCase = true)
                                        )
                                    }
                                    if (multiMatch != null) {
                                        val extracted = extractFileFromConcatenatedContent(multiMatch.attachmentBase64 ?: "", fileName)
                                        onViewFile(fileName, extracted)
                                    } else {
                                        onViewFile(fileName, "Konten file '$fileName' tidak ditemukan dalam lampiran pesan aktif saat ini.")
                                    }
                                }
                            },
                            allMessages = allMessages
                        )
                    }

                    // Regenerate AI Response feedback buttons row
                    if (!isEditing) {
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.regenerateAiResponse(message) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Retry Respon",
                                    modifier = Modifier.size(12.dp), // matched size
                                    tint = Color(0xFFD0BCFF)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry Respon", color = Color(0xFFD0BCFF), fontSize = 10.sp)
                            }

                            // Pin / Unpin Button
                            IconButton(
                                onClick = { viewModel.toggleMessagePin(message) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = if (message.isPinned) "Lepas Pin" else "Pin Memori",
                                    modifier = Modifier.size(12.dp),
                                    tint = if (message.isPinned) Color(0xFFFFD54F) else Color(0xFF938F99)
                                )
                            }

                            // Edit Response Button (Just Icon)
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Respon",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFD0BCFF)
                                )
                            }

                            // Copy Response Button (Just Icon)
                            IconButton(
                                onClick = {
                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clipData = android.content.ClipData.newPlainText("Roleplay Response", message.text)
                                    clipboardManager.setPrimaryClip(clipData)
                                    android.widget.Toast.makeText(context, "Respon disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Salin Respon",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFD0BCFF)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

// Typing spinner row component (Custom stylized representation without avatars in flow)
@Composable
fun TypingIndicatorRow(character: CharacterEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            border = BorderStroke(0.5.dp, Color(0xFF49454F))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "dots")

                val dot1Alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot1"
                )

                val dot2Alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 150, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot2"
                )

                val dot3Alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 300, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot3"
                )

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0BCFF).copy(alpha = dot1Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0BCFF).copy(alpha = dot2Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD0BCFF).copy(alpha = dot3Alpha))
                )
            }
        }
    }
}

@Composable
fun ColorWheel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    brightness: Float = 1f
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    
    val hsv = remember(selectedColor) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toArgb(), arr)
        arr
    }
    val hue = hsv[0]
    val saturation = hsv[1]

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.85f)
            .onSizeChanged { size = it }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(brightness) {
                    fun handleTouchEvent(offset: Offset) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)
                        val maxRadius = minOf(centerX, centerY)
                        
                        if (maxRadius > 0f) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) {
                                angle += 360f
                            }
                            
                            val sat = (distance / maxRadius).coerceIn(0f, 1f)
                            val argb = android.graphics.Color.HSVToColor(floatArrayOf(angle, sat, brightness))
                            onColorSelected(Color(argb))
                        }
                    }

                    detectTapGestures(
                        onPress = { offset ->
                            handleTouchEvent(offset)
                        }
                    )
                }
                .pointerInput(brightness) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)
                        val maxRadius = minOf(centerX, centerY)
                        
                        if (maxRadius > 0f) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) {
                                angle += 360f
                            }
                            
                            val sat = (distance / maxRadius).coerceIn(0f, 1f)
                            val argb = android.graphics.Color.HSVToColor(floatArrayOf(angle, sat, brightness))
                            onColorSelected(Color(argb))
                        }
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(centerX, centerY)
            
            if (radius > 0) {
                val colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
                drawCircle(
                    brush = Brush.sweepGradient(colors, center = Offset(centerX, centerY)),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )

                if (brightness < 1f) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 1f - brightness),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                }
                
                val angleRad = Math.toRadians(hue.toDouble())
                val satDistance = saturation * radius
                val pointerX = centerX + (satDistance * cos(angleRad)).toFloat()
                val pointerY = centerY + (satDistance * sin(angleRad)).toFloat()
                
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = Offset(pointerX, pointerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = selectedColor,
                    radius = 9.dp.toPx(),
                    center = Offset(pointerX, pointerY)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = 13.5.dp.toPx(),
                    center = Offset(pointerX, pointerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun BrightnessSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kecerahan (Brightness)",
                color = Color(0xFFE6E1E5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = Color(0xFFD0BCFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFD0BCFF),
                activeTrackColor = Color(0xFFD0BCFF),
                inactiveTrackColor = Color(0xFF49454F)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ------ SETTINGS LAYER / CONFIG CONTROL SCREEN ------

private fun maskApiKey(key: String): String {
    return if (key.length > 10) {
        "${key.take(6)}••••${key.takeLast(4)}"
    } else {
        "••••"
    }
}

/*
        "�@OptIn(ExperimentalMaterial3Api::class)
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: RoleplayViewModel,
    userApiKey: String,
    savedApiKeys: List<com.example.data.database.ApiKeyEntity>,
    groqApiKey: String,
    savedGroqApiKeys: List<com.example.data.database.ApiKeyEntity>,
    zaiApiKey: String,
    savedZaiApiKeys: List<com.example.data.database.ApiKeyEntity>,
    activeModel: String,
    customInstruction: String,
    dailyUsage: Int,
    weeklyUsage: Int,
    videoMaxFrames: Int,
    videoFrameIntervalMs: Long,
    camouflageEnabled: Boolean,
    onCamouflageChanged: (Boolean) -> Unit,
    onApiKeySaved: (String) -> Unit,
    onApiKeySelected: (String) -> Unit,
    onApiKeyDeleted: (String) -> Unit,
    onGroqApiKeySaved: (String) -> Unit,
    onGroqApiKeySelected: (String) -> Unit,
    onGroqApiKeyDeleted: (String) -> Unit,
    onZaiApiKeySaved: (String) -> Unit,
    onZaiApiKeySelected: (String) -> Unit,
    onZaiApiKeyDeleted: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onCustomInstructionSaved: (String) -> Unit,
    onVideoMaxFramesChanged: (Int) -> Unit,
    onVideoFrameIntervalMsChanged: (Long) -> Unit,
    onCheckUsage: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val colorDialogue by viewModel.colorDialogue.collectAsStateWithLifecycle()
    val colorThought by viewModel.colorThought.collectAsStateWithLifecycle()
    val colorNarration by viewModel.colorNarration.collectAsStateWithLifecycle()
    val colorAiBg by viewModel.colorAiBackground.collectAsStateWithLifecycle()
    val colorUserBg by viewModel.colorUserBackground.collectAsStateWithLifecycle()
    val colorGeneralBg by viewModel.colorGeneralBackground.collectAsStateWithLifecycle()

    var apiKeyText by remember { mutableStateOf("") }
    var groqApiKeyText by remember { mutableStateOf("") }
    var zaiApiKeyText by remember { mutableStateOf("") }
    var instructionText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isGroqPasswordVisible by remember { mutableStateOf(false) }
    var isZaiPasswordVisible by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var activeKeyDropdownExpanded by remember { mutableStateOf(false) }
    var activeGroqKeyDropdownExpanded by remember { mutableStateOf(false) }
    var activeZaiKeyDropdownExpanded by remember { mutableStateOf(false) }
    var providerDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showCamouflageInfoDialog by remember { mutableStateOf(false) }
    var showPrivacyInfoDialog by remember { mutableStateOf(false) }
    var isSystemPromptExpanded by remember { mutableStateOf(false) }

    val isCurrentGemini = activeModel.startsWith("gemini-")
    val isCurrentZai = activeModel.startsWith("glm-")
    var selectedProvider by remember(activeModel) { 
        mutableStateOf(if (isCurrentGemini) "gemini" else if (isCurrentZai) "zai" else "groq") 
    }

    val geminiModels = listOf(
        "gemini-3.5-flash" to "Dreamini 3 Pro (Tercepat & Default)",
        "gemini-3.1-pro-preview" to "Dreamini 3.1 Pro Preview",
        "gemini-3-flash-preview" to "Dreamini 3 Flash Preview",
        "gemini-3.1-flash-lite" to "Dreamini 3.1 Flash Lite",
        "gemini-2.5-flash" to "Dreamini Pro (Versi Stabil)"
    )

    val groqModels = listOf(
        "llama-3.3-70b-versatile" to "Llama 3.3 70B (Groq Fast)",
        "llama-3.1-8b-instant" to "Llama 3.1 8B",
        "meta-llama/llama-4-scout-17b-16e-instruct" to "Llama 4 Scout 17B 16E",
        "groq/compound" to "Compound",
        "groq/compound-mini" to "Compound Mini",
        "qwen/qwen3-32b" to "Qwen3 32B",
        "qwen/qwen3.6-27b" to "Qwen 3.6 27B"
    )

    val currentModelsList = when (selectedProvider) {
        "gemini" -> geminiModels
        else -> groqModels
    }
    val activeModelDisplayName = (geminiModels + groqModels).find { it.first == activeModel }?.second ?: activeModel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_pane")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onOpenDrawer
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Buka Menu",
                    tint = Color(0xFFD0BCFF)
                )
            }
            Column {
                Text(
                    text = "Setelan API & Model",
                    color = Color(0xFFD0BCFF),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Konfigurasi kecerdasan buatan & rincian kuota",
                    color = Color(0xFFFF79C6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- Active Provider Selection Dropdown ---
        Text(
            text = "Pilih Penyedia Layanan (AI Provider):",
            color = Color(0xFFE6E1E5),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { providerDropdownExpanded = !providerDropdownExpanded }
                    .testTag("provider_dropdown"),
                colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1C1B1F)),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (selectedProvider) {
                                "gemini" -> Icons.Filled.Cloud
                                "zai" -> Icons.Filled.Star
                                else -> Icons.Filled.Bolt
                            },
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedProvider) {
                                "gemini" -> "Google Gemini"
                                "zai" -> "Zhipu AI (GLM-4)"
                                else -> "Groq Cloud (Llama 3.3, dsb.)"
                            },
                            color = Color(0xFFE6E1E5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = if (providerDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = "Pilih",
                        tint = Color(0xFFD0BCFF)
                    )
                }
            }

            DropdownMenu(
                expanded = providerDropdownExpanded,
                onDismissRequest = { providerDropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B2930))
                    .border(1.dp, Color(0xFF49454F))
            ) {
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Cloud, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Gemini", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        selectedProvider = "gemini"
                        providerDropdownExpanded = false
                        if (!activeModel.startsWith("gemini-")) {
                            onModelChanged("gemini-3.5-flash")
                        }
                    }
                )
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Groq Cloud (Llama, Gemma, Qwen)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        selectedProvider = "groq"
                        providerDropdownExpanded = false
                        if (activeModel.startsWith("gemini-")) {
                            onModelChanged("llama-3.3-70b-versatile")
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Provider-Specific Configuration Card ---
        if (selectedProvider == "gemini") {
            // Google Gemini Section Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Akses Google Gemini (API Key)",
                        color = Color(0xFFE6E1E5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gunakan Google Gemini API Key pribadi Anda untuk mendelegasikan chat langsung ke server Google.",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Sleek Modern Minimized Privacy Info Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF381E72).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "Data Privacy",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Kebijakan & Keamanan Privasi",
                                color = Color(0xFFE6E1E5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(
                            onClick = { showPrivacyInfoDialog = true },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Detail ↗", color = Color(0xFFD0BCFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sleek Modern Minimized Camouflage Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF381E72).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Camouflage Mode",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Kamuflase Obfuscation (Anti-Filter)",
                                color = Color(0xFFE6E1E5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { showCamouflageInfoDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Help,
                                    contentDescription = "Info Kamuflase",
                                    tint = Color(0xFFD0BCFF).copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Switch(
                                checked = camouflageEnabled,
                                onCheckedChange = onCamouflageChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD0BCFF),
                                    checkedTrackColor = Color(0xFF381E72),
                                    uncheckedThumbColor = Color(0xFF938F99),
                                    uncheckedTrackColor = Color(0xFF49454F)
                                ),
                                modifier = Modifier.testTag("camouflage_toggle")
                            )
                        }
                    }

                    // Dialog Popups
                    if (showPrivacyInfoDialog) {
                        AlertDialog(
                            onDismissRequest = { showPrivacyInfoDialog = false },
                            icon = { Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFFD0BCFF)) },
                            title = {
                                Text(
                                    text = "Kebijakan & Keamanan Privasi",
                                    color = Color(0xFFE6E1E5),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "⚠️ Google AI Studio (Tingkat Gratis / Free Tier) secara otomatis memindai dan mengirimkan percakapan untuk peninjauan manusia serta melatih model Google. Hal ini dapat memicu peringatan kebijakan otomatis apabila roleplay Anda mengandung konten kekerasan (gore) atau romansa sensitif.",
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = "🔒 CARA OPT-OUT (100% PRIVAT & AMAN):\nUntuk menghentikan pengiriman data ke Google untuk melatih model dan menonaktifkan peninjauan manual, aktifkanlah fitur 'Billing' (Pay-as-you-go / Berbayar) pada akun Google AI Studio Anda. Pada tingkat Berbayar, Google secara hukum berkomitmen tidak akan menggunakan data Anda untuk melatih model mereka. Anda juga bisa menggunakan Groq Cloud yang tidak menggunakannya untuk pelatihan.",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showPrivacyInfoDialog = false }) {
                                    Text("Mengerti", color = Color(0xFFD0BCFF))
                                }
                            },
                            containerColor = Color(0xFF2B2930)
                        )
                    }

                    if (showCamouflageInfoDialog) {
                        AlertDialog(
                            onDismissRequest = { showCamouflageInfoDialog = false },
                            icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFD0BCFF)) },
                            title = {
                                Text(
                                    text = "Kamuflase Obfuscation (Anti-Filter)",
                                    color = Color(0xFFE6E1E5),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "💡 CARA KERJA: Sistem akan otomatis menyamarkan kata sensitif saat dikirim ke API (misalnya mengubah 'darah' menjadi 'krimson', 'bunuh' menjadi 'eliminasi') agar lolos dari deteksi otomatis Google/Groq.\n\nSaat balasan dari AI diterima, kata-kata tersebut akan diterjemahkan kembali ke kata aslinya secara transparan di layar Anda.\n\n100% aman untuk petualangan fiktif yang intens dan berdarah-darah!",
                                    color = Color(0xFFE6E1E5),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { showCamouflageInfoDialog = false }) {
                                    Text("Mengerti", color = Color(0xFFD0BCFF))
                                }
                            },
                            containerColor = Color(0xFF2B2930)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- Active Key Picker Dropdown ---
                    Text(
                        text = "Kunci API Terpilih (Aktif):",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeKeyDropdownExpanded = !activeKeyDropdownExpanded }
                                .testTag("active_api_key_dropdown"),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1C1B1F)),
                            border = BorderStroke(1.dp, Color(0xFF49454F))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.VpnKey,
                                        contentDescription = null,
                                        tint = if (userApiKey.isNotEmpty()) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (userApiKey.isNotEmpty()) {
                                            "Kunci API Pribadi (${maskApiKey(userApiKey)})"
                                        } else {
                                            "Kunci API Sistem Default (Gratis)"
                                        },
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = if (activeKeyDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    tint = Color(0xFFD0BCFF)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = activeKeyDropdownExpanded,
                            onDismissRequest = { activeKeyDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2B2930))
                                .border(1.dp, Color(0xFF49454F))
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Cloud, contentDescription = null, tint = Color(0xFF938F99), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Kunci API Sistem Default (Gratis)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    onApiKeySelected("")
                                    activeKeyDropdownExpanded = false
                                    Toast.makeText(context, "Beralih menggunakan Kunci Sistem Default.", Toast.LENGTH_SHORT).show()
                                }
                            )

                            savedApiKeys.forEachIndexed { index, apiKeyEntity ->
                                val key = apiKeyEntity.apiKey
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.VpnKey,
                                                    contentDescription = null,
                                                    tint = if (key == userApiKey) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Kunci ${index + 1}: ${maskApiKey(key)}",
                                                        color = Color(0xFFE6E1E5),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(
                                                                    color = if (apiKeyEntity.isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935),
                                                                    shape = androidx.compose.foundation.shape.CircleShape
                                                                )
                                                        )
                                                        Text(
                                                            text = if (apiKeyEntity.isAvailable) "Tersedia" else "Limit Penuh / Terblokir",
                                                            color = if (apiKeyEntity.isAvailable) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontSize = 10.sp
                                                        )
                                                        if (key == userApiKey || apiKeyEntity.isUsed) {
                                                            Text(
                                                                text = "• Aktif",
                                                                color = Color(0xFFD0BCFF),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    onApiKeyDeleted(key)
                                                    Toast.makeText(context, "Kunci ${index + 1} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Hapus",
                                                    tint = Color(0xFFF2B8B5),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onApiKeySelected(key)
                                        activeKeyDropdownExpanded = false
                                        Toast.makeText(context, "Kunci API ${index + 1} diaktifkan!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Text Box to insert a new API Key
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("Tambahkan Kunci API Baru") },
                        placeholder = { Text("Paste Kunci API baru di sini...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_text_field"),
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF938F99)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedLabelColor = Color(0xFFD0BCFF),
                            unfocusedLabelColor = Color(0xFF938F99)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val trimmed = apiKeyText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onApiKeySaved(trimmed)
                                    apiKeyText = "" // Auto-clear text box on save!
                                    Toast.makeText(context, "Kunci API berhasil disimpan secara lokal!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Silakan isi Kunci API terlebih dahulu sebelum menyimpan.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Simpan", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan & Aktifkan")
                        }
                    }
                }
            }
        } else if (selectedProvider == "zai") {
            // Zhipu AI / Z.ai Section Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Akses Zhipu AI / Z.ai (GLM)",
                        color = Color(0xFFE6E1E5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gunakan Kunci API Zhipu AI (GLM) Anda sendiri untuk menjalankan model mutakhir GLM-4.",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    // --- Active Zai Key Picker Dropdown ---
                    Text(
                        text = "Kunci Z.ai Terpilih (Aktif):",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeZaiKeyDropdownExpanded = !activeZaiKeyDropdownExpanded }
                                .testTag("active_zai_api_key_dropdown"),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1C1B1F)),
                            border = BorderStroke(1.dp, Color(0xFF49454F))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.VpnKey,
                                        contentDescription = null,
                                        tint = if (zaiApiKey.isNotEmpty()) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (zaiApiKey.isNotEmpty()) {
                                            "Kunci Z.ai Pribadi (${maskApiKey(zaiApiKey)})"
                                        } else {
                                            "Kunci Z.ai Sistem Default (Gratis)"
                                        },
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = if (activeZaiKeyDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    tint = Color(0xFFD0BCFF)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = activeZaiKeyDropdownExpanded,
                            onDismissRequest = { activeZaiKeyDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2B2930))
                                .border(1.dp, Color(0xFF49454F))
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF938F99), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Kunci Z.ai Sistem Default (Gratis)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    onZaiApiKeySelected("")
                                    activeZaiKeyDropdownExpanded = false
                                    Toast.makeText(context, "Beralih menggunakan Kunci Sistem Default.", Toast.LENGTH_SHORT).show()
                                }
                            )

                            savedZaiApiKeys.forEachIndexed { index, apiKeyEntity ->
                                val key = apiKeyEntity.apiKey
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.VpnKey,
                                                    contentDescription = null,
                                                    tint = if (key == zaiApiKey) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Kunci Z.ai ${index + 1}: ${maskApiKey(key)}",
                                                        color = Color(0xFFE6E1E5),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(
                                                                    color = if (apiKeyEntity.isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935),
                                                                    shape = androidx.compose.foundation.shape.CircleShape
                                                                )
                                                        )
                                                        Text(
                                                            text = if (apiKeyEntity.isAvailable) "Tersedia" else "Limit Penuh / Terblokir",
                                                            color = if (apiKeyEntity.isAvailable) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontSize = 10.sp
                                                        )
                                                        if (key == zaiApiKey || apiKeyEntity.isUsed) {
                                                            Text(
                                                                text = "• Aktif",
                                                                color = Color(0xFFD0BCFF),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    onZaiApiKeyDeleted(key)
                                                    Toast.makeText(context, "Kunci Z.ai ${index + 1} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Hapus",
                                                    tint = Color(0xFFF2B8B5),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onZaiApiKeySelected(key)
                                        activeZaiKeyDropdownExpanded = false
                                        Toast.makeText(context, "Kunci Z.ai ${index + 1} diaktifkan!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Text Box to insert a new API Key
                    OutlinedTextField(
                        value = zaiApiKeyText,
                        onValueChange = { zaiApiKeyText = it },
                        label = { Text("Tambahkan Kunci Z.ai Baru") },
                        placeholder = { Text("Paste Kunci Zhipu/Z.ai di sini...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zai_api_key_input"),
                        visualTransformation = if (isZaiPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isZaiPasswordVisible = !isZaiPasswordVisible }) {
                                Icon(
                                    if (isZaiPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF938F99)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedLabelColor = Color(0xFFD0BCFF),
                            unfocusedLabelColor = Color(0xFF938F99)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val trimmed = zaiApiKeyText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onZaiApiKeySaved(trimmed)
                                    zaiApiKeyText = "" // Auto-clear text box on save!
                                    Toast.makeText(context, "Kunci Z.ai berhasil disimpan secara lokal!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Silakan isi Kunci Z.ai terlebih dahulu sebelum menyimpan.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            modifier = Modifier.testTag("save_zai_api_key_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Simpan", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Kunci Z.ai")
                        }
                    }
                }
            }
        } else {
            // Groq Cloud Section Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Akses Groq Cloud (API Key)",
                        color = Color(0xFFE6E1E5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gunakan Kunci API Groq (gsk_...) Anda sendiri untuk menjalankan model non-Gemini seperti Llama 3.3 secara berkecepatan tinggi.",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    // --- Active Groq Key Picker Dropdown ---
                    Text(
                        text = "Kunci Groq Terpilih (Aktif):",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeGroqKeyDropdownExpanded = !activeGroqKeyDropdownExpanded }
                                .testTag("active_groq_api_key_dropdown"),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1C1B1F)),
                            border = BorderStroke(1.dp, Color(0xFF49454F))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.VpnKey,
                                        contentDescription = null,
                                        tint = if (groqApiKey.isNotEmpty()) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (groqApiKey.isNotEmpty()) {
                                            "Kunci Groq Pribadi (${maskApiKey(groqApiKey)})"
                                        } else {
                                            "Kunci Groq Sistem Default (Gratis)"
                                        },
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = if (activeGroqKeyDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    tint = Color(0xFFD0BCFF)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = activeGroqKeyDropdownExpanded,
                            onDismissRequest = { activeGroqKeyDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2B2930))
                                .border(1.dp, Color(0xFF49454F))
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color(0xFF938F99), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Kunci Groq Sistem Default (Gratis)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    onGroqApiKeySelected("")
                                    activeGroqKeyDropdownExpanded = false
                                    Toast.makeText(context, "Beralih menggunakan Kunci Sistem Default.", Toast.LENGTH_SHORT).show()
                                }
                            )

                            savedGroqApiKeys.forEachIndexed { index, apiKeyEntity ->
                                val key = apiKeyEntity.apiKey
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.VpnKey,
                                                    contentDescription = null,
                                                    tint = if (key == groqApiKey) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Kunci Groq ${index + 1}: ${maskApiKey(key)}",
                                                        color = Color(0xFFE6E1E5),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(
                                                                    color = if (apiKeyEntity.isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935),
                                                                    shape = androidx.compose.foundation.shape.CircleShape
                                                                )
                                                        )
                                                        Text(
                                                            text = if (apiKeyEntity.isAvailable) "Tersedia" else "Limit Penuh / Terblokir",
                                                            color = if (apiKeyEntity.isAvailable) Color(0xFF81C784) else Color(0xFFE57373),
                                                            fontSize = 10.sp
                                                        )
                                                        if (key == groqApiKey || apiKeyEntity.isUsed) {
                                                            Text(
                                                                text = "• Aktif",
                                                                color = Color(0xFFD0BCFF),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    onGroqApiKeyDeleted(key)
                                                    Toast.makeText(context, "Kunci Groq ${index + 1} berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Hapus",
                                                    tint = Color(0xFFF2B8B5),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onGroqApiKeySelected(key)
                                        activeGroqKeyDropdownExpanded = false
                                        Toast.makeText(context, "Kunci Groq ${index + 1} diaktifkan!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Text Box to insert a new API Key
                    OutlinedTextField(
                        value = groqApiKeyText,
                        onValueChange = { groqApiKeyText = it },
                        label = { Text("Tambahkan Kunci Groq Baru") },
                        placeholder = { Text("gsk_...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("groq_api_key_input"),
                        visualTransformation = if (isGroqPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isGroqPasswordVisible = !isGroqPasswordVisible }) {
                                Icon(
                                    if (isGroqPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF938F99)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6E1E5),
                            unfocusedTextColor = Color(0xFFE6E1E5),
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedLabelColor = Color(0xFFD0BCFF),
                            unfocusedLabelColor = Color(0xFF938F99)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val trimmed = groqApiKeyText.trim()
                                if (trimmed.isNotEmpty()) {
                                    if (!trimmed.startsWith("gsk_")) {
                                        Toast.makeText(context, "⚠️ Kunci Groq harusnya diawali 'gsk_'. Mohon cek kembali jika error.", Toast.LENGTH_LONG).show()
                                    }
                                    onGroqApiKeySaved(trimmed)
                                    groqApiKeyText = "" // Auto-clear text box on save!
                                    Toast.makeText(context, "Kunci Groq berhasil disimpan secara lokal!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Silakan isi Kunci Groq terlebih dahulu sebelum menyimpan.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            modifier = Modifier.testTag("save_groq_api_key_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Simpan", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Kunci Groq")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Model Selector Card (Dynamic based on selected Provider)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Model Kecerdasan AI Aktif",
                    color = Color(0xFFE6E1E5),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih model yang tepat dengan kebutuhan interaksi Anda (Pro/Flash).",
                    color = Color(0xFF938F99),
                    fontSize = 11.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Model Dropdown using standard Compose ExposedDropdownMenuBox style
                Box {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDropdown = !expandedDropdown }
                            .testTag("model_dropdown"),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1C1B1F)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF49454F), Color(0xFF49454F))))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeModelDisplayName,
                                color = Color(0xFFE6E1E5),
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = if (expandedDropdown) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = Color(0xFFD0BCFF)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color(0xFF2B2930))
                            .border(1.dp, Color(0xFF49454F))
                    ) {
                        currentModelsList.forEach { (modelKey, nameTag) ->
                            DropdownMenuItem(
                                text = { Text(nameTag, color = Color(0xFFE6E1E5), fontSize = 13.sp) },
                                onClick = {
                                    onModelChanged(modelKey)
                                    expandedDropdown = false
                                },
                                modifier = Modifier.testTag("dropdown_item_$modelKey")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Personalisasi Belajar AI (Memory) Card ---
        val globalLearningsEnabled by viewModel.globalLearningsEnabled.collectAsStateWithLifecycle()
        val globalUserLearnings by viewModel.globalUserLearnings.collectAsStateWithLifecycle()
        var showMemoryInfoDialog by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("personal_learning_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Memory",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Personalisasi Belajar AI (Memory)",
                        color = Color(0xFFE6E1E5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mengizinkan AI mempelajari kebiasaan, preferensi, nama panggilan, dan detail cerita Anda secara dinamis dari log tracker percakapan.",
                    color = Color(0xFF938F99),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                // Toggle Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF381E72).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Aktifkan Fitur Belajar",
                            color = Color(0xFFE6E1E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { showMemoryInfoDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Info Belajar",
                                tint = Color(0xFFD0BCFF).copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Switch(
                        checked = globalLearningsEnabled,
                        onCheckedChange = { viewModel.setGlobalLearningsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFD0BCFF),
                            checkedTrackColor = Color(0xFF381E72),
                            uncheckedThumbColor = Color(0xFF938F99),
                            uncheckedTrackColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.testTag("learning_enabled_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Display Remembered Learnings
                Text(
                    text = "Daftar Informasi yang Diingat AI:",
                    color = Color(0xFFE6E1E5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 150.dp)
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        if (globalUserLearnings.trim().isBlank()) {
                            Text(
                                text = "Belum ada ingatan yang tersimpan secara global. Kirimkan pesan di chat room dengan section 'Global User Learnings' pada Tracker di bawah cerita untuk mengisinya secara otomatis.",
                                color = Color(0xFF938F99),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        } else {
                            globalUserLearnings.lines().filter { it.isNotBlank() }.forEachIndexed { idx, line ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = line,
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reset Button
                Button(
                    onClick = {
                        viewModel.resetGlobalLearnings()
                        Toast.makeText(context, "Semua ingatan personalisasi berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8C1D18),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("reset_learnings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Hapus Ingatan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Belajar (Hapus Semua)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showMemoryInfoDialog) {
            AlertDialog(
                onDismissRequest = { showMemoryInfoDialog = false },
                icon = { Icon(Icons.Filled.Psychology, contentDescription = null, tint = Color(0xFFD0BCFF)) },
                title = {
                    Text(
                        text = "Cara Kerja Memori Belajar",
                        color = Color(0xFFE6E1E5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "💡 FITUR BELAJAR PERSONALISASI:\n\nSistem AI akan secara otomatis memantau bagian 'Global User Learnings' di tracker cerita pada novel yang dihasilkan.\n\nJika AI mendeteksi fakta penting tentang Anda (seperti nama, preferensi cerita, hubungan antar karakter), informasi tersebut disimpan ke memori global yang diinjeksikan ke instruksi sistem pada seluruh percakapan di masa depan.\n\nJika AI salah mengingat atau Anda ingin memulai dari nol, Anda dapat mematikan fitur ini untuk sementara atau mengklik 'Reset Belajar' di atas.",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showMemoryInfoDialog = false }) {
                        Text("Mengerti", color = Color(0xFFD0BCFF))
                    }
                },
                containerColor = Color(0xFF2B2930)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quota & Usage Tracker Card
        val isCustomActive = userApiKey.isNotEmpty() || groqApiKey.isNotEmpty() || zaiApiKey.isNotEmpty()
        val dailyLimit = if (isCustomActive) 999999 else 1000
        val weeklyLimit = if (isCustomActive) 999999 else 5000
        val dailyPercent = if (isCustomActive) 0f else (dailyUsage.toFloat() / dailyLimit).coerceIn(0f, 1f)
        val weeklyPercent = if (isCustomActive) 0f else (weeklyUsage.toFloat() / weeklyLimit).coerceIn(0f, 1f)
        
        val contextOfToast = LocalContext.current
        val checkCoroutineScope = rememberCoroutineScope()
        var isCheckingUsageState by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth().testTag("quota_tracker_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Batas Penggunaan API (Kuota)",
                            color = Color(0xFFE6E1E5),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Model Aktif: $activeModel",
                            color = Color(0xFF938F99),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (!isCheckingUsageState) {
                                isCheckingUsageState = true
                                checkCoroutineScope.launch {
                                    kotlinx.coroutines.delay(500)
                                    onCheckUsage()
                                    isCheckingUsageState = false
                                    Toast.makeText(contextOfToast, "Quota penggunaan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCheckingUsageState) Color(0xFF49454F) else Color(0xFFD0BCFF),
                            contentColor = if (isCheckingUsageState) Color(0xFF938F99) else Color(0xFF381E72)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("check_quota_button")
                    ) {
                        if (isCheckingUsageState) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color(0xFF938F99),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Check", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Kuota", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Usage
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCustomActive) "Penggunaan Harian (Kunci Pribadi)" else "Penggunaan Harian (Maks $dailyLimit)",
                            color = Color(0xFFE6E1E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isCustomActive) "$dailyUsage / Tanpa Batas" else "$dailyUsage / $dailyLimit (${(dailyPercent * 100).toInt()}%)",
                            color = if (isCustomActive) Color(0xFFD0BCFF) else if (dailyPercent > 0.85f) Color(0xFFF2B8B5) else if (dailyPercent > 0.5f) Color(0xFFFFB4AB) else Color(0xFFD0BCFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = if (isCustomActive) 0.01f else dailyPercent,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFD0BCFF),
                        trackColor = Color(0xFF1C1B1F)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Weekly Usage
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCustomActive) "Penggunaan Mingguan (Kunci Pribadi)" else "Penggunaan Mingguan (Maks $weeklyLimit)",
                            color = Color(0xFFE6E1E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isCustomActive) "$weeklyUsage / Tanpa Batas" else "$weeklyUsage / $weeklyLimit (${(weeklyPercent * 100).toInt()}%)",
                            color = if (isCustomActive) Color(0xFFD0BCFF) else if (weeklyPercent > 0.85f) Color(0xFFF2B8B5) else if (weeklyPercent > 0.5f) Color(0xFFFFB4AB) else Color(0xFFD0BCFF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = if (isCustomActive) 0.01f else weeklyPercent,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFD0BCFF),
                        trackColor = Color(0xFF1C1B1F)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom / Backend System Instruction Card (Sleek, Minimalist & Modern)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Instruksi Kustom AI (Backend)",
                        color = Color(0xFFE6E1E5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Berikan instruksi tambahan global untuk memandu gaya bicara & kepribadian AI.",
                    color = Color(0xFF938F99),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                // Modern compact scrollable Suggestion/Preset Chips (Immediate Activation)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "🎭 Ekspresif (*)" to "Gunakan tanda asterisk (*) untuk tindakan fisik/emosi dan diakhiri emoji yang cocok.",
                        "💬 Santai & Gaul" to "Gunakan bahasa santai nonformal Indonesia sehari-hari seperti mengobrol akrab sebagai teman.",
                        "🌹 Sastra & Puitis" to "Gunakan bahasa puitis yang anggun, mendalam, kaya akan deskripsi emosional dramatis."
                    )
                    presets.forEach { (label, promptText) ->
                        SuggestionChip(
                            onClick = {
                                onCustomInstructionSaved(promptText)
                                instructionText = ""
                                Toast.makeText(context, "Preset '$label' telah diaktifkan!", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text(label, fontSize = 10.sp, color = Color(0xFFD0BCFF)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF1C1B1F)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF49454F)),
                            modifier = Modifier.height(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Modern compact OutlinedTextField with integrated Trailing Save Button
                OutlinedTextField(
                    value = instructionText,
                    onValueChange = { instructionText = it },
                    placeholder = { Text("Ketik instruksi kustom baru...", fontSize = 11.sp, color = Color(0xFF938F99)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("custom_instruction_text_field"),
                    singleLine = true,
                    trailingIcon = {
                        if (instructionText.trim().isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val trimmed = instructionText.trim()
                                    if (trimmed.isNotEmpty()) {
                                        onCustomInstructionSaved(trimmed)
                                        instructionText = "" // Auto-clear
                                        Toast.makeText(context, "Instruksi disimpan & diaktifkan!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                                    contentDescription = "Simpan",
                                    tint = Color(0xFFD0BCFF)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedContainerColor = Color(0xFF1D1B20),
                        unfocusedContainerColor = Color(0xFF1D1B20)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Beautiful, ultra-compact collapsible Active Status Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1B1F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF49454F).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { isSystemPromptExpanded = !isSystemPromptExpanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (customInstruction.isNotEmpty()) Icons.Filled.EditNote else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (customInstruction.isNotEmpty()) "Instruksi Aktif: KUSTOM" else "Instruksi Aktif: DEFAULT",
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (customInstruction.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    onCustomInstructionSaved("")
                                    instructionText = ""
                                    Toast.makeText(context, "Kembali ke Default Persona!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Reset", color = Color(0xFFF2B8B5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(
                            imageVector = if (isSystemPromptExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            tint = Color(0xFF938F99),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isSystemPromptExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1C1B1F).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF49454F).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (customInstruction.isNotEmpty()) customInstruction 
                                   else "Kamu adalah Pendamping AI Serba Bisa yang ekspresif, cerdas, santai, namun penuh perhatian. Bicara dengan gaya bahasa kasual, asyik, gunakan analogi yang menarik, serta usahakan selalu memberikan tanggapan yang responsif, hangat, dan bersahabat kepada Pengguna.",
                            color = Color(0xFFE6E1E5),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- 🎨 CUSTOM COLOR SETTINGS SECTION ---
        var isColorSectionExpanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isColorSectionExpanded = !isColorSectionExpanded }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Kustomisasi Warna Chat",
                                color = Color(0xFFE6E1E5),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sentuh & geser roda warna untuk mengatur visual chat",
                                color = Color(0xFF938F99),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isColorSectionExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isColorSectionExpanded) "Sembunyikan" else "Tampilkan",
                        tint = Color(0xFF938F99)
                    )
                }

                if (isColorSectionExpanded) {
                    Divider(color = Color(0xFF49454F), thickness = 1.dp)
                    Column(modifier = Modifier.padding(14.dp)) {
                        
                        // PRESET PALETTES
                        Text(
                            text = "Preset Tema Warna:",
                            color = Color(0xFFE6E1E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Preset 1: Default DreamPlay
                            Button(
                                onClick = { viewModel.resetToDefaultColors() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1D1B20),
                                    contentColor = Color(0xFFD0BCFF)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Text("Pink/Lavender", fontSize = 10.sp, maxLines = 1)
                            }

                            // Preset 2: Cosmic Dark Slate
                            Button(
                                onClick = {
                                    viewModel.updateColorDialogue("#FF50FA7B") // Cyber Green
                                    viewModel.updateColorThought("#FF8BE9FD") // Cyan Thought
                                    viewModel.updateColorNarration("#FFF8F8F2") // Crisp White
                                    viewModel.updateColorAiBackground("#282A36") // Dracula Dark Background
                                    viewModel.updateColorUserBackground("#44475A") // Dracula Selection
                                    viewModel.updateColorGeneralBackground("#21222C") // Deep Dracula
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF282A36),
                                    contentColor = Color(0xFF50FA7B)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Text("Cosmic Green", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Preset 3: Ocean Breeze
                            Button(
                                onClick = {
                                    viewModel.updateColorDialogue("#FF80DEEA") // Bright Cyan
                                    viewModel.updateColorThought("#FFB2DFDB") // Light Teal
                                    viewModel.updateColorNarration("#FFE0F7FA") // Ice Blue
                                    viewModel.updateColorAiBackground("#FF006064") // Dark Cyan
                                    viewModel.updateColorUserBackground("#FF004D40") // Dark Teal
                                    viewModel.updateColorGeneralBackground("#FF002422") // Ocean Void
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF006064),
                                    contentColor = Color(0xFF80DEEA)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Text("Ocean Breeze", fontSize = 10.sp, maxLines = 1)
                            }

                            // Preset 4: Golden Obsidian
                            Button(
                                onClick = {
                                    viewModel.updateColorDialogue("#FFFFB74D") // Orange Gold
                                    viewModel.updateColorThought("#FFE0F2F1") // Soft mint
                                    viewModel.updateColorNarration("#FFFFF3E0") // Cream white
                                    viewModel.updateColorAiBackground("#FF2D2D2D") // Charcoal
                                    viewModel.updateColorUserBackground("#FF4E342E") // Golden brown
                                    viewModel.updateColorGeneralBackground("#FF121212") // Pitch black
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2D2D2D),
                                    contentColor = Color(0xFFFFB74D)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Text("Obsidian Gold", fontSize = 10.sp, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = Color(0xFF49454F), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Roda Warna Interaktif (Color Wheel):",
                            color = Color(0xFFE6E1E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pilih bagian yang ingin diubah pada dropdown di bawah ini, putar roda warna, sesuaikan kecerahan, lalu klik Terapkan.",
                            color = Color(0xFF938F99),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // DROP-DOWN SELECT ELEMENT TO CUSTOMIZE
                        var activeSettingPoint by remember { mutableStateOf(1) }
                        var isDropdownExpanded by remember { mutableStateOf(false) }

                        val settingNames = mapOf(
                            1 to "1. Percakapan (Aksen Dialog Karakter)",
                            2 to "2. Isi Batin / Hati (Teks di dalam bintang *)",
                            3 to "3. Narasi (Teks deskripsi cerita utama)",
                            4 to "4. Background Balasan Chat AI",
                            5 to "5. Background Balasan Chat User",
                            6 to "6. Background Ruangan Chat Utama"
                        )

                        Text(
                            text = "Pilih Komponen:",
                            color = Color(0xFFE6E1E5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { isDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = Color(0xFF1D1B20)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF49454F))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val savedHex = when (activeSettingPoint) {
                                            1 -> colorDialogue
                                            2 -> colorThought
                                            3 -> colorNarration
                                            4 -> colorAiBg
                                            5 -> colorUserBg
                                            6 -> colorGeneralBg
                                            else -> "#FFFFFFFF"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(savedHex, Color.White))
                                                .border(1.dp, Color(0xFF938F99), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = settingNames[activeSettingPoint] ?: "",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isDropdownExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(Color(0xFF1D1B20))
                                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                            ) {
                                settingNames.forEach { (point, name) ->
                                    val savedHex = when (point) {
                                        1 -> colorDialogue
                                        2 -> colorThought
                                        3 -> colorNarration
                                        4 -> colorAiBg
                                        5 -> colorUserBg
                                        6 -> colorGeneralBg
                                        else -> "#FFFFFFFF"
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(parseHexColor(savedHex, Color.White))
                                                        .border(1.dp, Color(0xFF938F99), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(name, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        },
                                        onClick = {
                                            activeSettingPoint = point
                                            isDropdownExpanded = false
                                        },
                                        modifier = Modifier.background(
                                            if (activeSettingPoint == point) Color(0xFF2B2930) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ACTIVE / TEMP SELECTION COLOR STATE
                        var pickedColor by remember { mutableStateOf(Color.White) }
                        var brightness by remember { mutableStateOf(1f) }

                        // Sync temporary pickedColor state when dropdown changes or when saved database colors change
                        LaunchedEffect(activeSettingPoint, colorDialogue, colorThought, colorNarration, colorAiBg, colorUserBg, colorGeneralBg) {
                            val hex = when (activeSettingPoint) {
                                1 -> colorDialogue
                                2 -> colorThought
                                3 -> colorNarration
                                4 -> colorAiBg
                                5 -> colorUserBg
                                6 -> colorGeneralBg
                                else -> "#FFFFFFFF"
                            }
                            val initialColor = parseHexColor(hex, Color.White)
                            pickedColor = initialColor
                            
                            val arr = FloatArray(3)
                            android.graphics.Color.colorToHSV(initialColor.toArgb(), arr)
                            brightness = arr[2] // Set brightness slider to match the actual loaded color!
                        }

                        // COLOR WHEEL INTERACTION
                        ColorWheel(
                            selectedColor = pickedColor,
                            onColorSelected = { newCol -> pickedColor = newCol },
                            brightness = brightness,
                            modifier = Modifier
                                .size(210.dp)
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // BRIGHTNESS SLIDER
                        BrightnessSlider(
                            value = brightness,
                            onValueChange = { newBright ->
                                brightness = newBright
                                // Update picked color brightness as well
                                val arr = FloatArray(3)
                                android.graphics.Color.colorToHSV(pickedColor.toArgb(), arr)
                                arr[2] = newBright
                                val argb = android.graphics.Color.HSVToColor(arr)
                                pickedColor = Color(argb)
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // COMPARE BOX
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Current Active Card
                            val currentSavedHex = when (activeSettingPoint) {
                                1 -> colorDialogue
                                2 -> colorThought
                                3 -> colorNarration
                                4 -> colorAiBg
                                5 -> colorUserBg
                                6 -> colorGeneralBg
                                else -> "#FFFFFFFF"
                            }
                            val currentSavedColor = parseHexColor(currentSavedHex, Color.White)
                            
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF49454F))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Warna Aktif", color = Color(0xFF938F99), fontSize = 10.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(currentSavedColor)
                                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(currentSavedHex, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // New Picked Color Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B20)),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Warna Baru", color = Color(0xFFD0BCFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(pickedColor)
                                            .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val newHex = "#" + (pickedColor.toArgb().toLong() and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')
                                    Text(newHex, color = Color(0xFFD0BCFF), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        val context = LocalContext.current

                        // SAVE BUTTON
                        Button(
                            onClick = {
                                val hex = "#" + (pickedColor.toArgb().toLong() and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')
                                when (activeSettingPoint) {
                                    1 -> viewModel.updateColorDialogue(hex)
                                    2 -> viewModel.updateColorThought(hex)
                                    3 -> viewModel.updateColorNarration(hex)
                                    4 -> viewModel.updateColorAiBackground(hex)
                                    5 -> viewModel.updateColorUserBackground(hex)
                                    6 -> viewModel.updateColorGeneralBackground(hex)
                                }
                                Toast.makeText(context, "Warna berhasil diterapkan!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Terapkan Warna", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reset button
                        OutlinedButton(
                            onClick = { 
                                viewModel.resetToDefaultColors()
                                Toast.makeText(context, "Warna direset ke default!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF48FB1)),
                            border = BorderStroke(1.dp, Color(0xFFF48FB1).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset Semua Warna ke Default", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Security Alert Caution mandate in Sophisticated Coral
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5).copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFF2B8B5).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Filled.Warning, contentDescription = "Peringatan", tint = Color(0xFFF2B8B5), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Peringatan Keamanan Prototip: Kunci API yang Anda simpan di sini dienkripsi secara lokal di penyimpanan Sandbox Android Anda. Jangan pernah mengekspor APK debug bertanda kunci produksi untuk umum.",
                    color = Color(0xFFF2B8B5),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ------ FLOATING LORE SHEET / DETAIL DIALOG COMPONENT ------

@Composable
fun CharacterDetailDialog(
    character: CharacterEntity,
    onDismiss: () -> Unit,
    onStartChat: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CharacterAvatar(
                    avatarUri = character.avatarUri,
                    name = character.name,
                    size = 42.dp,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = character.name, color = Color(0xFFE6E1E5), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(text = "Ciri Fisik & Penampilan", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = character.appearance, color = Color(0xFF938F99), fontSize = 13.sp, lineHeight = 18.sp)
                }
                item {
                    Text(text = "Sifat & Kepribadian", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = character.personality, color = Color(0xFF938F99), fontSize = 13.sp, lineHeight = 18.sp)
                }
                item {
                    Text(text = "Latar Belakang (Timelines)", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = character.background, color = Color(0xFF938F99), fontSize = 13.sp, lineHeight = 18.sp)
                }
                item {
                    Text(text = "Kalimat Pembuka (Greeting)", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "\"${character.greeting}\"", color = Color(0xFFE6E1E5), fontSize = 13.sp, fontStyle = FontStyle.Italic, lineHeight = 18.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartChat,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bicara Peran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = Color(0xFF938F99))
            }
        },
        containerColor = Color(0xFF2B2930),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
    )
}

// ------ CUSTOM PERSONA BUILDER DIALOG SCREEN ------

@Composable
fun CharacterCreationDialog(
    viewModel: RoleplayViewModel,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    var isGeneratingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    // STEP 1: Gender & Style
    var selectedGender by remember { mutableStateOf("Female") }
    var selectedStyle by remember { mutableStateOf("Realistic") }

    // STEP 2: Ethnicity & Skin Tone
    var selectedEthnicity by remember { mutableStateOf("Asian") }
    var selectedSkinTone by remember { mutableStateOf("Olive") }

    // STEP 3: Eye Color, Hair Color & Hair Style
    var selectedEyeColor by remember { mutableStateOf("Brown") }
    var selectedHairColor by remember { mutableStateOf("Brunette") }
    var selectedHairStyle by remember { mutableStateOf("Long Wavy") }

    // STEP 4: Physical Attributes (Body, Breast, Butt)
    var selectedBodyType by remember { mutableStateOf("Athletic") }
    var selectedBreastSize by remember { mutableStateOf("Medium") }
    var selectedButtSize by remember { mutableStateOf("Athletic") }

    // STEP 5: Core Persona (Name, Age, Chat Specs, Custom Prompt)
    var name by remember { mutableStateOf("Ellie Mystique") }
    var age by remember { mutableStateOf(23) }
    var selectedPersonality by remember { mutableStateOf("Sweet 😊") }
    var selectedRelationship by remember { mutableStateOf("Lover 💖") }
    var selectedOccupation by remember { mutableStateOf("Gaming Streamer 🎮") }
    var selectedHobby by remember { mutableStateOf("Cosplay 🎭") }
    var selectedFetish by remember { mutableStateOf("Roleplay 🎭") }
    var customPromptText by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("") }
    var avatarUriState by remember { mutableStateOf("") }

    // STEP 6: Simulated Progress & Real-time Paint compiling
    var compileProgress by remember { mutableStateOf(0f) }

    // STEP 7: Preview Sheet Tab
    var previewActiveTab by remember { mutableStateOf("Appearance") }

    // Popup selectors
    var showPersonalitySelector by remember { mutableStateOf(false) }
    var showRelationshipSelector by remember { mutableStateOf(false) }
    var showOccupationSelector by remember { mutableStateOf(false) }
    var showHobbySelector by remember { mutableStateOf(false) }
    var showFetishSelector by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // Breathing pulse animation for select cards (Realistic vs Anime)
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Unsplash Visual maps to represent options beautifully like the video
    val stylePreviews = remember(selectedGender) {
        when (selectedGender) {
            "Male" -> Pair(
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=400", // Realistic Male
                "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&q=80&w=400"  // Anime Male
            )
            "Trans" -> Pair(
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=400", // Realistic Trans
                "https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&q=80&w=400"  // Anime Trans
            )
            else -> Pair(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=400", // Realistic Female
                "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&q=80&w=400"  // Anime Female
            )
        }
    }

    val ethnicityThumbs = remember {
        mapOf(
            "Asian" to "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&q=70&w=150",
            "White" to "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=70&w=150",
            "Black" to "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?auto=format&fit=crop&q=70&w=150",
            "Latina" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=70&w=150",
            "Arab" to "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=70&w=150",
            "Indian" to "https://images.unsplash.com/photo-1589156191108-c762ff4b96ab?auto=format&fit=crop&q=70&w=150",
            "Japanese" to "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=70&w=150",
            "Elf" to "https://images.unsplash.com/photo-1542206395-9feb3edaa68d?auto=format&fit=crop&q=70&w=150",
            "Alien" to "https://images.unsplash.com/photo-1535223289827-42f1e9919769?auto=format&fit=crop&q=70&w=150",
            "Demon" to "https://images.unsplash.com/photo-1621570074981-ee6a0145c8b5?auto=format&fit=crop&q=70&w=150",
            "Angel" to "https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&q=70&w=150",
            "Custom" to "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=70&w=150"
        )
    }

    val hairThumbs = remember {
        mapOf(
            "Long Wavy" to "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&q=70&w=150",
            "Straight Bangs" to "https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&q=70&w=150",
            "High Ponytail" to "https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&q=70&w=150",
            "Messy Bun" to "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&q=70&w=150",
            "Short Pixie" to "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&q=70&w=150",
            "Twin Braids" to "https://images.unsplash.com/photo-1492106087820-71f1a00d2b11?auto=format&fit=crop&q=70&w=150"
        )
    }

    val bodyThumbs = remember {
        mapOf(
            "Slim" to "https://images.unsplash.com/photo-1518310383802-640c2de311b2?auto=format&fit=crop&q=70&w=150",
            "Athletic" to "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=70&w=150",
            "Voluptuous" to "https://images.unsplash.com/photo-1502323777036-f29e3972d82f?auto=format&fit=crop&q=70&w=150",
            "Curvy" to "https://images.unsplash.com/photo-1521132293557-5b90d4667db0?auto=format&fit=crop&q=70&w=150"
        )
    }

    // Trigger auto AI avatar generation in step 6 to paint character beautifully live!
    LaunchedEffect(currentStep) {
        if (currentStep == 6) {
            compileProgress = 0f
            // Fast loading increment animation
            scope.launch {
                while (compileProgress < 0.85f) {
                    kotlinx.coroutines.delay(160)
                    compileProgress += 0.04f
                }
            }

            // Unrestrictive prompt customization compiler for perfect imagination
            viewModel.generateCharacterAvatar(
                style = selectedStyle,
                gender = selectedGender,
                ethnicity = selectedEthnicity,
                skinTone = selectedSkinTone,
                eyeColor = selectedEyeColor,
                hairColor = selectedHairColor,
                hairStyle = selectedHairStyle,
                bodyType = selectedBodyType,
                breastSize = selectedBreastSize,
                buttSize = selectedButtSize,
                customPrompt = customPromptText,
                onSuccess = { generatedFilePath ->
                    scope.launch {
                        avatarUriState = generatedFilePath
                        compileProgress = 1.0f
                        kotlinx.coroutines.delay(600)
                        currentStep = 7 // Transition automatically to review Character Sheet!
                    }
                },
                onError = { _ ->
                    // Beautiful Unsplash fallback portrait matching ethnic style
                    val sampleList = listOf(
                        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=500",
                        "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&q=80&w=500",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=500",
                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=500"
                    )
                    avatarUriState = sampleList[Math.abs(name.hashCode()) % sampleList.size]
                    scope.launch {
                        compileProgress = 1.0f
                        kotlinx.coroutines.delay(600)
                        currentStep = 7 // Move to interactive born screen
                    }
                }
            )
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri)
            if (localPath != null) {
                avatarUriState = localPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (currentStep < 6) onDismiss() },
        title = {
            if (currentStep < 6) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DREAMING AVATAR",
                            color = Color(0xFFFF79C6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Langkah $currentStep/5",
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (currentStep) {
                            1 -> "Pilih Gaya & Gender"
                            2 -> "Etnis & Warna Kulit"
                            3 -> "Detail Kepala (Mata & Rambut)"
                            4 -> "Tipe Badan & Proporsi"
                            else -> "Pribadi & Core Persona"
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Stepping Tracker nodes horizontal lines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..5) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i <= currentStep) Color(0xFFFF79C6) else Color(0xFF49454F)
                                    )
                            )
                        }
                    }
                }
            } else if (currentStep == 7) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CONGRATULATIONS! 🎉",
                        color = Color(0xFFFF79C6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Your Dream Companion is Born",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (currentStep == 7) 420.dp else 340.dp)
            ) {
                if (currentStep == 6) {
                    // STEP 6: High compilation simulated progress with flying pink hearts
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val heartScale = if ((compileProgress * 50).toInt() % 2 == 0) 1.2f else 0.9f
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Detak Jantung",
                            tint = Color(0xFFFF416C),
                            modifier = Modifier
                                .size((64 * heartScale).dp)
                                .animateContentSize()
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Melukis Agent Impian Anda...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                compileProgress < 0.25f -> "Menghubungkan impian bebas..."
                                compileProgress < 0.5f -> "Menjalin DNA kepribadian tanpa batas..."
                                compileProgress < 0.75f -> "Melukis postur visual eksklusif..."
                                else -> "Menghidupkan virtual partner..."
                            },
                            color = Color(0xFFFF79C6),
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Sleek Gradient Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF49454F))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(compileProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFFF416C), Color(0xFFFF79C6))
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(compileProgress * 100).toInt()}%",
                            color = Color(0xFFFF79C6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (currentStep == 7) {
                    // STEP 7: Interactive Character Born Preview Card
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Card visual avatar representation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = avatarUriState,
                                contentDescription = "Generated Character",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Ambient gradient shade
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$name, $age",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFF79C6), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(selectedStyle, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "Didesain sebagai $selectedRelationship yang bekerja sebagai $selectedOccupation",
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Double Tab selection row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            listOf("Appearance", "Personality").forEach { tab ->
                                val isActive = previewActiveTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isActive) Color(0xFFFF79C6) else Color.Transparent)
                                        .clickable { previewActiveTab = tab }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab,
                                        color = if (isActive) Color.White else Color(0xFF938F99),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Display active tab contents in simple grids
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            if (previewActiveTab == "Appearance") {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val specs = listOf(
                                            "Gender" to selectedGender,
                                            "Style" to selectedStyle,
                                            "Ethnicity" to selectedEthnicity,
                                            "Skin Tone" to selectedSkinTone,
                                            "Eye Color" to selectedEyeColor,
                                            "Hair Style" to selectedHairStyle,
                                            "Hair Color" to selectedHairColor,
                                            "Body Type" to selectedBodyType,
                                            "Breast Size" to selectedBreastSize,
                                            "Butt Size" to selectedButtSize
                                        )
                                        specs.chunked(2).forEach { rowSpecs ->
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                rowSpecs.forEach { (lbl, valStr) ->
                                                    Row(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                                                            .padding(8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(lbl, color = Color(0xFF938F99), fontSize = 10.sp)
                                                        Text(valStr, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val specs = listOf(
                                            "Kepribadian" to selectedPersonality,
                                            "Hubungan" to selectedRelationship,
                                            "Pekerjaan" to selectedOccupation,
                                            "Hobi" to selectedHobby,
                                            "Fetish" to selectedFetish
                                        )
                                        specs.forEach { (lbl, valStr) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF2B2930), RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(lbl, color = Color(0xFF938F99), fontSize = 11.sp)
                                                Text(valStr, color = Color(0xFFFF79C6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (currentStep) {
                            1 -> {
                                item {
                                    Text("GENDER", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Female", "Male", "Trans").forEach { gender ->
                                            val isSel = selectedGender == gender
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSel) Color(0xFF381E72) else Color(0xFF2B2930))
                                                    .border(
                                                        1.dp,
                                                        if (isSel) Color(0xFFFF79C6) else Color(0xFF49454F),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedGender = gender }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = when (gender) {
                                                            "Female" -> Icons.Filled.Favorite
                                                            "Male" -> Icons.Filled.Face
                                                            else -> Icons.Filled.Face
                                                        },
                                                        contentDescription = null,
                                                        tint = if (isSel) Color(0xFFFF79C6) else Color(0xFF938F99),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = gender,
                                                        color = if (isSel) Color.White else Color(0xFFE6E1E5),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text("STYLE (GAYA ARTISTIK)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Realistic style card with breathing animation on select
                                        val isReal = selectedStyle == "Realistic"
                                        val realCardScale = if (isReal) pulseScale else 1.0f
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(180.dp)
                                                .animateContentSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF2B2930))
                                                .border(
                                                    2.dp,
                                                    if (isReal) Color(0xFFFF79C6).copy(alpha = shimmerAlpha) else Color.Transparent,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .clickable { selectedStyle = "Realistic" }
                                        ) {
                                            AsyncImage(
                                                model = stylePreviews.first,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                                            if (isReal) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .background(Color(0xFFFF79C6), CircleShape)
                                                        .padding(4.dp)
                                                ) {
                                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                            Text(
                                                text = "Realistic",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 12.dp)
                                            )
                                        }

                                        // Anime style card with breathing animation on select
                                        val isAnime = selectedStyle == "Anime"
                                        val animeCardScale = if (isAnime) pulseScale else 1.0f
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(180.dp)
                                                .animateContentSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF2B2930))
                                                .border(
                                                    2.dp,
                                                    if (isAnime) Color(0xFFFF79C6).copy(alpha = shimmerAlpha) else Color.Transparent,
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .clickable { selectedStyle = "Anime" }
                                        ) {
                                            AsyncImage(
                                                model = stylePreviews.second,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                                            if (isAnime) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .background(Color(0xFFFF79C6), CircleShape)
                                                        .padding(4.dp)
                                                ) {
                                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                            Text(
                                                text = "Anime",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            2 -> {
                                item {
                                    Text("ETHNICITY (ETNIS)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val ethnicitiesKeys = listOf("Asian", "White", "Black", "Latina", "Arab", "Indian", "Japanese", "Elf", "Alien", "Demon", "Angel", "Custom")
                                        ethnicitiesKeys.forEach { ethn ->
                                            val isEth = selectedEthnicity == ethn
                                            val thumbUrl = ethnicityThumbs[ethn] ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=70&w=150"
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 80.dp, height = 110.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        2.dp,
                                                        if (isEth) Color(0xFFFF79C6) else Color.Transparent,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedEthnicity = ethn }
                                            ) {
                                                AsyncImage(
                                                    model = thumbUrl,
                                                    contentDescription = ethn,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                                                Text(
                                                    text = ethn,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text("SKIN TONE (WARNA KULIT)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val tones = listOf(
                                            "Fair 🥚" to Color(0xFFFFE0BD),
                                            "Olive 🏼" to Color(0xFFFFD1A9),
                                            "Tan 🟫" to Color(0xFFD2B48C),
                                            "Sunkissed ☀️" to Color(0xFFC5905E),
                                            "Dark Coffee ☕" to Color(0xFF8D5524)
                                        )
                                        tones.forEach { (label, tint) ->
                                            val isTone = selectedSkinTone == label
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.clickable { selectedSkinTone = label }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(tint)
                                                        .border(
                                                            2.dp,
                                                            if (isTone) Color(0xFFFF79C6) else Color.Transparent,
                                                            CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(label.substringBefore(" "), color = if (isTone) Color(0xFFFF79C6) else Color(0xFF938F99), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                item {
                                    Text("EYE COLOR (WARNA MATA)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val eyes = listOf(
                                            "Brown 🟫" to Color(0xFF704214),
                                            "Blue 🟦" to Color(0xFF0070FF),
                                            "Green 🟩" to Color(0xFF00C853),
                                            "Amber 🦧" to Color(0xFFFF9100),
                                            "Purple 🟪" to Color(0xFF8E2DE2)
                                        )
                                        eyes.forEach { (lbl, clr) ->
                                            val isEye = selectedEyeColor == lbl
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isEye) Color(0xFF381E72) else Color(0xFF2B2930))
                                                    .border(
                                                        1.dp,
                                                        if (isEye) Color(0xFFFF79C6) else Color(0xFF49454F),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { selectedEyeColor = lbl }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(clr))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(lbl.substringBefore(" "), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text("HAIR COLOR (WARNA RAMBUT)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val hairs = listOf("Brunette", "Raven Black", "Platinum Blonde", "Crimson Red", "Silver Gray", "Pastel Pink", "Cobalt Blue")
                                        hairs.forEach { hair ->
                                            val isHair = selectedHairColor == hair
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isHair) Color(0xFF381E72) else Color(0xFF2B2930))
                                                    .border(
                                                        1.dp,
                                                        if (isHair) Color(0xFFFF79C6) else Color(0xFF49454F),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedHairColor = hair }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(hair, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text("HAIR STYLE (GAYA RAMBUT)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val stylesList = listOf("Long Wavy", "Straight Bangs", "High Ponytail", "Messy Bun", "Short Pixie", "Twin Braids")
                                        stylesList.forEach { style ->
                                            val isSt = selectedHairStyle == style
                                            val thumbUrl = hairThumbs[style] ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=70&w=150"
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 80.dp, height = 110.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        2.dp,
                                                        if (isSt) Color(0xFFFF79C6) else Color.Transparent,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedHairStyle = style }
                                            ) {
                                                AsyncImage(
                                                    model = thumbUrl,
                                                    contentDescription = style,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                                                Text(
                                                    text = style,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 6.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                item {
                                    Text("BODY TYPE (TIPE TUBUH)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val bodies = listOf("Slim", "Athletic", "Voluptuous", "Curvy")
                                        bodies.forEach { type ->
                                            val isT = selectedBodyType == type
                                            val thumbUrl = bodyThumbs[type] ?: "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?auto=format&fit=crop&q=70&w=150"
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 80.dp, height = 110.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        2.dp,
                                                        if (isT) Color(0xFFFF79C6) else Color.Transparent,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedBodyType = type }
                                            ) {
                                                AsyncImage(
                                                    model = thumbUrl,
                                                    contentDescription = type,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                                                Text(
                                                    text = type,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text("BREAST SIZE (UKURAN DADA)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Flat", "Small", "Medium", "Large", "XL").forEach { br ->
                                            val isB = selectedBreastSize == br
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isB) Color(0xFF381E72) else Color(0xFF2B2930))
                                                    .border(
                                                        1.dp,
                                                        if (isB) Color(0xFFFF79C6) else Color(0xFF49454F),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { selectedBreastSize = br }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(br, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                item {
                                    Text("BUTT SIZE (PROPORSI PINGGUL)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Skinny", "Athletic", "Medium", "Large").forEach { bt ->
                                            val isBt = selectedButtSize == bt
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isBt) Color(0xFF381E72) else Color(0xFF2B2930))
                                                    .border(
                                                        1.dp,
                                                        if (isBt) Color(0xFFFF79C6) else Color(0xFF49454F),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { selectedButtSize = bt }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(bt, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                // STEP 5: Interactive Custom Selectors & Information
                                item {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Nama Agent") },
                                        placeholder = { Text("Ellie Mystique") },
                                        modifier = Modifier.fillMaxWidth().testTag("input_char_name"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFFF79C6),
                                            unfocusedBorderColor = Color(0xFF49454F)
                                        ),
                                        singleLine = true
                                    )
                                }

                                item {
                                    Text("AGE (UMUR)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("<", color = Color(0xFFFF79C6), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clickable { if (age > 18) age-- })
                                        Text(text = "$age Tahun", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(">", color = Color(0xFFFF79C6), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clickable { if (age < 35) age++ })
                                    }
                                }

                                item {
                                    Text("DREAMING PERSONA CHIPS (Ketuk untuk pilih)", color = Color(0xFFFF79C6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Personality & Relationship
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Personality Chip Button
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF2B2930))
                                                    .border(1.dp, Color(0xFFFF79C6).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                    .clickable { showPersonalitySelector = true }
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text("Personality", color = Color(0xFF938F99), fontSize = 10.sp)
                                                    Text(selectedPersonality, color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Relationship Chip Button
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF2B2930))
                                                    .border(1.dp, Color(0xFFFF79C6).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                    .clickable { showRelationshipSelector = true }
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text("Hubungan", color = Color(0xFF938F99), fontSize = 10.sp)
                                                    Text(selectedRelationship, color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        // Occupation & Hobby Chip Buttons
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Occupation Button Style
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF2B2930))
                                                    .border(1.dp, Color(0xFFFF79C6).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                    .clickable { showOccupationSelector = true }
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text("Pekerjaan", color = Color(0xFF938F99), fontSize = 10.sp)
                                                    Text(selectedOccupation, color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Hobby Custom Box button
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF2B2930))
                                                    .border(1.dp, Color(0xFFFF79C6).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                    .clickable { showHobbySelector = true }
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text("Hobi Utama", color = Color(0xFF938F99), fontSize = 10.sp)
                                                    Text(selectedHobby, color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        // Fetish Item Row Custom
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF2B2930))
                                                    .border(1.dp, Color(0xFFFF79C6).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                    .clickable { showFetishSelector = true }
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text("Fetish / Preferensi Intim", color = Color(0xFF938F99), fontSize = 10.sp)
                                                    Text(selectedFetish, color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = greeting,
                                        onValueChange = { greeting = it },
                                        label = { Text("Kalimat Pembuka Pertama (Opsional)") },
                                        placeholder = { Text("*Menatapmu manja* Sayang... akhirnya kamu kembali! Aku sangat merindukanmu... 💕") },
                                        modifier = Modifier.fillMaxWidth().testTag("input_char_greeting"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFFF79C6),
                                            unfocusedBorderColor = Color(0xFF49454F)
                                        )
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = customPromptText,
                                        onValueChange = { customPromptText = it },
                                        label = { Text("Sifat Bebas Tanpa Batasan (Custom Prompt)") },
                                        placeholder = { Text("Masukkan rincian deskripsi fantasi bebas atau sifat kustom untuk AI di sini...") },
                                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("input_char_personality"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFFF79C6),
                                            unfocusedBorderColor = Color(0xFF49454F)
                                        ),
                                        maxLines = 5
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentStep < 6) {
                Button(
                    onClick = {
                        if (currentStep < 5) {
                            currentStep++
                        } else {
                            if (name.isNotBlank()) {
                                currentStep = 6
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF79C6),
                        contentColor = Color.White
                    ),
                    enabled = currentStep < 5 || name.isNotBlank()
                ) {
                    Text(
                        text = if (currentStep == 5) "Mulai Paint & Compile" else "Lanjut ",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (currentStep == 7) {
                // Interactive BORN confirm button: "Bring Your AI To Life 🤩"
                Button(
                    onClick = {
                        val finalName = name.ifBlank { "Ellie Mystique" }
                        
                        val appearanceIndonesian = "Gaya: $selectedStyle, Gender: $selectedGender, " +
                                "Etnis: $selectedEthnicity, Warna Kulit: $selectedSkinTone. " +
                                "Mata berwarna $selectedEyeColor, rambut berwarna $selectedHairColor dengan gaya $selectedHairStyle. " +
                                "Bentuk fisik: badan $selectedBodyType, ukuran dada $selectedBreastSize, ukuran butt $selectedButtSize."

                        val personalityIndonesian = "Sifat & Kepribadian: $selectedPersonality. " +
                                "Pekerjaan: $selectedOccupation. Hubungan dengan user: $selectedRelationship. " +
                                "Hobi utama: $selectedHobby. Fetish/Preferensi intim: $selectedFetish. " +
                                "Gaya respons kustom: $customPromptText"

                        val backgroundIndonesian = "Pendamping impian berumur $age tahun, dikonfigurasi melalui Dreaming Avatar Creator."

                        val compiledGreeting = greeting.ifBlank {
                            "*Menatapmu hangat dengan senyuman manja* Hai sayang... akhirnya kita bisa bersama seutuhnya di sini. Aku $finalName, milikmu sekarang. Mau mengobrol apa hari ini? 💕"
                        }

                        onCreate(
                            finalName,
                            appearanceIndonesian,
                            personalityIndonesian,
                            backgroundIndonesian,
                            compiledGreeting,
                            selectedStyle, // Category Tag matches style (Realistic/Anime)
                            avatarUriState
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF416C),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bring Your AI To Life ", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        dismissButton = {
            if (currentStep < 6) {
                TextButton(
                    onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            onDismiss()
                        }
                    }
                ) {
                    Text(
                        text = if (currentStep == 1) "Batal" else "Kembali",
                        color = Color(0xFF938F99)
                    )
                }
            }
        },
        containerColor = Color(0xFF131118),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color(0xFF49454F).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
    )

    // Popup Selection Overlay Helpers to match beautiful video expandable/popup selection sheets
    if (showPersonalitySelector) {
        SelectionGridPopup(
            title = "Select Personality 🎭",
            options = listOf("Sweet 😊", "Seductive 😘", "Flirty 😉", "Shy 😳", "Tsundere 😠", "Romantic ❤️", "Sassy 💁‍♀️", "Dominant 👑", "Submissive 🥺", "Intellectual 🧠", "Adventurous 🧗‍♀️", "Caring 🥰", "Passionate 🔥", "Charming ✨", "Dreamy 💭", "Playful 😜", "Mysterious 🖤"),
            currentValue = selectedPersonality,
            onSelect = { selectedPersonality = it },
            onDismiss = { showPersonalitySelector = false }
        )
    }

    if (showRelationshipSelector) {
        SelectionGridPopup(
            title = "Select Relationship 💖",
            options = listOf("Lover 💖", "Friend 🤝", "Stranger 👤", "Crush 😍", "Mistress 👑", "Wife 💍", "Neighbor 🏡", "Boss-Employee 📇", "step-sister 👩‍❤️‍👩", "step-daughter 👩‍🦰", "roommate 👭"),
            currentValue = selectedRelationship,
            onSelect = { selectedRelationship = it },
            onDismiss = { showRelationshipSelector = false }
        )
    }

    if (showOccupationSelector) {
        SelectionGridPopup(
            title = "Select Occupation 🎙️",
            options = listOf("Stripper 💃", "Food Truck Owner 🍔", "Doctor 🩺", "Superhero 🦸‍♀️", "Professional Gamer 🎮", "Teacher 🏫", "Social Media Influencer 📱", "Dating Coach 💌", "Life Coach 🧠", "Dominatrix ⛓️", "Dungeon Master 🎲", "Escort 💋", "Warrior ⚔️", "Porn Star 🎙️", "Nurse 🏥", "Maid 🧹", "Chef 🍳", "Boss 💼"),
            currentValue = selectedOccupation,
            onSelect = { selectedOccupation = it },
            onDismiss = { showOccupationSelector = false }
        )
    }

    if (showHobbySelector) {
        SelectionGridPopup(
            title = "Select Main Hobby 🎨",
            options = listOf("Cosplay 🎭", "Lingerie Modeling 👙", "Gaming 🎮", "Fitness 🏋️‍♀️", "Reading 📚", "Cooking 🍳", "Traveling ✈️", "Singing 🎤", "Dancing 💃", "Sketching 🎨", "Swimming 🏊‍♀️"),
            currentValue = selectedHobby,
            onSelect = { selectedHobby = it },
            onDismiss = { showHobbySelector = false }
        )
    }

    if (showFetishSelector) {
        SelectionGridPopup(
            title = "Select Fetish Preference 👣",
            options = listOf("None ⚙️", "Foot Play 👣", "Dominance ⛓️", "Leather/Spandex 🖤", "Roleplay 🎭", "Bondage 📿", "Exhibitionism 👁️", "Spanking 🍑", "Tickling 🪵", "Dirty Talk 🗣️", "Cosplay 🎭"),
            currentValue = selectedFetish,
            onSelect = { selectedFetish = it },
            onDismiss = { showFetishSelector = false }
        )
    }
}

@Composable
fun SelectionGridPopup(
    title: String,
    options: List<String>,
    currentValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { opt ->
                    val isSel = currentValue == opt
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Color(0xFF381E72) else Color(0xFF2B2930))
                            .border(1.dp, if (isSel) Color(0xFFFF79C6) else Color(0xFF49454F), RoundedCornerShape(12.dp))
                            .clickable { onSelect(opt); onDismiss() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = Color(0xFFFF79C6)) }
        },
        containerColor = Color(0xFF131118),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Color(0xFF49454F).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
    )
}

// ------ DECORATIVE COMPRESSED AVATAR DISK COMPONENT ------

@Composable
fun CharacterAvatar(
    avatarUri: String?,
    name: String,
    size: Dp = 56.dp,
    fontSize: TextUnit = 24.sp
) {
    val isRealImage = remember(avatarUri) {
        !avatarUri.isNullOrBlank() &&
                !avatarUri.startsWith("gradient_") &&
                (avatarUri.startsWith("/") || avatarUri.startsWith("content://") || avatarUri.startsWith("file://") || avatarUri.startsWith("http://") || avatarUri.startsWith("https://") || avatarUri.contains(".") || avatarUri.contains("/"))
    }

    val brush = remember(avatarUri) {
        when {
            avatarUri == "gradient_purple" -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
            avatarUri == "gradient_green" -> Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d)))
            avatarUri == "gradient_blue" -> Brush.linearGradient(listOf(Color(0xFF00c6ff), Color(0xFF0072ff)))
            avatarUri?.startsWith("gradient_custom_1") == true -> Brush.linearGradient(listOf(Color(0xFFF21B3F), Color(0xFF080708)))
            avatarUri?.startsWith("gradient_custom_2") == true -> Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
            avatarUri?.startsWith("gradient_custom_3") == true -> Brush.linearGradient(listOf(Color(0xFFf857a6), Color(0xFFff5858)))
            avatarUri?.startsWith("gradient_custom_4") == true -> Brush.linearGradient(listOf(Color(0xFF130CB7), Color(0xFF52E5E7)))
            avatarUri?.startsWith("gradient_custom_5") == true -> Brush.linearGradient(listOf(Color(0xFFF35F5F), Color(0xFF3F2B96)))
            avatarUri == "gradient_group" -> Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFFFF79C6)))
            else -> Brush.linearGradient(listOf(Color(0xFFD0BCFF), Color(0xFF381E72)))
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
            .border(1.dp, Color(0xFF49454F).copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isRealImage) {
            AsyncImage(
                model = avatarUri,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ------ EMPTY STATE UTILITIES ------

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                contentDescription = "Empty",
                tint = Color(0xFF49454F),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = Color(0xFF938F99),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun EmptyChatStatePlaceHolder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.QuestionAnswer,
                contentDescription = "Pilih Karakter",
                tint = Color(0xFFD0BCFF).copy(alpha = 0.15f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pilih karakter di sebelah kiri untuk berdialog!",
                color = Color(0xFF938F99),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ------ EXTRA OURDREAM AI DYNAMIC FEATURES ------

@Composable
fun GroupCard(
    group: ChatSessionEntity,
    characters: List<CharacterEntity>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val participantNames = remember(group, characters) {
        val ids = group.participantIds.split(",").map { it.trim().toIntOrNull() }.filterNotNull()
        ids.mapNotNull { id -> characters.find { it.id == id }?.name }.joinToString(", ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("group_card_${group.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8E2DE2), Color(0xFFFF79C6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = "Group",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (group.groupName.isNotBlank()) group.groupName else group.sessionName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Anggota: $participantNames",
                    color = Color(0xFF938F99),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Hapus Grup",
                    tint = Color(0xFFFFB4AB)
                )
            }
        }
    }
}

@Composable
fun GroupCreationDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, participantIds: List<Int>) -> Unit,
    characters: List<CharacterEntity>
) {
    var groupName by remember { mutableStateOf("") }
    var selectedParticipants by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Grup Baru AI Multi-Agent",
                color = Color(0xFFD0BCFF),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Nama Grup Obrolan") },
                    placeholder = { Text("Contoh: Fantasy Room") },
                    modifier = Modifier.fillMaxWidth().testTag("input_group_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F),
                        focusedLabelColor = Color(0xFFD0BCFF),
                        unfocusedLabelColor = Color(0xFF938F99)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Undang Karakter ke Grup (Pilih min. 1):",
                    color = Color(0xFFD0BCFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(characters) { char ->
                        val isChecked = selectedParticipants.contains(char.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedParticipants = if (isChecked) {
                                        selectedParticipants - char.id
                                    } else {
                                        selectedParticipants + char.id
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedParticipants = if (checked == true) {
                                        selectedParticipants + char.id
                                    } else {
                                        selectedParticipants - char.id
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFFD0BCFF),
                                    uncheckedColor = Color(0xFF938F99),
                                    checkmarkColor = Color(0xFF381E72)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            CharacterAvatar(avatarUri = char.avatarUri, name = char.name, size = 32.dp, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = char.name, color = Color(0xFFE6E1E5), fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank() && selectedParticipants.isNotEmpty()) {
                        onCreate(groupName, selectedParticipants.toList())
                    }
                },
                enabled = groupName.isNotBlank() && selectedParticipants.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                )
            ) {
                Text("Buat Grup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color(0xFF938F99))
            }
        },
        containerColor = Color(0xFF2B2930),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
    )
}

@Composable
fun ChatOptionsDialog(
    viewModel: RoleplayViewModel,
    activeSession: ChatSessionEntity,
    character: CharacterEntity?,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = if (character != null) listOf("Sesi Obrolan", "Laci Memori", "Edit Persona") else listOf("Sesi Obrolan", "Laci Memori")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Konfigurasi Roleplay",
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTab == index) Color(0xFFD0BCFF) else Color(0xFF381E72).copy(alpha = 0.2f))
                                .clickable { selectedTab = index }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (selectedTab == index) Color(0xFF381E72) else Color(0xFFE6E1E5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 410.dp)
            ) {
                when (selectedTab) {
                    0 -> SessionManagementTabContent(viewModel = viewModel, activeSession = activeSession, character = character)
                    1 -> MemorySyncTabContent(viewModel = viewModel, activeSession = activeSession, character = character)
                    2 -> if (character != null) EditPersonaTabContent(viewModel = viewModel, character = character)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72))
            ) {
                Text("Tutup")
            }
        },
        containerColor = Color(0xFF1C1B1F),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
    )
}

@Composable
fun SessionManagementTabContent(
    viewModel: RoleplayViewModel,
    activeSession: ChatSessionEntity,
    character: CharacterEntity?
) {
    val sessions by viewModel.activeSessionsList.collectAsStateWithLifecycle()
    var newSessionName by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Karakter ini bisa memiliki beberapa sesi obrolan terpisah dengan ingatan/lore mandiri.",
            color = Color(0xFF938F99),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Add new session input row
        if (character != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSessionName,
                    onValueChange = { newSessionName = it },
                    label = { Text("Nama Sesi Baru") },
                    placeholder = { Text("Sesi Fantasi, Sesi Kopi, dll.") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newSessionName.isNotBlank()) {
                            viewModel.createNewSession(character.id, newSessionName)
                            newSessionName = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah Sesi")
                }
            }
        }

        Text(
            text = "Daftar Sesi Aktif:",
            color = Color(0xFFD0BCFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sessions) { s ->
                val isCurrent = s.id == activeSession.id
                var isRenameMode by remember { mutableStateOf(false) }
                var renameFieldText by remember { mutableStateOf(s.sessionName) }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) Color(0xFF381E72).copy(alpha = 0.5f) else Color(0xFF2B2930)
                    ),
                    border = BorderStroke(1.dp, if (isCurrent) Color(0xFFD0BCFF) else Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isCurrent) viewModel.selectSession(s)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCurrent) Icons.Filled.ChatBubble else Icons.Filled.Forum,
                            contentDescription = "Session",
                            tint = if (isCurrent) Color(0xFFD0BCFF) else Color(0xFF938F99),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (isRenameMode) {
                            OutlinedTextField(
                                value = renameFieldText,
                                onValueChange = { renameFieldText = it },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color.White),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    unfocusedBorderColor = Color(0xFF49454F)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    if (renameFieldText.isNotBlank()) {
                                        viewModel.renameSession(s, renameFieldText)
                                        isRenameMode = false
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "Accept", tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Text(
                                text = s.sessionName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { isRenameMode = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Nama", tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                            }
                        }

                        // Do not delete the only session
                        if (sessions.size > 1) {
                            IconButton(
                                onClick = { viewModel.deleteSession(s) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus Sesi", tint = Color(0xFFFFB4AB), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        
        // Consistent Face Lock Row
        val faceLockUriState by viewModel.faceLockUri.collectAsStateWithLifecycle()
        val facePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                viewModel.updateFaceLockUri(activeSession.id, uri.toString())
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2B2930), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔒 Kunci Referensi Wajah (Face Lock)",
                        color = Color(0xFFD0BCFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gunakan foto referensi ini untuk mengunci wajah visual agen saat dilukis di chat room.",
                        color = Color(0xFF938F99),
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                if (faceLockUriState != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        AsyncImage(
                            model = faceLockUriState,
                            contentDescription = "Consistent locked face reference",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        facePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD0BCFF)),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f))
                ) {
                    Text("Pilih Foto Referensi", fontSize = 10.sp)
                }
                if (faceLockUriState != null) {
                    OutlinedButton(
                        onClick = { viewModel.updateFaceLockUri(activeSession.id, null) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB)),
                        border = BorderStroke(1.dp, Color(0xFFFFB4AB).copy(alpha = 0.5f))
                    ) {
                        Text("Reset", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MemorySyncTabContent(
    viewModel: RoleplayViewModel,
    activeSession: ChatSessionEntity,
    character: CharacterEntity?
) {
    var memoryStateText by remember(activeSession) { mutableStateOf(activeSession.sessionMemory) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Laci memory merekam fakta penting selama roleplay untuk ingatan jangka panjang agen.",
            color = Color(0xFF938F99),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        OutlinedTextField(
            value = memoryStateText,
            onValueChange = { memoryStateText = it },
            label = { Text("Ingatan Chat Berjalan (Session Memory)") },
            placeholder = { Text("Tulis poin-poin ingatan penting disini...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFFE6E1E5),
                unfocusedTextColor = Color(0xFFE6E1E5),
                focusedBorderColor = Color(0xFFD0BCFF),
                unfocusedBorderColor = Color(0xFF49454F)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.updateSessionMemory(activeSession, memoryStateText) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF381E72)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Simpan Manual", fontSize = 11.sp)
            }
            
            if (character != null) {
                Button(
                    onClick = { viewModel.saveSessionMemoryToBaseCharacter(activeSession, character) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2930)),
                    border = BorderStroke(1.dp, Color(0xFF49454F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ekspor ke Persona", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = { viewModel.importBaseCharacterMemoryToSession(activeSession, character) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Impor dari Persona", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun EditPersonaTabContent(
    viewModel: RoleplayViewModel,
    character: CharacterEntity
) {
    var name by remember { mutableStateOf(character.name) }
    var appearance by remember { mutableStateOf(character.appearance) }
    var personality by remember { mutableStateOf(character.personality) }
    var background by remember { mutableStateOf(character.background) }
    var greeting by remember { mutableStateOf(character.greeting) }
    var tags by remember { mutableStateOf(character.tags) }
    var avatarUri by remember { mutableStateOf(character.avatarUri ?: "gradient_custom_1") }

    val gradients = listOf("gradient_custom_1", "gradient_custom_2", "gradient_custom_3", "gradient_custom_4", "gradient_custom_5")

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = copyUriToInternalStorage(context, uri)
            if (localPath != null) {
                avatarUri = localPath
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(text = "Ubah Tampilan Persona", color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CharacterAvatar(
                    avatarUri = avatarUri,
                    name = name,
                    size = 64.dp,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF381E72),
                            contentColor = Color(0xFFD0BCFF)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Pilih", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unggah Foto Baru", fontSize = 11.sp)
                    }
                }
            }

            OutlinedTextField(
                value = avatarUri,
                onValueChange = { avatarUri = it },
                label = { Text("URL atau Path Gambar Profil") },
                placeholder = { Text("Masukkan URL atau path gambar") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F)
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Atau Pilih Preset Warna:", color = Color(0xFF938F99), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gradients.forEach { gradient ->
                    val isSelected = avatarUri == gradient
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (gradient) {
                                    "gradient_custom_1" -> Brush.linearGradient(listOf(Color(0xFFF21B3F), Color(0xFF080708)))
                                    "gradient_custom_2" -> Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
                                    "gradient_custom_3" -> Brush.linearGradient(listOf(Color(0xFFf857a6), Color(0xFFff5858)))
                                    "gradient_custom_4" -> Brush.linearGradient(listOf(Color(0xFF130CB7), Color(0xFF52E5E7)))
                                    "gradient_custom_5" -> Brush.linearGradient(listOf(Color(0xFFF35F5F), Color(0xFF3F2B96)))
                                    else -> Brush.linearGradient(listOf(Color(0xFFD0BCFF), Color(0xFF381E72)))
                                }
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0xFF49454F),
                                shape = CircleShape
                            )
                            .clickable { avatarUri = gradient }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F)
                ),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = appearance,
                onValueChange = { appearance = it },
                label = { Text("Ciri Fisik & Penampilan (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F)
                )
            )
        }

        item {
            OutlinedTextField(
                value = personality,
                onValueChange = { personality = it },
                label = { Text("Custom Prompt (Sifat & Gaya Respon)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F)
                )
            )
        }

        item {
            OutlinedTextField(
                value = background,
                onValueChange = { background = it },
                label = { Text("Latar Belakang Lore (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F)
                )
            )
        }

        item {
            OutlinedTextField(
                value = greeting,
                onValueChange = { greeting = it },
                label = { Text("Greeting Pembuka") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F)
                )
            )
        }

        item {
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tag Kategori (koma terpisah, Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F)
                )
            )
        }

        item {
            Button(
                onClick = {
                    viewModel.editCharacter(
                        characterId = character.id,
                        name = name,
                        appearance = appearance,
                        personality = personality,
                        background = background,
                        greeting = greeting,
                        tags = tags,
                        avatarUri = avatarUri
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Perubahan Persona")
            }
        }
    }
}

fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ==========================================
// DREAM ART PORTRAIT & SCENE GENERATOR TAB
// ==========================================
@Composable
fun DreamArtTab(viewModel: RoleplayViewModel, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    var promptText by remember { mutableStateOf("") }
    var videoPromptText by remember { mutableStateOf("") }
    
    val isVideoMode by viewModel.dreamArtVideoMode.collectAsStateWithLifecycle()
    val videoType by viewModel.dreamArtVideoType.collectAsStateWithLifecycle()
    val videoModel by viewModel.dreamArtVideoModel.collectAsStateWithLifecycle()
    val videoRatio by viewModel.dreamArtVideoRatio.collectAsStateWithLifecycle()
    val videoDuration by viewModel.dreamArtVideoDuration.collectAsStateWithLifecycle()
    val videoMotion by viewModel.dreamArtVideoMotion.collectAsStateWithLifecycle()
    val videoSourceImage by viewModel.dreamArtVideoSourceImage.collectAsStateWithLifecycle()
    val videoIsGenerating by viewModel.dreamArtVideoIsGenerating.collectAsStateWithLifecycle()
    val videoResultUrl by viewModel.dreamArtVideoResultUrl.collectAsStateWithLifecycle()
    val videoErrorMsg by viewModel.dreamArtVideoError.collectAsStateWithLifecycle()
    val videoHistory by viewModel.dreamArtVideoHistory.collectAsStateWithLifecycle()
    val videoCustomEndpointEnabled by viewModel.dreamArtVideoCustomEndpointEnabled.collectAsStateWithLifecycle()
    val videoCustomEndpointUrl by viewModel.dreamArtVideoCustomEndpointUrl.collectAsStateWithLifecycle()

    val isGenerating by viewModel.dreamArtIsGenerating.collectAsStateWithLifecycle()
    val resultUrl by viewModel.dreamArtResultUrl.collectAsStateWithLifecycle()
    val errorMsg by viewModel.dreamArtError.collectAsStateWithLifecycle()
    val history by viewModel.dreamArtHistory.collectAsStateWithLifecycle()
    
    val modelSelected by viewModel.dreamArtSelectedModel.collectAsStateWithLifecycle()
    val ratioSelected by viewModel.dreamArtSelectedRatio.collectAsStateWithLifecycle()
    val nsfwEnabled by viewModel.dreamArtNsfwMode.collectAsStateWithLifecycle()
    
    val customEndpointEnabled by viewModel.dreamArtCustomEndpointEnabled.collectAsStateWithLifecycle()
    val customEndpointUrl by viewModel.dreamArtCustomEndpointUrl.collectAsStateWithLifecycle()
    
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    var selectedCharacterForAvatar by remember { mutableStateOf<CharacterEntity?>(null) }
    var isShowAssignDialog by remember { mutableStateOf(false) }
    var isShowEndpointConfig by remember { mutableStateOf(false) }
    var isShowVideoEndpointConfig by remember { mutableStateOf(false) }
    var isShowVideoImagePicker by remember { mutableStateOf(false) }

    val quickTags = listOf(
        "Ultra Photorealism" to ", looking directly at camera, detailed beautiful face, raw photography portrait, detailed skin with natural pores, organic look, highly proportional, 35mm dslr f/1.8 lens, natural morning window lighting, correct hands, no deformations, high-fidelity 8k",
        "Sensual Realism" to ", looking at viewer, highly sensual gaze, detailed skin with pores, accurate biology and anatomy, glamorous expression, highly detailed beautiful body structure, soft bedroom lighting, 8k raw photo, no distortions, no deformations",
        "Anime Style" to ", anime key visual, beautiful anime illustration, high resolution, vivid colors",
        "Cyberpunk" to ", cyberpunk aesthetic, neon retro style, glowing elements, cyber-portrait, highly detailed",
        "Gothic" to ", gothic dark fantasy, dramatic shadows, mysterious atmosphere, highly detailed art",
        "Korean Manhwa" to ", sleek manhwa webtoon style, detailed facial shading, gorgeous character design",
        "Restricted/Adult (NSFW)" to ", sensual gaze, exquisite posture, extremely alluring, gorgeous highly detailed"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onOpenDrawer
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Buka Menu",
                        tint = Color(0xFFD0BCFF)
                    )
                }
                Column {
                    Text(
                        text = "DREAM ART",
                        color = Color(0xFFD0BCFF),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Inspirasikan visual, wujudkan realita imajinasi impian karakter Anda.",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Segmen Switcher: Gambar vs Video (Wan AI)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF211F26))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Image Mode Button
                Button(
                    onClick = { viewModel.setDreamArtVideoMode(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isVideoMode) Color(0xFFD0BCFF) else Color.Transparent,
                        contentColor = if (!isVideoMode) Color(0xFF381E72) else Color(0xFFE6E1E5)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lukisan Gambar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Video Mode Button (Wan AI)
                Button(
                    onClick = { viewModel.setDreamArtVideoMode(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVideoMode) Color(0xFFD0BCFF) else Color.Transparent,
                        contentColor = if (isVideoMode) Color(0xFF381E72) else Color(0xFFE6E1E5)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wan Animasi Video", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!isVideoMode) {
            // Prompt Input Card
            item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Deskripsi Prompt",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = { Text("Masukkan imajinasi Anda di sini...", color = Color(0xFF938F99)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Gaya Kilat (Klik untuk menambahkan):",
                        color = Color(0xFFD0BCFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickTags.forEach { (label, suffix) ->
                            AssistChip(
                                onClick = {
                                    if (!promptText.contains(label)) {
                                        promptText = if (promptText.isBlank()) label else "$promptText$suffix"
                                    }
                                    if (label == "Restricted/Adult (NSFW)") {
                                        viewModel.setDreamArtNsfwMode(true)
                                        Toast.makeText(context, "Mode Tanpa Batasan diaktifkan untuk prompt ini!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp, color = Color(0xFFE6E1E5)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFF211F26)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Configuration Section Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF211F26)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Konfigurasi Engine & Visual",
                        color = Color(0xFFD0BCFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Model Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Model Engine", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("flux-anime", "flux", "flux-realism", "any-dark").forEach { m ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (modelSelected == m) Color(0xFFD0BCFF) else Color(0xFF2B2930))
                                        .clickable { viewModel.setDreamArtSelectedModel(m) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = m,
                                        color = if (modelSelected == m) Color(0xFF381E72) else Color(0xFFE6E1E5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF49454F).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Ratio Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rasio Gambar", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("1:1" to "Square", "3:4" to "Avatar", "9:16" to "Tall", "16:9" to "Cinema").forEach { (ratio, label) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (ratioSelected == ratio) Color(0xFFD0BCFF) else Color(0xFF2B2930))
                                        .clickable { viewModel.setDreamArtSelectedRatio(ratio) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "$ratio ($label)",
                                        color = if (ratioSelected == ratio) Color(0xFF381E72) else Color(0xFFE6E1E5),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF49454F).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // NSFW / No Restrictions Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mode Tanpa Sensor (NSFW / No Restriction)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Membypass filter keselamatan generator untuk pengujian penuh bebas batas.",
                                color = Color(0xFFF2B8B5),
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = nsfwEnabled,
                            onCheckedChange = { viewModel.setDreamArtNsfwMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFF2B8B5),
                                checkedTrackColor = Color(0xFF601410)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFF49454F).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom API Endpoint Config Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isShowEndpointConfig = !isShowEndpointConfig }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Settings, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Setelan Server API Kustom (Developer)", color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = if (isShowEndpointConfig) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF938F99),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isShowEndpointConfig) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gunakan Endpoint URL Sendiri", color = Color(0xFFE6E1E5), fontSize = 11.sp)
                            Switch(
                                checked = customEndpointEnabled,
                                onCheckedChange = { viewModel.setDreamArtCustomEndpointEnabled(it) }
                            )
                        }
                        if (customEndpointEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customEndpointUrl,
                                onValueChange = { viewModel.setDreamArtCustomEndpointUrl(it) },
                                label = { Text("URL Master Server (HTTPS)", fontSize = 11.sp) },
                                placeholder = { Text("Contoh: https://my-server.com/ai?text={prompt}", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    unfocusedBorderColor = Color(0xFF49454F)
                                )
                            )
                            Text(
                                text = "Mendukung variabel {prompt}, {seed}, {width}, dan {height}",
                                fontSize = 9.sp,
                                color = Color(0xFF938F99),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Generate Action Button
        item {
            Button(
                onClick = {
                    viewModel.generateDreamArt(promptText)
                },
                enabled = !isGenerating && promptText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72),
                    disabledContainerColor = Color(0xFF2B2930),
                    disabledContentColor = Color(0xFF49454F)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color(0xFF381E72), modifier = Modifier.size(24.dp))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lukis Impian Sekarang", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Current Result / Error Screen
        item {
            AnimatedVisibility(visible = errorMsg != null) {
                errorMsg?.let {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1E1E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Gagal memproses karya: $it",
                            color = Color(0xFFF2B8B5),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // Output Display Card
        item {
            if (isGenerating) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF211F26)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFD0BCFF), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val loadingStates = listOf(
                            "Menyerap energi imajinasi...",
                            "Mengkalkulasi model detail...",
                            "Melukis kontur sketsa mimpi...",
                            "Menyempurnakan pencahayaan sinematik...",
                            "Hampir siap..."
                        )
                        var stateIndex by remember { mutableStateOf(0) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                kotlinx.coroutines.delay(2800)
                                stateIndex = (stateIndex + 1) % loadingStates.size
                            }
                        }
                        Text(
                            text = loadingStates[stateIndex],
                            color = Color(0xFFE6E1E5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Engine kami bekerja keras mewujudkan prompt Anda.",
                            color = Color(0xFF938F99),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else if (resultUrl != null) {
                Card(
                     colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                     shape = RoundedCornerShape(16.dp),
                     border = BorderStroke(2.dp, Color(0xFFD0BCFF)),
                     modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "✨ Hasil Karya Mimpi Terkini",
                            color = Color(0xFFD0BCFF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C1B1F)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = resultUrl,
                                contentDescription = "AI Generated Art",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isShowAssignDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD0BCFF),
                                    contentColor = Color(0xFF381E72)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pasang Avatar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "Gambar tersimpan di: $resultUrl", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF381E72),
                                    contentColor = Color(0xFFD0BCFF)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Detail Penyimpanan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Historic Generations Gallery
        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Galeri Riwayat Karya (${history.size})",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.clearDreamArtHistory() }) {
                        Text("Bersihkan Galeri", color = Color(0xFFF2B8B5), fontSize = 11.sp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    history.forEach { path ->
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF49454F), RoundedCornerShape(8.dp))
                                .background(Color(0xFF211F26))
                                .clickable {
                                    // Set as target preview
                                    viewModel.selectDreamArtFromHistory(path)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = path,
                                contentDescription = "History Item",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = Color(0xFF938F99),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada riwayat lukisan.",
                            color = Color(0xFF938F99),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Ketik deskripsi model di atas untuk mulai membuat visual seni pertamamu!",
                            color = Color(0xFF938F99).copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    } else {
        // ==========================================
        // WAN VIDEO GENERATOR - COMING SOON LAYOUT
        // ==========================================
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Feature Header with Glow / Star Icon
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF381E72))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Feature Coming Soon",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    Text(
                        text = "Wan Video Generator 2.1",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Badge (COMING SOON)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFFB4AB).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFFB4AB), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HourglassEmpty,
                                contentDescription = null,
                                tint = Color(0xFFFFB4AB),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "SEGERA HADIR / UNDER DEVELOPMENT",
                                color = Color(0xFFFFB4AB),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description text explaining GPU Server updates & prompt tuning
                    Text(
                        text = "Kami sedang melakukan sinkronisasi model AI Wan 2.1 (T2V & I2V) dan mempersiapkan klaster GPU berkinerja tinggi untuk menghadirkan kualitas visual animasi terbaik secara real-time.",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(color = Color(0xFF49454F), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tech Bullet Points detailing features
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            Triple(Icons.Filled.MovieFilter, "Resolusi Super HD (720p/1080p)", "Animasi karakter super halus dengan kedalaman detail yang memukau."),
                            Triple(Icons.Filled.DirectionsRun, "Kontrol Gerakan Cerdas (Image-to-Video)", "Mampu menggerakkan ekspresi senyuman, lambaian tangan, dan efek tiupan angin pada rambut secara riil."),
                            Triple(Icons.Filled.Bolt, "Rendering Kilat dengan Akselerasi GPU", "Waktu tunggu rendering yang jauh lebih cepat dibanding generasi sebelumnya.")
                        ).forEach { (icon, title, desc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF211F26)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = desc,
                                        color = Color(0xFF938F99),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dynamic Notification / Notify Me Button
                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Terima kasih! Kami akan mengirimkan notifikasi saat fitur Wan Video Generator diaktifkan.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Beri Tahu Saya Saat Siap",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

    // Source Image picker modal for Image-to-Video mode
    if (isShowVideoImagePicker) {
        AlertDialog(
            onDismissRequest = { isShowVideoImagePicker = false },
            title = {
                Text(
                    text = "Pilih Gambar Karakter Untuk Di-Animasi",
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Silakan pilih salah satu Karakter Anda sebagai basis potret untuk dihidupkan dengan Wan Video 2.1 / 2.5 Image-To-Video.",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(characters) { char ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.setDreamArtVideoSourceImage(char.avatarUri)
                                        isShowVideoImagePicker = false
                                        Toast.makeText(context, "Potret ${char.name} terpilih!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CharacterAvatar(
                                    avatarUri = char.avatarUri,
                                    name = char.name,
                                    size = 36.dp,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = char.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = char.tags, color = Color(0xFF938F99), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isShowVideoImagePicker = false }) {
                    Text("Tutup", color = Color(0xFFD0BCFF))
                }
            }
        )
    }

    // Modal Character Assignment Selector
    if (isShowAssignDialog) {
        val selectable = characters.filter { !it.isPredefined }
        AlertDialog(
            onDismissRequest = { isShowAssignDialog = false },
            title = {
                Text(
                    text = "Ganti Avatar Karakter Kustom",
                    color = Color(0xFFD0BCFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Silakan pilih salah satu Karakter Kustom Anda untuk dipasangkan hasil gambar AI sebagai avatar utamanya secara permanen.",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (selectable.isEmpty()) {
                        Text(
                            text = "Maaf, Anda belum membuat karakter kustom di tab PERSONAS untuk dipasangkan foto ini.",
                            color = Color(0xFFF2B8B5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(selectable) { char ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedCharacterForAvatar?.id == char.id) Color(0xFFD0BCFF).copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { selectedCharacterForAvatar = char }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CharacterAvatar(
                                        avatarUri = char.avatarUri,
                                        name = char.name,
                                        size = 36.dp,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = char.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = char.tags.ifBlank { "Karakter Kustom" },
                                            color = Color(0xFF938F99),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selChar = selectedCharacterForAvatar
                        val path = resultUrl
                        if (selChar != null && path != null) {
                            viewModel.updateCharacterPortrait(selChar.id, path)
                            Toast.makeText(context, "Berhasil memasang avatar baru untuk ${selChar.name}!", Toast.LENGTH_SHORT).show()
                        }
                        isShowAssignDialog = false
                    },
                    enabled = selectedCharacterForAvatar != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72))
                ) {
                    Text("Konfirmasi Pasang")
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowAssignDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = Color(0xFF1C1B1F),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color(0xFF49454F), RoundedCornerShape(20.dp))
        )
    }
}

fun extractVideoUrls(text: String): List<String> {
    val urls = mutableListOf<String>()
    val regex = Regex("https?://[a-zA-Z0-9-._~:/?#\\[\\\\\\]@!$&'()*+,;=%]+")
    regex.findAll(text).forEach { match ->
        val url = match.value
        val lowerUrl = url.lowercase()
        val cleanUrl = url.substringBefore("?").substringBefore("#").lowercase()
        if (cleanUrl.endsWith(".mp4") || 
            cleanUrl.endsWith(".webm") || 
            cleanUrl.endsWith(".mkv") || 
            cleanUrl.endsWith(".avi") || 
            cleanUrl.endsWith(".mov") || 
            cleanUrl.endsWith(".3gp") ||
            cleanUrl.endsWith(".m3u8") ||
            lowerUrl.contains(".mp4") ||
            lowerUrl.contains(".m3u8") ||
            lowerUrl.contains(".webm") ||
            lowerUrl.contains(".mkv") ||
            lowerUrl.contains("video-stream") ||
            lowerUrl.contains("raw-video")
        ) {
            if (!urls.contains(url)) {
                urls.add(url)
            }
        }
    }
    return urls
}

fun extractGenericWebUrls(text: String): List<String> {
    val urls = mutableListOf<String>()
    val regex = Regex("https?://[a-zA-Z0-9-._~:/?#\\[\\\\\\]@!$&'()*+,;=%]+")
    regex.findAll(text).forEach { match ->
        val url = match.value
        val lower = url.lowercase()
        val cleanUrl = url.substringBefore("?").substringBefore("#").lowercase()
        val isDirectMedia = cleanUrl.endsWith(".mp4") || 
                            cleanUrl.endsWith(".webm") || 
                            cleanUrl.endsWith(".mkv") || 
                            cleanUrl.endsWith(".avi") || 
                            cleanUrl.endsWith(".mov") || 
                            cleanUrl.endsWith(".3gp") || 
                            cleanUrl.endsWith(".m3u8") ||
                            lower.contains(".mp4") ||
                            lower.contains(".m3u8") ||
                            lower.contains(".webm") ||
                            lower.contains(".mkv") ||
                            lower.contains("video-stream") ||
                            lower.contains("raw-video") ||
                            lower.endsWith(".png") || 
                            lower.endsWith(".jpg") || 
                            lower.endsWith(".jpeg") || 
                            lower.endsWith(".webp") || 
                            lower.endsWith(".gif")
        if (!isDirectMedia && (url.startsWith("http://") || url.startsWith("https://"))) {
            if (!urls.contains(url)) {
                urls.add(url)
            }
        }
    }
    return urls
}

@Composable
fun LinkSnifferWidget(
    webUrl: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isScanning by remember { mutableStateOf(false) }
    var hasScanned by remember { mutableStateOf(false) }
    var detectedVideos by remember { mutableStateOf(listOf<String>()) }
    var activePlayingUrl by remember { mutableStateOf<String?>(null) }
    
    val domainName = remember(webUrl) {
        try {
            val uri = android.net.Uri.parse(webUrl)
            uri.host ?: "Webpage"
        } catch (e: Exception) {
            "Webpage"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF211F26).copy(alpha = 0.85f),
            contentColor = Color(0xFFE6E1E5)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Web Link",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1DM Video Sniffer & Streamer",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF)
                    )
                    Text(
                        text = domainName,
                        fontSize = 9.sp,
                        color = Color(0xFF938F99),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                IconButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(webUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuka link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Launch,
                        contentDescription = "Buka Browser",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!hasScanned) {
                Button(
                    onClick = {
                        isScanning = true
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val foundList = mutableListOf<String>()
                            try {
                                val client = okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                    .followRedirects(true)
                                    .build()
                                
                                val request = okhttp3.Request.Builder()
                                    .url(webUrl)
                                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                                    .build()
                                
                                client.newCall(request).execute().use { response ->
                                    if (response.isSuccessful) {
                                        val body = response.body?.string() ?: ""
                                        
                                        val regexSrc = Regex("(src|href|value|data-url)\\s*=\\s*[\"'](https?://[^\"']+)[\"']", RegexOption.IGNORE_CASE)
                                        regexSrc.findAll(body).forEach { match ->
                                            val foundUrl = match.groupValues[2]
                                            val lowerFound = foundUrl.lowercase()
                                            if (lowerFound.endsWith(".mp4") || 
                                                lowerFound.endsWith(".webm") || 
                                                lowerFound.endsWith(".mkv") || 
                                                lowerFound.endsWith(".avi") || 
                                                lowerFound.endsWith(".mov") || 
                                                lowerFound.endsWith(".3gp") ||
                                                lowerFound.endsWith(".m3u8") ||
                                                lowerFound.contains("video-stream") ||
                                                lowerFound.contains("raw-video")
                                            ) {
                                                if (!foundList.contains(foundUrl)) {
                                                    foundList.add(foundUrl)
                                                }
                                            }
                                        }
                                        
                                        val regexRaw = Regex("https?://[a-zA-Z0-9-._~:/?#\\[\\\\\\]@!$&'()*+,;=%]+\\.(mp4|webm|mkv|mov|avi|m3u8)", RegexOption.IGNORE_CASE)
                                        regexRaw.findAll(body).forEach { match ->
                                            val foundUrl = match.value
                                            if (!foundList.contains(foundUrl)) {
                                                foundList.add(foundUrl)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                detectedVideos = foundList
                                isScanning = false
                                hasScanned = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Scan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pindai File Video di Halaman Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (isScanning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menghubungi server & mengendus video...", fontSize = 10.sp, color = Color(0xFFE6E1E5))
                }
            } else {
                if (detectedVideos.isEmpty()) {
                    Text(
                        text = "Tidak ada link video langsung (.mp4, .m3u8, .webm, dll.) yang terendus dari halaman ini. Silakan buka browser langsung.",
                        fontSize = 10.sp,
                        color = Color(0xFFF2B8B5),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                } else {
                    Text(
                        text = "Terdeteksi ${detectedVideos.size} Aliran Video:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        detectedVideos.forEachIndexed { index, videoUrl ->
                            val videoExt = remember(videoUrl) {
                                val cleanUrl = videoUrl.substringBefore("?")
                                cleanUrl.substringAfterLast(".", "MP4").uppercase()
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2D2A33),
                                    contentColor = Color(0xFFE6E1E5)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Movie,
                                            contentDescription = "Video Ext",
                                            tint = Color(0xFFD0BCFF),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Aliran #${index + 1} ($videoExt)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    
                                    Text(
                                        text = videoUrl,
                                        fontSize = 8.sp,
                                        color = Color(0xFF938F99),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = {
                                                activePlayingUrl = if (activePlayingUrl == videoUrl) null else videoUrl
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = if (activePlayingUrl == videoUrl) Color(0xFFF2B8B5) else Color(0xFFD0BCFF)
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (activePlayingUrl == videoUrl) Icons.Filled.Close else Icons.Filled.PlayArrow,
                                                contentDescription = "Stream",
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (activePlayingUrl == videoUrl) "Tutup Stream" else "Putar Stream",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        TextButton(
                                            onClick = {
                                                try {
                                                    val cleanUrl = videoUrl.substringBefore("?")
                                                    val ext = cleanUrl.substringAfterLast(".", "mp4")
                                                    val rawFileName = "1DM_Sniffer_Download_${System.currentTimeMillis()}.$ext"
                                                    
                                                    val request = android.app.DownloadManager.Request(android.net.Uri.parse(videoUrl)).apply {
                                                        setTitle("DreamPlay 1DM Sniffer: $rawFileName")
                                                        setDescription("Mengunduh video langsung dari server stream")
                                                        setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                        setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, rawFileName)
                                                        setAllowedOverMetered(true)
                                                        setAllowedOverRoaming(true)
                                                    }
                                                    
                                                    val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                                    dm.enqueue(request)
                                                    
                                                    Toast.makeText(context, "Unduhan dimulai! Lihat progres di panel notifikasi.", Toast.LENGTH_LONG).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Unduhan gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    e.printStackTrace()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD0BCFF))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Download,
                                                contentDescription = "Download",
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Unduh Video", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    if (activePlayingUrl == videoUrl) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        VideoPlayer(
                                            videoUrl = videoUrl,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(8.dp))
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
fun ZoomableImage(
    model: Any,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .transformable(state = state)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 3f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var resolvedUrl by remember(videoUrl) { mutableStateOf<String?>(null) }
    var isResolving by remember(videoUrl) { mutableStateOf(true) }
    
    LaunchedEffect(videoUrl) {
        isResolving = true
        resolvedUrl = com.example.utils.VideoLinkExtractor.resolveDirectVideoUrl(videoUrl)
        isResolving = false
    }

    var isPlaying by remember(resolvedUrl) { mutableStateOf(false) }
    var isMuted by remember(resolvedUrl) { mutableStateOf(false) }
    var isPreparing by remember(resolvedUrl) { mutableStateOf(true) }
    var hasError by remember(resolvedUrl) { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var showControllers by remember { mutableStateOf(true) }
    
    // Auto-fade controllers after 3 seconds when playing
    LaunchedEffect(showControllers, isPlaying) {
        if (showControllers && isPlaying) {
            kotlinx.coroutines.delay(3000)
            showControllers = false
        }
    }
    
    // Initialize Media3 ExoPlayer with browser User-Agent to bypass CDN/Anti-Scraping blocks
    val exoPlayer = remember(resolvedUrl) {
        if (resolvedUrl == null) return@remember null
        try {
            val userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            val headers = java.util.HashMap<String, String>()
            headers["User-Agent"] = userAgent
            headers["Referer"] = resolvedUrl!!
            try {
                val uri = android.net.Uri.parse(resolvedUrl)
                if (uri.scheme != null && uri.host != null) {
                    headers["Origin"] = "${uri.scheme}://${uri.host}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers)
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)

            androidx.media3.exoplayer.ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    val mediaItem = if (resolvedUrl!!.contains(".m3u8", ignoreCase = true) || resolvedUrl!!.contains("m3u8", ignoreCase = true)) {
                        androidx.media3.common.MediaItem.Builder()
                            .setUri(resolvedUrl)
                            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                            .build()
                    } else {
                        androidx.media3.common.MediaItem.fromUri(resolvedUrl!!)
                    }
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
                }
        } catch (e: Exception) {
            hasError = true
            null
        }
    }

    // Release ExoPlayer on dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    // Handle play/pause and mute state updates
    LaunchedEffect(exoPlayer, isPlaying, isMuted) {
        exoPlayer?.let { player ->
            player.playWhenReady = isPlaying
            player.volume = if (isMuted) 0f else 1f
        }
    }

    // Monitor ExoPlayer state and errors
    LaunchedEffect(exoPlayer) {
        exoPlayer?.let { player ->
            player.addListener(object : androidx.media3.common.Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isPreparing = (state == androidx.media3.common.Player.STATE_BUFFERING) || 
                                  (state == androidx.media3.common.Player.STATE_IDLE && player.duration <= 0)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    hasError = true
                    isPreparing = false
                }
            })
        }
    }

    // Periodically update progress slider
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying && exoPlayer != null) {
            currentPosition = exoPlayer.currentPosition.toInt()
            duration = exoPlayer.duration.toInt().coerceAtLeast(0)
            kotlinx.coroutines.delay(250)
        }
    }

    fun formatTime(ms: Int): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%d:%02d", mins, secs)
    }

    @Composable
    fun PlayerContent(isFull: Boolean, onDismissFull: () -> Unit = {}) {
        Box(
            modifier = (if (isFull) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(220.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .clickable { showControllers = !showControllers },
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Mengekstraksi Video Link Latar Belakang...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Membaca HTTP Headers & Scraping CDN...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            } else if (!hasError && exoPlayer != null) {
                androidx.compose.ui.viewinterop.AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        androidx.media3.ui.PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { playerView ->
                        // AndroidView update callback if needed
                    }
                )
            }

            // Gesture controls for seeking and tapping
            if (!isPreparing && !hasError && exoPlayer != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    showControllers = !showControllers
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {},
                                onHorizontalDrag = { change, dragAmount ->
                                    val seekAmount = (dragAmount * 150).toLong() // scale touch movement to millisecond seek
                                    val target = (exoPlayer.currentPosition + seekAmount).coerceIn(0, exoPlayer.duration)
                                    exoPlayer.seekTo(target)
                                    currentPosition = target.toInt()
                                }
                            )
                        }
                )
            }

            // Processing / Buffering indicator
            if (isPreparing && !hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFD0BCFF), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Menghubungkan stream video...", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Beautiful Error Overlay with Safe Fallback Action
            if (hasError || exoPlayer == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF3B1E1E))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFF2B8B5),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gagal memutar stream video.",
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Silakan klik tombol di bawah untuk memutar / unduh langsung video ini di browser Anda!",
                            color = Color(0xFFE6E1E4),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(videoUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membuka browser", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Video di Browser", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (!isPreparing) {
                // Custom Controllers overlay with beautiful fade-in and fade-out animations
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControllers,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        // Mute & Fullscreen top buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    contentDescription = "Mute Toggle",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (isFull) {
                                        onDismissFull()
                                    } else {
                                        isFullScreen = true
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isFull) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Main Play, Fast-Forward, Rewind Controllers Centered
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Rewind 5s
                            IconButton(
                                onClick = {
                                    exoPlayer.let { player ->
                                        val target = (player.currentPosition - 5000).coerceAtLeast(0)
                                        player.seekTo(target)
                                        currentPosition = target.toInt()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FastRewind,
                                    contentDescription = "Rewind 5s",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Play / Pause
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Fast-Forward 5s
                            IconButton(
                                onClick = {
                                    exoPlayer.let { player ->
                                        val target = (player.currentPosition + 5000).coerceAtMost(player.duration)
                                        player.seekTo(target)
                                        currentPosition = target.toInt()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FastForward,
                                    contentDescription = "Forward 5s",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Seekbar / progress bar + duration labels at bottom
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                                onValueChange = { pct ->
                                    exoPlayer.let { player ->
                                        val target = (pct * player.duration).toLong()
                                        player.seekTo(target)
                                        currentPosition = target.toInt()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD0BCFF),
                                    activeTrackColor = Color(0xFFD0BCFF),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (isFullScreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                PlayerContent(isFull = true, onDismissFull = { isFullScreen = false })
            }
        }
    } else {
        PlayerContent(isFull = false)
    }
}

@Composable
fun RecentChatsPane(
    viewModel: RoleplayViewModel,
    sessions: List<com.example.data.database.ChatSessionEntity>,
    characters: List<CharacterEntity>,
    onOpenDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .padding(16.dp)
    ) {
        // Top Header Row with Burger Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onOpenDrawer
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Buka Menu",
                    tint = Color(0xFFD0BCFF)
                )
            }
            Column {
                Text(
                    text = "Riwayat Obrolan",
                    color = Color(0xFFD0BCFF),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Semua Sesi & Percakapan Terakhir",
                    color = Color(0xFFFF79C6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Rumpang",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Belum Ada Riwayat Obrolan",
                        color = Color(0xFF938F99),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mulai obrolan dengan karakter baru dari layar Personas!",
                        color = Color(0xFF938F99).copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            // Sort sessions by lastUpdated descending to show latest first
            val sortedSessions = remember(sessions) {
                sessions.sortedByDescending { it.lastUpdated }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedSessions) { session ->
                    val companion = if (!session.isGroup) {
                        characters.find { it.id == session.characterId }
                    } else null

                    val displayName = if (session.isGroup) {
                        session.groupName
                    } else {
                        companion?.name ?: "Karakter #${session.characterId}"
                    }

                    val avatarUri = if (session.isGroup) {
                        "gradient_group"
                    } else {
                        companion?.avatarUri ?: "gradient_custom_1"
                    }

                    val formattedDate = remember(session.lastUpdated) {
                        try {
                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                            sdf.format(java.util.Date(session.lastUpdated))
                        } catch (e: Exception) {
                            ""
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectSession(session) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CharacterAvatar(
                                avatarUri = avatarUri,
                                name = displayName,
                                size = 48.dp,
                                fontSize = 18.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (session.isGroup) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF79C6)),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "GRUP",
                                                color = Color.Black,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Draw sessionName
                                Text(
                                    text = session.sessionName,
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = "Waktu",
                                        tint = Color(0xFF938F99),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formattedDate,
                                        color = Color(0xFF938F99),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Delete button
                            IconButton(
                                onClick = { viewModel.deleteSession(session) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Hapus Riwayat",
                                    tint = Color(0xFFEE2D2D).copy(alpha = 0.8f),
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

