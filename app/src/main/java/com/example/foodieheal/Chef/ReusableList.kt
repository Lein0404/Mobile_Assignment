package com.example.foodieheal.Chef

import com.example.foodieheal.R

val States = listOf(
    "Pulau Pinang",
    "Kedah",
    "Perak",
    "Perlis",
    "Selangor",
    "Negeri Sembilan",
    "Johor",
    "Melaka",
    "Pahang",
    "Terengganu",
    "Sabah",
    "Sarawak"
)

val StateResList = listOf(
    R.string.state_pulau_pinang,
    R.string.state_kedah,
    R.string.state_perak,
    R.string.state_perlis,
    R.string.state_selangor,
    R.string.state_negeri_sembilan,
    R.string.state_johor,
    R.string.state_melaka,
    R.string.state_pahang,
    R.string.state_terengganu,
    R.string.state_sabah,
    R.string.state_sarawak
)

fun getStateDbName(resId: Int): String = when (resId) {
    R.string.state_pulau_pinang -> "Pulau Pinang"
    R.string.state_kedah -> "Kedah"
    R.string.state_perak -> "Perak"
    R.string.state_perlis -> "Perlis"
    R.string.state_selangor -> "Selangor"
    R.string.state_negeri_sembilan -> "Negeri Sembilan"
    R.string.state_johor -> "Johor"
    R.string.state_melaka -> "Melaka"
    R.string.state_pahang -> "Pahang"
    R.string.state_terengganu -> "Terengganu"
    R.string.state_sabah -> "Sabah"
    R.string.state_sarawak -> "Sarawak"
    else -> ""
}

fun getStateResId(name: String): Int? = when (name.trim().lowercase()) {
    "pulau pinang" -> R.string.state_pulau_pinang
    "kedah" -> R.string.state_kedah
    "perak" -> R.string.state_perak
    "perlis" -> R.string.state_perlis
    "selangor" -> R.string.state_selangor
    "negeri sembilan" -> R.string.state_negeri_sembilan
    "johor" -> R.string.state_johor
    "melaka" -> R.string.state_melaka
    "pahang" -> R.string.state_pahang
    "terengganu" -> R.string.state_terengganu
    "sabah" -> R.string.state_sabah
    "sarawak" -> R.string.state_sarawak
    else -> null
}

val HealthPreferenceResList = listOf(
    R.string.health_pref_no_preference,
    R.string.health_pref_low_carb,
    R.string.health_pref_vegetarian,
    R.string.health_pref_vegan,
    R.string.health_pref_halal,
    R.string.health_pref_keto,
    R.string.health_pref_gluten_free
)

fun getHealthPrefDbName(resId: Int): String = when (resId) {
    R.string.health_pref_no_preference -> "No Preference"
    R.string.health_pref_low_carb -> "Low Carb"
    R.string.health_pref_vegetarian -> "Vegetarian"
    R.string.health_pref_vegan -> "Vegan"
    R.string.health_pref_halal -> "Halal"
    R.string.health_pref_keto -> "Keto"
    R.string.health_pref_gluten_free -> "Gluten-Free"
    else -> ""
}

fun getHealthPrefResId(name: String): Int? = when (name.trim().lowercase()) {
    "no preference" -> R.string.health_pref_no_preference
    "low carb" -> R.string.health_pref_low_carb
    "vegetarian" -> R.string.health_pref_vegetarian
    "vegan" -> R.string.health_pref_vegan
    "halal" -> R.string.health_pref_halal
    "keto" -> R.string.health_pref_keto
    "gluten-free", "gluten free" -> R.string.health_pref_gluten_free
    else -> null
}

val GenderResList = listOf(
    R.string.gender_male,
    R.string.gender_female
)

fun getGenderDbName(resId: Int): String = when (resId) {
    R.string.gender_male -> "Male"
    R.string.gender_female -> "Female"
    else -> ""
}

fun getGenderResId(gender: String): Int? = when (gender.trim().lowercase()) {
    "male" -> R.string.gender_male
    "female" -> R.string.gender_female
    else -> null
}

fun getRecipeCourseResId(course: String): Int? = when (course.trim().lowercase()) {
    "all" -> R.string.recipe_course_all
    "breakfast" -> R.string.recipe_course_breakfast
    "lunch" -> R.string.recipe_course_lunch
    "dinner" -> R.string.recipe_course_dinner
    "snack", "snacks" -> R.string.recipe_course_snack
    "dessert", "desserts" -> R.string.recipe_course_dessert
    "beverage", "beverages", "drink", "drinks" -> R.string.recipe_course_beverage
    else -> null
}

fun getCookingSkillResId(skill: String): Int? = when (skill.trim().lowercase()) {
    "standard" -> R.string.recipe_skill_standard
    "beginner" -> R.string.recipe_skill_beginner
    "intermediate" -> R.string.recipe_skill_intermediate
    "advanced" -> R.string.recipe_skill_advanced
    "master/expert", "master", "expert" -> R.string.recipe_skill_master_expert
    else -> null
}


