package com.example.presenza_secureencrypted

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.presenza_secureencrypted.databinding.FragmentRegisterStudentBinding

class RegisterStudentFragment : Fragment() {
    private var _binding: FragmentRegisterStudentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Section Dropdown
        val sections = arrayOf("AIML", "AI", "CSE")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sections)
        binding.actvSection.setAdapter(adapter)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnNext.setOnClickListener {
            val rollNo = binding.etRollNo.text.toString().trim()
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val section = binding.actvSection.text.toString().trim()

            if (rollNo.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || section.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to RecognitionFragment for enrollment
            // We pass the data to RecognitionFragment so it can save the embedding with these details
            val recognitionFragment = RecognitionFragment().apply {
                arguments = Bundle().apply {
                    putString("ROLL_NO", rollNo)
                    putString("FIRST_NAME", firstName)
                    putString("LAST_NAME", lastName)
                    putString("SECTION", section)
                    putBoolean("IS_ENROLLMENT", true)
                }
            }

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.slide_out_right)
                .replace(R.id.fragment_container, recognitionFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
