package ru.tinelix.muchatter.utils;

import ru.tinelix.muchatter.utils.Message;

import java.util.ArrayList;

public class Handler extends Object {

    ArrayList<Message> messages;

    public Handler(){
        messages = new ArrayList<>();
    }

    public final Message obtainMessage(int what, Object obj, Handler handler) {
        Message message = new Message(what, obj, this);
        messages.add(message);
        return message;
    }

    public final Message obtainMessage(int what, Object obj) {
        Message message = new Message(what, obj, this);
        messages.add(message);
        return message;
    }

    public final Message obtainMessage(int what) {
        Message message = new Message(what, this);
        messages.add(message);
        return message;
    }

    public void handleMessage(Message msg) {
        messages.remove(msg);
    }

    public final boolean hasMessages() {
        return !messages.isEmpty();
    }
}
