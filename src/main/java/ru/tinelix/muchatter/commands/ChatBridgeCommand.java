package ru.tinelix.muchatter.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.ResultSet;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.InlineKeyboardMarkupBuilder;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.bridges.IRCBridge;
import ru.tinelix.muchatter.bridges.IRCBridge.IRCServer;
import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLProcessor;
import ru.tinelix.muchatter.core.Locale;
import ru.tinelix.muchatter.utils.DoubleArrayList;

public class ChatBridgeCommand extends BotCommand {

    public static final String COMMAND_NAME = "ChatBridge";

    public static final String RUN_CHAT_BRIDGE_WIZARD_CALLBACK              = "runChatBridgeWizard";
    public static final String RUN_IRC_BRIDGE_WIZARD_CALLBACK               = "runIRCBridgeWizard";
    public static final String CONTINUE_IRC_BRIDGE_WIZARD_CALLBACK          = "continueIRCBridgeWizard";
    public static final String SET_GROUPCHAT_IRC_BRIDGE_NETWORK_CALLBACK    = "setGroupChatBridgeNetwork";
    public static final String SET_GROUPCHAT_IRC_BRIDGE_CHANNEL_CALLBACK    = "setGroupChatBridgeChannel";
    public static final String AGREE_WITH_IRC_NETWORK_TOS_CALLBACK          = "agreeWithIRCNetworkToS";

    public ChatBridgeCommand(MuChatter chatter, DatabaseEngine dbEngine) {
        super(chatter, dbEngine);
        int stageCount = 0;
    }

    public void run() {
        try {
            DoubleArrayList<InlineKeyboardButton> buttons = new DoubleArrayList<>();

            String forms = "";

            SendMessage message = null;
            InlineKeyboardMarkup markupInline = null;

            String msgText = "";

            int formsLength = 0;

            if(mIsGroupChat) {
                message = new SendMessage(
                    Long.toString(mTgChat.getId()),
                    Locale.translate(mUiLanguage, "irc_bridge_desc")
                );

                buttons.addToInnerArray(
                    0, 0,
                    createInlineButton(
                        Locale.translate(mUiLanguage, "bridge_buttons", 0),
                        ChatBridgeCommand.CONTINUE_IRC_BRIDGE_WIZARD_CALLBACK
                    )
                );

                if(markupInline == null) {
                    InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

                    for(int i = 0; i < buttons.size(); i++) {
                        builder.keyboardRow(new InlineKeyboardRow(buttons.get(i)));
                    }

                    markupInline = builder.build();

                    message.setReplyMarkup(markupInline);
                }

                message.setParseMode("HTML");

                message.disableWebPagePreview();

                mChatter.getTelegramClient().execute(message);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void runFromCallback(String msgText) {

    }

    public void update(long msgId) {
        try {
            DoubleArrayList<InlineKeyboardButton> buttons = new DoubleArrayList<>();
            int formsLength = Locale.getLocaleArrayLength(mUiLanguage, "profile_forms");
            String forms = "";

            EditMessageText message = null;

            String msgText = "";

            InlineKeyboardMarkup markupInline = null;

            if(mIsGroupChat) {

                if(mMsgText.startsWith(ChatBridgeCommand.CONTINUE_IRC_BRIDGE_WIZARD_CALLBACK)) {
                    boolean hasNumberValue = mMsgText.split("_").length >= 3 && parseInt(mMsgText.split("_")[2]) >= 0;

                    String stage = mMsgText.split("_").length >= 2 ? mMsgText.split("_")[1] : "";

                    boolean hasStage = mMsgText.split("_").length >= 2 &&
                                            (mMsgText.split("_")[1].equals("server") ||
                                             mMsgText.split("_")[1].equals("rules") ||
                                             mMsgText.split("_")[1].equals("rulesw") ||
                                             mMsgText.split("_")[1].equals("success")
                                            );

                    int numberValue = 0;
                    int stageNumber = 0;
                    IRCServer server = null;

                    String servers = "";
                    String nextStage = "";

                    if(hasStage && hasNumberValue) {
                        numberValue = parseInt(mMsgText.split("_")[2]);
                        server = mChatter.mConfig.irc_servers.get(numberValue - 1);

                        switch(stage) {
                            case "server":
                                stageNumber = 0;
                                nextStage = "rules";
                                break;
                            case "rules":
                                stageNumber = 1;
                                nextStage = "channel";
                                break;
                            case "rulesw":
                                stageNumber = 2;
                                nextStage = "channel";
                                break;
                            case "channel":
                                stageNumber = 3;
                                nextStage = "success";
                                break;
                            case "success":
                                stageNumber = 4;
                                break;
                        }

                        if(server.use_rules_cmd) {
                            msgText = String.format(
                                Locale.translate(mUiLanguage, "please_wait")
                            );
                            mChatter.getIRCBridge().sendCommand("/rules", msgId, numberValue - 1);
                        } else {
                            stageNumber = 2;
                            msgText = String.format(
                                Locale.translate(mUiLanguage, "irc_bridge_stages", stageNumber),
                                server.rules_link
                            );
                        }
                    } else {
                        for(int i = 0; i < mChatter.mConfig.irc_servers.size(); i++) {
                            server = mChatter.mConfig.irc_servers.get(i);
                            servers += String.format("%d. <B>%s %s</B>\r\n<CODE>%s:%d</CODE>\r\n\r\n",
                                i + 1,
                                server.emoji, server.name, server.address, server.port
                            );
                        }

                        msgText = String.format(
                                Locale.translate(mUiLanguage, "irc_bridge_stages", 0),
                                mChatter.getIRCBridge().getActiveServersCount(),
                                mChatter.mConfig.irc_servers.size(),
                                servers
                            );

                        markupInline = createT9Layout(
                            ChatBridgeCommand.CONTINUE_IRC_BRIDGE_WIZARD_CALLBACK + "_rules",
                            1, mChatter.getIRCBridge().getActiveServersCount(), false
                        );
                    }

                    message = EditMessageText.builder()
                        .chatId(mTgChat.getId())
                        .messageId((int)msgId)
                        .text(msgText)
                        .build();

                    if(markupInline == null) {
                        InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();

                        for(int i = 0; i < buttons.size(); i++) {
                            builder.keyboardRow(new InlineKeyboardRow(buttons.get(i)));
                        }

                        markupInline = builder.build();
                    }

                    message.setReplyMarkup(markupInline);

                    message.setParseMode("HTML");
                    message.disableWebPagePreview();
                    mChatter.getTelegramClient().execute(message);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private int parseInt(String numberStr) {
        int numberValue = 0;

        try {
            numberValue = Integer.parseInt(numberStr);
        } catch(Exception ex) {
            numberValue = -1;
        }
        return numberValue;
    }
}
