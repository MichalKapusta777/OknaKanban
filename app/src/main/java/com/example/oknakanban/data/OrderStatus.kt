package com.example.oknakanban.data

enum class OrderStatus(val label: String) {
    NEW("Nowe"),
    PLANNED("Zaplanowane"),
    TODAY("Na dziś"),
    IN_PROGRESS("W trakcie"),
    OVERDUE("Po terminie"),
    DONE("Zakończone")
}
