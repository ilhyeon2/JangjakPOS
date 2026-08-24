package com.example.jangjakpos.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.navigation.compose.*
import com.example.jangjakpos.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getAppVersion(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}

fun downloadAndInstallApk(context: Context, apkUrl: String) {
    val fileName = "JangjakPOS_update.apk"
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
    if (file.exists()) file.delete()

    val request = DownloadManager.Request(Uri.parse(apkUrl))
        .setTitle("JangjakPOS 업데이트")
        .setDescription("최신 버전 앱을 다운로드 중입니다...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

    val downloadId = downloadManager.enqueue(request)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId && c != null) {
                val downloadedFile = File(c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (downloadedFile.exists()) {
                    val uri = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", downloadedFile)
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    c.startActivity(installIntent)
                }
                c.unregisterReceiver(this)
            }
        }
    }
    context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
}

object SettingsManager {
    private const val PREFS_NAME = "JangjakPosSettings"

    var volume by mutableStateOf(60)
    var isVibrationEnabled by mutableStateOf(true)
    var tableColorIndex by mutableStateOf(0)
    var menuColorIndex by mutableStateOf(0)

    val recommendedColors = listOf(
        Color(0xFFFFFFFF), 
        Color(0xFFD3E3FD), 
        Color(0xFFC8E6C9), 
        Color(0xFFFFE0B2), 
        Color(0xFFF8BBD0)  
    )

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        volume = prefs.getInt("volume", 60)
        isVibrationEnabled = prefs.getBoolean("vibration", true)
        tableColorIndex = prefs.getInt("tableColor", 0)
        menuColorIndex = prefs.getInt("menuColor", 0)
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("volume", volume)
            .putBoolean("vibration", isVibrationEnabled)
            .putInt("tableColor", tableColorIndex)
            .putInt("menuColor", menuColorIndex)
            .apply()
    }
}

@Composable
fun PosApp() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SettingsManager.init(context)
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController) }
        composable("order/{tableId}") { backStackEntry ->
            val tableId = backStackEntry.arguments?.getString("tableId")?.toIntOrNull() ?: 1
            OrderScreen(tableId, navController)
        }
        composable("admin_login") { AdminLoginScreen(navController) }
        composable("admin") { AdminScreen(navController) }
        composable("menu_settings") { MenuSettingsScreen(navController) }
        composable("password_settings") { PasswordSettingsScreen(navController) }
        composable("system_settings") { SystemSettingsScreen(navController) } 
    }
}

@Composable
fun MainScreen(navController: androidx.navigation.NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    
    val currentAppVersion = remember { getAppVersion(context) }

    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var currentDate by remember { mutableStateOf(dayFormat.format(Date())) }
    var updateTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentDate = dayFormat.format(now)
            DataManager.checkAndResetDaily(currentDate)
            updateTrigger++
            delay(60000L)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "장작떼기 POS", 
                style = if (isLandscape) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentDate, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { navController.navigate("admin_login") }) {
                    Icon(Icons.Default.Settings, contentDescription = "관리자 페이지", modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        val columnsCount = if (isLandscape) 4 else 2
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            modifier = Modifier.weight(1f)
        ) {
            items(DataManager.tables.size) { index ->
                val dummy = updateTrigger
                val table = DataManager.tables[index]
                val totalAmount = table.orders.sumOf { it.menuItem.price * it.quantity }
                val numFormat = NumberFormat.getNumberInstance(Locale.KOREA)
                
                Card(
                    modifier = Modifier.padding(6.dp).fillMaxWidth().height(160.dp).clickable {
                        navController.navigate("order/${table.id}")
                    },
                    colors = CardDefaults.cardColors(containerColor = SettingsManager.recommendedColors[SettingsManager.tableColorIndex]),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("테이블 ${table.id}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (table.orders.isNotEmpty()) {
                                val firstOrder = table.orders[0]
                                Text(
                                    text = "${firstOrder.menuItem.name} x ${firstOrder.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (table.orders.size > 1) {
                                    Text(
                                        text = "외 ${table.orders.size - 1}개",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                }
                            } else {
                                Text("비어있음", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                        
                        Text(
                            text = if (totalAmount > 0) "${numFormat.format(totalAmount)}원" else "",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.End),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Text(
            text = "ver. $currentAppVersion",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, end = 8.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun OrderScreen(tableId: Int, navController: androidx.navigation.NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var showCheckout by remember { mutableStateOf(false) }
    val table = DataManager.tables.find { it.id == tableId } ?: return
    var updateTrigger by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var topMessage by remember { mutableStateOf<String?>(null) }
    var highlightedItemName by remember { mutableStateOf<String?>(null) }

    fun showTopMessage(msg: String) {
        topMessage = msg
        coroutineScope.launch {
            delay(1500L)
            if (topMessage == msg) {
                topMessage = null
            }
        }
    }

    val highlightItem: (String) -> Unit = { name ->
        highlightedItemName = name
        coroutineScope.launch {
            delay(1000L)
            if (highlightedItemName == name) {
                highlightedItemName = null 
            }
        }
    }

    val onItemModified: (Int, String) -> Unit = { index, name ->
        highlightItem(name)
        coroutineScope.launch {
            delay(50) 
            if (index in 0 until table.orders.size) {
                listState.animateScrollToItem(index)
            }
        }
    }

    if (showCheckout) {
        CheckoutScreen(tableId, navController) { showCheckout = false }
        return
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("테이블 $tableId 주문 내역", style = MaterialTheme.typography.headlineSmall)
                
                AnimatedVisibility(
                    visible = topMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = topMessage ?: "",
                            modifier = Modifier.padding(8.dp),
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val dummy = updateTrigger
                        items(table.orders.toList()) { order ->
                            OrderListItem(
                                order = order, 
                                table = table, 
                                isHighlighted = (order.menuItem.name == highlightedItemName),
                                onMessage = { msg -> showTopMessage(msg) },
                                onHighlight = { highlightItem(order.menuItem.name) }
                            ) { updateTrigger++ }
                        }
                    }
                }
                Button(
                    onClick = { showCheckout = true }, 
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) { 
                    Text("정산 보기", style = MaterialTheme.typography.titleLarge) 
                }
            }
            
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(2f)) {
                items(DataManager.menuItems.size) { index -> 
                    MenuButton(index, table, onItemModified = onItemModified) { updateTrigger++ } 
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("테이블 $tableId 주문 내역", style = MaterialTheme.typography.headlineSmall)
            
            AnimatedVisibility(
                visible = topMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = topMessage ?: "",
                        modifier = Modifier.padding(8.dp),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val dummy = updateTrigger
                    items(table.orders.toList()) { order ->
                        OrderListItem(
                            order = order, 
                            table = table, 
                            isHighlighted = (order.menuItem.name == highlightedItemName),
                            onMessage = { msg -> showTopMessage(msg) },
                            onHighlight = { highlightItem(order.menuItem.name) }
                        ) { updateTrigger++ }
                    }
                }
            }
            
            Button(
                onClick = { showCheckout = true }, 
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 8.dp)
            ) { 
                Text("정산 보기", style = MaterialTheme.typography.titleLarge) 
            }
            
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1.8f)) {
                items(DataManager.menuItems.size) { index -> 
                    MenuButton(index, table, onItemModified = onItemModified) { updateTrigger++ } 
                }
            }
        }
    }
}

@Composable
fun OrderListItem(
    order: OrderItem, 
    table: Table, 
    isHighlighted: Boolean,
    onMessage: (String) -> Unit, 
    onHighlight: () -> Unit,
    onUpdate: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isHighlighted) Color.Red else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 500), 
        label = "textColorAnimation"
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = order.menuItem.name, 
            style = MaterialTheme.typography.bodyLarge, 
            color = textColor, 
            modifier = Modifier.weight(1.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = "${order.quantity}개", 
            style = MaterialTheme.typography.bodyLarge, 
            color = textColor, 
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = {
                if (order.quantity > 0) {
                    order.quantity--
                    if (order.quantity == 0) {
                        table.orders.remove(order)
                    } else {
                        onHighlight() 
                    }
                    DataManager.saveTables()
                    onMessage("${order.menuItem.name} 1개 취소됨")
                    onUpdate()
                }
            },
            modifier = Modifier.size(width = 44.dp, height = 36.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MenuButton(index: Int, table: Table, onItemModified: (Int, String) -> Unit, onUpdate: () -> Unit) {
    val menu = DataManager.menuItems[index]
    var isPressed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() 

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressAnimation"
    )

    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .height(110.dp) 
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        val existingIndex = table.orders.indexOfFirst { it.menuItem.name == menu.name }
                        val targetIndex = if (existingIndex != -1) {
                            table.orders[existingIndex].quantity++
                            existingIndex
                        } else {
                            table.orders.add(OrderItem(menu, 1))
                            table.orders.size - 1
                        }
                        DataManager.saveTables()

                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val ringerMode = audioManager.ringerMode

                        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
                            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                try {
                                    val volume = SettingsManager.volume 
                                    if (volume > 0) {
                                        val toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, volume)
                                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 30) 
                                        
                                        coroutineScope.launch {
                                            delay(250L)
                                            toneGen.release()
                                        }
                                    }
                                } catch (e: Exception) {
                                }
                            }
                            
                            if (SettingsManager.isVibrationEnabled) {
                                try {
                                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                        vibratorManager.defaultVibrator
                                    } else {
                                        @Suppress("DEPRECATION")
                                        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    }

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(30)
                                    }
                                } catch (e: Exception) {
                                }
                            }
                        }

                        onUpdate()
                        onItemModified(targetIndex, menu.name)
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = SettingsManager.recommendedColors[SettingsManager.menuColorIndex]),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(), 
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = menu.name, 
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold, 
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${menu.price}원", 
                style = MaterialTheme.typography.bodyLarge,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun CheckoutScreen(tableId: Int, navController: androidx.navigation.NavController, onCancel: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val table = DataManager.tables.find { it.id == tableId } ?: return
    val totalAmount = table.orders.sumOf { it.menuItem.price * it.quantity }
    val numFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("정산 내역", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLandscape) {
            val leftItems = table.orders.take(6)
            val centerItems = table.orders.drop(6).take(6)
            val rightItems = table.orders.drop(12) 
            
            Row(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    leftItems.forEach { order -> CheckoutItemRow(order, numFormat) }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    centerItems.forEach { order -> CheckoutItemRow(order, numFormat) }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    rightItems.forEach { order -> CheckoutItemRow(order, numFormat) }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("합계: ${numFormat.format(totalAmount)}원", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    CheckoutActionButtons(tableId, totalAmount, table, navController, onCancel)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(table.orders) { order ->
                    CheckoutItemRow(order, numFormat)
                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Text("합계: ${numFormat.format(totalAmount)}원", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                CheckoutActionButtons(tableId, totalAmount, table, navController, onCancel)
            }
        }
    }
}

@Composable
fun CheckoutItemRow(order: OrderItem, numFormat: NumberFormat) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = order.menuItem.name, 
            style = MaterialTheme.typography.bodyLarge, 
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("x ${order.quantity}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
        Text("${numFormat.format(order.menuItem.price * order.quantity)}원", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
fun CheckoutActionButtons(tableId: Int, totalAmount: Int, table: Table, navController: androidx.navigation.NavController, onCancel: () -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onCancel, modifier = Modifier.weight(1f).height(55.dp).padding(end = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { 
            Text("취소") 
        }
        Button(onClick = {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val receipt = Receipt(dateFormat.format(Date()), totalAmount, table.orders.toList())
            DataManager.receipts.add(receipt)
            DataManager.saveReceipts()
            DataManager.clearTable(tableId)
            navController.popBackStack()
        }, modifier = Modifier.weight(1f).height(55.dp).padding(start = 4.dp)) {
            Text("지급완료")
        }
    }
}

@Composable
fun AdminLoginScreen(navController: androidx.navigation.NavController) {
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("관리자 비밀번호", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.SpaceBetween) {
             Button(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f).height(50.dp).padding(end = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("취소")
            }
            Button(onClick = {
                if (password == DataManager.adminPassword) {
                    navController.navigate("admin") { popUpTo("main") }
                }
            }, modifier = Modifier.weight(1f).height(50.dp).padding(start = 8.dp)) { 
                Text("확인") 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: androidx.navigation.NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val numFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val sdfDisplay = SimpleDateFormat("yy년 M월 d일", Locale.getDefault())
    
    var selectedMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val currentAppVersion = remember { getAppVersion(context) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            val success = DataManager.exportBackup(context, it)
            if (success) Toast.makeText(context, "백업 파일이 안전하게 저장되었습니다.", Toast.LENGTH_SHORT).show()
            else Toast.makeText(context, "백업 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val success = DataManager.importBackup(context, it)
            if (success) {
                Toast.makeText(context, "데이터 복원 완료! 최신 정보 확인을 위해 메인으로 이동합니다.", Toast.LENGTH_LONG).show()
                navController.navigate("main") { popUpTo(0) }
            } else {
                Toast.makeText(context, "복원 실패: 올바른 백업 파일인지 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val selectedDate = Date(selectedMillis)
    val selectedDayStr = sdfDay.format(selectedDate)
    val selectedMonthStr = sdfMonth.format(selectedDate)
    val displayDateStr = sdfDisplay.format(selectedDate)

    val dailyReceipts = DataManager.receipts.filter { it.date.startsWith(selectedDayStr) }
    val dailyTotal = dailyReceipts.sumOf { it.totalAmount }
    val monthlyTotal = DataManager.receipts.filter { it.date.startsWith(selectedMonthStr) }.sumOf { it.totalAmount }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        LaunchedEffect(datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let {
                if (it != selectedMillis) { selectedMillis = it; showDatePicker = false }
            }
        }
        Dialog(onDismissRequest = { showDatePicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(450.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        DatePicker(state = datePickerState, showModeToggle = false, title = { }, headline = { })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDatePicker = false }) { Text("닫기") }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.navigate("main") { popUpTo(0) } }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "메인으로 돌아가기")
            }
            TextButton(onClick = { showDatePicker = true }) {
                Text(text = "📅 $displayDateStr", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "설정", modifier = Modifier.size(28.dp))
                }
                DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                    
                    // 💡 예외 발생 시 e.message 를 출력하여 원인을 파악하도록 수정한 부분
                    DropdownMenuItem(text = { Text("앱 업데이트 확인") }, onClick = { 
                        expandedMenu = false
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val url = java.net.URL("https://raw.githubusercontent.com/ilhyeon2/JangjakPOS/main/release.txt")
                                val text = url.readText().trim()
                                val lines = text.lines().filter { it.isNotBlank() }
                                
                                withContext(Dispatchers.Main) {
                                    if (lines.isNotEmpty()) {
                                        val targetVersion = lines[0].trim()
                                        val downloadLink = if (lines.size > 1) lines[1].trim() else ""
                                        
                                        if (targetVersion != currentAppVersion) {
                                            if (downloadLink.startsWith("http")) {
                                                Toast.makeText(context, "새 버전($targetVersion) 다운로드를 시작합니다...", Toast.LENGTH_SHORT).show()
                                                downloadAndInstallApk(context, downloadLink)
                                            } else {
                                                Toast.makeText(context, "업데이트 링크가 누락되어 업데이트를 진행할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "현재 최신 버전($currentAppVersion)을 사용 중입니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "서버의 업데이트 정보를 읽을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    // 💡 에러 원인을 직접 화면에 출력
                                    Toast.makeText(context, "업데이트 실패 원인: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    })
                    Divider()
                    
                    DropdownMenuItem(text = { Text("시스템 설정") }, onClick = { expandedMenu = false; navController.navigate("system_settings") }) 
                    DropdownMenuItem(text = { Text("메뉴 관리") }, onClick = { expandedMenu = false; navController.navigate("menu_settings") })
                    DropdownMenuItem(text = { Text("비밀번호 변경") }, onClick = { expandedMenu = false; navController.navigate("password_settings") })
                    Divider()
                    DropdownMenuItem(text = { Text("데이터 백업 (내보내기)") }, onClick = { 
                        expandedMenu = false
                        val exportDateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                        val fileName = "JangjakPOS_Backup_${exportDateFormat.format(Date())}.zip"
                        exportLauncher.launch(fileName)
                    })
                    DropdownMenuItem(text = { Text("데이터 복원 (가져오기)") }, onClick = { 
                        expandedMenu = false
                        importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
                    })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp)) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        AdminSalesCard("월별 누적 매출 합계 :", monthlyTotal, numFormat)
                        Spacer(modifier = Modifier.height(16.dp))
                        AdminSalesCard("일별 누적 매출 합계 :", dailyTotal, numFormat)
                    }
                }
                
                LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp)) {
                    if (dailyReceipts.isEmpty()) {
                        item { Text("해당 날짜의 정산 내역이 없습니다.", modifier = Modifier.padding(16.dp), color = Color.Gray) }
                    } else {
                        items(dailyReceipts) { receipt -> AdminReceiptCard(receipt, numFormat) }
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                AdminSalesCard("월별 누적 매출", monthlyTotal, numFormat, modifier = Modifier.weight(1f).padding(end = 4.dp))
                AdminSalesCard("일별 누적 매출", dailyTotal, numFormat, modifier = Modifier.weight(1f).padding(start = 4.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("정산 내역", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (dailyReceipts.isEmpty()) {
                    item { Text("해당 날짜의 정산 내역이 없습니다.", modifier = Modifier.padding(16.dp), color = Color.Gray, textAlign = TextAlign.Center) }
                } else {
                    items(dailyReceipts) { receipt -> AdminReceiptCard(receipt, numFormat) }
                }
            }
        }
    }
}

@Composable
fun AdminSalesCard(title: String, amount: Int, numFormat: NumberFormat, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E5474))) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${numFormat.format(amount)}원", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AdminReceiptCard(receipt: Receipt, numFormat: NumberFormat) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(receipt.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("${numFormat.format(receipt.totalAmount)}원", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            receipt.items.forEach { item ->
                Text("- ${item.menuItem.name} x ${item.quantity}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SystemSettingsScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var tempVolume by remember { mutableStateOf(SettingsManager.volume.toFloat()) }
    var tempVibration by remember { mutableStateOf(SettingsManager.isVibrationEnabled) }
    var tempTableColor by remember { mutableStateOf(SettingsManager.tableColorIndex) }
    var tempMenuColor by remember { mutableStateOf(SettingsManager.menuColorIndex) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "뒤로가기") }
            Text("시스템 설정", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    SettingsManager.volume = tempVolume.toInt()
                    SettingsManager.isVibrationEnabled = tempVibration
                    SettingsManager.tableColorIndex = tempTableColor
                    SettingsManager.menuColorIndex = tempMenuColor
                    SettingsManager.save(context)
                    Toast.makeText(context, "시스템 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }, 
                modifier = Modifier.height(40.dp)
            ) { Text("저장") }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("소리/진동 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("터치음 볼륨: ${tempVolume.toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = tempVolume,
                    onValueChange = { tempVolume = it },
                    onValueChangeFinished = {
                        try {
                            val volume = tempVolume.toInt()
                            if (volume > 0) {
                                val toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, volume)
                                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 30) 
                                coroutineScope.launch {
                                    delay(250L)
                                    toneGen.release()
                                }
                            }
                        } catch (e: Exception) {
                        }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("버튼 터치 진동 켜기", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = tempVibration, onCheckedChange = { tempVibration = it })
                }
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("화면 테마 색상 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("메인 화면 테이블 카드 색상", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SettingsManager.recommendedColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (tempTableColor == index) 3.dp else 1.dp, 
                                    color = if (tempTableColor == index) MaterialTheme.colorScheme.primary else Color.LightGray, 
                                    shape = CircleShape
                                )
                                .clickable { tempTableColor = index }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("주문 화면 메뉴 버튼 색상", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SettingsManager.recommendedColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (tempMenuColor == index) 3.dp else 1.dp, 
                                    color = if (tempMenuColor == index) MaterialTheme.colorScheme.primary else Color.LightGray, 
                                    shape = CircleShape
                                )
                                .clickable { tempMenuColor = index }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSettingsScreen(navController: androidx.navigation.NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var menuList by remember { mutableStateOf(DataManager.menuItems.toList()) }
    var newMenuName by remember { mutableStateOf("") }
    var newMenuPrice by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "원복 및 뒤로가기") }
            Text("메뉴 관리", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    DataManager.menuItems = menuList.toMutableList()
                    DataManager.saveMenus()
                    Toast.makeText(context, "적용되었습니다.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }, 
                modifier = Modifier.height(40.dp)
            ) { Text("적용") }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(
                count = menuList.size,
                key = { index -> System.identityHashCode(menuList[index]) }
            ) { index ->
                val menu = menuList[index]
                var priceText by remember(menu) { mutableStateOf(menu.price.toString()) }
                
                val isDragged = index == draggedIndex
                val zIndex = if (isDragged) 1f else 0f
                val elevation = if (isDragged) 12.dp else 2.dp
                val currentIndex by rememberUpdatedState(index)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .zIndex(zIndex)
                        .graphicsLayer {
                            translationY = if (isDragged) dragOffset else 0f
                            scaleX = if (isDragged) 1.02f else 1f
                            scaleY = if (isDragged) 1.02f else 1f
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                ) {
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "이동",
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 4.dp)
                                .pointerInput(menu) {
                                    detectDragGestures(
                                        onDragStart = { draggedIndex = currentIndex; dragOffset = 0f },
                                        onDragEnd = { draggedIndex = null; dragOffset = 0f },
                                        onDragCancel = { draggedIndex = null; dragOffset = 0f },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y
                                            val threshold = 100f
                                            val cIdx = draggedIndex ?: return@detectDragGestures
                                            
                                            if (dragOffset > threshold && cIdx < menuList.size - 1) {
                                                val newList = menuList.toMutableList()
                                                val temp = newList[cIdx]
                                                newList[cIdx] = newList[cIdx + 1]
                                                newList[cIdx + 1] = temp
                                                menuList = newList.toList()
                                                draggedIndex = cIdx + 1
                                                dragOffset -= threshold
                                            } else if (dragOffset < -threshold && cIdx > 0) {
                                                val newList = menuList.toMutableList()
                                                val temp = newList[cIdx]
                                                newList[cIdx] = newList[cIdx - 1]
                                                newList[cIdx - 1] = temp
                                                menuList = newList.toList()
                                                draggedIndex = cIdx - 1
                                                dragOffset += threshold
                                            }
                                            coroutineScope.launch { listState.scrollBy(dragAmount.y) }
                                        }
                                    )
                                }
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                if (index > 0) {
                                    val newList = menuList.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    menuList = newList.toList()
                                    coroutineScope.launch { listState.scrollBy(-120f) }
                                }
                            }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.KeyboardArrowUp, "위로") }
                            IconButton(onClick = {
                                if (index < menuList.size - 1) {
                                    val newList = menuList.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    menuList = newList.toList()
                                    coroutineScope.launch { listState.scrollBy(120f) }
                                }
                            }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.KeyboardArrowDown, "아래로") }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            menu.name, 
                            modifier = Modifier.weight(1f), 
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { 
                                priceText = it
                                it.toIntOrNull()?.let { newPrice ->
                                    val newList = menuList.toMutableList()
                                    newList[index].price = newPrice
                                    menuList = newList.toList()
                                }
                            },
                            modifier = Modifier.width(100.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                        )
                        
                        IconButton(onClick = {
                            val newList = menuList.toMutableList()
                            newList.removeAt(index)
                            menuList = newList.toList()
                        }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "삭제", tint = Color.Red) }
                    }
                }
            }
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newMenuName, onValueChange = { newMenuName = it }, label = { Text("메뉴명") }, modifier = Modifier.weight(1f), singleLine = true)
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(value = newMenuPrice, onValueChange = { newMenuPrice = it }, label = { Text("가격") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val price = newMenuPrice.toIntOrNull()
                    if (newMenuName.isNotBlank() && price != null) {
                        val newList = menuList.toMutableList()
                        newList.add(MenuItem(newMenuName, price))
                        menuList = newList.toList()
                        newMenuName = ""; newMenuPrice = ""
                        coroutineScope.launch { listState.scrollToItem(menuList.size - 1) }
                    }
                }, modifier = Modifier.height(55.dp)) {
                    Icon(Icons.Default.Add, "추가")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("추가")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = newMenuName, onValueChange = { newMenuName = it }, label = { Text("새 메뉴명") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newMenuPrice, onValueChange = { newMenuPrice = it }, label = { Text("가격") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val price = newMenuPrice.toIntOrNull()
                        if (newMenuName.isNotBlank() && price != null) {
                            val newList = menuList.toMutableList()
                            newList.add(MenuItem(newMenuName, price))
                            menuList = newList.toList()
                            newMenuName = ""; newMenuPrice = ""
                            coroutineScope.launch { listState.scrollToItem(menuList.size - 1) }
                        }
                    }, modifier = Modifier.height(55.dp)) { Icon(Icons.Default.Add, "추가") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSettingsScreen(navController: androidx.navigation.NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "뒤로 가기") }
        }
        
        Text("비밀번호 변경", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(48.dp))
        
        if (isLandscape) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth(0.9f)) {
                Column(modifier = Modifier.weight(1f).padding(end = 32.dp)) {
                    OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = { Text("현재 비밀번호") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("새 비밀번호") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = {
                        if (currentPassword == DataManager.adminPassword) {
                            if (newPassword.isNotBlank()) { DataManager.savePassword(newPassword); message = "비밀번호가 성공적으로 변경되었습니다."; currentPassword = ""; newPassword = "" } else { message = "새 비밀번호를 입력해주세요." }
                        } else { message = "현재 비밀번호가 일치하지 않습니다." }
                    }, modifier = Modifier.width(120.dp).height(50.dp)) { Text("확인") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }, modifier = Modifier.width(120.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("취소") }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it }, label = { Text("현재 비밀번호") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("새 비밀번호") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f).height(50.dp).padding(end = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("취소") }
                Button(onClick = {
                    if (currentPassword == DataManager.adminPassword) {
                        if (newPassword.isNotBlank()) { DataManager.savePassword(newPassword); message = "비밀번호가 성공적으로 변경되었습니다."; currentPassword = ""; newPassword = "" } else { message = "새 비밀번호를 입력해주세요." }
                    } else { message = "현재 비밀번호가 일치하지 않습니다." }
                }, modifier = Modifier.weight(1f).height(50.dp).padding(start = 8.dp)) { Text("확인") }
            }
        }
        
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(message, color = if (message.contains("성공")) Color.Blue else Color.Red, style = MaterialTheme.typography.titleMedium)
        }
    }
}
