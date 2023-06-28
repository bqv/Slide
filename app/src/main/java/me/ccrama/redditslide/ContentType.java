package me.ccrama.redditslide;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;

import ltd.ucode.slide.R;

import net.dean.jraw.models.Submission;

import java.util.HashMap;

/**
 * Created by ccrama on 5/26/2015.
 */
public class ContentType extends ltd.ucode.slide.ContentType {
    /**
     * Returns a string identifier for a submission e.g. Link, GIF, NSFW Image
     *
     * @param submission Submission to get the description for
     * @return the String identifier
     */
    private static int getContentID(Submission submission) {
        return getContentID(getContentType(submission), submission.isNsfw());
    }

    public static int getContentID(Type contentType, boolean nsfw) {
        if (nsfw) {
            switch (contentType) {
                case ALBUM:
                    return R.string.type_nsfw_album;
                case REDDIT_GALLERY:
                    return R.string.type_nsfw_gallery;
                case EMBEDDED:
                    return R.string.type_nsfw_emb;
                case EXTERNAL:
                case LINK:
                    return R.string.type_nsfw_link;
                case GIF:
                    return R.string.type_nsfw_gif;
                case IMAGE:
                    return R.string.type_nsfw_img;
                case TUMBLR:
                    return R.string.type_nsfw_tumblr;
                case IMGUR:
                    return R.string.type_nsfw_imgur;
                case VIDEO:
                case VREDDIT_DIRECT:
                case VREDDIT_REDIRECT:
                    return R.string.type_nsfw_video;
            }
        } else {
            switch (contentType) {
                case ALBUM:
                    return R.string.type_album;
                case REDDIT_GALLERY:
                    return R.string.type_gallery;
                case XKCD:
                    return R.string.type_xkcd;
                case DEVIANTART:
                    return R.string.type_deviantart;
                case EMBEDDED:
                    return R.string.type_emb;
                case EXTERNAL:
                    return R.string.type_external;
                case GIF:
                    return R.string.type_gif;
                case IMAGE:
                    return R.string.type_img;
                case IMGUR:
                    return R.string.type_imgur;
                case LINK:
                    return R.string.type_link;
                case TUMBLR:
                    return R.string.type_tumblr;
                case NONE:
                    return R.string.type_title_only;
                case REDDIT:
                    return R.string.type_reddit;
                case SELF:
                    return R.string.type_selftext;
                case STREAMABLE:
                    return R.string.type_streamable;
                case VIDEO:
                    return R.string.type_youtube;
                case VREDDIT_REDIRECT:
                case VREDDIT_DIRECT:
                    return R.string.type_vreddit;

            }
        }
        return R.string.type_link;
    }

    static HashMap<String, String> contentDescriptions = new HashMap<>();

    /**
     * Returns a description of the submission, for example "Link", "NSFW link", if the link is set
     * to open externally it returns the package name of the app that opens it, or "External"
     *
     * @param submission The submission to describe
     * @param context    Current context
     * @return The content description
     */
    public static String getContentDescription(Submission submission, Context context) {
        final int generic = getContentID(submission);
        final Resources res = context.getResources();
        final String domain = submission.getDomain();

        if (generic != R.string.type_external) {
            return res.getString(generic);
        }

        if (contentDescriptions.containsKey(domain)) {
            return contentDescriptions.get(domain);
        }

        try {
            final PackageManager pm = context.getPackageManager();
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(submission.getUrl()));
            final String packageName = pm.resolveActivity(intent, 0).activityInfo.packageName;
            String description;

            if (!packageName.equals("android")) {
                description = pm.getApplicationLabel(
                        pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA))
                        .toString();
            } else {
                description = res.getString(generic);
            }

            // Looking up a package name takes a long time (3~10ms), memoize it
            contentDescriptions.put(domain, description);
            return description;
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            contentDescriptions.put(domain, res.getString(generic));
            return res.getString(generic);
        }
    }
}
