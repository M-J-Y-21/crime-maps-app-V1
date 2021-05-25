package com.rkpandey.mymaps.models

import java.io.Serializable

data class Place(
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val colour: Float
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1095118160427263382
    }
}