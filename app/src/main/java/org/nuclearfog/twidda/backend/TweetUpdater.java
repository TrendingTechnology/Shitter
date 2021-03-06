package org.nuclearfog.twidda.backend;

import android.os.AsyncTask;

import org.nuclearfog.twidda.activity.TweetPopup;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.engine.TwitterEngine;
import org.nuclearfog.twidda.backend.holder.TweetHolder;

import java.lang.ref.WeakReference;

/**
 * Background task for uploading tweet
 *
 * @see TweetPopup
 */
public class TweetUpdater extends AsyncTask<TweetHolder, Void, Boolean> {


    private EngineException twException;
    private final WeakReference<TweetPopup> callback;
    private final TwitterEngine mTwitter;

    /**
     * initialize task
     *
     * @param callback Activity context
     */
    public TweetUpdater(TweetPopup callback) {
        super();
        this.callback = new WeakReference<>(callback);
        mTwitter = TwitterEngine.getInstance(callback);
    }


    @Override
    protected void onPreExecute() {
        if (callback.get() != null) {
            callback.get().setLoading(true);
        }
    }


    @Override
    protected Boolean doInBackground(TweetHolder[] param) {
        try {
            TweetHolder tweet = param[0];
            mTwitter.uploadStatus(tweet);
            return true;
        } catch (EngineException twException) {
            this.twException = twException;
            return false;
        }
    }


    @Override
    protected void onPostExecute(Boolean success) {
        if (callback.get() != null) {
            callback.get().setLoading(false);
            if (success) {
                callback.get().onSuccess();
            } else {
                callback.get().onError(twException);
            }
        }
    }
}