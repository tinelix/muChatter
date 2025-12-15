package ru.tinelix.muchatter.bridges;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CompletableFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.util.concurrent.TimeUnit;

import ru.tinelix.muchatter.core.interfaces.LogColorFormatter;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.core.MuChatter.ChatterConfig;
import ru.tinelix.muchatter.db.SQLProcessor;

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

    public static class IRCServer {
        public String address;
        public int port;
        public String encoding;
        public String ns_passwd;
    }

    public IRCBridge(ChatterConfig config, MuChatter chatter) {
        mConfig     = config;
        mChatter    = chatter;
        mThreads    = new IRCBridgeThread[mConfig.limits.max_bridge_threads];
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
                    mThreads[i] = new IRCBridgeThread(mConfig, mConfig.irc_servers.get(i));
                    mThreads[i].start();
                }
            }
        } catch (Exception e) {
            onError("IRCBridge: " + e.getMessage());
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

        public IRCBridgeThread(ChatterConfig config, IRCServer server) {
            mConfig             = config;
            mServer             = server;
            mMsgQueue           = new LinkedBlockingQueue<>();
            mInputBuffer        = new byte[4096];
            mNickChangeAttempts = 1;
        }

        public void sendMessage(String message) {
            try {
                mMsgQueue.put(message);
            } catch(Exception ex) {}
        }

        public void handleMessage(String message) {
            try {
                switch(message) {
                    case "connect":
                        connect();
                        break;
                    case "listen":
                        onInfo(
                            String.format("Listening %s:%s...", mServer.address, mServer.port)
                        );
                        listen();
                        break;
                }

                if(message.startsWith("join #")) {
                    joinChannel("test");
                }
             } catch (Exception e) {

             }
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
                    mOut.write(String.format(
                                "USER %s %s %s :%s TG<->IRC Bridge - https://t.me/%s\r\n",
                                mConfig.irc_nickname,
                                mConfig.irc_nickname,
                                mConfig.irc_nickname,
                                mConfig.bot_name,
                                mConfig.bot_username
                            ));
                    mOut.flush();
                }

                mOut.write(
                    mNickChangeAttempts < 2 ?
                        String.format("NICK %s\r\n", mConfig.irc_nickname) :
                        String.format("NICK %s%d\r\n", mConfig.irc_nickname, mNickChangeAttempts)
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
                ByteArrayOutputStream packet = new ByteArrayOutputStream();

                synchronized(mIn) {
                    String msg;

                    while((bufBytesRead = mClient.getInputStream().read(mInputBuffer)) != -1) {
                        packet.write(mInputBuffer, 0, bufBytesRead);
                        parseIRCPacket(packet);
                        mConnected = true;

                        msg = mMsgQueue.poll(100, TimeUnit.MILLISECONDS);
                        if(msg != null)
                            handleMessage(msg);
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

        private void parseIRCPacket(ByteArrayOutputStream packet)  {

            String data;

            try {
                data = packet.toString(mServer.encoding);

                String[] lines = data.split("\r?\n");
                boolean incompleteLine = false;

                packet.reset();

                if (!data.endsWith("\n") && lines.length > 0) {
                    packet.write(lines[lines.length - 1].getBytes(mServer.encoding));
                    incompleteLine = true;
                }

                for(int i = 0; i < lines.length; i++) {

                    String[] msgArray = lines[i].split(" ");

                    if(i == lines.length - 1 && incompleteLine) {
                        break;
                    }

                    if(lines[i].startsWith("PING")) {
                        mOut.write(lines[i].replace("PING", "PONG") + "\r\n");
                        mOut.flush();
                    } else if(msgArray.length > 3) {
                        String msgCode = msgArray[1];
                        String msgTo   = msgArray[2];

                        int msgTextOffset = msgArray[0].length() + 1
                                            + msgArray[1].length() + 1
                                            + msgArray[2].length() + 1;

                        String msgText = msgArray[3].charAt(0) == ':' ?
                                            lines[i].substring(msgTextOffset) :
                                            lines[i].substring(msgTextOffset + 1);

                        switch(msgCode) {
                            case "433":
                                mNickChangeAttempts++;
                                IRCBridge.this.onWarning(
                                    String.format(
                                        "[%s:%d] Nickname %s is already taken, let's try change to %s.",
                                        mServer.address, mServer.port,
                                        mConfig.irc_nickname, mConfig.irc_nickname + mNickChangeAttempts
                                    )
                                );
                                identifyMe();
                                break;
                            case "001":
                                IRCBridge.this.onSuccess(
                                    String.format("[%s:%d] Connected [%s]!", mServer.address, mServer.port)
                                );
                                break;
                            case "396":
                                IRCBridge.this.onInfo(
                                    String.format(
                                        "[%s:%d] %s!%s@%s is now displayed IRC usermask",
                                        mServer.address, mServer.port, msgTo, msgTo, msgArray[3]
                                    )
                                );
                                break;
                            case "266":
                                IRCBridge.this.onInfo(
                                    String.format("[%s:%d] There are currently %s users on the %s server",
                                                mServer.address, mServer.port, parseCurrentUserNumbers(msgArray), mServer.address
                                    )
                                );
                                break;
                        }
                    } else
                        continue;
                }
            } catch(Exception e) {
                data = "";
            }
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
