/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.util

import android.net.Uri
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/** Provides [SettingsCache] sandboxed from system settings for testing. */
class SettingsCacheSandbox {
    private val values = mutableMapOf<Uri, Int>()
    private val intListenables = mutableMapOf<Uri, MutableListenableRef<Int>>()

    val listenable = MutableListenableRef(false)

    /**
     * Fake cache that delegates:
     * - [SettingsCache.getValue] and [SettingsCache.getIntValue] to [values]
     * - [SettingsCache.getListenableRef] to [MutableListenableRef]
     * - [SettingsCache.getIntListenableRef] to [intListenables]
     */
    val cache =
        mock<SettingsCache> {
            on { getValue(any<Uri>()) } doAnswer { values.getOrDefault(it.getArgument(0), 0) == 1 }
            on { getIntValue(any<Uri>()) } doAnswer { values.getOrDefault(it.getArgument(0), 0) }
            on { getIntValue(any<Uri>(), any<Int>()) } doAnswer {
                values[it.getArgument<Uri>(0)] ?: it.getArgument<Int>(1)
            }

            on { getListenableRef(any<Uri>()) } doReturn listenable
            on { getIntListenableRef(any<Uri>()) } doAnswer {
                intListenable(it.getArgument<Uri>(0), 0)
            }
            on { getIntListenableRef(any<Uri>(), any<Int>()) } doAnswer {
                intListenable(it.getArgument<Uri>(0), it.getArgument<Int>(1))
            }
        }

    operator fun get(key: Uri): Int? = values[key]

    operator fun set(key: Uri, value: Int) {
        if (value == values[key]) return
        values[key] = value
        listenable.dispatchValue(value == 1)
        intListenables[key]?.dispatchValue(value)
    }

    private fun intListenable(uri: Uri, defaultValue: Int): MutableListenableRef<Int> =
        intListenables.getOrPut(uri) {
            MutableListenableRef(values.getOrDefault(uri, defaultValue))
        }
}
