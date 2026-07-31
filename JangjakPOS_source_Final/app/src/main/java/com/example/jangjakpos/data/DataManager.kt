package com.example.jangjakpos.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 메뉴 정보 수정이 가능하도록 var로 변경
data class MenuItem(var name: String, var price: Int)
data class OrderItem(val menuItem: MenuItem, var quantity: Int)
data class Table(val id: Int, var orders: MutableList<OrderItem> = mutableListOf())
data class Receipt(val date: String, val totalAmount: Int, val items: List<OrderItem>)

object DataManager {
    private lateinit var dataFile: File
    private lateinit var receiptFile: File
    private lateinit var menuFile: File
    private lateinit var dateFile: File
    private lateinit var passwordFile: File // 비밀번호 저장용 파일 추가
    private val gson = Gson()

    var tables = List(7) { Table(it + 1) }
    var receipts = mutableListOf<Receipt>()
    var menuItems = mutableListOf<MenuItem>()
    var adminPassword = "1234"
    private var lastDateStr = ""

    fun init(context: Context) {
        val dir = context.filesDir
        dataFile = File(dir, "tables.json")
        receiptFile = File(dir, "receipts.json")
        menuFile = File(dir, "menus.json")
        dateFile = File(dir, "last_date.txt")
        passwordFile = File(dir, "password.txt") 
        loadData()
    }

    private fun loadData() {
        // 비밀번호 로드 (없으면 기본값 1234 생성)
        if (passwordFile.exists()) {
            adminPassword = passwordFile.readText()
        } else {
            adminPassword = "1234"
            savePassword(adminPassword)
        }

        if (menuFile.exists()) {
            val type = object : TypeToken<MutableList<MenuItem>>() {}.type
            menuItems = gson.fromJson(menuFile.readText(), type) ?: mutableListOf()
        } else {
            menuItems = mutableListOf(
                MenuItem("오리주물럭", 35000), MenuItem("오리로스", 35000),
                MenuItem("삼겹살(1인)", 13000), MenuItem("추가반마리", 20000),
                MenuItem("된장찌개", 2000), MenuItem("볶음밥", 2000),
                MenuItem("공기밥", 1000), MenuItem("쫄면", 2000),
                MenuItem("떡", 2000), MenuItem("소주", 4000),
                MenuItem("맥주", 5000), MenuItem("막걸리", 4000),
                MenuItem("청하", 6000), MenuItem("백세주", 10000),
                MenuItem("음료수", 2000)
            )
            saveMenus()
        }
        if (dataFile.exists()) {
            val type = object : TypeToken<List<Table>>() {}.type
            tables = gson.fromJson(dataFile.readText(), type) ?: List(7) { Table(it + 1) }
        }
        if (receiptFile.exists()) {
            val type = object : TypeToken<MutableList<Receipt>>() {}.type
            receipts = gson.fromJson(receiptFile.readText(), type) ?: mutableListOf()
        }
        
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (dateFile.exists()) {
            lastDateStr = dateFile.readText()
            checkAndResetDaily(todayStr)
        } else {
            lastDateStr = todayStr
            dateFile.writeText(lastDateStr)
        }
    }

    fun checkAndResetDaily(todayStr: String) {
        if (lastDateStr != todayStr) {
            tables.forEach { it.orders.clear() }
            saveTables()
            lastDateStr = todayStr
            dateFile.writeText(lastDateStr)
        }
    }

    fun saveTables() { dataFile.writeText(gson.toJson(tables)) }
    fun saveReceipts() { receiptFile.writeText(gson.toJson(receipts)) }
    fun saveMenus() { menuFile.writeText(gson.toJson(menuItems)) }
    
    // 비밀번호 저장 함수
    fun savePassword(newPwd: String) { 
        adminPassword = newPwd
        passwordFile.writeText(adminPassword) 
    }
    
    fun clearTable(tableId: Int) {
        tables.find { it.id == tableId }?.orders?.clear()
        saveTables()
    }
}
