package com.example.simplemusicplayer

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.simplemusicplayer.databinding.MediaItemBinding

class ViewHolder(private val binding: MediaItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(musicEntity: MusicEntity, onClick: (MusicEntity, Int) -> Unit, position: Int) {
        Glide.with(binding.imageView)
            .load(musicEntity.image)
            .centerCrop()
            .override(200, 200)
            .into(binding.imageView)
        binding.singerTextView.text = musicEntity.singer
        binding.songNameTextView.text = musicEntity.korSongName
        binding.root.setOnClickListener {
            onClick(musicEntity, position)
        }
    }
}