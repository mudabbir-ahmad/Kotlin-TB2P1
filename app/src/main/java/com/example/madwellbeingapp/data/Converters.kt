package com.example.madwellbeingapp.data

import androidx.room.TypeConverter
import com.example.madwellbeingapp.data.model.HabitCategory

class Converters {

    @TypeConverter
    fun fromCategory(category: HabitCategory): String {
        return category.name
    }

    @TypeConverter
    fun toCategory(value: String): HabitCategory {
        return HabitCategory.valueOf(value)
    }
}

