package com.example.civilink.data

data class ReportData(
    val userId: String?,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String?,
    val problemStatement: String?,
    val spinnerSelectedItem: String?, // Add spinnerSelectedItem property
    val intValue: Int?, // Add intValue property
    val timestamp: Long?// Add timestamp property
) {
    // Required empty constructor for Firebase
    constructor() : this("", 0.0, 0.0, "", "", null, null, null)
}
