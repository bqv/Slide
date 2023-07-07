package me.ccrama.redditslide.ImgurAlbum

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import com.afollestad.materialdialogs.MaterialDialog
import ltd.ucode.slide.App
import me.ccrama.redditslide.util.ImgurUtils
import me.ccrama.redditslide.util.LogUtil
import me.ccrama.redditslide.util.ProgressRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException

open class UploadImgur : AsyncTask<Uri?, Int?, JSONObject?>() {
    @JvmField
    var c: Context? = null

    @JvmField
    var dialog: MaterialDialog? = null

    override fun doInBackground(vararg sub: Uri?): JSONObject? {
        val bitmap = ImgurUtils.createFile(sub[0], c!!)
        val client = App.client
        try {
            val formBody: RequestBody = MultipartBody.Builder()
                .addFormDataPart(
                    "image", bitmap.name,
                    RequestBody.create("image/*".toMediaType(), bitmap)
                )
                .build()
            val body = ProgressRequestBody(formBody) { values: Int -> publishProgress(values) }
            val request: Request = Request.Builder()
                .header("Authorization", "Client-ID bef87913eb202e9")
                .url("https://api.imgur.com/3/image")
                .post(body)
                .build()
            val response = client!!.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return JSONObject(response.body!!.string())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
    }

    override fun onProgressUpdate(vararg values: Int?) {
        //dialog!!.setProgress(values[0])
        LogUtil.v("Progress:" + values[0])
    }
}
