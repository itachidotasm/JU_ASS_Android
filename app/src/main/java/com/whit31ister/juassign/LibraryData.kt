package com.whit31ister.juassign

import com.google.gson.annotations.SerializedName

data class Manifest(
    @SerializedName("files") val files: List<AssignmentFile>
)

data class AssignmentFile(
    @SerializedName("path") val path: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String
)
