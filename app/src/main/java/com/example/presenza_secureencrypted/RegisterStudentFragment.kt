package com.example.presenza_secureencrypted

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
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

        // Enforce 12-digit validation logic
        setupRollNoValidation()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnNext.setOnClickListener {
            val rollNo = binding.etRollNo.text.toString().trim()
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val section = binding.actvSection.text.toString().trim()

            if (rollNo.length != 12) {
                Toast.makeText(context, "Roll Number must be exactly 12 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (firstName.isEmpty() || lastName.isEmpty() || section.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to RecognitionFragment for enrollment
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

    private fun setupRollNoValidation() {
        binding.etRollNo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                if (length == 12) {
                    // Turn Green
                    val greenColor = ContextCompat.getColor(requireContext(), R.color.success_green)
                    val colorStateList = ColorStateList.valueOf(greenColor)
                    binding.tilRollNo.setBoxStrokeColorStateList(colorStateList)
                    binding.tilRollNo.hintTextColor = colorStateList
                    binding.tilRollNo.defaultHintTextColor = colorStateList
                } else {
                    // Reset to default (White)
                    val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
                    val colorStateList = ColorStateList.valueOf(whiteColor)
                    binding.tilRollNo.setBoxStrokeColorStateList(colorStateList)
                    binding.tilRollNo.hintTextColor = colorStateList
                    binding.tilRollNo.defaultHintTextColor = colorStateList
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
