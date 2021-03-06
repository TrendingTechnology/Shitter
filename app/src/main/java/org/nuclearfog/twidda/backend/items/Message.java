package org.nuclearfog.twidda.backend.items;

import androidx.annotation.NonNull;

import twitter4j.DirectMessage;
import twitter4j.URLEntity;

public class Message {

    private final long messageId;
    private final long time;
    private final User sender;
    private final User receiver;
    private final String message;

    /**
     * construct message object from twitter information
     *
     * @param dm       direct message information
     * @param sender   sender user
     * @param receiver receiver user
     */
    public Message(DirectMessage dm, twitter4j.User sender, twitter4j.User receiver) {
        this.sender = new User(sender);
        this.receiver = new User(receiver);
        messageId = dm.getId();
        time = dm.getCreatedAt().getTime();
        message = getText(dm);
    }

    /**
     * construct message object from database information
     *
     * @param messageId ID of direct message
     * @param sender    sender user
     * @param receiver  receiver user
     * @param time      timestamp long format
     * @param message   message text
     */
    public Message(long messageId, User sender, User receiver, long time, String message) {
        this.messageId = messageId;
        this.sender = sender;
        this.receiver = receiver;
        this.time = time;
        this.message = message;
    }

    /**
     * get message ID
     *
     * @return message ID
     */
    public long getId() {
        return messageId;
    }

    /**
     * get sender of DM
     *
     * @return user
     */
    public User getSender() {
        return sender;
    }

    /**
     * get receiver of DM
     *
     * @return user
     */
    public User getReceiver() {
        return receiver;
    }

    /**
     * get Message content
     *
     * @return message
     */
    public String getText() {
        return message;
    }

    /**
     * get time of DM
     *
     * @return raw time
     */
    public long getTime() {
        return time;
    }

    /**
     * Resolve shortened tweet links
     *
     * @param message Tweet
     * @return Tweet string with resolved URL entities
     */
    private String getText(DirectMessage message) {
        String text = message.getText();
        if (text != null && !text.isEmpty()) {
            URLEntity[] entities = message.getURLEntities();
            StringBuilder messageBuilder = new StringBuilder(message.getText());
            for (int i = entities.length - 1; i >= 0; i--) {
                URLEntity entity = entities[i];
                messageBuilder.replace(entity.getStart(), entity.getEnd(), entity.getExpandedURL());
            }
            return messageBuilder.toString();
        } else {
            return "";
        }
    }

    @NonNull
    @Override
    public String toString() {
        return sender.getScreenname() + " to " + receiver.getScreenname() + " " + message;
    }
}