package com.example.civilink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.civilink.databinding.FragmentImageDisplayBinding
import com.squareup.picasso.Picasso
//ashishmaley
class ImageDisplayFragment : Fragment() {
    private lateinit var binding: FragmentImageDisplayBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImageDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the image URI or resource ID from arguments
        val imageUri = arguments?.getString("key")

        val latitude = arguments?.getDouble("latitude", 0.0)
        val longitude = arguments?.getDouble("longitude", 0.0)

        // Load the image using Picasso (or any other image loading library)
        if (!imageUri.isNullOrEmpty()) {
            Picasso.get()
                .load(imageUri)
                .into(binding.capturedImageView)
        }
    }
}
