package com.example.civilink.data
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _photoUri = MutableLiveData<String>()
    val photoUri: LiveData<String> get() = _photoUri

    fun setPhotoUri(uri: String) {
        _photoUri.value = uri
    }
}




