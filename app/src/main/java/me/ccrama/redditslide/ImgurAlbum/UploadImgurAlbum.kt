package me.ccrama.redditslide.ImgurAlbum

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import com.afollestad.materialdialogs.MaterialDialog
import ltd.ucode.slide.App
import me.ccrama.redditslide.util.ImgurUtils
import me.ccrama.redditslide.util.ProgressRequestBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import org.json.JSONObject
import java.io.IOException

open class UploadImgurAlbum : AsyncTask<Uri?, Int?, String?>() {
    @JvmField
    var finalUrl: String? = null
    @JvmField
    var c: Context? = null
    var totalCount = 0
    var uploadCount = 0
    @JvmField
    var dialog: MaterialDialog? = null

    override fun doInBackground(vararg sub: Uri?): String? {
        totalCount = sub.size
        val client = App.client
        var albumurl: String
        run {
            val request: Request = Request.Builder()
                .header("Authorization", "Client-ID bef87913eb202e9")
                .url("https://api.imgur.com/3/album")
                .post(object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return null
                    }

                    override fun writeTo(sink: BufferedSink) {}
                })
                .build()
            var response: Response? = null
            try {
                response = client!!.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                val album = JSONObject(response.body!!.string())
                albumurl = album.getJSONObject("data").getString("deletehash")
                finalUrl = "http://imgur.com/a/" + album.getJSONObject("data").getString("id")
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        try {
            val formBodyBuilder = MultipartBody.Builder()
            for (uri in sub) {
                val bitmap = ImgurUtils.createFile(uri, c!!)
                formBodyBuilder.addFormDataPart(
                    "image", bitmap.name,
                    RequestBody.create("image/*".toMediaType(), bitmap)
                )
                formBodyBuilder.addFormDataPart("album", albumurl)
                val formBody: MultipartBody = formBodyBuilder.build()
                val body = ProgressRequestBody(formBody) { values: Int -> publishProgress(values) }
                val request = Request.Builder()
                    .header("Authorization", "Client-ID bef87913eb202e9")
                    .url("https://api.imgur.com/3/image")
                    .post(body)
                    .build()
                val response = client!!.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        val progress = values[0]!!
        //if (progress < dialog!!.getCurrentProgress() || uploadCount == 0) {
        //    uploadCount += 1
        //}
        dialog!!.setTitle("Image $uploadCount/$totalCount")
        //dialog!!.setProgress(progress)
    }
}
