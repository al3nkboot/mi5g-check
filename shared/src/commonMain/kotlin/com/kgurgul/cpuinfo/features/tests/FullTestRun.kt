package com.kgurgul.cpuinfo.features.tests

/** Walks through [tests] in order for «فحص شامل»; each step is recorded as pass/fail or skipped. */
class FullTestRun(private val tests: List<HwTest> = HwTest.entries) {
    var index: Int = 0
        private set

    val current: HwTest? get() = tests.getOrNull(index)
    val isFinished: Boolean get() = index >= tests.size

    /** Records [result] (null = skipped) for the current test and moves on. Returns the result to store. */
    fun advance(result: Boolean?): Pair<HwTest, Boolean?>? {
        val test = current ?: return null
        index++
        return test to result
    }
}

/** Device report built from the session results; values are resolved to text by the UI. */
data class TestReport(
    val model: String,
    val androidVersion: String,
    val batteryHealthPercent: Int?,
    val date: String,
    val results: List<Pair<HwTest, Boolean?>>,
) {
    val passed: Int get() = results.count { it.second == true }
    val failed: Int get() = results.count { it.second == false }
    val notRun: Int get() = results.count { it.second == null }

    companion object {
        fun from(
            results: Map<HwTest, Boolean>,
            model: String,
            androidVersion: String,
            batteryHealthPercent: Int?,
            date: String,
        ) = TestReport(model, androidVersion, batteryHealthPercent, date, HwTest.entries.map { it to results[it] })
    }
}
