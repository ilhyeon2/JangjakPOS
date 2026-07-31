package com.example.jangjakpos.ui

import android.widget.Toast
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
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var currentTime by remember { mutableStateOf(dateFormat.format(Date())) }
    var updateTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = dateFormat.format(now)
            DataManager.checkAndResetDaily(dayFormat.format(now))
            updateTrigger++
            delay(60000L)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("장작떼기 POS", style = MaterialTheme.typography.headlineLarge)
            Text("현재 시간: $currentTime", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), 
            modifier = Modifier.weight(1f)
        ) {
            items(7) { index ->
                val dummy = updateTrigger
                val table = DataManager.tables[index]
                val totalAmount = table.orders.sumOf { it.menuItem.price * it.quantity }
                val numFormat = NumberFormat.getNumberInstance(Locale.KOREA)
                
                Card(
                    modifier = Modifier.padding(8.dp).fillMaxWidth().height(180.dp).clickable {
                        navController.navigate("order/${table.id}")
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
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
            
            item {
                Card(
                    modifier = Modifier.padding(8.dp).fillMaxWidth().height(180.dp).clickable {
                        navController.navigate("admin_login")
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("관리자", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderScreen(tableId: Int, navController: androidx.navigation.NavController) {
    var showCheckout by remember { mutableStateOf(false) }
    val table = DataManager.tables.find { it.id == tableId } ?: return
    var updateTrigger by remember { mutableStateOf(0) }

    if (showCheckout) {
        CheckoutScreen(tableId, navController) { showCheckout = false }
        return
    }

    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text("테이블 $tableId 주문 내역", style = MaterialTheme.typography.headlineSmall)
            LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
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
            Button(onClick = { showCheckout = true }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("정산") }
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp)) { Text("뒤로 가기") }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp), 
            modifier = Modifier.weight(2f)
        ) {
            items(DataManager.menuItems.size) { index ->
                val menu = DataManager.menuItems[index]
                Card(modifier = Modifier.padding(4.dp)) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(menu.name, style = MaterialTheme.typography.bodyMedium)
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
                                updateTrigger++
                            }, modifier = Modifier.weight(1f).padding(end = 2.dp)) { Text("-") }
                            
                            Button(onClick = {
                                val existing = table.orders.find { it.menuItem.name == menu.name }
                                if (existing != null) existing.quantity++
                                else table.orders.add(OrderItem(menu, 1))
                                DataManager.saveTables()
                                updateTrigger++
                            }, modifier = Modifier.weight(1f).padding(start = 2.dp)) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutScreen(tableId: Int, navController: androidx.navigation.NavController, onCancel: () -> Unit) {
    val table = DataManager.tables.find { it.id == tableId } ?: return
    val totalAmount = table.orders.sumOf { it.menuItem.price * it.quantity }
    val numFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    
    val leftItems = table.orders.take(6)
    val centerItems = table.orders.drop(6).take(6)
    val rightItems = table.orders.drop(12) 

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("정산 내역", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                leftItems.forEach { order ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(order.menuItem.name, style = MaterialTheme.typography.bodyLarge)
                        Text("x ${order.quantity}", style = MaterialTheme.typography.bodyLarge)
                        Text("${numFormat.format(order.menuItem.price * order.quantity)}원", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                centerItems.forEach { order ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(order.menuItem.name, style = MaterialTheme.typography.bodyLarge)
                        Text("x ${order.quantity}", style = MaterialTheme.typography.bodyLarge)
                        Text("${numFormat.format(order.menuItem.price * order.quantity)}원", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                rightItems.forEach { order ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(order.menuItem.name, style = MaterialTheme.typography.bodyLarge)
                        Text("x ${order.quantity}", style = MaterialTheme.typography.bodyLarge)
                        Text("${numFormat.format(order.menuItem.price * order.quantity)}원", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("합계: ${numFormat.format(totalAmount)}원", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val receipt = Receipt(dateFormat.format(Date()), totalAmount, table.orders.toList())
                        DataManager.receipts.add(receipt)
                        DataManager.saveReceipts()
                        
                        DataManager.clearTable(tableId)
                        navController.popBackStack()
                    }, modifier = Modifier.weight(1f).height(55.dp).padding(end = 4.dp)) {
                        Text("지급완료")
                    }
                    
                    Button(onClick = onCancel, modifier = Modifier.weight(1f).height(55.dp).padding(start = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { 
                        Text("취소") 
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLoginScreen(navController: androidx.navigation.NavController) {
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("관리자 비밀번호를 입력하세요", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (password == DataManager.adminPassword) {
                navController.navigate("admin") { popUpTo("main") }
            }
        }) { Text("확인") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(navController: androidx.navigation.NavController) {
    val numFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val sdfDisplay = SimpleDateFormat("yy년 M월 d일", Locale.getDefault())
    
    var selectedMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                if (it != selectedMillis) {
                    selectedMillis = it
                    showDatePicker = false
                }
            }
        }

        Dialog(onDismissRequest = { showDatePicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(400.dp)) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showDatePicker = true }) {
                Text(text = "📅 $displayDateStr", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box {
                IconButton(onClick = { 
                    expandedMenu = true
                    Toast.makeText(context, "브랜치: ilhyeon2-patch-0731", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "설정", modifier = Modifier.size(32.dp))
                }
                DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                    DropdownMenuItem(text = { Text("메뉴 관리") }, onClick = { expandedMenu = false; navController.navigate("menu_settings") })
                    DropdownMenuItem(text = { Text("비밀번호 변경") }, onClick = { expandedMenu = false; navController.navigate("password_settings") })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp)) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Card(modifier = Modifier.fillMaxWidth().height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E5474))) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                            Text("월별 누적 매출 합계 :", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${numFormat.format(monthlyTotal)}원", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth().height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E5474))) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                            Text("일별 누적 매출 합계 :", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${numFormat.format(dailyTotal)}원", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
                Button(onClick = { navController.navigate("main") { popUpTo(0) } }, modifier = Modifier.fillMaxWidth().height(55.dp).padding(top = 8.dp)) {
                    Text("메인으로 돌아가기")
                }
            }
            
            LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp)) {
                if (dailyReceipts.isEmpty()) {
                    item { Text("해당 날짜의 정산 내역이 없습니다.", modifier = Modifier.padding(16.dp), color = Color.Gray) }
                } else {
                    items(dailyReceipts) { receipt ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("결제일시: ${receipt.date}", style = MaterialTheme.typography.bodyMedium)
                                Text("결제금액: ${numFormat.format(receipt.totalAmount)}원", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                receipt.items.forEach { item ->
                                    Text("- ${item.menuItem.name} x ${item.quantity}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSettingsScreen(navController: androidx.navigation.NavController) {
    // 적용 버튼을 눌러야만 원본에 저장되도록 상태 분리
    var menuList by remember { mutableStateOf(DataManager.menuItems.toList()) }
    var newMenuName by remember { mutableStateOf("") }
    var newMenuPrice by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 플로팅 및 위치 변경을 위한 글로벌 상태
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, "원복 및 뒤로가기")
            }
            Text("메뉴 관리", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    DataManager.menuItems = menuList.toMutableList()
                    DataManager.saveMenus()
                    Toast.makeText(context, "메뉴가 성공적으로 적용되었습니다.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }, 
                modifier = Modifier.height(45.dp)
            ) {
                Text("적용")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            // key를 메모리 주소값으로 설정하여 UI 노드가 데이터 위치를 완벽하게 따라가도록 구성
            items(
                count = menuList.size,
                key = { index -> System.identityHashCode(menuList[index]) }
            ) { index ->
                val menu = menuList[index]
                var priceText by remember(menu) { mutableStateOf(menu.price.toString()) }
                
                // 현재 아이템이 드래그 중인지 판단하여 시각적 효과(플로팅) 부여
                val isDragged = index == draggedIndex
                val zIndex = if (isDragged) 1f else 0f
                val elevation = if (isDragged) 12.dp else 2.dp
                
                val currentIndex by rememberUpdatedState(index)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .zIndex(zIndex) // 선택된 카드를 맨 위로
                        .graphicsLayer {
                            translationY = if (isDragged) dragOffset else 0f
                            scaleX = if (isDragged) 1.02f else 1f // 약간 커지는 효과
                            scaleY = if (isDragged) 1.02f else 1f
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation) // 그림자 진하게
                ) {
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "드래그하여 이동",
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 8.dp)
                                .pointerInput(menu) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedIndex = currentIndex
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y
                                            
                                            // 일정 높이(120px) 이상 이동 시 실시간 데이터 스위치
                                            val threshold = 120f
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

                                            // 플로팅 뷰를 따라 리스트가 자동으로 스크롤됨
                                            coroutineScope.launch {
                                                listState.scrollBy(dragAmount.y)
                                            }
                                        }
                                    )
                                }
                        )

                        // 보조용 화살표 버튼 (유지)
                        Column {
                            IconButton(onClick = {
                                if (index > 0) {
                                    val newList = menuList.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    menuList = newList.toList()
                                    coroutineScope.launch { listState.scrollBy(-150f) }
                                }
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, "위로")
                            }
                            IconButton(onClick = {
                                if (index < menuList.size - 1) {
                                    val newList = menuList.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    menuList = newList.toList()
                                    coroutineScope.launch { listState.scrollBy(150f) }
                                }
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, "아래로")
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(menu.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        
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
                            modifier = Modifier.width(140.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        IconButton(onClick = {
                            val newList = menuList.toMutableList()
                            newList.removeAt(index)
                            menuList = newList.toList()
                        }) {
                            Icon(Icons.Default.Delete, "삭제", tint = Color.Red)
                        }
                    }
                }
            }
        }
        
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newMenuName,
                onValueChange = { newMenuName = it },
                label = { Text("메뉴명") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = newMenuPrice,
                onValueChange = { newMenuPrice = it },
                label = { Text("가격") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val price = newMenuPrice.toIntOrNull()
                if (newMenuName.isNotBlank() && price != null) {
                    val newList = menuList.toMutableList()
                    newList.add(MenuItem(newMenuName, price))
                    menuList = newList.toList()
                    
                    newMenuName = ""
                    newMenuPrice = ""
                    
                    coroutineScope.launch { listState.scrollToItem(menuList.size - 1) }
                }
            }, modifier = Modifier.height(55.dp)) {
                Icon(Icons.Default.Add, "추가")
                Spacer(modifier = Modifier.width(4.dp))
                Text("추가")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSettingsScreen(navController: androidx.navigation.NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, "뒤로 가기")
            }
        }
        
        Text("비밀번호 변경", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(48.dp))
        
        // 텍스트 창 크기를 넓게 확보하여 잘림 방지 (weight(1f) 적용)
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.9f) 
        ) {
            
            Column(modifier = Modifier.weight(1f).padding(end = 32.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("현재 비밀번호") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(), // 가로로 꽉 차게 확장
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("새 비밀번호") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(), // 가로로 꽉 차게 확장
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        if (currentPassword == DataManager.adminPassword) {
                            if (newPassword.isNotBlank()) {
                                DataManager.savePassword(newPassword)
                                message = "비밀번호가 성공적으로 변경되었습니다."
                                currentPassword = ""
                                newPassword = ""
                            } else {
                                message = "새 비밀번호를 입력해주세요."
                            }
                        } else {
                            message = "현재 비밀번호가 일치하지 않습니다."
                        }
                    }, 
                    modifier = Modifier.width(120.dp).height(50.dp)
                ) {
                    Text("확인")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { navController.popBackStack() }, 
                    modifier = Modifier.width(120.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("취소")
                }
            }
        }
        
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(message, color = if (message.contains("성공")) Color.Blue else Color.Red, style = MaterialTheme.typography.titleMedium)
        }
    }
}
