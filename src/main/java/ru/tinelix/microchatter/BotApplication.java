package ru.tinelix.microbot;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.*;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.microbot.core.Microchatter;

public class BotApplication {
	public static void main(String[] args) {
		printLogo();
		Microchatter bot = new Microchatter();
		
		try {
			TelegramBotsLongPollingApplication app = 
				new TelegramBotsLongPollingApplication();
		
			app.registerBot(bot.getBotToken(), bot);
		
			System.out.println("Microchatter successfully started!\r\n");
		
			Thread.currentThread().join();
		} catch(Exception ex) {
			
		}
	}
	
	public static void printLogo() {
		System.out.printf(
			"\r\nMicrochatter for Telegram\r\n" +
			"Copyright (c) 2025 Dmitry Tretyakov (aka. Tinelix).\r\n" +
			"https://github.com/tinelix/microchatter\r\n\r\n"
		);

		System.out.printf(
			"Working directory: %s\r\n\r\n", 
			Paths.get(".").toAbsolutePath().normalize()
		);
	}
}
