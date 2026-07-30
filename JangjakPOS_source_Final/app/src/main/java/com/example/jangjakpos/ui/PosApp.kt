package com.example.jangjakpos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.jangjakpos.data.*
import kotlinx.coroutines.delay
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
    }
}

@Composable
fun MainScreen(navController: androidx.navigation.NavController) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var currentTime by remember { mutableStateOf(dateFormat.format(Date())) }
    
    // UI 갱신용 트리거
    var updateTrigger by remember { mutableStateOf(0) }

    // 1분마다 시간 갱신 및 날짜 변경 시 리셋 체크
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = dateFormat.format(now)
            DataManager.checkAndResetDaily(dayFormat.format(now))
            updateTrigger++
            delay(60000L) // 1분 대기
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("장작떼기 POS", style = MaterialTheme.typography.headlineLarge)
            Text("현재 시간: $currentTime", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // 고정 4열로 화면을 가득 채우도록 설정
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), 
            modifier = Modifier.weight(1f)
        ) {
            items(7) { index ->
                val dummy = updateTrigger
                val table = DataManager.tables[index]
                
                Card(
                    modifier = Modifier.padding(6.dp).fillMaxWidth().height(200.dp).clickable {
                        navController.navigate("order/${table.id}")
                    },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Text("테이블 ${table.id}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // 테이블 내부에서 메뉴 내역이 스크롤되도록 설정
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            if (table.orders.isEmpty()) {
                                Text("비어있음", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            } else {
                                table.orders.forEach { order ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(order.menuItem.name, style = MaterialTheme.typography.bodyMedium)
                                        Text("${order.quantity}개", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.padding(6.dp).fillMaxWidth().height(200.dp).clickable {
                        navController.navigate("admin_login")
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("관리자", style = MaterialTheme.typography.headlineSmall)
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
    
    // 강제 UI 갱신을 위한 변수
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
                                updateTrigger++ // 수량 변경 시 즉시 UI 업데이트
                            }, modifier = Modifier.weight(1f).padding(end = 2.dp)) { Text("-") }
                            
                            Button(onClick = {
                                val existing = table.orders.find { it.menuItem.name == menu.name }
                                if (existing != null) existing.quantity++
                                else table.orders.add(OrderItem(menu, 1))
                                DataManager.saveTables()
                                updateTrigger++ // 수량 변경 시 즉시 UI 업데이트
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
    
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("정산 내역", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(0.6f)) {
            items(table.orders.size) { index ->
                val order = table.orders[index]
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(order.menuItem.name, style = MaterialTheme.typography.titleMedium)
                    Text("x ${order.quantity}", style = MaterialTheme.typography.titleMedium)
                    Text("${order.menuItem.price * order.quantity}원", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        
        Text("합계: ${totalAmount}원", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        Row {
            Button(onClick = {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val receipt = Receipt(dateFormat.format(Date()), totalAmount, table.orders.toList())
                DataManager.receipts.add(receipt)
                DataManager.saveReceipts()
                
                DataManager.clearTable(tableId)
                navController.popBackStack()
            }, modifier = Modifier.padding(end = 16.dp).height(50.dp).width(120.dp)) {
                Text("지급완료")
            }
            Button(onClick = onCancel, modifier = Modifier.height(50.dp).width(120.dp)) { 
                Text("취소") 
            }
        }
    }
}

@Composable
fun AdminLoginScreen(navController: androidx.navigation.NavController) {
    var password by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("관리자 비밀번호를 입력하세요 (기본: 1234)", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it })
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
    var selectedDateStr by remember { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDateStr = sdf.format(Date(millis.toLong())) 
        } ?: run {
            selectedDateStr = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("관리자 페이지 (정산 내역)", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { navController.navigate("main") { popUpTo(0) } }) {
                Text("메인으로 돌아가기")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxSize()) {
            // 좌측: 달력 (상단 공간 최소화)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    title = { },     // 상단 공간 제거
                    headline = { },  // 큰 텍스트 제거
                    modifier = Modifier.padding(top = 0.dp)
                )
                Button(onClick = { selectedDateStr = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("전체 내역 보기")
                }
            }
            
            // 우측: 정산 내역 및 매출 합계 (스크롤 처리)
            Column(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                val filteredReceipts = if (selectedDateStr != null) {
                    DataManager.receipts.filter { it.date.startsWith(selectedDateStr!!) }
                } else {
                    DataManager.receipts
                }
                val totalSales = filteredReceipts.sumOf { it.totalAmount }
                
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedDateStr != null) "${selectedDateStr} 매출 합계" else "전체 누적 매출 합계", 
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("${totalSales}원", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredReceipts) { receipt ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("결제일시: ${receipt.date}", style = MaterialTheme.typography.bodyMedium)
                                Text("결제금액: ${receipt.totalAmount}원", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
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
