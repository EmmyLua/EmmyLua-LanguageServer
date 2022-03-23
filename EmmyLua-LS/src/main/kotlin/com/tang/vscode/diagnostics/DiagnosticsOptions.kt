package com.tang.vscode.diagnostics

object DiagnosticsOptions {
    // 参数验证诊断
    var parameterValidation = false

    // any 类型是否可以赋值给其他类型
    var anyTypeCanAssignToAnyDefineType = true

    // any 类型是否可以接受任何变量的赋值
    var defineAnyTypeCanBeAssignedByAnyVariable = true

    // nil 是否可以赋值给任何定义类型
    var defineTypeCanReceiveNilType = true

    var fieldValidation = true
}