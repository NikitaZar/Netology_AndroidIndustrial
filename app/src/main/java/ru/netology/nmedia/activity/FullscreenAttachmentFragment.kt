package ru.netology.nmedia.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.netology.nmedia.activity.FullscreenAttachmentFragment.Companion.url
import ru.netology.nmedia.databinding.FragmentFullscreenAttachmentBinding
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.view.load

class FullscreenAttachmentFragment : Fragment() {

    companion object {
        var Bundle.url: String? by StringArg
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFullscreenAttachmentBinding.inflate(
            inflater,
            container,
            false
        )

        arguments?.url?.let { url -> binding.attachment.load(url) }

        return binding.root
    }


}