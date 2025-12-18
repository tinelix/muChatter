package ru.tinelix.muchatter.bridges;

import java.util.ArrayList;

public class IRCPacket {
    private boolean                  incoming;
    private String                   from;
    private String                   channel;
    private String                   command;
    private String                   nickname;
    private String                   text;
    private String                   msgText;
    private String                   msgCode;
    private String                   encoding;
    private ArrayList<IRCNickname>   channelNicks;
    private byte[]                   data;

    public IRCPacket(String command, String text, String encoding) {
        incoming = false;
        this.command = command;
        this.text = text;
        this.encoding = encoding;

        try {
            if(command.equals("PONG"))
                data = String.format("%s :%s\r\n", command, text).getBytes(encoding);
            else
                data = String.format("%s %s\r\n", command, text).getBytes(encoding);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public IRCPacket(String command, String channel, String text, String encoding) {
        incoming = false;
        this.command = command;
        this.channel = channel;
        this.text = text;
        this.encoding = encoding;

        try {
            data = String.format("%s %s :%s\r\n", command, channel, text).getBytes(encoding);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public IRCPacket(String from, String msgCode, String nickname, String text, String encoding) {
        incoming = true;
        this.from = from;
        this.msgCode = msgCode;
        this.nickname = nickname;
        this.text = text;
        this.encoding = encoding;

        try {
            data = String.format(":%s %s %s :%s\r\n", from, msgCode, nickname, text).getBytes(encoding);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public IRCPacket(byte[] data, String encoding) {
        this.data = data;

        this.text = "";
        this.from = "";
        this.command = "";
        this.msgCode = "";
        this.nickname = "";
        this.text = "";
        this.encoding = encoding;

        parse();
    }

    public boolean parse() {
        try {
            String dataStr = new String(this.data, this.encoding);
            String[] words = dataStr.split(" ");
            channelNicks = new ArrayList<>();
            int startOfText = 0;

            incoming = dataStr.startsWith(":") || dataStr.startsWith("PING");

            if(incoming && words.length > 0) {

                for(int i = 0; i < words.length; i++) {
                    String plainWord = "";
                    if(words[i].startsWith(":") && !words[i].equals(":")) {
                        plainWord = words[i].substring(1);
                    } else {
                        plainWord = words[i];
                    }

                    switch(i) {
                        case 0:
                            if(words[0].equals("PING") || words[0].equals("ERROR")) {
                                this.command = words[i];
                            } else {
                                this.from = plainWord;
                            }
                            break;
                        case 1:
                            if(!this.command.equals("PING"))
                                this.msgCode = words[i];
                            else
                                this.text = plainWord;
                            break;
                        case 2:
                            if(this.msgCode.equals("JOIN") || msgCode.equals("PART") ||
                            this.msgCode.equals("PRIVMSG"))
                                this.channel = words[i];
                            else
                                this.nickname = words[i];
                            break;
                        case 3:
                            if(this.msgCode.equals("353")) {
                                continue;
                            } else if(this.msgCode.equals("332") || this.msgCode.equals("333") || this.msgCode.equals("366")) {
                                this.channel = words[i];
                            } else if(this.msgCode.equals("NOTICE")) {
                                if(i < words.length - 1)
                                    this.text += plainWord + " ";
                                else
                                    this.text += plainWord;
                            }
                            break;
                        case 4:
                            if(this.msgCode.equals("353")) {
                                this.channel = words[i];
                                continue;
                            }
                        default:
                            if(this.msgCode.equals("353")) {
                                channelNicks.add(new IRCNickname(plainWord));
                            } else {
                                if(i < words.length - 1)
                                    this.text += plainWord + " ";
                                else
                                    this.text += plainWord;
                            }
                            break;
                    }
                }
            }

            return true;
        } catch(Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public byte[] getRawData() {
        return this.data;
    }

    public String getStringData() {
        try {
            return new String(this.data, this.encoding);
        } catch(Exception e) {
            return null;
        }
    }


    public String getChannel() {
        return this.channel;
    }

    public String getCommand() {
        return this.command;
    }

    public String getFrom() {
        return this.from;
    }

    public String getMessageCode() {
        return this.msgCode;
    }

    public String getText() {
        return this.text;
    }

    public static class IRCNickname {
        private String name;
        private String usermask;
        private int status;

        public static final int STATUS_FOUNDER          = 6;
        public static final int STATUS_ADMINISTRATOR    = 5;
        public static final int STATUS_IRC_OPERATOR     = 4;
        public static final int STATUS_OPERATOR         = 3;
        public static final int STATUS_HALF_OPERATOR    = 2;
        public static final int STATUS_SPEAKER          = 1;
        public static final int STATUS_MEMBER           = 0;

        public IRCNickname(String name) {
            if(name.startsWith("~") && name.length() > 1) {
                this.name = name.substring(1);
                this.status = STATUS_FOUNDER;
            } else if(name.startsWith("@") && name.length() > 1) {
                this.name = name.substring(1);
                this.status = STATUS_OPERATOR;
            } else if(name.startsWith("+") && name.length() > 1) {
                this.name = name.substring(1);
                this.status = STATUS_SPEAKER;
            } else {
                this.name = name;
                this.status = STATUS_MEMBER;
            }
        }
    }

}
