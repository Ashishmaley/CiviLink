package com.example.civilink
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.civilink.databinding.ActivityImageAndProblemStatementBinding

class ImageAndProblemStatement : AppCompatActivity() {

    private lateinit var binding: ActivityImageAndProblemStatementBinding
    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageAndProblemStatementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val data = intent.getStringExtra("key")
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        val bundle = Bundle()
        bundle.putString("key", data)
        bundle.putDouble("latitude", latitude)
        bundle.putDouble("longitude", longitude)
        navController.navigate(R.id.imageDisplayFragment, bundle)
    }
}
