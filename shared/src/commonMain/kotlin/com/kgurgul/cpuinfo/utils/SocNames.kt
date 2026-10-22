package com.kgurgul.cpuinfo.utils

/** Small table mapping common SoC model codes to marketing names (Mi5G Check addition). */
object SocNames {

    private val names =
        mapOf(
            // Qualcomm
            "SM8750" to "Snapdragon 8 Elite",
            "SM8650" to "Snapdragon 8 Gen 3",
            "SM8635" to "Snapdragon 8s Gen 3",
            "SM8550" to "Snapdragon 8 Gen 2",
            "SM8475" to "Snapdragon 8+ Gen 1",
            "SM8450" to "Snapdragon 8 Gen 1",
            "SM8350" to "Snapdragon 888",
            "SM8250" to "Snapdragon 865",
            "SM7675" to "Snapdragon 7+ Gen 3",
            "SM7550" to "Snapdragon 7 Gen 3",
            "SM7475" to "Snapdragon 7+ Gen 2",
            "SM7450" to "Snapdragon 7 Gen 1",
            "SM7325" to "Snapdragon 778G",
            "SM6450" to "Snapdragon 6 Gen 1",
            "SM6375" to "Snapdragon 695",
            "SM4450" to "Snapdragon 4 Gen 2",
            // MediaTek
            "MT6991" to "Dimensity 9400",
            "MT6989" to "Dimensity 9300",
            "MT6985" to "Dimensity 9200",
            "MT6983" to "Dimensity 9000",
            "MT6897" to "Dimensity 8300",
            "MT6896" to "Dimensity 8200",
            "MT6895" to "Dimensity 8100",
            "MT6789" to "Helio G99",
            // Samsung Exynos
            "S5E9945" to "Exynos 2400",
            "S5E9925" to "Exynos 2200",
            "S5E8845" to "Exynos 1480",
            "S5E8835" to "Exynos 1380",
            // Google Tensor
            "GS101" to "Tensor",
            "GS201" to "Tensor G2",
            "ZUMA" to "Tensor G3",
            "ZUMAPRO" to "Tensor G4",
        )

    private val tokenRegex = Regex("[A-Za-z0-9]+")

    /** Returns e.g. "Snapdragon 8 Elite (SM8750)", or [name] unchanged if no known code. */
    fun withMarketingName(name: String): String {
        val code = tokenRegex.findAll(name).map { it.value.uppercase() }.firstOrNull { it in names }
            ?: return name
        val marketing = names.getValue(code)
        return if (name.contains(marketing, ignoreCase = true)) name else "$marketing ($code)"
    }
}
