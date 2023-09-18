package  com.example.civilink
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.civilink.data.ImageViewModel
import com.example.civilink.data.SharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.squareup.picasso.Picasso

class MyBottomSheetFragment : BottomSheetDialogFragment() {
    private var imageUrl: String? = null
    private lateinit var imageViewModel: SharedViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_bottom_sheet, container, false)
        Log.d("MyBottomSheetFragment", "onCreateView called")
        val imageViewModel: ImageViewModel by activityViewModels()
        val imageView = view.findViewById<ImageView>(R.id.imageView)
        var userEmail = view.findViewById<TextView>(R.id.useId)
        var problemDescription = view.findViewById<TextView>(R.id.problem)

        val imageUrl = imageViewModel.selectedImageUrl
        if (imageUrl != null) {
            Log.d("bottomUP", "$imageUrl")
            userEmail.text = imageViewModel.userEmail
            problemDescription.text = imageViewModel.problemDescription

            Picasso.get()
                .load(imageUrl)
                .into(imageView)
        }
        return view
    }


    fun showCustom(manager: FragmentManager, tag: String?) {
        Log.d("bottomUP", "tran")
        Log.d("bottomUP", "$tag")

        if (tag != null) {
            Log.d("bottomUP", "transection")
            val transaction = manager.beginTransaction()
            transaction.add(this, tag)
            transaction.commitAllowingStateLoss()
        }
    }

//    override fun onDestroyView() {
//        // Cancel any ongoing image loading when the fragment is destroyed
//        Picasso.get().cancelRequest(imageView)
//        super.onDestroyView()
//    }
}
