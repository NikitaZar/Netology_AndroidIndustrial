package ru.netology.nmedia.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.databinding.FragmentAuthBinding
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class AuthFragment : Fragment() {

    companion object {}

    private val postViewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAuthBinding.inflate(inflater, container, false)

        with(binding) {

            btSignIn.setOnClickListener {
                val login = login.text.toString()
                val pass = password.text.toString()
                postViewModel.updateUser(login, pass)
                findNavController().navigateUp()
            }
        }

        return binding.root
    }
}