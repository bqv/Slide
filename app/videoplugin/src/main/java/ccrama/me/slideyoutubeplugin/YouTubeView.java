package ccrama.me.slideyoutubeplugin;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import ccrama.me.slidevideoplugin.R;


public class YouTubeView extends BaseYouTubeView implements YouTubePlayer.OnInitializedListener {
    public static final String EXTRA_URL = "url";
    private static final int RECOVERY_DIALOG_REQUEST = 1;
    int millis = 0;
    String playlist;
    String video;
    private YouTubePlayerView youTubeView;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        String url;
        int length;
        overrideSwipeFromAnywhere();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube);
        Uri data = getIntent().getData();
        if (data == null) {
            url = getIntent().getExtras().getString(EXTRA_URL, "");
        } else {
            url = data.toString();
        }
        String url2 = Html.fromHtml(url).toString();
        this.video = url2;
        Log.v("Slide", "URL is " + url2);
        if (url2.contains("#t=")) {
            url2 = url2.replace("#t=", url2.contains("?") ? "&t=" : "?t=");
        }
        try {
            Uri i = Uri.parse(url2);
            if (i.getQueryParameterNames().contains("t")) {
                this.millis = getTimeFromUrl(i.getQueryParameter("t"));
            } else if (i.getQueryParameterNames().contains("start")) {
                this.millis = getTimeFromUrl(i.getQueryParameter("start"));
            }
            Log.v("Slide", "Checking playlist");
            if (i.getQueryParameterNames().contains("list")) {
                this.playlist = i.getQueryParameter("list");
                Log.v("Slide", "Playlist is " + this.playlist);
            }
            if (i.getQueryParameterNames().contains("v")) {
                this.video = i.getQueryParameter("v");
            } else if (i.getQueryParameterNames().contains("w")) {
                this.video = i.getQueryParameter("w");
            } else if (url2.toLowerCase().contains("youtu.be")) {
                this.video = i.getLastPathSegment();
            }
            if (i.getQueryParameterNames().contains("u")) {
                String param = i.getQueryParameter("u");
                int indexOf = param.indexOf("=") + 1;
                if (param.contains("&")) {
                    length = param.indexOf("&");
                } else {
                    length = param.length();
                }
                this.video = param.substring(indexOf, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            final String finalUrl = url2;
            new AlertDialog.Builder(this.getSwipeBackLayout().getContext()).setTitle((CharSequence) "Uh oh, something went wrong").setMessage((CharSequence) "This video could not be opened. Would you like to try opening externally?").setPositiveButton((CharSequence) "OK", (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent("android.intent.action.VIEW", YouTubeView.formatURL(finalUrl));
                    if (intent.resolveActivity(YouTubeView.this.getPackageManager()) != null) {
                        YouTubeView.this.startActivity(intent);
                    }
                }
            }).setNegativeButton((CharSequence) "NO", (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    YouTubeView.this.finish();
                }
            }).show();
        }
        this.youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        this.youTubeView.initialize("193292713714-hbhe9ndnvk82uemdnvgmcgo6curhm6r2.apps.googleusercontent.com", this);
    }

    public static Uri formatURL(String url) {
        if (url.startsWith("//")) {
            url = url + "https:";
        }
        Uri uri = Uri.parse(url);
        return uri.buildUpon().scheme(uri.getScheme().toLowerCase()).build();
    }

    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, 1).show();
        }
    }

    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
        if (!wasRestored) {
            player.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
            if (this.playlist == null || this.playlist.isEmpty()) {
                player.loadVideo(this.video, this.millis);
            } else {
                player.loadPlaylist(this.playlist);
            }
            player.addFullscreenControlFlag(1);
            return;
        }
        player.play();
    }

    public static int getTimeFromUrl(String time) {
        int timeAdd = 0;
        for (String s : time.split("s|m|h")) {
            if (time.contains(s + "s")) {
                timeAdd += Integer.valueOf(s).intValue();
            } else if (time.contains(s + "m")) {
                timeAdd += Integer.valueOf(s).intValue() * 60;
            } else if (time.contains(s + "h")) {
                timeAdd += Integer.valueOf(s).intValue() * 3600;
            }
        }
        if (timeAdd == 0) {
            timeAdd += Integer.valueOf(time).intValue();
        }
        return timeAdd * 1000;
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            getYouTubePlayerProvider().initialize("193292713714-hbhe9ndnvk82uemdnvgmcgo6curhm6r2.apps.googleusercontent.com", this);
        }
    }

    private YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerView) findViewById(R.id.youtube_view);
    }
}
