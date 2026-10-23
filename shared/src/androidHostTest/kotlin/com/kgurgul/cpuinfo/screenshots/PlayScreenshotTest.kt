package com.kgurgul.cpuinfo.screenshots

import org.robolectric.annotation.Config

/** Same screens at 1080x1920 (360x640dp @ xxhdpi) for Google Play (aspect ratio <= 2:1). */
@Config(sdk = [35], qualifiers = "ar-rEG-ldrtl-w360dp-h640dp-night-xxhdpi")
class PlayScreenshotTest : ArabicScreenshotTest()
