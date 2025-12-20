package ru.tinelix.muchatter.bridges;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Exchanger;
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

    private ChatterConfig                                   mConfig;
    private IRCBridgeThread[]                               mThreads;
    private MuChatter                                       mChatter;
    private boolean                                         mConnected;
    private final BlockingQueue<HashMap<String, Object>>    mMsgQueue;
    private Handler                                         mHandler;

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
                            HashMap<String, Object> map = (HashMap<String, Object>) msg.obj;
                            super.handleMessage(msg);
                            handleBridgeOutput(map);
                        }
                    };
        mMsgQueue = new LinkedBlockingQueue<>();
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
                    mThreads[i] = new IRCBridgeThread(i, mConfig, mConfig.irc_servers.get(i),
                                                      mHandler, mMsgQueue);
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

                if(map.containsKey("msgId")) {
                    onInfo(String.format("MSGID: %d", (long)map.get("msgId")));
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(String command, long msgId, int index) {
        try {
            if(mThreads[index] != null && mThreads[index].isAlive()) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("command", command);
                map.put("msgId", msgId);
                map.put("to", mConfig.irc_servers.get(index).address);
                mMsgQueue.put(map);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public int getActiveServersCount() {
        int count = 0;

        for(int i = 0; i < mThreads.length; i++) {
            if(mThreads[i] != null && mThreads[i].isAlive())
                count++;
        }

        return count;
    }

    public class IRCBridgeThread extends Thread {
        private ChatterConfig                                       mConfig;
        private IRCServer                                           mServer;
        private SocketChannel                                       mClientChannel;
        private ByteBuffer                                          mIn;
        private ByteBuffer                                          mOut;
        private boolean                                             mIdentified;
        private boolean                                             mConnected;
        private final BlockingQueue<HashMap<String, Object>>        mMsgQueue;
        private int                                                 mNickChangeAttempts;
        private Handler                                             mParentHandler;
        private int                                                 mIndex;
        private Selector                                            mSelector;

        public IRCBridgeThread(int index, ChatterConfig config, IRCServer server, Handler handler,
                               BlockingQueue<HashMap<String, Object>> msgQueue) {
            mConfig             = config;
            mServer             = server;
            mMsgQueue           = msgQueue;
            mNickChangeAttempts = 1;
            mParentHandler      = handler;
            mIndex              = index;
            try {
                mIn = ByteBuffer.allocate(4096);
                mOut = ByteBuffer.allocate(4096);
                mSelector = Selector.open();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        private void handleCommand(HashMap<String, Object> map) {
            try {
                IRCBridge.this.onInfo("TEST 1!");
                String command = "";
                String to = "";
                long msgId = 0;

                if(map.containsKey("command")) {
                    command = (String) map.get("command");
                    msgId = (Long) map.get("msgId");
                    to = (String) map.get("to");

                    if(to.equals(mServer.address)) {
                        switch(command) {
                            case "/rules":
                                sendResponse("RULES\r\n".getBytes());
                                break;
                        }
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        public void connect() {
            try {
                mClientChannel = SocketChannel.open();
                mClientChannel.configureBlocking(false);
                onInfo(
                    String.format("Connecting to %s:%s...", mServer.address, mServer.port)
                );

                mClientChannel.connect(new InetSocketAddress(mServer.address, mServer.port));
                mConnected = true;

                mClientChannel.register(mSelector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

                listen();
            } catch (Exception e) {
                IRCBridge.this.onError(
                    String.format("IRCBridge: [%s:%d] ", mServer.address, mServer.port) + e.getMessage()
                );
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                mHandler = new Handler() {
                                    @Override
                                    public void handleMessage(Message msg) {
                                        super.handleMessage(msg);
                                        handleCommand((HashMap<String, Object>)msg.obj);
                                    }
                               };
                connect();
            } catch (Exception e) {
                IRCBridge.this.onError(
                    String.format("IRCBridge: [%s:%d] ", mServer.address, mServer.port) + e.getMessage()
                );
                e.printStackTrace();
            }
        }

        private void identifyMe() {

            if(mNickChangeAttempts > 6) {
                IRCBridge.this.onError("The limit for changing nicknames has been reached.");
                return;
            } else if(mIdentified) {
                return;
            }

            try {
                if(mNickChangeAttempts < 2) {
                    sendResponse(
                        new IRCPacket("USER",
                            String.format(
                                "%s %s %s :%s TG<->IRC Bridge - https://t.me/%s",
                                mConfig.irc_nickname,
                                mConfig.irc_nickname,
                                mConfig.irc_nickname,
                                mConfig.bot_name,
                                mConfig.bot_username
                            ), mServer.encoding
                        ).getRawData()
                    );
                }

                sendResponse(
                    new IRCPacket("NICK", mNickChangeAttempts < 2 ?
                        String.format("%s", mConfig.irc_nickname) :
                        String.format("%s%d", mConfig.irc_nickname, mNickChangeAttempts),
                        mServer.encoding
                    ).getRawData()
                );

                onInfo(
                    String.format("[%s:%s] The bot has been identified", mServer.address, mServer.port)
                );
                mIdentified = true;
            } catch (Exception e) {
                IRCBridge.this.onError(e.getMessage());
                e.printStackTrace();
            }
        }

        public boolean listen() {
            try {
                int bufBytesRead;

                HashMap<String, Object> map = new HashMap<>();

                while(true) {
                    int readyChannels = mSelector.select();

                    map = mMsgQueue.poll(50, TimeUnit.MILLISECONDS);

                    if(map != null)
                        handleCommand(map);

                    if (readyChannels == 0) continue;

                    Set<SelectionKey> selectedKeys = mSelector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    HashMap<String, Object> output_map = new HashMap<>();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    IRCPacket packet = null;

                    while(keyIterator.hasNext()) {
                        SocketChannel channel = null;
                        SelectionKey key = keyIterator.next();
                        ByteBuffer out = (ByteBuffer) key.attachment();

                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isConnectable()) {
                            channel = (SocketChannel) key.channel();
                            try {
                                mConnected = channel.finishConnect();
                                if(mConnected) {
                                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                    identifyMe();
                                }
                            } catch (Exception e) {

                            }
                        } else {
                            channel = mClientChannel;
                        }

                        if (key.isReadable()) {
                            int bytesRead = channel.read(mIn);

                            if(bytesRead > 0) {
                                mIn.flip();
                                baos.write(mIn.array());
                                packet = parseIRCPacket(baos);
                                IRCBridge.this.onInfo("\"" + new String(mIn.array(), mServer.encoding) + "\"");
                                mIn.clear();

                            }
                        }

                        if(key.isWritable() && packet != null) {
                            switch(packet.getCommand()) {
                                case "PING":
                                    sendResponse(
                                        new IRCPacket(
                                            "PONG", packet.getText(), mServer.encoding
                                        ).getRawData()
                                    );
                                    break;
                                case "001":
                                    IRCBridge.this.onSuccess(
                                        String.format(
                                            "[%s:%d] Connected",
                                            mServer.address, mServer.port
                                        )
                                    );

                                    output_map.put("index", mIndex);

                                    mParentHandler.obtainMessage(0, output_map).sendToTarget();
                                    break;
                                case "PRIVMSG":
                                case "232":
                                    output_map.put("packet", packet);
                                    output_map.put("server", mServer);
                                    output_map.put("index", mIndex);

                                    if(map != null) {
                                        if(map.containsKey("msgId")) {
                                            output_map.put("msgId", map.get("msgId"));
                                        }
                                    }
                                    mParentHandler.obtainMessage(0, output_map).sendToTarget();
                                    IRCBridge.this.onInfo("TEST 2");
                                    break;
                                case "309":
                                    if(map != null)
                                        map = null;

                                    output_map.put("packet", packet);
                                    output_map.put("server", mServer);
                                    break;
                                case "ERROR":
                                        IRCBridge.this.onError(
                                        String.format(
                                            "[%s:%d] %s",
                                            mServer.address, mServer.port, packet.getText()
                                        )
                                    );
                                    break;
                                }
                        }

                        keyIterator.remove();
                        mConnected = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                IRCBridge.this.onError(e.getMessage());
            }

            return true;
        }

        private void sendResponse(byte[] response) throws Exception {
            ByteBuffer buf = ByteBuffer.wrap(response);
            int written = mClientChannel.write(buf);
            buf.clear();
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
                mOut.wrap(new IRCPacket(
                                "JOIN",
                                "#" + channelName,
                                mServer.encoding
                            ).getRawData());
                while(mOut.hasRemaining()) {
                    mClientChannel.write(mOut);
                }
            } catch (Exception e) {
                e.printStackTrace();
                IRCBridge.this.onError(e.getMessage());
            }
        }

        public Handler getHandler() {
            return mHandler;
        }
    }
}
