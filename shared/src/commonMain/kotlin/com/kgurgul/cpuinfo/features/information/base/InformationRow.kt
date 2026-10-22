/*
 * Copyright KG Soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kgurgul.cpuinfo.features.information.base

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kgurgul.cpuinfo.ui.components.CpuDivider
import com.kgurgul.cpuinfo.ui.components.ItemValueRow
import com.kgurgul.cpuinfo.ui.theme.spacingSmall

@Composable
fun InformationRow(
    title: String,
    value: String,
    isLastItem: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        if (value.isEmpty()) {
            MaterialTheme.colorScheme.tertiaryFixed
        } else {
            MaterialTheme.colorScheme.onBackground
        }
    // Keep Latin/number values (e.g. "420 dpi", "50.1 MP") in left-to-right order inside RTL layouts.
    val nullableValue = value.ifEmpty { null }
        ?.let { if (it.none { c -> c in '\u0600'..'\u06FF' }) "\u200E$it\u200E" else it }
    ItemValueRow(
        title = title,
        value = nullableValue,
        contentColor = contentColor,
        modifier = modifier,
    )
    if (!isLastItem) {
        CpuDivider(modifier = Modifier.padding(top = spacingSmall))
    }
}
