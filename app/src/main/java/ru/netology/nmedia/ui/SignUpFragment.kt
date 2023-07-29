package ru.netology.nmedia.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.databinding.FragmentSignUpBinding
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.viewmodel.AuthViewModel

@AndroidEntryPoint
class SignUpFragment : Fragment() {
    private val viewModelAuth: AuthViewModel by viewModels(ownerProducer = ::requireParentFragment)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignUpBinding.inflate(inflater, container, false)

        binding.buttonSignUp.setOnClickListener {

            if (binding.password.text?.trim().toString() == binding.passwordConfirm.text?.trim()
                    .toString()
            ) {
                val name = binding.name.text?.trim().toString()
                val login = binding.login.text?.trim().toString()
                val pass = binding.passwordConfirm.text?.trim().toString()
                viewModelAuth.registration(name, login, pass)
                AndroidUtils.hideKeyboard(it)
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "password is not correct", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigateUp()
        }
        return binding.root
    }
}