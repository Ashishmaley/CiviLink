package com.example.civilink.image_and_problem
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.civilink.databinding.ActivityImageAndProblemStatementBinding
import androidx.lifecycle.ViewModelProvider
import com.example.civilink.R
import com.example.civilink.data.models.SharedViewModel

@Suppress("DEPRECATION")
class ImageAndProblemStatement : AppCompatActivity() {

    private lateinit var binding: ActivityImageAndProblemStatementBinding
    private lateinit var navController: NavController
    private lateinit var sharedViewModel: SharedViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.black) // Set status bar color to black
            // Set navigation bar color (if available)
            window.navigationBarColor = resources.getColor(R.color.black) // Set navigation bar color to black
        }
        binding = ActivityImageAndProblemStatementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView2) as NavHostFragment

        navController = navHostFragment.navController
        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        val data = intent.getStringExtra("key")
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        data?.let { sharedViewModel.setImageUri(it) }
        sharedViewModel.setLatitude(latitude)
        sharedViewModel.setLongitude(longitude)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView2, ImageDisplayFragment())
            .commit()
    }

    fun BackButton(view: View) {
        super.onBackPressed()
    }

    fun navigateToProblem(view: View) {
            val transaction = this.supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainerView2, ProblemStatmentFragment())
            transaction.addToBackStack(null)
            transaction.commit()
    }

}
