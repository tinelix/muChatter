package ru.tinelix.muchatter.utils;

import ru.tinelix.muchatter.utils.Handler;

public class Message extends Object {

    public int what;
    public Object obj;
    Handler target;

    public Message(int what, Object obj) {
        this.what = what;
        this.obj = obj;
    }

    public Message(int what, Object obj, Handler target) {
        this.what = what;
        this.obj = obj;
        this.target = target;
    }

    public Message(int what, Handler target) {
        this.what = what;
        this.target = target;
    }

    public Message(int what) {
        this.what = what;
    }

    public void sendToTarget() {
        this.target.handleMessage(this);
    }
}
