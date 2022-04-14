package ru.netology.nmedia.dto

data class TextItemSeparator(
    override val id: Long,
    val text: String,
) : FeedItem