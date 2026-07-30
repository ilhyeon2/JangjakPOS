package com.example.jangjakpos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.jangjakpos.data.*
import kotlinx.coroutines.delay
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
            items(7, key = { index -> "$index-$updateTrigger" }) { index ->
                val table = DataManager.tables[index]
                val totalAmount = table.orders.sumOf { it.menuItem.price * it.quantity }
                val numFormat = NumberFormat.getNumberInstance(Locale.KOREA)
                
                Card(
                    modifier = Modifier.padding(8.dp).fillMaxWidth().height(160.dp).clickable {
                        navController.navigate("order/${table.id}")
                    }
                ) {
                    // 요구사항 1: 테이블 번호 아래 주문내역, 총금액은 가장 아래 부분에 배치
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxSize(), 
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("테이블 ${table.id}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(modifier = Modifier.height(60.dp)) {
                                if (table.orders.isNotEmpty()) {
                                    table.orders.take(3).forEach { order ->
                                        Text(
                                            text = "${order.menuItem.name} x ${order.quantity}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (table.orders.size > 3) {
                                        Text("외 ${table.orders.size - 3}건", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                        
                        Text(
                            text = if (totalAmount > 0) "${numFormat.format(totalAmount)}원" else "비어있음",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.padding(8.dp).fillMaxWidth().height(160.dp).clickable {
                        navController.navigate("admin_login")
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("관리자", style = MaterialTheme.typography.titleMedium)
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
                items(table.orders.size, key = { index -> "$index-$updateTrigger" }) { index ->
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
    
    // 요구사항 2: 왼쪽 화면에 5개 내역 우선 출력, 공간 부족 시 오른쪽 공간에 나머지 내역 표시
    val leftOrders = table.orders.take(5)
    val rightOrders = table.orders.drop(5)
    
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("정산 내역", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.weight(1f).fillMaxWidth(0.8f)) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                leftOrders.forEach { order ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(order.menuItem.name, style = MaterialTheme.typography.titleMedium)
                        Text("x ${order.quantity}", style = MaterialTheme.typography.titleMedium)
                        Text("${numFormat.format(order.menuItem.price * order.quantity)}원", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (rightOrders.isNotEmpty()) {
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    rightOrders.forEach { order ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(order.menuItem.name, style = MaterialTheme.typography.titleMedium)
                            Text("x ${order.quantity}", style = MaterialTheme.typography.titleMedium)
                            Text("${numFormat.format(order.menuItem.price * order.quantity)}원", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        
        Text("합계: ${numFormat.format(totalAmount)}원", style = MaterialTheme.typography.headlineMedium)
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
        OutlinedTextField(value = password, onValueChange = { password = it }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
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
    
    // 요구사항 3: 메인으로 나갔다 들어오면 항상 현재 날짜로 초기화되도록 시스템 현재 시간 사용
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var showDatePicker by remember { mutableStateOf(false) }
    
    val selectedDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val selectedDate = Date(selectedDateMillis)
    
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val uiDateFormat = SimpleDateFormat("yy년 M월 d일", Locale.KOREA)
    
    val selectedDayStr = dayFormat.format(selectedDate)
    val selectedMonthStr = monthFormat.format(selectedDate)

    val dailyReceipts = DataManager.receipts.filter { it.date.startsWith(selectedDayStr) }
    val monthlyReceipts = DataManager.receipts.filter { it.date.startsWith(selectedMonthStr) }

    val dailyTotal = dailyReceipts.sumOf { it.totalAmount }
    val monthlyTotal = monthlyReceipts.sumOf { it.totalAmount }

    // 요구사항 3: 날짜 클릭 시 전체 화면에 달력 표시
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("관리자 페이지", style = MaterialTheme.typography.titleMedium)
         
            // ✅ 아래와 같이 Alignment.CenterVertically로 수정해주세요.
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.clickable { showDatePicker = true }.padding(8.dp)
            ) {
                Text("📅 ${uiDateFormat.format(selectedDate)}", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxSize()) {
            // 요구사항 4: 왼쪽에는 월별/일별 매출 합계 표시
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E5470))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("월별 누적 매출 합계 :", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${numFormat.format(monthlyTotal)}원", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E5470))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("일별 누적 매출 합계 :", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${numFormat.format(dailyTotal)}원", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }

                Button(
                    onClick = { navController.navigate("main") { popUpTo(0) } },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A68A6))
                ) {
                    Text("메인으로 돌아가기")
                }
            }
            
            // 요구사항 4: 우측에는 선택된 일별 매출 내역이 스크롤되어 표시
            LazyColumn(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                items(dailyReceipts) { receipt ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFE6F7))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("결제일시: ${receipt.date}", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("결제금액: ${numFormat.format(receipt.totalAmount)}원", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            receipt.items.forEach { item ->
                                Text("- ${item.menuItem.name} x ${item.quantity}", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
