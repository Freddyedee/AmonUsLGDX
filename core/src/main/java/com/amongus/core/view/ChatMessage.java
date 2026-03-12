package com.amongus.core.view;

import com.amongus.core.api.player.PlayerId;

public class ChatMessage {
    private final PlayerId senderId;
    private final String senderName;
    private final String text;

    public ChatMessage(PlayerId senderId, String senderName, String text) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
    }

    public PlayerId getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getText() { return text; }
}
