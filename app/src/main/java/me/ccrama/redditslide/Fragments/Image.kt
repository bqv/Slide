package me.ccrama.redditslide.Fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener
import ltd.ucode.slide.App
import ltd.ucode.slide.R

class Image : Fragment() {
    var url: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            R.layout.submission_imagecard, container, false
        ) as ViewGroup
        val image = rootView.findViewById<SubsamplingScaleImageView>(R.id.image)
        val title = rootView.findViewById<TextView>(R.id.title)
        val desc = rootView.findViewById<TextView>(R.id.desc)
        title.visibility = View.GONE
        desc.visibility = View.GONE
        (requireContext().applicationContext as App).imageLoader!!
            .loadImage(url,
                object : SimpleImageLoadingListener() {
                    override fun onLoadingComplete(
                        imageUri: String,
                        view: View,
                        loadedImage: Bitmap
                    ) {
                        image.setImage(ImageSource.Bitmap(loadedImage))
                    }
                })
        return rootView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        url = bundle!!.getString("url")
    }
}
