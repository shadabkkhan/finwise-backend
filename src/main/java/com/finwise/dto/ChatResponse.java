package com.finwise.dto;

public class ChatResponse {

    private String reply;

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public ChatResponse(String reply) {
        this.reply = reply;
    }

}
