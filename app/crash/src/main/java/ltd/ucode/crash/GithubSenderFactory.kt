package ltd.ucode.crash

import android.content.Context
import com.google.auto.service.AutoService
import org.acra.config.CoreConfiguration
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory

@AutoService(ReportSenderFactory::class)
class GithubSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender {
        return GithubSender(context)
    }

    override fun enabled(config: CoreConfiguration): Boolean {
        return config.enabled()
    }
}
