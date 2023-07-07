package me.ccrama.redditslide.Activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import ltd.ucode.slide.R
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.ui.BaseActivity
import me.ccrama.redditslide.Visuals.Palette
import me.ccrama.redditslide.util.BlendModeUtil
import me.ccrama.redditslide.util.FileUtil
import me.ccrama.redditslide.views.CanvasView
import me.ccrama.redditslide.views.DoEditorActions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Draw : BaseActivity() {
    var drawView: CanvasView? = null
    var color: View? = null
    var bitmap: Bitmap? = null
    var enabled = false
    private val cropImageLauncher: ActivityResultLauncher<CropImageContractOptions> =
        registerForActivityResult(
            CropImageContract()
        ) { result: CropImageView.CropResult -> cropImageResult(result) }

    override fun onCreate(savedInstance: Bundle?) {
        overrideSwipeFromAnywhere()
        disableSwipeBackLayout()
        super.onCreate(savedInstance)
        applyColorTheme("")
        setContentView(R.layout.activity_draw)
        drawView = findViewById<View>(R.id.paintView) as CanvasView?
        drawView!!.setBaseColor(Color.parseColor("#303030"))
        color = findViewById<View>(R.id.color)
        val options: CropImageContractOptions = CropImageContractOptions(uri, CropImageOptions())
            .setGuidelines(CropImageView.Guidelines.ON)
        cropImageLauncher.launch(options)
        setSupportActionBar(findViewById<View>(R.id.toolbar) as Toolbar?)
        setupAppBar(R.id.toolbar, "", true, Color.parseColor("#212121"), R.id.toolbar)
    }

    val lastColor: Int
        get() = SettingValues.colours.getInt("drawColor", Palette.getDefaultAccent())

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
        }
        if (id == R.id.done && enabled) {
            val image: File //image to share
            //check to see if the cache/shared_images directory is present
            val imagesDir: File = File(
                this@Draw.getCacheDir().toString() + File.separator + "shared_image"
            )
            if (!imagesDir.exists()) {
                imagesDir.mkdir() //create the folder if it doesn't exist
            } else {
                FileUtil.deleteFilesInDir(imagesDir)
            }
            try {
                //creates a file in the cache; filename will be prefixed with "img" and end with ".png"
                image = File.createTempFile("img", ".png", imagesDir)
                var out: FileOutputStream? = null
                try {
                    //convert image to png
                    out = FileOutputStream(image)
                    Bitmap.createBitmap(
                        drawView!!.bitmap,
                        0,
                        drawView!!.height.toInt(),
                        drawView!!.right.toInt(),
                        (drawView!!.bottom - drawView!!.height).toInt()
                    )
                        .compress(Bitmap.CompressFormat.JPEG, 100, out)
                } finally {
                    if (out != null) {
                        out.close()
                        val contentUri = FileUtil.getFileUri(image, this)
                        if (contentUri != null) {
                            val intent = FileUtil.getFileIntent(image, Intent(), this)
                            setResult(Activity.RESULT_OK, intent)
                        } else {
                            //todo error Toast.makeText(this, getString(R.string.err_share_image), Toast.LENGTH_LONG).show();
                        }
                        finish()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                //todo error Toast.makeText(this, getString(R.string.err_share_image), Toast.LENGTH_LONG).show();
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
        if (id == R.id.undo) {
            drawView!!.undo()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.draw_menu, menu)
        return true
    }

    private fun cropImageResult(result: CropImageView.CropResult) {
        if (result.isSuccessful) {
            bitmap = result.getBitmap(this)!!.copy(Bitmap.Config.RGB_565, true)
            BlendModeUtil.tintDrawableAsModulate(color!!.background, lastColor)
            color!!.setOnClickListener { v: View? ->
                MaterialDialog(this@Draw)
                    .title(res = R.string.choose_color_title)
                    .colorChooser(IntArray(0), allowCustomArgb = true) { _, selectedColor ->
                        drawView!!.paintStrokeColor = selectedColor
                        BlendModeUtil.tintDrawableAsModulate(color!!.background, selectedColor)
                        SettingValues.colours.edit().putInt("drawColor", selectedColor).commit()
                    }
                    .show()
            }
            drawView!!.drawBitmap(bitmap)
            drawView!!.paintStrokeColor = lastColor
            drawView!!.paintStrokeWidth = 20f
            enabled = true
        } else {
            finish()
        }
    }

    companion object {
        @JvmField
        var uri: Uri? = null
        var editor: DoEditorActions? = null
    }
}
