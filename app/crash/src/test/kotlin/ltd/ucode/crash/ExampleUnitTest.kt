package ltd.ucode.crash

import org.acra.ReportField
import org.acra.data.CrashReportData
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    @Test
    fun makeIssue() {
        val manager = GithubIssueTracker()
        val issue = manager[""]

        val reportData: CrashReportData = CrashReportData()
        reportData.put(ReportField.DEVICE_ID, "FAKE_ID")

        val jsonObject = JSONObject()
        jsonObject.put("VERSION_CODE", -1)
        jsonObject.put("VERSION_NAME", "Test")
        reportData.put(ReportField.BUILD_CONFIG, jsonObject)

        val throwable = Throwable()
        reportData.put(ReportField.STACK_TRACE, throwable.stackTrace.toString())

        issue.post(reportData)
    }
}
