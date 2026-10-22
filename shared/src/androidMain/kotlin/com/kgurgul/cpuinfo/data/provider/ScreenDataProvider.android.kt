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
package com.kgurgul.cpuinfo.data.provider

import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.WindowManager
import com.kgurgul.cpuinfo.domain.model.ItemValue
import com.kgurgul.cpuinfo.shared.Res
import com.kgurgul.cpuinfo.shared.absolute_height
import com.kgurgul.cpuinfo.shared.absolute_width
import com.kgurgul.cpuinfo.shared.density
import com.kgurgul.cpuinfo.shared.density_class
import com.kgurgul.cpuinfo.shared.dp_height
import com.kgurgul.cpuinfo.shared.dp_width
import com.kgurgul.cpuinfo.shared.height
import com.kgurgul.cpuinfo.shared.large
import com.kgurgul.cpuinfo.shared.normal
import com.kgurgul.cpuinfo.shared.refresh_rate
import com.kgurgul.cpuinfo.shared.screen_class
import com.kgurgul.cpuinfo.shared.screen_density_dpi
import com.kgurgul.cpuinfo.shared.small
import com.kgurgul.cpuinfo.shared.unknown
import com.kgurgul.cpuinfo.shared.width
import com.kgurgul.cpuinfo.utils.round2
import com.kgurgul.cpuinfo.features.perf.PerfMath
import android.os.Build
import com.kgurgul.cpuinfo.shared.battery_not_available
import com.kgurgul.cpuinfo.shared.display_hdr
import com.kgurgul.cpuinfo.shared.display_hdr_now
import com.kgurgul.cpuinfo.shared.display_max_luminance
import com.kgurgul.cpuinfo.shared.display_refresh_rates
import com.kgurgul.cpuinfo.shared.display_resolution_native
import com.kgurgul.cpuinfo.shared.display_size_estimated
import com.kgurgul.cpuinfo.shared.display_wide_gamut
import com.kgurgul.cpuinfo.shared.no
import com.kgurgul.cpuinfo.shared.yes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class ScreenDataProvider actual constructor() : IScreenDataProvider, KoinComponent {

    private val resources: Resources by inject()
    private val windowManager: WindowManager by inject()

    actual override suspend fun getData(): List<ItemValue> {
        return buildList {
            add(getScreenClass())
            addAll(getDensityItems())
            addAll(getInfoFromDisplayMetrics())
        }
    }

    @Suppress("DEPRECATION")
    actual override fun getOrientationFlow(): Flow<String> = flow {
        val display = windowManager.defaultDisplay
        emit(display.rotation.toString())
    }

    private fun getScreenClass(): ItemValue {
        val screenClass =
            when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                Configuration.SCREENLAYOUT_SIZE_LARGE -> Res.string.large
                Configuration.SCREENLAYOUT_SIZE_NORMAL -> Res.string.normal
                Configuration.SCREENLAYOUT_SIZE_SMALL -> Res.string.small
                else -> Res.string.unknown
            }
        return ItemValue.NameValueResource(Res.string.screen_class, screenClass)
    }

    private fun getDensityItems(): List<ItemValue> {
        val densityDpi = resources.displayMetrics.densityDpi
        val densityClass: String
        when (densityDpi) {
            DisplayMetrics.DENSITY_LOW -> {
                densityClass = "ldpi"
            }

            DisplayMetrics.DENSITY_MEDIUM -> {
                densityClass = "mdpi"
            }

            DisplayMetrics.DENSITY_TV,
            DisplayMetrics.DENSITY_HIGH -> {
                densityClass = "hdpi"
            }

            DisplayMetrics.DENSITY_XHIGH,
            DisplayMetrics.DENSITY_280 -> {
                densityClass = "xhdpi"
            }

            DisplayMetrics.DENSITY_XXHIGH,
            DisplayMetrics.DENSITY_360,
            DisplayMetrics.DENSITY_400,
            DisplayMetrics.DENSITY_420 -> {
                densityClass = "xxhdpi"
            }

            DisplayMetrics.DENSITY_XXXHIGH,
            DisplayMetrics.DENSITY_560 -> {
                densityClass = "xxxhdpi"
            }

            else -> {
                densityClass = ""
            }
        }
        return listOf(
            ItemValue.NameResource(Res.string.density_class, densityClass),
            ItemValue.NameResource(Res.string.screen_density_dpi, "$densityDpi dpi"),
        )
    }

    @Suppress("DEPRECATION")
    private fun getInfoFromDisplayMetrics(): List<ItemValue> {
        val functionsList = mutableListOf<ItemValue>()
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        try {
            display.getRealMetrics(metrics)
            functionsList.add(ItemValue.NameResource(Res.string.width, "${metrics.widthPixels} px"))
            functionsList.add(
                ItemValue.NameResource(Res.string.height, "${metrics.heightPixels} px")
            )

            val density = metrics.density
            val dpHeight = metrics.heightPixels / density
            val dpWidth = metrics.widthPixels / density
            functionsList.add(ItemValue.NameResource(Res.string.dp_width, "${dpWidth.toInt()} dp"))
            functionsList.add(
                ItemValue.NameResource(Res.string.dp_height, "${dpHeight.toInt()} dp")
            )
            functionsList.add(ItemValue.NameResource(Res.string.density, "$density"))
        } catch (_: Exception) {
            // Do nothing
        }

        display.getMetrics(metrics)
        functionsList.add(
            ItemValue.NameResource(Res.string.absolute_width, "${metrics.widthPixels} px")
        )
        functionsList.add(
            ItemValue.NameResource(Res.string.absolute_height, "${metrics.heightPixels} px")
        )

        val refreshRate = display.refreshRate
        functionsList.add(
            ItemValue.NameResource(Res.string.refresh_rate, "${refreshRate.round2()} Hz")
        )

        functionsList.addAll(getExtendedDisplayInfo(display))
        return functionsList
    }

    /** Supported refresh rates, native resolution, estimated size, HDR and wide color gamut. */
    @Suppress("DEPRECATION")
    private fun getExtendedDisplayInfo(display: android.view.Display): List<ItemValue> = buildList {
        val na = Res.string.battery_not_available
        val modes = runCatching { display.supportedModes.toList() }.getOrDefault(emptyList())
        val rates = PerfMath.formatRefreshRates(modes.map { it.refreshRate })
        add(if (rates != null) ItemValue.NameResource(Res.string.display_refresh_rates, rates) else ItemValue.NameValueResource(Res.string.display_refresh_rates, na))
        val native = modes.maxByOrNull { it.physicalWidth.toLong() * it.physicalHeight }
        if (native != null) {
            add(ItemValue.NameResource(Res.string.display_resolution_native, "${native.physicalWidth} × ${native.physicalHeight}"))
        }
        val real = DisplayMetrics().also { runCatching { display.getRealMetrics(it) } }
        val inches = PerfMath.diagonalInches(real.widthPixels, real.heightPixels, real.xdpi, real.ydpi)
        add(if (inches != null) ItemValue.NameResource(Res.string.display_size_estimated, "$inches″") else ItemValue.NameValueResource(Res.string.display_size_estimated, na))
        val hdr = runCatching { display.hdrCapabilities?.supportedHdrTypes }.getOrNull()
        if (hdr == null) {
            add(ItemValue.NameValueResource(Res.string.display_hdr, na))
        } else if (hdr.isEmpty()) {
            add(ItemValue.NameValueResource(Res.string.display_hdr, Res.string.no))
        } else {
            add(ItemValue.NameResource(Res.string.display_hdr, hdr.distinct().joinToString(", ") { hdrName(it) }))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(ItemValue.NameValueResource(Res.string.display_hdr_now, if (display.isHdr) Res.string.yes else Res.string.no))
            add(ItemValue.NameValueResource(Res.string.display_wide_gamut, if (display.isWideColorGamut) Res.string.yes else Res.string.no))
        } else {
            add(ItemValue.NameValueResource(Res.string.display_wide_gamut, na))
        }
        val lum = runCatching { display.hdrCapabilities?.desiredMaxLuminance }.getOrNull()
        add(
            if (lum != null && lum > 1f && lum < 20000f) ItemValue.NameResource(Res.string.display_max_luminance, "${lum.toInt()} nits")
            else ItemValue.NameValueResource(Res.string.display_max_luminance, na)
        )
    }

    private fun hdrName(type: Int) = when (type) {
        1 -> "Dolby Vision"
        2 -> "HDR10"
        3 -> "HLG"
        4 -> "HDR10+"
        else -> "HDR($type)"
    }
}
