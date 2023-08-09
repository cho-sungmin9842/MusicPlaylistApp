package com.example.simplemusicplayer

import com.google.gson.annotations.SerializedName

data class MusicList(
    @SerializedName("music")
    val music: List<MusicEntity>
)

data class MusicEntity(
    @SerializedName("korSongName")
    val korSongName: String,
    @SerializedName("engSongName")
    val engSongName: String,
    @SerializedName("singer")
    val singer: String,
    @SerializedName("image")
    val image: String
):java.io.Serializable