package com.tang.vscode.diagnostics

enum class InspectionsLevel(val level: String){
    None("none"),
    Warning("warning"),
    Error("error"),
}

object DiagnosticsOptions {
    // any 类型是否可以赋值给其他类型
    var anyTypeCanAssignToAnyDefineType = true

    // any 类型是否可以接受任何变量的赋值
    var defineAnyTypeCanBeAssignedByAnyVariable = true

    // nil 是否可以赋值给任何定义类型
    var defineTypeCanReceiveNilType = false

    var fieldValidation = InspectionsLevel.None

    // 参数验证诊断
    var parameterValidation = InspectionsLevel.None

    var undeclaredVariable = InspectionsLevel.None

    var assignValidation = InspectionsLevel.None

    var deprecated = InspectionsLevel.None
}