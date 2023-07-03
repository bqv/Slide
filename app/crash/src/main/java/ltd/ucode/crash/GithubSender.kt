package ltd.ucode.crash

import android.content.Context
import android.util.Log
import org.acra.ReportField
import org.acra.data.CrashReportData
import org.acra.ktx.sendSilentlyWithAcra
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import kotlin.concurrent.thread

class GithubSender(context: Context) : ReportSender {
    private val issues: GithubIssueTracker by lazy {
        GithubIssueTracker(context)
    }

    @Throws(ReportSenderException::class)
    override fun send(context: Context, errorContent: CrashReportData) {
        var metaException: Exception? = null

        try {
            thread(name = javaClass.simpleName) {
                val title: String = errorContent.getTitle()

                val issue =
                    issues[errorContent.getString(ReportField.STACK_TRACE_HASH)!!]

                val messages = if (!issue.isNew) {
                    Log.d(BuildConfig.LIBRARY_PACKAGE_NAME, "Corroborating")

                    errorContent.toUpvote()
                } else {
                    errorContent.toMessages()
                }

                messages.forEach {
                    issue.post(title, it)
                }
            }.join()
        } catch (e: Exception) {
            metaException = e
            throw ReportSenderException("Failed to send report", e)
        } finally {
            metaException?.sendSilentlyWithAcra()
        }
    }

    companion object {
        const val SIZE_LIMIT: Int = 65536

        private val shortReportFields = listOf(
            ReportField.REPORT_ID, ReportField.STACK_TRACE_HASH,

            ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.PACKAGE_NAME,
            ReportField.PHONE_MODEL, ReportField.ANDROID_VERSION,
            ReportField.BRAND, ReportField.PRODUCT,
            ReportField.FILE_PATH,

            ReportField.TOTAL_MEM_SIZE, ReportField.AVAILABLE_MEM_SIZE,
            ReportField.IS_SILENT,
            ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE,
            ReportField.INSTALLATION_ID,
        )

        private val midReportFields = listOf(
            ReportField.BUILD_CONFIG,
            ReportField.CUSTOM_DATA,
            ReportField.INITIAL_CONFIGURATION,
            ReportField.CRASH_CONFIGURATION,
            ReportField.DISPLAY,
            ReportField.USER_COMMENT,
            ReportField.THREAD_DETAILS,
        )

        private val longReportFields = listOf(
            ReportField.BUILD,
            ReportField.STACK_TRACE,

            ReportField.DEVICE_FEATURES,
            ReportField.ENVIRONMENT,
            ReportField.SHARED_PREFERENCES,

            ReportField.LOGCAT,
            ReportField.EVENTSLOG,
        )
        private val excludeReportFields = listOf(
            ReportField.USER_EMAIL,
            ReportField.DUMPSYS_MEMINFO,
        )
    }

    private val ReportField.text: String
        get() {
            return name
        }

    fun CrashReportData.getTitle(): String {
        return this.getString(ReportField.STACK_TRACE)
            ?.split("\n")?.firstOrNull()
            ?: "No Stack Trace"
    }

    @Throws(UnsupportedEncodingException::class)
    fun CrashReportData.toUpvote(): List<String> {
        val fieldMap = this.toMap().byReportField()

        val message =
            fieldMap.filterKeys(shortReportFields::contains)
                .toSortedMap(compareBy(shortReportFields::indexOf))
                .map { "**${it.key.text}**: ${it.value}" }
                .let(listOf(
                    "# New Report\n",
                )::plus)
                .joinToString("\n") // really shouldn't be > SIZE_LIMIT

        return listOf(message)
    }

    @Throws(UnsupportedEncodingException::class)
    fun CrashReportData.toMessages(): List<String> {
        val fieldMap = this.toMap().byReportField()

        val messages = mutableListOf<String>()

        val shortData =
            fieldMap.filterKeys(shortReportFields::contains)
                .toSortedMap(compareBy(shortReportFields::indexOf))
                .map { "**${it.key.text}**: ${it.value}" }
                .let(listOf(
                    "<!--CRASH:${fieldMap[ReportField.STACK_TRACE_HASH]}-->\n",
                    "# Crash Report\n",
                )::plus)
                .joinToString("\n") // really shouldn't be > SIZE_LIMIT
        messages.add(shortData)

        val midData =
            fieldMap.filterKeys(midReportFields::contains)
                .map { "### ${it.key.text}:\n${it.value}" }
        StringBuilder().let { accumulator ->
            for (entry in midData) {
                if (accumulator.length + entry.length + 1 > SIZE_LIMIT) {
                    messages.add(accumulator.toString())
                    accumulator.clear()
                }
                if (accumulator.isNotEmpty()) {
                    accumulator.append("\n")
                }
                accumulator.append(entry)
            }
            messages.add(accumulator.toString())
        }

        val longData =
            fieldMap.filterKeys(longReportFields::contains)
                .toList().sortedBy { (_, value) -> value.length }.toMap()
                .map { "### ${it.key.text}:\n```\n  ${it.value}\n```" }
                .plus(
                    fieldMap.filterKeys { !shortReportFields.contains(it) }
                        .filterKeys { !midReportFields.contains(it) }
                        .filterKeys { !longReportFields.contains(it) }
                        .filterKeys { !excludeReportFields.contains(it) }
                        .map { "### ${it.key.text}:\n```${it.value}```\n" }
                        .joinToString("\n")
                        .ifBlank { null }
                ).filterNotNull()
                .map { if (it.length > SIZE_LIMIT) it.substring(0, SIZE_LIMIT-3) + "..." else it }
        messages.addAll(longData)

        return messages
    }

    private fun Map<String, Any?>.byReportField(joiner: String = "\n  "): Map<ReportField, String> {
        val keyMap: Map<String, ReportField> = ReportField.values().associateBy { it.toString() }
        return mapValues {
            if (it.value is JSONObject) {
                flatten(it.value as JSONObject).joinToString(joiner)
            } else {
                it.value.toString()
            }
        }.mapKeys { keyMap[it.key]!! }
    }

    private fun flatten(json: JSONObject): List<String> {
        return json.keys().asSequence().toList().flatMap { key ->
            val value: Any? = try {
                json[key]
            } catch (e: JSONException) {
                null
            }
            if (value is JSONObject) {
                flatten(value).map { "$key.$it" }
            } else {
                listOf("$key=$value")
            }
        }
    }
}
