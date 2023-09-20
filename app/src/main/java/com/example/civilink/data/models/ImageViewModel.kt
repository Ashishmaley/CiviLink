package com.example.civilink.data.models

import androidx.lifecycle.ViewModel

class ImageViewModel : ViewModel() {
    var reportId: String?=null
    var longitude: Double?= null
    var latitude: Double?=null
    var selectedImageUrl: String? = null
    var userEmail: String? = null
    var problemDescription: String? = null
}
