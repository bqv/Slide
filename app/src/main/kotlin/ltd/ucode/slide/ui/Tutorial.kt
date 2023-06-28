package ltd.ucode.slide.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import ltd.ucode.slide.App
import ltd.ucode.slide.SettingValues
import ltd.ucode.slide.databinding.ActivityTutorialBinding
import ltd.ucode.slide.databinding.ChooseaccentBinding
import ltd.ucode.slide.databinding.ChoosemainBinding
import ltd.ucode.slide.databinding.ChoosethemesmallBinding
import ltd.ucode.slide.databinding.FragmentPersonalizeBinding
import ltd.ucode.slide.databinding.FragmentWelcomeBinding
import me.ccrama.redditslide.Visuals.ColorPreferences
import me.ccrama.redditslide.Visuals.FontPreferences
import me.ccrama.redditslide.Visuals.Palette

class Tutorial : AppCompatActivity() {
    private var back = 0
    private var binding: ActivityTutorialBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = theme
        theme.applyStyle(FontPreferences(this).commentFontStyle.resId, true)
        theme.applyStyle(FontPreferences(this).postFontStyle.resId, true)
        theme.applyStyle(ColorPreferences(this).fontStyle.baseId, true)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // The pager adapter, which provides the pages to the view pager widget.
        binding!!.tutorialViewPager.adapter = TutorialPagerAdapter(supportFragmentManager)
        if (intent.hasExtra("page")) {
            binding!!.tutorialViewPager.currentItem = 1
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = this.window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor =
                Palette.getDarkerColor(Color.parseColor("#FF5252"))
        }
    }

    override fun onBackPressed() {
        val currentItem = binding!!.tutorialViewPager.currentItem
        if (currentItem == POS_WELCOME) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
        } else {
            // Otherwise, select the previous step.
            binding!!.tutorialViewPager.currentItem = currentItem - 1
        }
    }

    class Welcome : Fragment() {
        private var welcomeBinding: FragmentWelcomeBinding? = null
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            welcomeBinding = FragmentWelcomeBinding.inflate(inflater, container, false)
            welcomeBinding!!.welcomeGetStarted.setOnClickListener { v1: View? ->
                (activity as Tutorial?)!!.binding!!.tutorialViewPager.currentItem = 1
            }
            return welcomeBinding!!.root
        }

        override fun onDestroyView() {
            super.onDestroyView()
            welcomeBinding = null
        }
    }

    class Personalize : Fragment() {
        private var personalizeBinding: FragmentPersonalizeBinding? = null
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            (activity as Tutorial?)!!.back = ColorPreferences(context).fontStyle.themeType
            personalizeBinding = FragmentPersonalizeBinding.inflate(inflater, container, false)
            val getFontColor = requireActivity().resources.getColor(
                ColorPreferences(context).fontStyle.color
            )
            me.ccrama.redditslide.util.BlendModeUtil.tintImageViewAsSrcAtop(
                personalizeBinding!!.secondaryColorPreview,
                getFontColor
            )
            me.ccrama.redditslide.util.BlendModeUtil.tintImageViewAsSrcAtop(
                personalizeBinding!!.primaryColorPreview,
                Palette.getDefaultColor()
            )
            personalizeBinding!!.header.setBackgroundColor(Palette.getDefaultColor())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window = requireActivity().window
                window.statusBarColor = Palette.getDarkerColor(Palette.getDefaultColor())
            }
            personalizeBinding!!.primaryColor.setOnClickListener { v: View? ->
                val choosemainBinding = ChoosemainBinding.inflate(
                    requireActivity().layoutInflater
                )
                choosemainBinding.title.setBackgroundColor(Palette.getDefaultColor())
                choosemainBinding.picker.colors = ColorPreferences.getBaseColors(context)
                for (i in choosemainBinding.picker.colors!!) {
                    for (i2 in ColorPreferences.getColors(context, i)) {
                        if (i2 == Palette.getDefaultColor()) {
                            choosemainBinding.picker.setSelectedColor(i)
                            choosemainBinding.picker2.colors =
                                ColorPreferences.getColors(context, i)
                            choosemainBinding.picker2.setSelectedColor(i2)
                            break
                        }
                    }
                }
                choosemainBinding.picker.setOnColorChangedListener(object :
                    uz.shift.colorpicker.OnColorChangedListener {
                    override fun onColorChanged(c: Int) {
                        choosemainBinding.picker2.colors = ColorPreferences.getColors(context, c)
                        choosemainBinding.picker2.setSelectedColor(c)
                    }
                })
                choosemainBinding.picker2.setOnColorChangedListener(object :
                    uz.shift.colorpicker.OnColorChangedListener {
                    override fun onColorChanged(c: Int) {
                        choosemainBinding.title.setBackgroundColor(choosemainBinding.picker2.color)
                        personalizeBinding!!.header.setBackgroundColor(choosemainBinding.picker2.color)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val window = requireActivity().window
                            window.statusBarColor =
                                Palette.getDarkerColor(choosemainBinding.picker2.color)
                        }
                    }
                })
                choosemainBinding.ok.setOnClickListener { v13: View? ->
                    SettingValues.colours.edit().putInt("DEFAULTCOLOR", choosemainBinding.picker2.color)
                        .apply()
                    finishDialogLayout()
                }
                AlertDialog.Builder(requireContext())
                    .setView(choosemainBinding.root)
                    .show()
            }
            personalizeBinding!!.secondaryColor.setOnClickListener { v: View? ->
                val accentBinding = ChooseaccentBinding.inflate(
                    requireActivity().layoutInflater
                )
                accentBinding.title.setBackgroundColor(Palette.getDefaultColor())
                val arrs = IntArray(ColorPreferences.getNumColorsFromThemeType(0))
                var i = 0
                for (type in ColorPreferences.Theme.values()) {
                    if (type.themeType
                        == ColorPreferences.ColorThemeOptions.Dark.value
                    ) {
                        arrs[i] = ContextCompat.getColor(requireActivity(), type.color)
                        i++
                    }
                }
                accentBinding.picker3.colors = arrs
                accentBinding.picker3.setSelectedColor(ColorPreferences(activity).getColor(""))
                accentBinding.ok.setOnClickListener { v12: View? ->
                    val color = accentBinding.picker3.color
                    var theme: ColorPreferences.Theme? = null
                    for (type in ColorPreferences.Theme.values()) {
                        if (ContextCompat.getColor(requireActivity(), type.color) == color
                            && (activity as Tutorial?)!!.back == type.themeType
                        ) {
                            theme = type
                            break
                        }
                    }
                    ColorPreferences(activity).fontStyle = theme
                    finishDialogLayout()
                }
                AlertDialog.Builder(requireActivity())
                    .setView(accentBinding.root)
                    .show()
            }
            personalizeBinding!!.baseColor.setOnClickListener { v: View? ->
                val themesmallBinding = ChoosethemesmallBinding.inflate(
                    requireActivity().layoutInflater
                )
                val themesmallBindingRoot: View = themesmallBinding.root
                themesmallBinding.title.setBackgroundColor(Palette.getDefaultColor())
                for (pair in ColorPreferences.themePairList) {
                    themesmallBindingRoot.findViewById<View>(pair.first!!)
                        .setOnClickListener { v14: View? ->
                            val names = ColorPreferences(activity).fontStyle
                                .title
                                .split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            val name = names[names.size - 1]
                            val newName = name.replace("(", "")
                            for (theme in ColorPreferences.Theme.values()) {
                                if (theme.toString().contains(newName)
                                    && theme.themeType == pair.second
                                ) {
                                    (activity as Tutorial?)!!.back = theme.themeType
                                    ColorPreferences(activity).fontStyle = theme
                                    finishDialogLayout()
                                    break
                                }
                            }
                        }
                }
                AlertDialog.Builder(requireActivity())
                    .setView(themesmallBindingRoot)
                    .show()
            }
            personalizeBinding!!.done.setOnClickListener { v1: View? ->
                SettingValues.colours.edit().putString("Tutorial", "S").commit()
                SettingValues.appRestart.edit().putString("startScreen", "a").apply()
                App.forceRestart(activity, false)
            }
            return personalizeBinding!!.root
        }

        override fun onDestroyView() {
            super.onDestroyView()
            personalizeBinding = null
        }

        private fun finishDialogLayout() {
            val intent = Intent(activity, Tutorial::class.java)
            intent.putExtra("page", 1)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            requireActivity().overridePendingTransition(0, 0)
            requireActivity().finish()
            requireActivity().overridePendingTransition(0, 0)
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class TutorialPagerAdapter internal constructor(fm: FragmentManager?) :
        FragmentStatePagerAdapter(
            fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                POS_WELCOME -> Welcome()
                POS_PERSONALIZE -> Personalize()
                else -> Welcome()
            }
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }
    }

    companion object {
        /**
         * The pages (wizard steps) to show in this demo.
         */
        private const val POS_WELCOME = 0
        private const val POS_PERSONALIZE = 1
        private const val NUM_PAGES = 2
    }
}
