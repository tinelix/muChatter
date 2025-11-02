package ru.tinelix.muchatter.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Thread;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.tinelix.muchatter.bridges.IRCBridge;
import ru.tinelix.muchatter.core.interfaces.LogColorFormatter;
import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.db.DatabaseEngine;


public class MuChatter implements LongPollingSingleThreadUpdateConsumer, LogColorFormatter {
		
	public static class ChatterConfig {
		public String 	tg_token;
		public String 	bot_username;
		public String 	bot_name;
		public long   	tg_bot_owner_id;
		public boolean	use_irc_bridge;
		public String 	irc_server;
		public int		irc_port;
		public String 	irc_encoding;
		public String 	irc_nickname;
		public String 	irc_ns_passwd;
		public String 	license;
	}	
		
	private static String VERSION = "0.0.0";
		
	public static final String RESET_COLOR 		= "\u001B[0m";
	public static final String SUCCESS_COLOR 	= "\u001B[32m"; // Green
	public static final String WARNING_COLOR 	= "\u001B[33m"; // Yellow
	public static final String ERROR_COLOR 		= "\u001B[31m"; // Red
	public static final String INFO_COLOR      	= "\u001B[36m"; // Cyan

	public ChatterConfig mConfig;

	private TelegramClient 				mClient;
	private IRCBridge 					mIRCBridge;
	private Thread						mIRCThread;
	private HashMap<Long, BotCallback>	mBotCallbackMap;

	private DatabaseEngine mDatabase;
		
	public MuChatter() {
			this.mConfig = new ChatterConfig();

			try {
				FileInputStream inputStream = new FileInputStream("config/bot.json");
				ObjectMapper mapper = new ObjectMapper();

				mConfig = mapper.readValue(
					inputStream, ChatterConfig.class
				);
				mClient = new OkHttpTelegramClient(getBotToken());

				mIRCBridge = new IRCBridge(mConfig, this);
				mIRCThread = new Thread(mIRCBridge);

				mIRCThread.setDaemon(true);
				mIRCThread.start();

				mDatabase = new DatabaseEngine();
				mDatabase.connect();

			} catch(java.io.IOException | java.lang.NullPointerException e) {
				onError("Please create 'config/bot.json' file and try again.\r\n");
				e.printStackTrace();
			}
	}
		
	public void onUpdateReceived(Update update) {
		
	}
	
	public String getBotToken() {
		return mConfig.tg_token;
	}
		
	public String getBotUsername() {
		return mConfig.bot_username;
	}

	public void makeTemporaryCallback(String name, Chat tgChat, User tgFrom) {
		if(mBotCallbackMap == null)
			mBotCallbackMap = new HashMap<>();

		mBotCallbackMap.put(tgChat.getId(), new BotCallback(name, tgChat, tgFrom));
	}

	public BotCallback getTemporaryCallback(Chat tgChat) {
		if(mBotCallbackMap != null && mBotCallbackMap.containsKey(tgChat.getId()))
			return mBotCallbackMap.get(tgChat.getId());
		else
			return null;
	}

	public void removeTemporaryCallback(String name, Chat tgChat, User tgFrom) {
		mBotCallbackMap.remove(tgChat.getId());
	}

	public TelegramClient getTelegramClient() {
		return mClient;
	}

	public String getIRCNickname() {
		return mConfig.use_irc_bridge ? null : mConfig.irc_nickname;
	}
		
	@Override
	public boolean onSuccess(String message) {
		System.out.println(
			SUCCESS_COLOR + "[SUCC] " + RESET_COLOR + message
		);
		return true;
	}
		
	@Override
	public boolean onPadding(String message) {
		System.out.println(
			RESET_COLOR + "       " + message
		);
		return true;
	}
		
	@Override
	public boolean onInfo(String message) {
		System.out.println(
			INFO_COLOR + "[INFO] " + RESET_COLOR + message
		);
		return true;
	}

	@Override
	public boolean onWarning(String message) {
		System.out.println(
			WARNING_COLOR + "[WARN] " + RESET_COLOR + message
		);
		return true;
	}

	@Override
	public boolean onError(String message) {
		System.out.println(
			ERROR_COLOR + "[ERR ] " + RESET_COLOR + message
		);
		return true;
	}

	@Override
	public void consume(Update update) {
		// We check if the update has a message and the message has text
		if (update.hasMessage() && update.getMessage().hasText()) {

			User 	tgFrom     = update.getMessage().getFrom();
			Chat 	tgChat     = update.getMessage().getChat();
			String  tgMsgText  = update.getMessage().getText();

			BotCallback callback = getTemporaryCallback(tgChat);

			if(callback != null) {
				BotCommand command = BotCommand.resolveByCallback(
					this, mDatabase, tgChat, tgFrom, callback.getName()
				);

				if(command != null)
					command.runFromCallback(tgMsgText);
			} else {
				BotCommand command = BotCommand.resolve(
					this, mDatabase, tgChat, tgFrom, tgMsgText
				);

				if(command != null)
					command.run();
			}
		} else if (update.hasCallbackQuery()) {
			User tgFrom   = update.getCallbackQuery().getFrom();
			Chat tgChat   = update.getCallbackQuery().getMessage().getChat();
			long msgId    = update.getCallbackQuery().getMessage().getMessageId();
			String cbData = update.getCallbackQuery().getData();

			BotCommand command = BotCommand.resolveByCallback(
				this, mDatabase, tgChat, tgFrom, cbData
			);

			if(command != null)
				command.update(msgId);
		}
	}

	public class BotCallback {
		private User mTgFrom;
		private Chat mTgChat;
		private String mName;

		public BotCallback(String name, Chat tgChat, User tgFrom) {
			this.mName = name;
			this.mTgFrom = tgFrom;
			this.mTgChat = tgChat;
		}

		protected String getName() {
			return this.mName;
		}

		protected User getFrom() {
			return this.mTgFrom;
		}
	}
}
