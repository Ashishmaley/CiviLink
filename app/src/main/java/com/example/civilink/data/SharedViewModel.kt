package com.example.civilink.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val imageUri = MutableLiveData<String>()
    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()

    fun setImageUri(uri: String) {
        imageUri.value = uri
    }

    fun setLatitude(lat: Double) {
        latitude.value = lat
    }

    fun setLongitude(long: Double) {
        longitude.value = long
    }
}





