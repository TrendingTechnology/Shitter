package org.nuclearfog.twidda.backend;

import android.os.AsyncTask;

import org.nuclearfog.twidda.activity.UserProfile;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.engine.TwitterEngine;
import org.nuclearfog.twidda.backend.items.Relation;
import org.nuclearfog.twidda.backend.items.User;
import org.nuclearfog.twidda.database.AppDatabase;

import java.lang.ref.WeakReference;

/**
 * This background task loads profile information about a twitter user and take actions
 *
 * @see UserProfile
 */
public class UserAction extends AsyncTask<UserAction.Action, User, Relation> {

    /**
     * actions to be taken
     */
    public enum Action {
        /**
         * Load profile information
         */
        PROFILE_lOAD,
        /**
         * load profile from database first
         */
        PROFILE_DB,
        /**
         * follow user
         */
        ACTION_FOLLOW,
        /**
         * un-follow user
         */
        ACTION_UNFOLLOW,
        /**
         * block user
         */
        ACTION_BLOCK,
        /**
         * un-block user
         */
        ACTION_UNBLOCK,
        /**
         * mute user
         */
        ACTION_MUTE,
        /**
         * un-mute user
         */
        ACTION_UNMUTE
    }

    private EngineException twException;
    private WeakReference<UserProfile> callback;
    private TwitterEngine mTwitter;
    private AppDatabase db;
    private long userId;
    private String screenName;

    /**
     * @param callback Callback to return the result
     * @param user     twitter user information
     */
    public UserAction(UserProfile callback, User user) {
        this(callback, user.getId(), user.getScreenname());
    }

    /**
     * @param callback   Callback to return the result
     * @param userId     ID of the twitter user
     * @param screenName username alternative to User ID
     */
    public UserAction(UserProfile callback, long userId, String screenName) {
        super();
        this.callback = new WeakReference<>(callback);
        mTwitter = TwitterEngine.getInstance(callback);
        db = new AppDatabase(callback);
        this.userId = userId;
        this.screenName = screenName;
    }


    @Override
    protected Relation doInBackground(Action[] action) {
        try {
            switch (action[0]) {
                case PROFILE_DB:
                    // load user information from database
                    User user;
                    if (userId > 0) {
                        user = db.getUser(userId);
                        if (user != null) {
                            publishProgress(user);
                        }
                    }

                case PROFILE_lOAD:
                    // load user information from twitter
                    user = mTwitter.getUser(userId, screenName);
                    publishProgress(user);
                    db.storeUser(user);
                    // load user relations from twitter
                    Relation relation = mTwitter.getConnection(userId, screenName);
                    if (!relation.isHome()) {
                        boolean muteUser = relation.isBlocked() || relation.isMuted();
                        db.muteUser(userId, muteUser);
                    }
                    return relation;

                case ACTION_FOLLOW:
                    user = mTwitter.followUser(userId);
                    publishProgress(user);
                    break;

                case ACTION_UNFOLLOW:
                    user = mTwitter.unfollowUser(userId);
                    publishProgress(user);
                    break;

                case ACTION_BLOCK:
                    user = mTwitter.blockUser(userId);
                    publishProgress(user);
                    db.muteUser(userId, true);
                    break;

                case ACTION_UNBLOCK:
                    user = mTwitter.unblockUser(userId);
                    publishProgress(user);
                    db.muteUser(userId, false);
                    break;

                case ACTION_MUTE:
                    user = mTwitter.muteUser(userId);
                    publishProgress(user);
                    db.muteUser(userId, true);
                    break;

                case ACTION_UNMUTE:
                    user = mTwitter.unmuteUser(userId);
                    publishProgress(user);
                    db.muteUser(userId, false);
                    break;
            }
            return mTwitter.getConnection(userId, screenName);
        } catch (EngineException twException) {
            this.twException = twException;
            return null;
        }
    }


    @Override
    protected void onProgressUpdate(User[] users) {
        if (callback.get() != null) {
            callback.get().setUser(users[0]);
        }
    }


    @Override
    protected void onPostExecute(Relation properties) {
        if (callback.get() != null) {
            if (properties != null) {
                callback.get().onAction(properties);
            } else {
                callback.get().onError(twException);
            }
        }
    }
}