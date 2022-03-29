package ru.netology.nmedia.activity

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toFile
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        val pickPhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    ImagePicker.RESULT_ERROR -> {
                        Snackbar.make(
                            binding.root,
                            ImagePicker.getError(it.data),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    Activity.RESULT_OK -> {
                        val uri: Uri? = it.data?.data
                        postViewModel.changeAvatar(uri)
                    }
                }
            }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .provider(ImageProvider.GALLERY)
                .galleryMimeTypes(
                    arrayOf(
                        "image/png",
                        "image/jpeg",
                    )
                )
                .createIntent(pickPhotoLauncher::launch)
        }

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .provider(ImageProvider.CAMERA)
                .createIntent(pickPhotoLauncher::launch)
        }

        binding.removePhoto.setOnClickListener {
            postViewModel.changeAvatar(null)
        }

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

            postViewModel.avatar.observe(viewLifecycleOwner) {
                if (it.uri == null) {
                    val emptyAvatarDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_empty_avatar, null)
                    binding.avatar.setImageDrawable(emptyAvatarDrawable)
                    return@observe
                }
                binding.avatar.setImageURI(it.uri)
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