package ru.tinelix.muchatter.bridges;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CompletableFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.util.concurrent.TimeUnit;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import ru.tinelix.muchatter.bridges.IRCPacket;
import ru.tinelix.muchatter.core.interfaces.LogColorFormatter;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.core.MuChatter.ChatterConfig;
import ru.tinelix.muchatter.db.SQLProcessor;
import ru.tinelix.muchatter.utils.Handler;
import ru.tinelix.muchatter.utils.Message;

public class IRCBridge implements LogColorFormatter {

    public static final String RESET_COLOR 		= "\u001B[0m";
    public static final String SUCCESS_COLOR 	= "\u001B[32m"; // Green
    public static final String WARNING_COLOR 	= "\u001B[33m"; // Yellow
    public static final String ERROR_COLOR 		= "\u001B[31m"; // Red
    public static final String INFO_COLOR      	= "\u001B[36m"; // Cyan

    private ArrayList<Socket>       mClients;
    private ChatterConfig           mConfig;
    private IRCBridgeThread[]       mThreads;
    private MuChatter               mChatter;
    private boolean                 mConnected;
    private Handler                 mHandler;

    public static class IRCServer {
        public String name;
        public String emoji;
        public String address;
        public int port;
        public String encoding;
        public String ns_passwd;
        public String rules_link;
        public boolean use_rules_cmd;
    }

    public IRCBridge(ChatterConfig config, MuChatter chatter) {
        mConfig     = config;
        mChatter    = chatter;
        mThreads    = new IRCBridgeThread[mConfig.limits.max_bridge_threads];
        mHandler    = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            handleBridgeOutput((HashMap<String, Object>) msg.obj);
                        }
                    };
    }

    @Override
    public boolean onSuccess(String message) {
        System.out.println(
        	IRCBridge.SUCCESS_COLOR + "[SUCC] " + IRCBridge.RESET_COLOR + "IRCBridge: " + message
        );
        return true;
    }

    @Override
    public boolean onPadding(String message) {
        System.out.println(
        	IRCBridge.RESET_COLOR + "       " + "IRCBridge: " + message
        );
        return true;
    }

	@Override
    public boolean onInfo(String message) {
        System.out.println(
        	IRCBridge.INFO_COLOR + "[INFO] " + IRCBridge.RESET_COLOR + "IRCBridge: " + message
        );
        return true;
    }

    @Override
    public boolean onWarning(String message) {
    	System.out.println(
			IRCBridge.WARNING_COLOR + "[WARN] " + IRCBridge.RESET_COLOR + "IRCBridge: " + message
		);
		return true;
    }

    @Override
    public boolean onError(String message) {
        System.out.println(
        	IRCBridge.ERROR_COLOR + "[ERR ] " + IRCBridge.RESET_COLOR + "IRCBridge: " + message
        );
        return true;
    }

    public void start() {
         try {
            if(mConfig.irc_nickname != null) {
                int serversSize = mConfig.irc_servers.size();

                int i = 0;

                int maxServers = serversSize < mConfig.limits.max_bridge_threads ?
                                    serversSize : mConfig.limits.max_bridge_threads;

                for(i = 0; i < maxServers; i++) {
                    mThreads[i] = new IRCBridgeThread(mConfig, mConfig.irc_servers.get(i), mHandler);
                    mThreads[i].start();
                }
            }
        } catch (Exception e) {
            onError("IRCBridge: " + e.getMessage());
        }
    }

    private void handleBridgeOutput(HashMap<String, Object> map) {
        try {
            if(map.containsKey("packet") && map.containsKey("server")) {
                if(map.get("packet") instanceof IRCPacket && map.get("server") instanceof IRCServer) {
                    IRCPacket packet = (IRCPacket) map.get("packet");
                    IRCServer server = (IRCServer) map.get("server");

                    SendMessage message = null;

                    switch(packet.getMessageCode()) {
                        case "PRIVMSG":
                            String fromNick = packet.getFrom().split("@")[0];
                            message = new SendMessage(
                                "1833116067",
                                String.format("<I>%s, %s - %s:</I>\r\n%s", fromNick, packet.getChannel(), server.address)
                            );
                            break;
                    }

                    if(message != null) {
                        message.setParseMode("HTML");
                        message.disableWebPagePreview();
                        mChatter.getTelegramClient().execute(message);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public class IRCBridgeThread extends Thread {
        private ChatterConfig                   mConfig;
        private IRCServer                       mServer;
        private Socket                          mClient;
        private BufferedReader                  mIn;
        private BufferedWriter                  mOut;
        private boolean                         mIdentified;
        private boolean                         mConnected;
        private final BlockingQueue<String>     mMsgQueue;
        private byte[]                          mInputBuffer;
        private int                             mNickChangeAttempts;
        private Handler                         mHandler;

        public IRCBridgeThread(ChatterConfig config, IRCServer server, Handler handler) {
            mConfig             = config;
            mServer             = server;
            mMsgQueue           = new LinkedBlockingQueue<>();
            mInputBuffer        = new byte[4096];
            mNickChangeAttempts = 1;
            mHandler            = handler;
        }

        public void sendMessage(String message) {
            try {
                mMsgQueue.put(message);
            } catch(Exception ex) {}
        }

        public void connect() {
            try {
                mClient = new Socket(mServer.address, mServer.port);

                onInfo(
                    String.format("Connecting to %s:%s...", mServer.address, mServer.port)
                );

                mIn = new BufferedReader(
                    new InputStreamReader(mClient.getInputStream())
                );

                mOut = new BufferedWriter(
                    new OutputStreamWriter(mClient.getOutputStream())
                );

                mConnected = true;

                identifyMe();
            } catch (Exception e) {
                IRCBridge.this.onError(
                    String.format("IRCBridge: [%s:%d] ", mServer.address, mServer.port) + e.getMessage()
                );
            }
        }

        public void run() {
            try {
                connect();
                listen();
            } catch (Exception e) {
                IRCBridge.this.onError(
                    String.format("IRCBridge: [%s:%d] ", mServer.address, mServer.port) + e.getMessage()
                );
            }
        }

        private void identifyMe() {
            if(mNickChangeAttempts > 6)
                return;

            try {
                if(mNickChangeAttempts < 2) {
                    mOut.write(
                        new IRCPacket("USER",
                            String.format(
                                "%s %s %s :%s TG<->IRC Bridge - https://t.me/%s\r\n",
                                mConfig.irc_nickname,
                                mConfig.irc_nickname,
                                mConfig.irc_nickname,
                                mConfig.bot_name,
                                mConfig.bot_username
                            ), mServer.encoding
                        ).getStringData()
                    );
                    mOut.flush();
                }

                mOut.write(
                    new IRCPacket("NICK", mNickChangeAttempts < 2 ?
                        String.format("%s\r\n", mConfig.irc_nickname) :
                        String.format("%s%d\r\n", mConfig.irc_nickname, mNickChangeAttempts),
                        mServer.encoding
                    ).getStringData()
                );
                mOut.flush();

                onInfo(
                    String.format("[%s:%s] The bot has been identified", mServer.address, mServer.port)
                );
            } catch (Exception e) {
                IRCBridge.this.onError(e.getMessage());
            }
        }

        public boolean listen() {
            try {
                int bufBytesRead;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                synchronized(mIn) {
                    String msg;

                    while((bufBytesRead = mClient.getInputStream().read(mInputBuffer)) != -1) {
                        stream.write(mInputBuffer, 0, bufBytesRead);
                        IRCPacket packet = parseIRCPacket(stream);

                        if(packet.getCommand().equals("PING")) {
                            mOut.write(
                                new IRCPacket(
                                    "PONG", packet.getText(), mServer.encoding
                                ).getStringData()
                            );
                            mOut.flush();
                        } else if(packet.getMessageCode().equals("001")) {
                            IRCBridge.this.onSuccess(
                                String.format(
                                    "[%s:%d] Connected",
                                    mServer.address, mServer.port
                                )
                            );
                        } else if(packet.getMessageCode().equals("PRIVMSG")) {
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("packet", packet);
                            map.put("server", mServer);
                            mHandler.obtainMessage(0, map).sendToTarget();
                        }

                        mConnected = true;
                    }

                    IRCBridge.this.onError(
                        String.format(
                            "IRCBridge: [%s:%d] Connection closed",
                            mServer.address, mServer.port
                        )
                    );
                    mConnected = false;
                }
                return mConnected;
            } catch (Exception e) {
                e.printStackTrace();
                IRCBridge.this.onError(e.getMessage());
            }

            return false;
        }

        private IRCPacket parseIRCPacket(ByteArrayOutputStream stream)  {

            String dataStr;

            try {
                dataStr = stream.toString(mServer.encoding);

                String[] lines = dataStr.split("\r?\n");
                boolean incompleteLine = false;

                byte[] data = stream.toByteArray();

                stream.reset();

                if (!dataStr.endsWith("\n") && lines.length > 0) {
                    stream.write(lines[lines.length - 1].getBytes(mServer.encoding));
                    incompleteLine = true;
                }

                return new IRCPacket(data, mServer.encoding);
            } catch(Exception e) {
                dataStr = "";
            }

            return null;
        }

        private int parseCurrentUserNumbers(String[] msgArray) {
            int users = 0;

            for(int i = 3; i < msgArray.length; i++) {
                try {
                    users = Integer.parseInt(msgArray[i]);
                    break;
                } catch(Exception e) {

                }
            }

            return users;
        }

        public void joinChannel(String channelName) {
            try {
                mOut.write(String.format(
                                "JOIN #%s\r\n",
                                channelName
                            ));
                mOut.flush();
            } catch (Exception e) {
                e.printStackTrace();
                IRCBridge.this.onError(e.getMessage());
            }
        }
    }
}
