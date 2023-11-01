package com.example.civilink.data

data class SolvedReport(
    val reportId: String,
    val solvedBy: String,
    val report: ReportData, // Assuming ReportData is your data class for the report
    val solvedTime: String,
    val imageUrl: String
){
    constructor() : this("", "",ReportData(), "", "")
}
