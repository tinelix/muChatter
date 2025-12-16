package ru.tinelix.muchatter.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.sql.ResultSet;

import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLProcessor;
import ru.tinelix.muchatter.core.Locale;
import ru.tinelix.muchatter.utils.DoubleArrayList;

public class ChatBridgeCommand extends BotCommand {

    public static final String RUN_IRC_BRIDGE_WIZARD                        = "runIRCBridgeWizard";
    public static final String SET_GROUPCHAT_IRC_BRIDGE_NETWORK_CALLBACK    = "setGroupChatBridgeNetwork";
    public static final String SET_GROUPCHAT_IRC_BRIDGE_CHANNEL_CALLBACK    = "setGroupChatBridgeChannel";
    public static final String AGREE_WITH_IRC_NETWORK_TOS_CALLBACK          = "agreeWithIRCNetworkToS";

    public ChatBridgeCommand(MuChatter chatter, DatabaseEngine dbEngine) {
        super(chatter, dbEngine);
    }

    public void run() {
        try {
            DoubleArrayList<InlineKeyboardButton> buttons = new DoubleArrayList<>();

            String forms = "";

            SendMessage message = null;

            String msgText = "";

            int formsLength = 0;

            if(mIsGroupChat) {

            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void runFromCallback(String msgText) {

    }

    public void update(long msgId) {

    }
}
