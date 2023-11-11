package com.example.civilink.data

class NotificationLocation {
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var token: String = ""

    // Add a no-argument constructor
    constructor()

    // Your existing constructor
    constructor(latitude: Double, longitude: Double, token: String) {
        this.latitude = latitude
        this.longitude = longitude
        this.token = token
    }
}
