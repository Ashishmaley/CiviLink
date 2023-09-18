package com.example.civilink.image_and_problem
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.civilink.databinding.ActivityImageAndProblemStatementBinding
import androidx.lifecycle.ViewModelProvider
import com.example.civilink.R
import com.example.civilink.data.SharedViewModel

@Suppress("DEPRECATION")
class ImageAndProblemStatement : AppCompatActivity() {

    private lateinit var binding: ActivityImageAndProblemStatementBinding
    private lateinit var navController: NavController
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageAndProblemStatementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView2) as NavHostFragment

        navController = navHostFragment.navController
        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        // Get the data from the intent
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
