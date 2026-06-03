package com.example.presenza_secureencrypted

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.presenza_secureencrypted.databinding.FragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getDatabase(requireContext())

        // Get group name from arguments
        val groupName = arguments?.getString("GROUP_NAME") ?: "General Section"
        
        // Update the UI with the group name
        binding.tvGroupName.text = groupName
        binding.tvWelcome.text = "Marking Attendance for"

        updateStats()

        binding.btnMarkAttendance.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, RecognitionFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateStats() {
        lifecycleScope.launch {
            // 1. Get Total Students from Local Database
            val users = withContext(Dispatchers.IO) {
                database.userDao().getAll()
            }
            binding.tvStudentCount.text = users.size.toString()

            // 2. Get Today's Recognized count from Firebase
            try {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("attendance")
                    .whereGreaterThanOrEqualTo("timestamp", today)
                    .get()
                    .await()
                
                binding.tvRecognizedCount.text = snapshot.size().toString()
            } catch (e: Exception) {
                binding.tvRecognizedCount.text = "0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
