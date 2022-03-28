package ru.netology.nmedia.activity

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentRegistrationBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class RegistrationFragment : Fragment() {

    private val postViewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    companion object {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRegistrationBinding.inflate(inflater, container, false)

        with(binding) {
            btSignUp.setOnClickListener {
                val name = name.text.toString()
                val login = login.text.toString()
                val pass = password.text.toString()
                val confirmPass = confirmPassword.text.toString()

                if (name.isNotBlank() &&
                    login.isNotBlank() &&
                    pass.isNotBlank()
                ) {
                    when (pass == confirmPass) {
                        true -> {
                            postViewModel.registerUser(login, pass, name)
                            findNavController().navigateUp()
                        }
                        false -> Snackbar.make(
                            binding.root,
                            R.string.pass_different,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            passwordVisibility.setOnClickListener {
                Log.i("setOnClickListener", "true")
                password.transformationMethod?.let {
                    password.transformationMethod = null
                } ?: run {
                    password.transformationMethod = PasswordTransformationMethod()
                }
                confirmPassword.transformationMethod = password.transformationMethod
            }
        }

        return binding.root
    }
}