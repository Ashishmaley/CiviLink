package com.example.civilink.data.models
import androidx.lifecycle.ViewModel

class ImageViewModel : ViewModel() {
    var selectedImageUrl: String? = null
    var userEmail: String? = null
    var problemDescription: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var reportId: String? = null
    var spinnerSelectedItem: String? = null // Add this property
    var intValue: Int? = null // Add this property
    var timestamp: Long? = null // Add this property
}
