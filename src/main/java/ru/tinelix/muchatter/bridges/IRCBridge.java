package ru.tinelix.muchatter.bridges;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;

import ru.tinelix.muchatter.core.interfaces.LogColorFormatter;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.core.MuChatter.ChatterConfig;
import ru.tinelix.muchatter.db.SQLProcessor;

public class IRCBridge implements LogColorFormatter, Runnable {

    public static final String RESET_COLOR 		= "\u001B[0m";
    public static final String SUCCESS_COLOR 	= "\u001B[32m"; // Green
    public static final String WARNING_COLOR 	= "\u001B[33m"; // Yellow
    public static final String ERROR_COLOR 		= "\u001B[31m"; // Red
    public static final String INFO_COLOR      	= "\u001B[36m"; // Cyan

    private Socket                  mClient;
    private BufferedReader          mIn;
    private BufferedWriter          mOut;
    private boolean                 mIdentified;
    private boolean                 mConnected;
    private ChatterConfig           mConfig;
    private MuChatter               mChatter;
    private BlockingQueue<String>   mMsgQueue;

    public IRCBridge(ChatterConfig config, MuChatter chatter) {
        mConfig = config;
        mChatter = chatter;
    }

    private void identifyMe() {
        try {
            mOut.write(String.format(
                        "USER %s %s %s :%s\r\n",
                        mConfig.irc_nickname,
                        mConfig.irc_nickname,
                        mConfig.irc_nickname,
                        mConfig.bot_name
                    ));
            mOut.flush();

            mOut.write(String.format(
                        "NICK %s\r\n",
                        mConfig.irc_nickname
                    ));
            mOut.flush();
        } catch (Exception e) {
            onError(e.getMessage());
        }
    }

    public CompletableFuture<Boolean> listen() {
        try {
            String bufLine;

            while ((bufLine = mIn.readLine()) != null) {
                if (bufLine.startsWith("PING")) {
                    mOut.write(bufLine.replace("PING", "PONG") + "\r\n");
                    mOut.flush();
                } else {
                    parseBufferLine(bufLine);
                }
            }
            return completedFuture(true);
        } catch (Exception e) {
            e.printStackTrace();
            onError(e.getMessage());
        }

        return completedFuture(false);
    }

    private void parseBufferLine(String line) {
        String[] msgArray = line.split(" ");

        if(msgArray[0].equals(":" + mConfig.irc_server)) {
            if(msgArray.length > 3) {
                String msgCode = msgArray[1];
                String msgTo   = msgArray[2];

                int msgTextOffset = msgArray[0].length() + 1
                                  + msgArray[1].length() + 1
                                  + msgArray[2].length() + 1;

                String msgText = msgArray[3].charAt(0) == ':' ?
                                     line.substring(msgTextOffset) :
                                     line.substring(msgTextOffset + 1);

                switch(msgCode) {
                    case "001":
                        onSuccess("Connected!");
                        break;
                    case "396":
                        onInfo(
                            String.format(
                                "%s!%s@%s is now displayed IRC address",
                                msgTo, msgTo, msgArray[3]
                            )
                        );
                        break;
                    case "266":
                        onInfo(
                            String.format("There are currently %s users on the %s server",
                                          msgArray[3], mConfig.irc_server
                            )
                        );
                        break;
                }
            }
        }
    }

    public synchronized void joinChannel(String channelName) {
        try {
            mOut.write(String.format(
                            "JOIN #%s\r\n",
                            channelName
                        ));
            mOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
            onError(e.getMessage());
        }
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

    @Override
    public void run() {
         try {
            if(mConfig.irc_nickname != null) {
                onInfo(
                    String.format("Connecting to %s:%s...", mConfig.irc_server, mConfig.irc_port)
                );
                mClient = new Socket(mConfig.irc_server, mConfig.irc_port);

                mIn  = new BufferedReader(
                        new InputStreamReader(mClient.getInputStream())
                      );
                mOut = new BufferedWriter(
                        new OutputStreamWriter(mClient.getOutputStream())
                      );

                identifyMe();
                listen();
            }
        } catch (Exception e) {
            onError("IRCBridge: " + e.getMessage());
        }
    }
}
