package com.example.presenza_secureencrypted

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.presenza_secureencrypted.databinding.FragmentManageStudentsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageStudentsFragment : Fragment() {
    private var _binding: FragmentManageStudentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private lateinit var adapter: StudentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        loadStudents()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = StudentAdapter { user ->
            showDeleteConfirmation(user)
        }
        binding.rvStudents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStudents.adapter = adapter
    }

    private fun loadStudents() {
        lifecycleScope.launch {
            val users = withContext(Dispatchers.IO) {
                database.userDao().getAll()
            }
            if (users.isEmpty()) {
                binding.tvNoStudents.visibility = View.VISIBLE
                binding.rvStudents.visibility = View.GONE
            } else {
                binding.tvNoStudents.visibility = View.GONE
                binding.rvStudents.visibility = View.VISIBLE
                adapter.submitList(users)
            }
        }
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to remove ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteStudent(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteStudent(user: User) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.userDao().delete(user)
            }
            Toast.makeText(context, "${user.name} removed from database", Toast.LENGTH_SHORT).show()
            loadStudents()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
