package org.nuclearfog.twidda.backend.items;

import twitter4j.DirectMessage;
import twitter4j.User;

public class Message {

    private final long messageId;
    private final long time;

    private final TwitterUser sender;
    private final TwitterUser receiver;

    private final String message;


    public Message(DirectMessage dm, User sender, User receiver) {
        this.sender = new TwitterUser(sender);
        this.receiver = new TwitterUser(receiver);
        messageId = dm.getId();
        time = dm.getCreatedAt().getTime();
        message = dm.getText();
    }


    public Message(long messageId, TwitterUser sender, TwitterUser receiver, long time, String message) {
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
    public TwitterUser getSender() {
        return sender;
    }

    /**
     * get receiver of DM
     *
     * @return user
     */
    public TwitterUser getReceiver() {
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

}