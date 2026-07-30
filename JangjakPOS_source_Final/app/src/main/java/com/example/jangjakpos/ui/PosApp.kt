package com.example.jangjakpos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.jangjakpos.data.*
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
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val currentTime = remember { mutableStateOf(dateFormat.format(Date())) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("장작떼기 POS", style = MaterialTheme.typography.headlineLarge)
            Text("현재 시간: ${currentTime.value}", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // 고정 4열 -> 적응형 그리드로 변경 (가로 길이에 맞춰 테이블 크기와 개수 자동 조절)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp), 
            modifier = Modifier.weight(1f)
        ) {
            items(7) { index ->
                val table = DataManager.tables[index]
                val totalAmount = table.orders.sumOf { it.menuItem.price * it.quantity }
                Card(
                    modifier = Modifier.padding(8.dp).fillMaxWidth().aspectRatio(1.2f).clickable {
                        navController.navigate("order/${table.id}")
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("테이블 ${table.id}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(if (totalAmount > 0) "${totalAmount}원" else "비어있음")
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.padding(8.dp).fillMaxWidth().aspectRatio(1.2f).clickable {
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
    var orders by remember { mutableStateOf(table.orders.toList()) }

    if (showCheckout) {
        CheckoutScreen(tableId, navController) { showCheckout = false }
        return
    }

    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text("테이블 $tableId 주문 내역", style = MaterialTheme.typography.headlineSmall)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(orders) { order ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(order.menuItem.name)
                        Text("${order.quantity}개")
                        Text("${order.menuItem.price * order.quantity}원")
                    }
                }
            }
            Button(onClick = { showCheckout = true }, modifier = Modifier.fillMaxWidth()) { Text("정산") }
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("뒤로 가기") }
        }
        
        // 고정 3열 -> 적응형 그리드로 변경 (화면 비율에 맞춰 메뉴 버튼 배열 자동 조절)
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
                                orders = table.orders.map { it.copy() }
                            }, modifier = Modifier.weight(1f).padding(end = 2.dp)) { Text("-") }
                            
                            Button(onClick = {
                                val existing = table.orders.find { it.menuItem.name == menu.name }
                                if (existing != null) existing.quantity++
                                else table.orders.add(OrderItem(menu, 1))
                                DataManager.saveTables()
                                orders = table.orders.map { it.copy() }
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
            items(table.orders) { order ->
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
    
    // 달력 스크롤 처리를 위한 상태 추가
    val scrollState = rememberScrollState()

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
            // 좌측: 달력 (세로 스크롤 추가)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
                    .verticalScroll(scrollState) // 기기 화면이 작을 경우 스크롤 허용
            ) {
                Text("날짜 선택", style = MaterialTheme.typography.titleMedium)
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(onClick = { selectedDateStr = null }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    Text("전체 내역 보기")
                }
            }
            
            // 우측: 정산 내역 및 매출 합계
            Column(modifier = Modifier.weight(1.2f)) {
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
                
                LazyColumn(modifier = Modifier.weight(1f)) {
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
