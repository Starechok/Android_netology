package ru.netology.nmedia.util

import android.os.Bundle
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object LongArg: ReadWriteProperty<Bundle, Long?> {
    override fun setValue(thisRef: Bundle, property: KProperty<*>, value: Long?) {
        thisRef.putSerializable(property.name, value)
    }

    override fun getValue(thisRef: Bundle, property: KProperty<*>): Long? =
        thisRef.getSerializable(property.name) as? Long
    }
