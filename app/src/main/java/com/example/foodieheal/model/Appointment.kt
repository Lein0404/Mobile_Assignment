package com.example.foodieheal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    @SerialName("AppointmentID")
    val AppointmentID : String? = null,

    @SerialName("Date")
    val Date : String,

    @SerialName("Start_Time")
    val Start_Time : String,

    @SerialName("End_Time")
    val End_Time : String,

    @SerialName("Address")
    val Address : String,

    @SerialName("Postcode")
    val Postcode : String,

    @SerialName("State")
    val State : String,

    @SerialName("Note")
    val Note : String,

    @SerialName("Serving_Size")
    val Serving_Size : Int,

    @SerialName("Health_Preference")
    val Health_Preference : String,

    @SerialName("Total_Price")
    val Total_Price : Double,

    @SerialName("Status")
    val Status : String,

    @SerialName("rating")
    val rating : Double ?= null,

    @SerialName("Reject_Reason")
    val Reject_Reason : String ?= null,

    @SerialName("chefId")
    val chefId : String ="",

    @SerialName("userId")
    val userId : String = ""
)

