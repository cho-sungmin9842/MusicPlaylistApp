package com.example.simplemusicplayer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.simplemusicplayer.databinding.MediaItemBinding

class MediaListAdapter(private val onClick: (MusicEntity, Int) -> Unit) :
    ListAdapter<MusicEntity, ViewHolder>(diff) {
    companion object {
        private val diff = object : DiffUtil.ItemCallback<MusicEntity>() {
            override fun areItemsTheSame(oldItem: MusicEntity, newItem: MusicEntity): Boolean {
                return oldItem.korSongName == newItem.korSongName
            }

            override fun areContentsTheSame(oldItem: MusicEntity, newItem: MusicEntity): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            MediaItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position], onClick, position)
    }
}