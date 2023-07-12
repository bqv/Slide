package ltd.ucode.slide.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import ltd.ucode.network.lemmy.api.request.GetSiteRequest
import ltd.ucode.network.lemmy.data.id.LanguageId
import ltd.ucode.slide.data.ContentDatabase
import ltd.ucode.slide.data.entity.Language
import ltd.ucode.slide.data.entity.Site
import ltd.ucode.slide.data.entity.Tagline
import javax.inject.Inject
import kotlin.concurrent.thread

class SiteRepository @Inject constructor(
        @ApplicationContext private val context: Context,
        private val contentDatabase: ContentDatabase,
        private val networkRepository: NetworkRepository,
) {
    fun getSiteByName(name: String): Flow<Site> {
        thread {
            runBlocking(Dispatchers.IO) {
                val response = networkRepository[name]
                    .getSite(GetSiteRequest())
                response.onSuccess {
                    val site = when (val site = contentDatabase.sites.get(it.domain).singleOrNull()) {
                        null -> {
                            Site.from(it).also { contentDatabase.sites.add(it) }
                        }
                        else -> {
                            site.copy(it).also { contentDatabase.sites.update(it) }
                        }
                    }

                    val taglines = Tagline.from(it, site).also {
                        contentDatabase.sites.upsert(it)
                    }

                    val languages = Language.from(it, site).map { (language, image) ->
                        contentDatabase.languages.upsert(language, image)
                    }

                    site
                }.success
            }
        }

        return contentDatabase.sites.flow(name)
    }

    fun getLanguageBySiteId(languageId: LanguageId, site: Site): Flow<Language> {
        return contentDatabase.languages.flow(languageId.id, site.rowId)
    }
}
