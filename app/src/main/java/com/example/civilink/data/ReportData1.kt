package com.example.civilink.data

data class ReportData1(
    val userId: String?,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String?,
    val problemStatement: String?,
    val spinnerSelectedItem: String?, // Add spinnerSelectedItem property
    val intValue: Int?, // Add intValue property
    val timestamp: Long?,// Add timestamp property
    var reportId: String? = null
) {
    // Required empty constructor for Firebase
    constructor() : this("", 0.0, 0.0, "", "", null, null, null,"")
}
