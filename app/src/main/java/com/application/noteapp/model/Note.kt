package com.application.noteapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "Note")
data class Note(
    @PrimaryKey(autoGenerate = true)
    var id:Int = 0,
    val title:String,
    val content:String,
    val date:String,
    val color:Int = -1,
    val fontId:Int,
    var alarm_set:Boolean,
    var alarm_date:String,
    var is_locked:Boolean,
    var is_favorite:Boolean
):Serializable
