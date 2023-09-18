package com.example.civilink.data
data class ReportData(
    val userId: String?,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String?,
    val problemDescription: String?
) {
    // Required empty constructor for Firebase
    constructor() : this("", 0.0, 0.0, "", "")
}

