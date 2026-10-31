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
package com.kgurgul.cpuinfo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CpuNavigationSuiteScaffold(
    navigationItems: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val navigationSuitColors =
        NavigationSuiteDefaults.colors(
            shortNavigationBarContainerColor = MaterialTheme.colorScheme.primary,
            shortNavigationBarContentColor = MaterialTheme.colorScheme.onPrimary,
            wideNavigationRailColors =
                WideNavigationRailDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            navigationBarContainerColor = MaterialTheme.colorScheme.primary,
            navigationBarContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationRailContainerColor = MaterialTheme.colorScheme.primary,
            navigationRailContentColor = MaterialTheme.colorScheme.onPrimary,
        )
    BoxWithConstraints(modifier = modifier) {
        val suiteType = when {
            maxWidth < 600.dp -> NavigationSuiteType.NavigationBar
            maxWidth < 840.dp -> NavigationSuiteType.ShortNavigationBarMedium
            else -> NavigationSuiteType.WideNavigationRailExpanded
        }
        NavigationSuiteScaffold(
            navigationItems = navigationItems,
            navigationSuiteType = suiteType,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            navigationSuiteColors = navigationSuitColors,
            navigationItemVerticalArrangement = Arrangement.Center,
            modifier = Modifier,
            content = content,
        )
    }
}

object CpuNavigationSuiteScaffoldDefault {

    private const val NOT_SELECTED_ALPHA = 0.7f

    @Composable
    fun itemColors() =
        NavigationItemColors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondary,
            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
            selectedIndicatorColor = MaterialTheme.colorScheme.secondary,
            unselectedIconColor =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = NOT_SELECTED_ALPHA),
            unselectedTextColor =
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = NOT_SELECTED_ALPHA),
            disabledIconColor =
                MaterialTheme.colorScheme.onPrimary.copy(alpha = NOT_SELECTED_ALPHA),
            disabledTextColor =
                MaterialTheme.colorScheme.onPrimary.copy(alpha = NOT_SELECTED_ALPHA),
        )
}
