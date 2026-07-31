package com.example.jangjakpos.ui

import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.navigation.compose.*
import com.example.jangjakpos.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PosApp() {
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
    }
}

@Composable
fun MainScreen(navController: androidx.navigation.NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
                modifier = Modifier.weight(1f)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (table.orders.size > 1) {
                                    Text(
                                        text = "외 ${table.orders.size - 1}개",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                Text("비어있음", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                        
                        Text(
                            text = if (totalAmount > 0) "${numFormat.format(totalAmount)}원" else "",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderScreen(tableId: Int, navController: androidx.navigation.NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var showCheckout by remember { mutableStateOf(false) }
    val table = DataManager.tables.find { it.id == tableId } ?: return
    var updateTrigger by remember { mutableStateOf(0) }

    if (showCheckout) {
        CheckoutScreen(tableId, navController) { showCheckout = false }
        return
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("테이블 $tableId 주문 내역", style = MaterialTheme.typography.headlineSmall)
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val dummy = updateTrigger
                        items(table.orders.size) { index ->
                            val order = table.orders[index]
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(order.menuItem.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${order.quantity}개", style = MaterialTheme.typography.bodyLarge)
                                Text("${order.menuItem.price * order.quantity}원", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                Button(onClick = { showCheckout = true }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("정산") }
                Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("뒤로 가기") }
            }
            
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 130.dp), modifier = Modifier.weight(2f)) {
                items(DataManager.menuItems.size) { index -> MenuButton(index, table) { updateTrigger++ } }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("테이블 $tableId 주문 내역", style = MaterialTheme.typography.headlineSmall)
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val dummy = updateTrigger
                    items(table.orders.size) { index ->
                        val order = table.orders[index]
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(order.menuItem.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("${order.quantity}개", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                            Text("${order.menuItem.price * order.quantity}원", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f).height(50.dp).padding(end = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("뒤로 가기") }
                Button(onClick = { showCheckout = true }, modifier = Modifier.weight(1f).height(50.dp).padding(start = 4.dp)) { Text("정산") }
            }
            
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 100.dp), modifier = Modifier.weight(1.5f)) {
                items(DataManager.menuItems.size) { index -> MenuButton(index, table) { updateTrigger++ } }
            }
        }
    }
}

@Composable
fun MenuButton(index: Int, table: Table, onUpdate: () -> Unit) {
    val menu = DataManager.menuItems[index]
    Card(modifier = Modifier.padding(4.dp)) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(menu.name, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Text("${menu.price}원", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    val existing = table.orders.find { it.menuItem.name == menu.name }
                    if (existing != null && existing.quantity > 0) {
                        existing.quantity--
                        if(existing.quantity == 0) table.orders.remove(existing)
                    }
                    DataManager.saveTables()
                    onUpdate()
                }, modifier = Modifier.weight(1f).padding(end = 2.dp), contentPadding = PaddingValues(0.dp)) { Text("-") }
                
                Button(onClick = {
                    val existing = table.orders.find { it.menuItem.name == menu.name }
                    if (existing != null) existing.quantity++
                    else table.orders.add(OrderItem(menu, 1))
                    DataManager.saveTables()
                    onUpdate()
                }, modifier = Modifier.weight(1f).padding(start = 2.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
            }
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
        Text(order.menuItem.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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

    // 데이터 백업 내보내기 런처
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            val success = DataManager.exportBackup(context, it)
            if (success) Toast.makeText(context, "백업 파일이 안전하게 저장되었습니다.", Toast.LENGTH_SHORT).show()
            else Toast.makeText(context, "백업 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 데이터 복원 가져오기 런처
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
                        Text(menu.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        
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
