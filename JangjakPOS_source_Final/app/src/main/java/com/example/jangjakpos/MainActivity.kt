package com.example.jangjakpos
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 1. 데이터 모델 정의
// ==========================================
data class MenuItem(val id: Int, val name: String, val price: Int)

data class OrderItem(
    val menu: MenuItem,
    var quantity: Int = 1
)

// ==========================================
// 2. 메인 POS 화면 컴포저블
// ==========================================
@Composable
fun PosMainScreen() {
    // 테스트용 메뉴 데이터
    val menuList = listOf(
        MenuItem(1, "장작구이 통닭", 20000),
        MenuItem(2, "치즈 장작구이", 23000),
        MenuItem(3, "국물 떡볶이", 15000),
        MenuItem(4, "생맥주 500cc", 4500),
        MenuItem(5, "소주", 5000),
        MenuItem(6, "음료수", 2000)
    )

    // 주문 내역 상태 관리
    val orderList = remember { mutableStateListOf<OrderItem>() }

    // 총 결제 금액 계산
    val totalPrice = orderList.sumOf { it.menu.price * it.quantity }

    // 화면을 가로로 5:5 비율로 나눔 (왼쪽: 주문내역, 오른쪽: 메뉴판)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // [왼쪽 영역] 주문 내역 리스트
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "주문 내역",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                
                // 최신 버전에 맞게 HorizontalDivider 로 변경
                HorizontalDivider()

                // 주문 리스트 (스크롤 가능)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(orderList) { order ->
                        OrderItemRow(
                            order = order,
                            onIncrease = {
                                val index = orderList.indexOf(order)
                                if (index != -1) { // 안전 장치 추가
                                    orderList[index] = order.copy(quantity = order.quantity + 1)
                                }
                            },
                            onDecrease = {
                                val index = orderList.indexOf(order)
                                if (index != -1) { // 안전 장치 추가
                                    if (order.quantity > 1) {
                                        orderList[index] = order.copy(quantity = order.quantity - 1)
                                    } else {
                                        orderList.remove(order)
                                    }
                                }
                            },
                            onDelete = {
                                orderList.remove(order)
                            }
                        )
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }

                HorizontalDivider(thickness = 2.dp)

                // 총 결제 금액
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("총 결제 금액", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("%,d원".format(totalPrice), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // [오른쪽 영역] 메뉴판 (2열 그리드)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // 2열 구조
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuList) { menu ->
                    MenuGridItem(menu = menu) { selectedMenu ->
                        // 메뉴 클릭 시 주문 내역에 추가 (이미 있으면 수량 증가)
                        val existingOrder = orderList.find { it.menu.id == selectedMenu.id }
                        if (existingOrder != null) {
                            val index = orderList.indexOf(existingOrder)
                            if (index != -1) {
                                orderList[index] = existingOrder.copy(quantity = existingOrder.quantity + 1)
                            }
                        } else {
                            orderList.add(OrderItem(selectedMenu, 1))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. 주문 내역의 개별 아이템 행
// ==========================================
@Composable
fun OrderItemRow(
    order: OrderItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 왼쪽 영역: 메뉴명 및 가격
        Column(modifier = Modifier.weight(1.5f)) {
            Text(
                text = order.menu.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "%,d원".format(order.menu.price),
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // 2. 가운데 영역: 수량 조절 및 수량 표시 (정중앙 위치)
        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "감소")
            }
            
            Text(
                text = "${order.quantity}",
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "증가")
            }
        }

        // 3. 오른쪽 영역: 삭제 버튼 (끝으로 밀착)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ==========================================
// 4. 메뉴판의 개별 메뉴 버튼 (클릭 애니메이션 적용)
// ==========================================
@Composable
fun MenuGridItem(menu: MenuItem, onClick: (MenuItem) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // 클릭 시 살짝 작아지는 애니메이션 효과 (에러 방지용 label 추가)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "ButtonScaleAnimation"
    )

    Button(
        onClick = { onClick(menu) },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f) // 버튼 비율 설정
            .scale(scale),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = menu.name,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%,d원".format(menu.price),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
        }
    }
}
