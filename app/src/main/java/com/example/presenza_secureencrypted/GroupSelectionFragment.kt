package com.example.presenza_secureencrypted

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.presenza_secureencrypted.databinding.FragmentGroupSelectionBinding

class GroupSelectionFragment : Fragment() {
    private var _binding: FragmentGroupSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.cvCseAiMl.setOnClickListener {
            navigateToHome("CSE (AI & ML)")
        }

        binding.cvCseAi.setOnClickListener {
            navigateToHome("CSE (AI)")
        }

        binding.cvCse.setOnClickListener {
            navigateToHome("CSE")
        }
    }

    private fun navigateToHome(groupName: String) {
        val homeFragment = HomeFragment().apply {
            arguments = Bundle().apply {
                putString("GROUP_NAME", groupName)
            }
        }
        
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, homeFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}