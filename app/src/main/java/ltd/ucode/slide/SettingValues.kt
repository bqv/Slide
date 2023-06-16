package ltd.ucode.slide

import android.content.SharedPreferences
import android.content.res.Configuration
import ltd.ucode.slide.App.Companion.appContext
import me.ccrama.redditslide.Constants
import me.ccrama.redditslide.Views.CreateCardView.CardEnum
import me.ccrama.redditslide.Visuals.Palette.ThemeEnum
import me.ccrama.redditslide.ui.settings.SettingsHandlingFragment
import me.ccrama.redditslide.util.SortingUtil
import net.dean.jraw.models.CommentSort
import net.dean.jraw.paginators.Sorting
import net.dean.jraw.paginators.TimePeriod
import java.util.Calendar
import java.util.Locale

object SettingValues {
    private var prefs: SharedPreferences? = null
    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    const val PREF_SINGLE = "Single"
    const val PREF_FAB = "Fab"
    const val PREF_UPVOTE_PERCENTAGE = "upvotePercentage"
    const val PREF_FAB_TYPE = "FabType"
    const val PREF_DAY_TIME = "day"
    const val PREF_VOTE_GESTURES = "voteGestures"
    const val PREF_NIGHT_MODE_STATE = "nightModeState"
    const val PREF_NIGHT_MODE = "nightMode"
    const val PREF_NIGHT_THEME = "nightTheme"
    const val PREF_TYPE_IN_TEXT = "typeInText"
    const val PREF_AUTOHIDE_COMMENTS = "autohideComments"
    const val PREF_SHOW_COLLAPSE_EXPAND = "showCollapseExpand"
    const val PREF_NO_IMAGES = "noImages"
    const val PREF_AUTOTHEME = "autotime"
    const val PREVIEWS_LEFT = "previewsLeft"
    const val PREF_ALPHABETIZE_SUBSCRIBE = "alphabetizeSubscribe"
    const val PREF_COLOR_BACK = "colorBack"
    const val PREF_IMAGE_SUBFOLDERS = "imageSubfolders"
    const val PREF_IMAGE_DOWNLOAD_BUTTON = "imageDownloadButton"
    const val PREF_COLOR_NAV_BAR = "colorNavBar"
    const val PREF_READER_MODE = "readerDefault"
    const val PREF_READER_NIGHT = "readernight"
    const val PREF_COLOR_EVERYWHERE = "colorEverywhere"
    const val PREF_EXPANDED_TOOLBAR = "expandedToolbar"
    const val PREF_SWAP = "Swap"
    const val PREF_ACTIONBAR_VISIBLE = "actionbarVisible"
    const val PREF_SMALL_TAG = "smallTag"
    const val PREF_ACTIONBAR_TAP = "actionbarTap"
    const val PREF_STORE_HISTORY = "storehistory"
    const val PREF_STORE_NSFW_HISTORY = "storensfw"
    const val PREF_SCROLL_SEEN = "scrollSeen"
    const val PREF_TITLE_FILTERS = "titleFilters"
    const val PREF_TEXT_FILTERS = "textFilters"
    const val PREF_DOMAIN_FILTERS = "domainFilters"
    const val PREF_ALWAYS_EXTERNAL = "alwaysExternal"
    const val PREF_DRAFTS = "drafts"
    const val PREF_SUBREDDIT_FILTERS = "subredditFilters"
    const val PREF_ABBREVIATE_SCORES = "abbreviateScores"
    const val PREF_HIDE_POST_AWARDS = "hidePostAwards"
    const val PREF_HIDE_COMMENT_AWARDS = "hideCommentAwards"
    const val PREF_FLAIR_FILTERS = "subFlairFilters"
    const val PREF_COMMENT_LAST_VISIT = "commentLastVisit"
    const val PREF_VOTES_INFO_LINE = "votesInfoLine"
    const val PREF_TYPE_INFO_LINE = "typeInfoLine"
    const val PREF_COMMENT_PAGER = "commentPager"
    const val PREF_COLLAPSE_COMMENTS = "collapseCOmments"
    const val PREF_COLLAPSE_COMMENTS_DEFAULT = "collapseCommentsDefault"
    const val PREF_COLLAPSE_DELETED_COMMENTS = "collapseDeletedComments"
    const val PREF_RIGHT_HANDED_COMMENT_MENU = "rightHandedCommentMenu"
    const val PREF_DUAL_PORTRAIT = "dualPortrait"
    const val PREF_SINGLE_COLUMN_MULTI = "singleColumnMultiWindow"
    const val PREF_CROP_IMAGE = "cropImage"
    const val PREF_COMMENT_FAB = "commentFab"
    const val PREF_SWITCH_THUMB = "switchThumb"
    const val PREF_BIG_THUMBS = "bigThumbnails"
    const val PREF_NO_THUMB = "noThumbnails"
    const val PREF_LOW_RES_ALWAYS = "lowResAlways"
    const val PREF_LOW_RES_MOBILE = "lowRes"
    const val PREF_IMAGE_LQ = "imageLq"
    const val PREF_COLOR_SUB_NAME = "colorSubName"
    const val PREF_OVERRIDE_LANGUAGE = "overrideLanguage"
    const val PREF_IMMERSIVE_MODE = "immersiveMode"
    const val PREF_SHOW_DOMAIN = "showDomain"
    const val PREF_CARD_TEXT = "cardText"
    const val PREF_ZOOM_DEFAULT = "zoomDefault"
    const val PREF_SUBREDDIT_SEARCH_METHOD = "subredditSearchMethod"
    const val PREF_BACK_BUTTON_BEHAVIOR = "backButtonBehavior"
    const val PREF_LQ_LOW = "lqLow"
    const val PREF_LQ_MID = "lqMid"
    const val PREF_LQ_HIGH = "lqHigh"
    const val PREF_LQ_VIDEOS = "lqVideos"
    const val PREF_SOUND_NOTIFS = "soundNotifs"
    const val PREF_COOKIES = "storeCookies"
    const val PREF_NIGHT_START = "nightStart"
    const val PREF_NIGHT_END = "nightEnd"
    const val PREF_SHOW_NSFW_CONTENT = "showNSFWContent"
    const val PREF_HIDE_NSFW_PREVIEW = "hideNSFWPreviews"
    const val PREF_HIDE_NSFW_COLLECTION = "hideNSFWPreviewsCollection"
    const val PREF_IGNORE_SUB_SETTINGS = "ignoreSub"
    const val PREF_HIGHLIGHT_TIME = "highlightTime"
    const val PREF_MUTE = "muted"
    const val PREF_LINK_HANDLING_MODE = "linkHandlingMode"
    const val PREF_FULL_COMMENT_OVERRIDE = "fullCommentOverride"
    const val PREF_ALBUM = "album"
    const val PREF_GIF = "gif"
    const val PREF_HQGIF = "hqgif"
    const val PREF_FASTSCROLL = "Fastscroll"
    const val PREF_FAB_CLEAR = "fabClear"
    const val PREF_HIDEBUTTON = "Hidebutton"
    const val PREF_SAVE_BUTTON = "saveButton"
    const val PREF_IMAGE = "image"
    const val PREF_SELFTEXT_IMAGE_COMMENT = "selftextImageComment"
    const val SYNCCIT_AUTH = "SYNCCIT_AUTH"
    const val SYNCCIT_NAME = "SYNCCIT_NAME"
    const val PREF_BLUR = "blur"
    const val PREF_ALBUM_SWIPE = "albumswipe"
    const val PREF_COMMENT_NAV = "commentVolumeNav"
    const val PREF_COLOR_COMMENT_DEPTH = "colorCommentDepth"
    const val COMMENT_DEPTH = "commentDepth"
    const val COMMENT_COUNT = "commentcount"
    const val PREF_USER_FILTERS = "userFilters"
    const val PREF_COLOR_ICON = "colorIcon"
    const val PREF_PEEK = "peek"
    const val PREF_LARGE_LINKS = "largeLinks"
    const val PREF_LARGE_DEPTH = "largeDepth"
    const val PREF_TITLE_TOP = "titleTop"
    const val PREF_HIGHLIGHT_COMMENT_OP = "commentOP"
    const val PREF_LONG_LINK = "shareLongLink"
    const val PREF_SELECTED_BROWSER = "selectedBrowser"
    const val PREF_SELECTED_DRAWER_ITEMS = "selectedDrawerItems"
    const val PREF_MOD_REMOVAL_TYPE = "removalReasonType"
    const val PREF_MOD_TOOLBOX_ENABLED = "toolboxEnabled"
    const val PREF_MOD_TOOLBOX_MESSAGE = "toolboxMessageType"
    const val PREF_MOD_TOOLBOX_STICKY = "toolboxSticky"
    const val PREF_MOD_TOOLBOX_LOCK = "toolboxLock"
    const val PREF_MOD_TOOLBOX_MODMAIL = "toolboxModmail"
    const val PREF_ALWAYS_SHOW_FAB = "alwaysShowFAB"
    const val PREF_HIGH_COLORSPACE_IMAGES = "highMemoryImages"
    const val PREF_LAST_INBOX = "lastInbox"
    const val PREF_LAYOUT = "PRESET"
    const val PREF_MIDDLE_IMAGE = "middleCard"
    var defaultCardView: CardEnum = CardEnum.LARGE
        set(value) {
            field = value
            prefs!!.edit()
                .putString("defaultCardViewNew", value.name)
                .apply()
        }
    var defaultSorting: Sorting = Sorting.HOT
        set(value) {
            field = value
            prefs!!.edit()
                .putString("defaultSorting", value.name)
                .apply()
        }
    var timePeriod: TimePeriod = TimePeriod.DAY
        set(value) {
            field = value
            prefs!!.edit()
                .putString("timePeriod", value.name)
                .apply()
        }
    var defaultCommentSorting: CommentSort = CommentSort.CONFIDENCE
        set(value) {
            field = value
            prefs!!.edit()
                .putString("defaultCommentSortingNew", value.name)
                .apply()
        }
    var middleImage = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_MIDDLE_IMAGE, value)
                .apply()
        }
    var bigPicEnabled = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean("bigPicEnabled", value)
                .apply()
        }
    var bigPicCropped = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean("bigPicCropped", value)
                .apply()
        }
    var colorMatchingMode: ColorMatchingMode? = null
    var colorIndicator: ColorIndicator? = null
    var theme: ThemeEnum? = null

    var expandedToolbar = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_EXPANDED_TOOLBAR, value)
                .apply()
        }
    var single = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_SINGLE, value)
                .apply()
        }
    @JvmField var swap = false
    @JvmField var album = false
    @JvmField var cache = false
    @JvmField var expandedSettings = false
    @JvmField var fabComments = false
    @JvmField var largeDepth = false
    var cacheDefault = false
    @JvmField var image = false
    @JvmField var video = false
    @JvmField var upvotePercentage = false
    @JvmField var colorBack = false
    @JvmField var colorNavBar = false
    var actionbarVisible = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_ACTIONBAR_VISIBLE, value)
                .apply()
        }
    var actionbarTap = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_ACTIONBAR_TAP, value)
                .apply()
        }
    @JvmField var commentAutoHide = false
    @JvmField var showCollapseExpand = false
    var fullCommentOverride = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_FULL_COMMENT_OVERRIDE, value)
                .apply()
        }
    @JvmField var lowResAlways = false
    @JvmField var noImages = false
    @JvmField var lowResMobile = false
    var blurCheck = false
    @JvmField var readerNight = false
    @JvmField var swipeAnywhere = false
    var commentLastVisit = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_COMMENT_LAST_VISIT, value)
                .apply()
        }
    @JvmField var storeHistory = false
    var showNSFWContent = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_SHOW_NSFW_CONTENT, value)
                .apply()
        }
    @JvmField var storeNSFWHistory = false
    @JvmField var scrollSeen = false
    var saveButton = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_SAVE_BUTTON, value)
                .apply()
        }
    @JvmField var voteGestures = false
    @JvmField var colorEverywhere = false
    @JvmField var gif = false
    @JvmField var hqgif = false
    @JvmField var colorCommentDepth = false
    @JvmField var commentVolumeNav = false
    var postNav = false
    @JvmField var cropImage = false
    var smallTag = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_SMALL_TAG, value)
                .apply()
        }
    var typeInfoLine = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_TYPE_INFO_LINE, value)
                .apply()
        }
    var votesInfoLine = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_VOTES_INFO_LINE, value)
                .apply()
        }
    var readerMode = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_READER_MODE, value)
                .apply()
        }
    @JvmField var collapseComments = false
    @JvmField var collapseCommentsDefault = false
    @JvmField var collapseDeletedComments = false
    @JvmField var rightHandedCommentMenu = false
    var abbreviateScores = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_ABBREVIATE_SCORES, value)
                .apply()
        }
    var hidePostAwards = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_HIDE_POST_AWARDS, value)
                .apply()
        }
    @JvmField var hideCommentAwards = false
    @JvmField var shareLongLink = false
    var isMuted = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_MUTE, value)
                .apply()
        }
    var subredditSearchMethod = 0
        set(value) {
            field = value
            prefs!!.edit()
                .putInt(PREF_SUBREDDIT_SEARCH_METHOD, value)
                .apply()
        }
    var backButtonBehavior = 0
        set(value) {
            field = value
            prefs!!.edit()
                .putInt(PREF_BACK_BUTTON_BEHAVIOR, value)
                .apply()
        }
    var nightStart = 0
        set(value) {
            field = value
            prefs!!.edit()
                .putInt(PREF_NIGHT_START, value)
                .apply()
        }
    var nightEnd = 0
        set(value) {
            field = value
            prefs!!.edit()
                .putInt(PREF_NIGHT_END, value)
                .apply()
        }
    var linkHandlingMode = 0
        set(value) {
            field = value
            prefs!!.edit()
                .putInt(PREF_LINK_HANDLING_MODE, value)
                .apply()
        }
    @JvmField var previews = 0
    @JvmField var synccitName: String? = null
    @JvmField var synccitAuth: String? = null
    var titleFilters: Set<String> = emptySet()
        set(value) {
            field = value
            prefs!!.edit()
                .putStringSet(PREF_TITLE_FILTERS, value)
                .apply()
        }
    var textFilters: Set<String> = emptySet()
        set(value) {
            field = value
            prefs!!.edit()
                .putStringSet(PREF_TEXT_FILTERS, value)
                .apply()
        }
    var domainFilters: Set<String> = emptySet()
        set(value) {
            field = value
            prefs!!.edit()
                .putStringSet(PREF_DOMAIN_FILTERS, value)
                .apply()
        }
    var subredditFilters: Set<String> = emptySet()
        set(value) {
            field = value
            prefs!!.edit()
                .putStringSet(PREF_SUBREDDIT_FILTERS, value)
                .apply()
        }
    var flairFilters: Set<String> = emptySet()
        set(value) {
            field = value
            prefs!!.edit()
                .putStringSet(PREF_FLAIR_FILTERS, value)
                .apply()
        }
    var alwaysExternal: Set<String> = emptySet()
        set(value) {
            field = value
            prefs!!.edit()
                .putStringSet(PREF_ALWAYS_EXTERNAL, value)
                .apply()
        }
    var userFilters: Set<String> = emptySet()
        set(value) {
            field = value
            prefs!!.edit()
                .putStringSet(PREF_USER_FILTERS, value)
                .apply()
        }
    @JvmField var loadImageLq = false
    @JvmField var ignoreSubSetting = false
    @JvmField var hideNSFWCollection = false
    var highColorspaceImages = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_HIGH_COLORSPACE_IMAGES, value)
                .apply()
        }
    @JvmField var fastscroll = false
    var fab = true
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_FAB, value)
                .apply()
        }
    var fabType = Constants.FAB_POST
        set(value) {
            field = value
            prefs!!.edit()
                .putInt(PREF_FAB_TYPE, value)
                .apply()
        }
    var hideButton = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_HIDEBUTTON, value)
                .apply()
        }
    @JvmField var isPro = false
    var customtabs = false
    var titleTop = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_TITLE_TOP, value)
                .apply()
        }
    var dualPortrait = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_DUAL_PORTRAIT, value)
                .apply()
        }
    var singleColumnMultiWindow = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_SINGLE_COLUMN_MULTI, value)
                .apply()
        }
    var nightModeState = 0
        set(value) {
            field = value
            prefs!!.edit().putInt(PREF_NIGHT_MODE_STATE, value)
                .apply()
        }
    var imageSubfolders = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_IMAGE_SUBFOLDERS, value)
                .apply()
        }
    var imageDownloadButton = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_IMAGE_DOWNLOAD_BUTTON, value)
                .apply()
        }
    var autoTime = false
    var albumSwipe = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_ALBUM_SWIPE, value)
                .apply()
        }
    var switchThumb = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_SWITCH_THUMB, value)
                .apply()
        }
    var bigThumbnails = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean("bigThumbnails", value)
                .apply()
        }
    var noThumbnails = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean("noThumbnails", value)
                .apply()
        }
    var commentPager = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_COMMENT_PAGER, value)
                .apply()
        }
    var alphabetizeOnSubscribe = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_ALPHABETIZE_SUBSCRIBE, value)
                .apply()
        }
    @JvmField var colorSubName = false
    var hideSelftextLeadImage = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_SELFTEXT_IMAGE_COMMENT, value)
                .apply()
        }
    var overrideLanguage = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_OVERRIDE_LANGUAGE, value)
                .apply()
        }
    var immersiveMode = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_IMMERSIVE_MODE, value)
                .apply()
        }
    var showDomain = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_SHOW_DOMAIN, value)
                .apply()
        }
    var cardText = false
        set(value) {
            field = value
            prefs!!.edit().putBoolean(PREF_CARD_TEXT, value)
                .apply()
        }
    var alwaysZoom = false
    @JvmField var lqLow = false
    @JvmField var lqMid = true
    @JvmField var lqHigh = false
    @JvmField var lqVideos = false
    @JvmField var currentTheme = 0 //current base theme (Light, Dark, Dark blue, etc.)
    var nightTheme = 0
        set(value) {
            field = value
            prefs!!.edit()
                .putInt(PREF_NIGHT_THEME, value)
                .apply()
        }
    @JvmField var typeInText = false
    var notifSound = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_SOUND_NOTIFS, value)
                .apply()
        }
    var cookies = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_COOKIES, value)
                .apply()
        }
    @JvmField var colorIcon = false
    @JvmField var peek = false
    @JvmField var largeLinks = false
    @JvmField var highlightCommentOP = false
    @JvmField var highlightTime = false
    var selectedBrowser: String = ""
        set(value) {
            field = value
            prefs!!.edit()
                .putString(PREF_SELECTED_BROWSER, value)
                .apply()
        }
    var selectedDrawerItems: Long = 0
        set(value) {
            field = value
            prefs!!.edit()
                .putLong(PREF_COOKIES, value)
                .apply()
        }
    var forcedNightModeState = ForcedState.NOT_FORCED
    @JvmField var toolboxEnabled = false
    @JvmField var removalReasonType = 0
    @JvmField var toolboxMessageType = 0
    @JvmField var toolboxSticky = false
    @JvmField var toolboxLock = false
    @JvmField var toolboxModmail = false
    var alwaysShowFAB = false
        set(value) {
            field = value
            prefs!!.edit()
                .putBoolean(PREF_ALWAYS_SHOW_FAB, value)
                .apply()
        }

    fun setAllValues(settings: SharedPreferences) {
        prefs = settings
        defaultCardView = CardEnum.valueOf(
            settings.getString("defaultCardViewNew", "LARGE")!!.uppercase(Locale.getDefault())
        )
        middleImage = settings.getBoolean("middleCard", false)
        bigPicCropped = settings.getBoolean("bigPicCropped", true)
        bigPicEnabled = settings.getBoolean("bigPicEnabled", true)
        alwaysShowFAB = settings.getBoolean("alwaysShowFAB", false)
        colorMatchingMode = ColorMatchingMode.valueOf(
            settings.getString("ccolorMatchingModeNew", "MATCH_EXTERNALLY")!!
        )
        colorIndicator = ColorIndicator.valueOf(
            settings.getString("colorIndicatorNew", "CARD_BACKGROUND")!!
        )
        defaultSorting = Sorting.valueOf(settings.getString("defaultSorting", "HOT")!!)
        timePeriod = TimePeriod.valueOf(settings.getString("timePeriod", "DAY")!!)
        defaultCommentSorting =
            CommentSort.valueOf(settings.getString("defaultCommentSortingNew", "CONFIDENCE")!!)
        showNSFWContent = prefs!!.getBoolean(PREF_SHOW_NSFW_CONTENT, false)
        hideNSFWCollection = prefs!!.getBoolean(PREF_HIDE_NSFW_COLLECTION, true)
        ignoreSubSetting = prefs!!.getBoolean(PREF_IGNORE_SUB_SETTINGS, false)
        single = prefs!!.getBoolean(PREF_SINGLE, false)
        readerNight = prefs!!.getBoolean(PREF_READER_NIGHT, false)
        blurCheck = prefs!!.getBoolean(PREF_BLUR, false)
        overrideLanguage = prefs!!.getBoolean(PREF_OVERRIDE_LANGUAGE, false)
        immersiveMode = prefs!!.getBoolean(PREF_IMMERSIVE_MODE, false)
        largeDepth = prefs!!.getBoolean(PREF_LARGE_DEPTH, false)
        readerMode = prefs!!.getBoolean(PREF_READER_MODE, false)
        imageSubfolders = prefs!!.getBoolean(PREF_IMAGE_SUBFOLDERS, false)
        imageDownloadButton = prefs!!.getBoolean(PREF_IMAGE_DOWNLOAD_BUTTON, true)
        isMuted = prefs!!.getBoolean(PREF_MUTE, false)
        commentVolumeNav = prefs!!.getBoolean(PREF_COMMENT_NAV, false)
        postNav = false
        fab = prefs!!.getBoolean(PREF_FAB, true)
        fabType = prefs!!.getInt(PREF_FAB_TYPE, Constants.FAB_DISMISS)
        if (fabType > 3 || fabType < 0) {
            fabType = Constants.FAB_DISMISS
            prefs!!.edit().putInt(PREF_FAB_TYPE, Constants.FAB_DISMISS).apply()
        }
        subredditSearchMethod = prefs!!.getInt(
            PREF_SUBREDDIT_SEARCH_METHOD,
            Constants.SUBREDDIT_SEARCH_METHOD_DRAWER
        )
        if (subredditSearchMethod > 3 || subredditSearchMethod < 0) {
            subredditSearchMethod = 1
            prefs!!.edit().putInt(PREF_SUBREDDIT_SEARCH_METHOD, 1).apply()
        }
        backButtonBehavior = prefs!!.getInt(
            PREF_BACK_BUTTON_BEHAVIOR,
            Constants.BackButtonBehaviorOptions.ConfirmExit.value
        )
        highlightTime = prefs!!.getBoolean(PREF_HIGHLIGHT_TIME, true)

        // TODO: Remove the old pref check in a later version
        // This handles forward migration from the old night_mode boolean state
        nightModeState = prefs!!.getInt(
            PREF_NIGHT_MODE_STATE, (if (prefs!!.getBoolean(
                    PREF_NIGHT_MODE, false
                )
            ) NightModeState.MANUAL else NightModeState.DISABLED).ordinal
        )
        nightTheme = prefs!!.getInt(PREF_NIGHT_THEME, 0)
        autoTime = prefs!!.getBoolean(PREF_AUTOTHEME, false)
        colorBack = prefs!!.getBoolean(PREF_COLOR_BACK, false)
        cardText = prefs!!.getBoolean(PREF_CARD_TEXT, false)
        colorNavBar = prefs!!.getBoolean(PREF_COLOR_NAV_BAR, false)
        shareLongLink = prefs!!.getBoolean(PREF_LONG_LINK, false)
        colorEverywhere = prefs!!.getBoolean(PREF_COLOR_EVERYWHERE, true)
        colorCommentDepth = prefs!!.getBoolean(PREF_COLOR_COMMENT_DEPTH, true)
        alwaysZoom = prefs!!.getBoolean(PREF_ZOOM_DEFAULT, true)
        collapseComments = prefs!!.getBoolean(PREF_COLLAPSE_COMMENTS, false)
        collapseCommentsDefault = prefs!!.getBoolean(PREF_COLLAPSE_COMMENTS_DEFAULT, false)
        collapseDeletedComments = prefs!!.getBoolean(PREF_COLLAPSE_DELETED_COMMENTS, false)
        rightHandedCommentMenu = prefs!!.getBoolean(PREF_RIGHT_HANDED_COMMENT_MENU, false)
        commentAutoHide = prefs!!.getBoolean(PREF_AUTOHIDE_COMMENTS, false)
        showCollapseExpand = prefs!!.getBoolean(PREF_SHOW_COLLAPSE_EXPAND, false)
        highlightCommentOP = prefs!!.getBoolean(PREF_HIGHLIGHT_COMMENT_OP, true)
        typeInfoLine = prefs!!.getBoolean(PREF_TYPE_INFO_LINE, false)
        votesInfoLine = prefs!!.getBoolean(PREF_VOTES_INFO_LINE, false)
        titleTop = prefs!!.getBoolean(PREF_TITLE_TOP, true)
        lqLow = prefs!!.getBoolean(PREF_LQ_LOW, false)
        lqMid = prefs!!.getBoolean(PREF_LQ_MID, true)
        lqHigh = prefs!!.getBoolean(PREF_LQ_HIGH, false)
        lqVideos = prefs!!.getBoolean(PREF_LQ_VIDEOS, true)
        highColorspaceImages = prefs!!.getBoolean(PREF_HIGH_COLORSPACE_IMAGES, false)
        noImages = prefs!!.getBoolean(PREF_NO_IMAGES, false)
        abbreviateScores = prefs!!.getBoolean(PREF_ABBREVIATE_SCORES, true)
        hidePostAwards = prefs!!.getBoolean(PREF_HIDE_POST_AWARDS, false)
        hideCommentAwards = prefs!!.getBoolean(PREF_HIDE_COMMENT_AWARDS, false)
        lowResAlways = prefs!!.getBoolean(PREF_LOW_RES_ALWAYS, false)
        lowResMobile = prefs!!.getBoolean(PREF_LOW_RES_MOBILE, false)
        loadImageLq = prefs!!.getBoolean(PREF_IMAGE_LQ, false)
        showDomain = prefs!!.getBoolean(PREF_SHOW_DOMAIN, false)
        expandedToolbar = prefs!!.getBoolean(PREF_EXPANDED_TOOLBAR, false)
        voteGestures = prefs!!.getBoolean(PREF_VOTE_GESTURES, false)
        fullCommentOverride = prefs!!.getBoolean(PREF_FULL_COMMENT_OVERRIDE, false)
        alphabetizeOnSubscribe = prefs!!.getBoolean(PREF_ALPHABETIZE_SUBSCRIBE, false)
        commentPager = prefs!!.getBoolean(PREF_COMMENT_PAGER, false)
        smallTag = prefs!!.getBoolean(PREF_SMALL_TAG, false)
        swap = prefs!!.getBoolean(PREF_SWAP, false)
        hideSelftextLeadImage = prefs!!.getBoolean(PREF_SELFTEXT_IMAGE_COMMENT, false)
        image = prefs!!.getBoolean(PREF_IMAGE, true)
        cache = true
        cacheDefault = false
        storeHistory = prefs!!.getBoolean(PREF_STORE_HISTORY, true)
        upvotePercentage = prefs!!.getBoolean(PREF_UPVOTE_PERCENTAGE, false)
        storeNSFWHistory = prefs!!.getBoolean(PREF_STORE_NSFW_HISTORY, false)
        scrollSeen = prefs!!.getBoolean(PREF_SCROLL_SEEN, false)
        synccitName = prefs!!.getString(SYNCCIT_NAME, "")
        synccitAuth = prefs!!.getString(SYNCCIT_AUTH, "")
        notifSound = prefs!!.getBoolean(PREF_SOUND_NOTIFS, false)
        cookies = prefs!!.getBoolean(PREF_COOKIES, true)
        linkHandlingMode = prefs!!.getInt(
            PREF_LINK_HANDLING_MODE,
            SettingsHandlingFragment.LinkHandlingMode.EXTERNAL.value
        )
        previews = prefs!!.getInt(PREVIEWS_LEFT, 10)
        nightStart = prefs!!.getInt(PREF_NIGHT_START, 9)
        nightEnd = prefs!!.getInt(PREF_NIGHT_END, 5)
        fabComments = prefs!!.getBoolean(PREF_COMMENT_FAB, false)
        largeLinks = prefs!!.getBoolean(PREF_LARGE_LINKS, false)

        // SharedPreferences' StringSets should never be modified, so we duplicate them into a new HashSet
        titleFilters = HashSet(prefs!!.getStringSet(PREF_TITLE_FILTERS, HashSet()))
        textFilters = HashSet(prefs!!.getStringSet(PREF_TEXT_FILTERS, HashSet()))
        domainFilters = HashSet(prefs!!.getStringSet(PREF_DOMAIN_FILTERS, HashSet()))
        subredditFilters = HashSet(prefs!!.getStringSet(PREF_SUBREDDIT_FILTERS, HashSet()))
        alwaysExternal = HashSet(prefs!!.getStringSet(PREF_ALWAYS_EXTERNAL, HashSet()))
        flairFilters = HashSet(prefs!!.getStringSet(PREF_FLAIR_FILTERS, HashSet()))
        userFilters = HashSet(prefs!!.getStringSet(PREF_USER_FILTERS, HashSet()))
        dualPortrait = prefs!!.getBoolean(PREF_DUAL_PORTRAIT, false)
        singleColumnMultiWindow = prefs!!.getBoolean(PREF_SINGLE_COLUMN_MULTI, false)
        colorSubName = prefs!!.getBoolean(PREF_COLOR_SUB_NAME, false)
        cropImage = prefs!!.getBoolean(PREF_CROP_IMAGE, true)
        switchThumb = prefs!!.getBoolean(PREF_SWITCH_THUMB, true)
        bigThumbnails = prefs!!.getBoolean(PREF_BIG_THUMBS, false)
        noThumbnails = prefs!!.getBoolean(PREF_NO_THUMB, false)
        swipeAnywhere = true //override this always now
        album = prefs!!.getBoolean(PREF_ALBUM, true)
        albumSwipe = prefs!!.getBoolean(PREF_ALBUM_SWIPE, true)
        commentLastVisit = prefs!!.getBoolean(PREF_COMMENT_LAST_VISIT, false)
        gif = prefs!!.getBoolean(PREF_GIF, true)
        hqgif = prefs!!.getBoolean(PREF_HQGIF, false)
        video = true
        fastscroll = prefs!!.getBoolean(PREF_FASTSCROLL, true)
        typeInText = prefs!!.getBoolean(PREF_TYPE_IN_TEXT, false)
        hideButton = prefs!!.getBoolean(PREF_HIDEBUTTON, false)
        saveButton = prefs!!.getBoolean(PREF_SAVE_BUTTON, false)
        actionbarVisible = prefs!!.getBoolean(PREF_ACTIONBAR_VISIBLE, true)
        actionbarTap = prefs!!.getBoolean(PREF_ACTIONBAR_TAP, false)
        colorIcon = prefs!!.getBoolean(PREF_COLOR_ICON, false)
        peek = prefs!!.getBoolean(PREF_PEEK, false)
        selectedBrowser = prefs!!.getString(PREF_SELECTED_BROWSER, "") ?: ""
        selectedDrawerItems = prefs!!.getLong(PREF_SELECTED_DRAWER_ITEMS, -1)
        toolboxEnabled = prefs!!.getBoolean(PREF_MOD_TOOLBOX_ENABLED, false)
        removalReasonType = prefs!!.getInt(PREF_MOD_REMOVAL_TYPE, RemovalReasonType.SLIDE.ordinal)
        toolboxMessageType =
            prefs!!.getInt(PREF_MOD_TOOLBOX_MESSAGE, ToolboxRemovalMessageType.COMMENT.ordinal)
        toolboxSticky = prefs!!.getBoolean(PREF_MOD_TOOLBOX_STICKY, false)
        toolboxLock = prefs!!.getBoolean(PREF_MOD_TOOLBOX_LOCK, false)
        toolboxModmail = prefs!!.getBoolean(PREF_MOD_TOOLBOX_MODMAIL, false)
    }

    @JvmStatic fun hasPicsEnabled(sub: String): Boolean {
        return prefs!!.contains("picsenabled" + sub.toLowerCase(Locale.ENGLISH))
    }

    @JvmStatic fun setPicsEnabled(sub: String, checked: Boolean) {
        prefs!!.edit().putBoolean("picsenabled" + sub.lowercase(), checked).apply()
    }

    @JvmStatic fun resetPicsEnabled(sub: String) {
        prefs!!.edit().remove("picsenabled" + sub.lowercase()).apply()
    }

    @JvmStatic fun resetPicsEnabledAll() {
        val e = prefs!!.edit()
        for ((key) in prefs!!.all) {
            if (key.startsWith("picsenabled")) {
                e.remove(key) //reset all overridden values
            }
        }
        e.apply()
    }

    @JvmStatic fun isPicsEnabled(subreddit: String?): Boolean {
        return if (subreddit == null) bigPicEnabled else prefs!!.getBoolean(
            "picsenabled" + subreddit.lowercase(),
            bigPicEnabled
        )
    }

    @JvmStatic fun isSelftextEnabled(subreddit: String?): Boolean {
        return if (subreddit == null) cardText else prefs!!.getBoolean(
            "cardtextenabled" + subreddit.lowercase(),
            cardText
        )
    }

    @JvmStatic fun setSelftextEnabled(sub: String, checked: Boolean) {
        prefs!!.edit()
            .putBoolean("cardtextenabled" + sub.lowercase(), checked)
            .apply()
    }

    @JvmStatic val isNSFWEnabled: Boolean
        get() = prefs!!.getBoolean(PREF_HIDE_NSFW_PREVIEW + Authentication.name, true)

    @JvmStatic fun resetSelftextEnabled(subreddit: String) {
        prefs!!.edit().remove("cardtextenabled" + subreddit.lowercase()).apply()
    }

    @JvmStatic fun setDefaultCommentSorting(commentSorting: CommentSort, subreddit: String) {
        prefs!!.edit()
            .putString(
                "defaultComment" + subreddit.lowercase(),
                commentSorting.name
            )
            .apply()
    }

    @JvmStatic fun getCommentSorting(sub: String): CommentSort {
        return CommentSort.valueOf(
            prefs!!.getString(
                "defaultComment" + sub.lowercase(),
                defaultCommentSorting!!.name
            )!!
        )
    }

    @JvmStatic fun setSubSorting(linkSorting: Sorting, time: TimePeriod, subreddit: String) {
        prefs!!.edit()
            .putString(
                "defaultSort" + subreddit.lowercase(),
                linkSorting.name
            )
            .apply()
        prefs!!.edit()
            .putString("defaultTime" + subreddit.lowercase(), time.name)
            .apply()
    }

    @JvmStatic fun getSubmissionSort(sub: String): Sorting? {
        val subreddit = sub.lowercase()
        return if (SortingUtil.sorting.containsKey(subreddit)) {
            SortingUtil.sorting[subreddit]
        } else {
            Sorting.valueOf(
                prefs!!.getString(
                    "defaultSort" + sub.lowercase(),
                    SortingUtil.defaultSorting.name
                )!!
            )
        }
    }

    @JvmStatic fun getSubmissionTimePeriod(sub: String): TimePeriod? {
        val subreddit = sub.lowercase()
        return if (SortingUtil.times.containsKey(subreddit)) {
            SortingUtil.times[subreddit]
        } else {
            TimePeriod.valueOf(
                prefs!!.getString(
                    "defaultTime" + sub.lowercase(),
                    SortingUtil.timePeriod.name
                )!!
            )
        }
    }// unset forced state if forcing is now unnecessary - allows for normal night mode on/off transitions

    @JvmStatic val lastInbox: Long
        get() {
            val value = prefs!!.getLong(PREF_LAST_INBOX,
                System.currentTimeMillis() - (60 * 1000 * 60))
            prefs!!.edit().putLong(PREF_LAST_INBOX, System.currentTimeMillis()).apply()
            return value
        }

    @JvmStatic var commentDepth: Int?
        get() {
            val value = prefs!!.getString(COMMENT_DEPTH, "")!!
            return when (value.isBlank()) {
                true -> null
                false -> Integer.parseInt(value)
            }
        }
        set(value) {
            if (value != null)
                prefs!!.edit().putString(COMMENT_DEPTH, value.toString()).apply()
        }

    @JvmStatic var commentCount: Int?
        get() {
            val value = prefs!!.getString(COMMENT_COUNT, "")!!
            return when (value.isBlank()) {
                true -> null
                false -> Integer.parseInt(value)
            }
        }
        set(value) {
            if (value != null)
                prefs!!.edit().putString(COMMENT_COUNT, value.toString()).apply()
        }

    /* Logic for the now rather complicated night mode:
       *
       * Normal       | Forced            | Actual state
       * -----------------------------------------------------
       * Disabled     | On/Off            | Forced state
       * On           | On - gets unset   | On
       * Off          | Off - gets unset  | Off
       * On           | Off               | Off
       * Off          | On                | On
       * On/Off       | Unset             | Normal state
       *
       * Forced night mode state is intentionally not persisted between app runs and defaults to unset
       */
    @JvmStatic val isNight: Boolean
        get() =/* Logic for the now rather complicated night mode:
              *
              * Normal       | Forced            | Actual state
              * -----------------------------------------------------
              * Disabled     | On/Off            | Forced state
              * On           | On - gets unset   | On
              * Off          | Off - gets unset  | Off
              * On           | Off               | Off
              * Off          | On                | On
              * On/Off       | Unset             | Normal state
              *
              * Forced night mode state is intentionally not persisted between app runs and defaults to unset
              */
            if (isPro && NightModeState.isEnabled) {
                var night = false
                if (App.canUseNightModeAuto && nightModeState == NightModeState.AUTOMATIC.ordinal) {
                    night = (appContext.resources.configuration.uiMode
                            and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                } else if (nightModeState == NightModeState.MANUAL.ordinal) {
                    val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
                    night = hour >= nightStart + 12 || hour < nightEnd
                }

                // unset forced state if forcing is now unnecessary - allows for normal night mode on/off transitions
                if (forcedNightModeState == (if (night) ForcedState.FORCED_ON else ForcedState.FORCED_OFF)) {
                    forcedNightModeState = ForcedState.NOT_FORCED
                }
                if (forcedNightModeState == ForcedState.FORCED_ON || forcedNightModeState == ForcedState.FORCED_OFF) {
                    forcedNightModeState == ForcedState.FORCED_ON
                } else {
                    night
                }
            } else {
                false
            }

    @JvmStatic fun getBaseSubmissionSort(sub: String): Sorting {
        return Sorting.valueOf(
            prefs!!.getString(
                "defaultSort" + sub.lowercase(),
                SortingUtil.defaultSorting.name
            )!!
        )
    }

    @JvmStatic fun getBaseTimePeriod(sub: String): TimePeriod {
        return TimePeriod.valueOf(
            prefs!!.getString(
                "defaultTime" + sub.lowercase(),
                SortingUtil.timePeriod.name
            )!!
        )
    }

    @JvmStatic fun decreasePreviewsLeft(): Int {
        prefs!!.edit()
            .putInt(
                PREVIEWS_LEFT,
                previews - 1
            )
            .apply()
        previews = prefs!!.getInt(
            PREVIEWS_LEFT, 10
        )
        return previews
    }

    @JvmStatic fun hasSort(subreddit: String): Boolean {
        return prefs!!.contains("defaultSort" + subreddit.lowercase())
    }

    @JvmStatic fun clearSort(subreddit: String) {
        prefs!!.edit().remove("defaultSort" + subreddit.lowercase())
            .apply()
        prefs!!.edit().remove("defaultTime" + subreddit.lowercase())
            .apply()
    }

    @JvmStatic fun getLayoutSettings(subreddit: String): Boolean? {
        return prefs!!.contains(PREF_LAYOUT + subreddit.lowercase(Locale.ENGLISH))
    }

    @JvmStatic fun setLayoutSettings(subreddit: String, state: Boolean) {
        if (state) {
            prefs!!.edit().remove(PREF_LAYOUT + subreddit.lowercase(Locale.ENGLISH))
                .apply()
        } else {
            prefs!!.edit().putBoolean(PREF_LAYOUT + subreddit.lowercase(Locale.ENGLISH), true)
                .apply()
        }
    }

    @JvmStatic fun setListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        clearListener()

        prefsListener = listener
        prefs!!.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    @JvmStatic fun clearListener() {
        prefs!!.registerOnSharedPreferenceChangeListener(prefsListener)
        prefsListener = null
    }

    @Deprecated("use properties")
    @JvmStatic fun editInt(settingValueString: String, value: Int) {
        prefs!!.edit().putInt(settingValueString, value).apply()
    }

    @Deprecated("use properties")
    @JvmStatic fun editBoolean(settingValueString: String, value: Boolean) {
        prefs!!.edit().putBoolean(settingValueString, value).apply()
    }

    enum class RemovalReasonType {
        SLIDE, TOOLBOX, REDDIT
    }

    enum class ToolboxRemovalMessageType {
        COMMENT, PM, BOTH, NONE
    }

    enum class ColorIndicator {
        CARD_BACKGROUND, TEXT_COLOR, NONE
    }

    enum class ColorMatchingMode {
        ALWAYS_MATCH, MATCH_EXTERNALLY
    }

    enum class NightModeState {
        DISABLED, MANUAL, AUTOMATIC;

        companion object {
            val isEnabled: Boolean
                get() = nightModeState != DISABLED.ordinal || forcedNightModeState != ForcedState.NOT_FORCED
        }
    }

    enum class ForcedState {
        NOT_FORCED, FORCED_ON, FORCED_OFF
    }
}

object Preferences {
    val settings: SharedPreferences by lazy { getSharedPreferences("SETTINGS") }

    val authentication: SharedPreferences by lazy { getSharedPreferences("AUTHENTICATION") }

    val colours: SharedPreferences by lazy { getSharedPreferences("COLOUR") }

    val appRestart: SharedPreferences by lazy { getSharedPreferences("APP_RESTART") }

    val tags: SharedPreferences by lazy { getSharedPreferences("TAGS") }

    val seen: SharedPreferences by lazy { getSharedPreferences("SEEN") }

    val hidden: SharedPreferences by lazy { getSharedPreferences("HIDDEN") }

    val hiddenPosts: SharedPreferences by lazy { getSharedPreferences("HIDDEN_POSTS") }

    val albums: SharedPreferences by lazy { getSharedPreferences("ALBUMS") }

    val tumblr: SharedPreferences by lazy { getSharedPreferences("TUMBLR") }

    val cache: SharedPreferences by lazy { getSharedPreferences("CACHE") }

    val subscriptions: SharedPreferences by lazy { getSharedPreferences("SUBS") }

    val filters: SharedPreferences by lazy { getSharedPreferences("FILTERS") }

    val upgrade: SharedPreferences by lazy { getSharedPreferences("UPGRADE") }
}

private fun getSharedPreferences(name: String, mode: Int = 0): SharedPreferences {
    return appContext.getSharedPreferences(name, mode)
}
