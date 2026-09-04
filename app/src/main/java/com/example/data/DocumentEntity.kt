package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val format: String,
    val contentJson: String,
    val plainTextPreview: String,
    val wordCount: Int,
    val characterCount: Int,
    val lastModified: Long
)
