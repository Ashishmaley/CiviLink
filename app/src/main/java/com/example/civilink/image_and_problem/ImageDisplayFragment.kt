package com.example.civilink.image_and_problem

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.civilink.databinding.FragmentImageDisplayBinding
import com.squareup.picasso.Picasso
import androidx.lifecycle.ViewModelProvider
import com.example.civilink.data.models.SharedViewModel
//ashishmaley

class ImageDisplayFragment : Fragment() {
    private lateinit var binding: FragmentImageDisplayBinding
    private lateinit var sharedViewModel: SharedViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        // Observe the LiveData from the shared ViewModel
        sharedViewModel.imageUri.observe(viewLifecycleOwner) { uri ->
            if (!uri.isNullOrEmpty()) {
                Picasso.get()
                    .load(uri)
                    .into(binding.capturedImageView)
            }
        }
        sharedViewModel.latitude.observe(viewLifecycleOwner) { lat ->
            binding.latitudeTextView.text = "Latitude: $lat"
        }
        sharedViewModel.longitude.observe(viewLifecycleOwner) { long ->
            binding.longitudeTextView.text = "Longitude: $long"
        }
    }

    override fun onResume() {
        super.onResume()
        parentFragmentManager.popBackStack()
    }
}
