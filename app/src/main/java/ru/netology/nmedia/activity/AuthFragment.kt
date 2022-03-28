package ru.netology.nmedia.activity

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.databinding.FragmentAuthBinding
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel


class AuthFragment : Fragment() {

    companion object {}

    private val postViewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentAuthBinding.inflate(inflater, container, false)

        with(binding) {

            btSignIn.setOnClickListener {
                val login = login.text.toString()
                val pass = password.text.toString()
                postViewModel.updateUser(login, pass)
                findNavController().navigateUp()
            }

            passwordVisibility.setOnClickListener {
                password.transformationMethod?.let {
                    password.transformationMethod = null
                } ?: run {
                    password.transformationMethod = PasswordTransformationMethod()
                }
            }
        }

        return binding.root
    }
}