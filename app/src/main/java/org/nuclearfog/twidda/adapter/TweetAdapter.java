package org.nuclearfog.twidda.adapter;

import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.squareup.picasso.Picasso;

import org.nuclearfog.tag.Tagger;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.helper.FontTool;
import org.nuclearfog.twidda.backend.items.Tweet;
import org.nuclearfog.twidda.backend.items.TwitterUser;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class TweetAdapter extends Adapter<TweetAdapter.ItemHolder> {

    private WeakReference<TweetClickListener> itemClickListener;
    private NumberFormat formatter;
    private List<Tweet> tweets;
    private GlobalSettings settings;

    public TweetAdapter(TweetClickListener l, GlobalSettings settings) {
        itemClickListener = new WeakReference<>(l);
        formatter = NumberFormat.getIntegerInstance();
        tweets = new ArrayList<>();
        this.settings = settings;
    }

    @MainThread
    public void add(@NonNull List<Tweet> newTweets) {
        tweets.clear();
        tweets.addAll(newTweets);
        notifyDataSetChanged();
    }

    @MainThread
    public void addFirst(@NonNull List<Tweet> newTweets) {
        if (!newTweets.isEmpty()) {
            tweets.addAll(0, newTweets);
            notifyItemRangeInserted(0, newTweets.size());
        }
    }

    @MainThread
    public void clear() {
        tweets.clear();
        notifyDataSetChanged();
    }

    @MainThread
    public void remove(long id) {
        int index = -1;
        for (int pos = 0; pos < tweets.size() && index < 0; pos++) {
            if (tweets.get(pos).getId() == id) {
                tweets.remove(pos);
                index = pos;
            }
        }
        if (index != -1)
            notifyItemRemoved(index);
    }

    public boolean isEmpty() {
        return tweets.isEmpty();
    }

    @Override
    public long getItemId(int index) {
        return tweets.get(index).getId();
    }

    @Override
    public int getItemCount() {
        return tweets.size();
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tweet, parent, false);
        final ItemHolder vh = new ItemHolder(v);
        FontTool.setViewFontAndColor(settings, v);

        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener.get() != null) {
                    int position = vh.getLayoutPosition();
                    if (position != NO_POSITION)
                        itemClickListener.get().onTweetClick(tweets.get(position));
                }
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ItemHolder vh, int index) {
        Tweet tweet = tweets.get(index);
        TwitterUser user = tweet.getUser();

        if (tweet.getEmbeddedTweet() != null) {
            String retweeter = "RT " + user.getScreenname();
            vh.retweeter.setText(retweeter);
            tweet = tweet.getEmbeddedTweet();
            user = tweet.getUser();
        } else {
            vh.retweeter.setText("");
        }
        Spanned text = Tagger.makeTextWithLinks(tweet.getTweet(), settings.getHighlightColor());
        vh.tweet.setText(text);
        vh.username.setText(user.getUsername());
        vh.screenname.setText(user.getScreenname());
        vh.retweet.setText(formatter.format(tweet.getRetweetCount()));
        vh.favorite.setText(formatter.format(tweet.getFavorCount()));
        vh.time.setText(getTimeString(tweet.getTime()));
        if (tweet.retweeted())
            vh.retweet.setCompoundDrawablesWithIntrinsicBounds(R.drawable.retweet_enabled, 0, 0, 0);
        else
            vh.retweet.setCompoundDrawablesWithIntrinsicBounds(R.drawable.retweet, 0, 0, 0);
        if (tweet.favored())
            vh.favorite.setCompoundDrawablesWithIntrinsicBounds(R.drawable.favorite_enabled, 0, 0, 0);
        else
            vh.favorite.setCompoundDrawablesWithIntrinsicBounds(R.drawable.favorite, 0, 0, 0);
        if (user.isVerified())
            vh.username.setCompoundDrawablesWithIntrinsicBounds(R.drawable.verify, 0, 0, 0);
        else
            vh.username.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        if (user.isLocked())
            vh.screenname.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, 0, 0);
        else
            vh.screenname.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        if (settings.getImageLoad()) {
            String pbLink = user.getImageLink();
            if (!user.hasDefaultProfileImage())
                pbLink += "_mini";
            Picasso.get().load(pbLink).into(vh.profile);
        }
        else
            vh.profile.setImageResource(0);
    }

    private String getTimeString(long time) {
        long diff = new Date().getTime() - time;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        if (weeks > 4) {
            Date tweetDate = new Date(time);
            return SimpleDateFormat.getDateInstance().format(tweetDate);
        }
        if (weeks > 0)
            return weeks + " w";
        if (days > 0)
            return days + " d";
        if (hours > 0)
            return hours + " h";
        if (minutes > 0)
            return minutes + " m";
        return seconds + " s";
    }

    static class ItemHolder extends ViewHolder {
        final TextView username, screenname, tweet, retweet;
        final TextView favorite, retweeter, time;
        final ImageView profile;

        ItemHolder(View v) {
            super(v);
            username = v.findViewById(R.id.username);
            screenname = v.findViewById(R.id.screenname);
            tweet = v.findViewById(R.id.tweettext);
            retweet = v.findViewById(R.id.retweet_number);
            favorite = v.findViewById(R.id.favorite_number);
            retweeter = v.findViewById(R.id.retweeter);
            time = v.findViewById(R.id.time);
            profile = v.findViewById(R.id.tweetPb);
        }
    }

    /**
     * Listener for tweet click
     */
    public interface TweetClickListener {

        /**
         * handle click action
         *
         * @param tweet clicked tweet
         */
        void onTweetClick(Tweet tweet);
    }
}