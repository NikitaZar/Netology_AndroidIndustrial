package ru.netology.nmedia.activity

import android.os.Bundle
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
    private val authViewModel: AuthViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentAuthBinding.inflate(inflater, container, false)

        with(binding) {

            //password.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            //password.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD

            btSignIn.setOnClickListener {
                val login = login.text.toString()
                val pass = password.text.toString()
                postViewModel.updateUser(login, pass)
                findNavController().navigateUp()
            }
        }

        authViewModel.data.observe(viewLifecycleOwner){

        }


        return binding.root
    }
}