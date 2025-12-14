package ru.tinelix.muchatter.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ru.tinelix.muchatter.core.interfaces.LogColorFormatter;
import ru.tinelix.muchatter.core.Locale;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLCreator;

public class SQLProcessor implements LogColorFormatter {
	
	private DatabaseEngine dbEngine;
	private Connection conn;
	public int last_error_code;
	public String last_error_desc;

	public Set<String> validTables = new HashSet<>(
		Arrays.asList(
			"versions",
			"users", "user_stats", "user_reps",
            "group_settings", "user_settings",
			"group_stats", "group_captchas",
			"group_bridges", "channels", "groups",
			"warns", "spam_filters",
			"user_blocklists"
		)
	);
	
	public SQLProcessor(DatabaseEngine dbEngine) {
		this.dbEngine = dbEngine;
		this.conn = dbEngine.conn;
	}
	
	public int createTables() {
		int sql_conn = dbEngine.checkSQLConnection();
		if(sql_conn < 0)
			return sql_conn;
			
        try (Statement stmt = conn.createStatement()) {
			 // Database
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_VERSIONS_TABLE);

			 // Users
             stmt.executeUpdate(SQLCreator.SQL_CREATE_USERS_TABLE);
             stmt.executeUpdate(SQLCreator.SQL_CREATE_USER_BLOCKLISTS_TABLE);
             stmt.executeUpdate(SQLCreator.SQL_CREATE_USER_STATS_TABLE);
             stmt.executeUpdate(SQLCreator.SQL_CREATE_USER_REPUTATIONS_TABLE);
             stmt.executeUpdate(SQLCreator.SQL_CREATE_USER_SETTINGS_TABLE);

             // Channels
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_CHANNELS_TABLE);

			 // Groups
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUPS_TABLE);
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUP_WARNINGS_TABLE);
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUP_STATS_TABLE);
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUP_SETTINGS_TABLE);
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUP_CAPTCHAS_TABLE);
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUP_BRIDGES_TABLE);

			 // Spamfilters
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_SPAM_FILTERS_TABLE);

			 onSuccess("Created 13 tables successfully.");
        } catch (SQLException e) {
			dbEngine.last_exception = e;
			e.printStackTrace();
			onError("Cannot create tables. Please try again.");
			last_error_code = -1;
			return -3;
        }
        return 0;
	}

	public static void registerUserIntoDb(MuChatter chatter, DatabaseEngine dbEngine, Chat tgChat, User tgUser) {
        try {
            if(!dbEngine.ifExist("users", "tg_user_id", tgUser.getId())) {
                GetChat chatInfoApi = GetChat
                                            .builder()
                                            .chatId(tgChat.getId())
                                            .build();

                LinkedHashMap<String, Object> values = new LinkedHashMap<>();

                ChatFullInfo chatInfo = chatter.getTelegramClient().execute(chatInfoApi);

                ZonedDateTime current_dt = LocalDateTime.now().atZone(ZoneId.of(chatter.getDefaultTimezone()));

                values.put("tg_user_id",  tgUser.getId());
                values.put("tg_nickname", tgUser.getUserName());
                values.put("first_name",  tgUser.getFirstName());
                values.put("last_name",   tgUser.getLastName());
                values.put("birth_date",  chatInfo.getBirthdate() == null ?
                           "1800-01-01" : String.format("%d-%02d-%02d",
                                                        chatInfo.getBirthdate().getYear() == null ?
                                                                "1800" : chatInfo.getBirthdate().getYear(),
                                                        chatInfo.getBirthdate().getMonth(),
                                                        chatInfo.getBirthdate().getDay()
                                          )
                          );
                values.put("reg_date",
                           String.format("%d-%02d-%02d",
                                         current_dt.getYear(),
                                         current_dt.getMonthValue(),
                                         current_dt.getDayOfMonth())
                          );

                values.put("interests", null);
                dbEngine.add("users", values);
            }

            if(!dbEngine.ifExist("user_settings", "tg_user_id", tgUser.getId())) {
                LinkedHashMap<String, Object> values = new LinkedHashMap<>();

                values.put("tg_user_id", tgUser.getId());
                values.put("ui_language", Locale.isExist(tgUser.getLanguageCode()) ? "en" : tgUser.getLanguageCode());
                values.put("timezone", 180);
                values.put("levels", false);
                values.put("reps", true);

                dbEngine.add("user_settings", values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ResultSet getUserFromDb(MuChatter chatter, DatabaseEngine dbEngine, User tgUser, String suffix) {
        try {
            String table_name = "users";
            ResultSet resultSet;

            if(suffix != null && !suffix.equals(""))
                table_name = String.format("user_%s", suffix);

            if(dbEngine.ifExist(table_name, "tg_user_id", tgUser.getId())) {
                resultSet = dbEngine.selectEquals("*", table_name, "tg_user_id", tgUser.getId());
                resultSet.next();
                return resultSet;
            } else
                return null;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void updateUserIntoDb(MuChatter chatter, DatabaseEngine dbEngine, User tgUser) {
        try {
            GetChat chatInfoApi = GetChat
                    .builder()
                    .chatId(tgUser.getId())
                    .build();

            ChatFullInfo chatInfo = chatter.getTelegramClient().execute(chatInfoApi);

            if(dbEngine.ifExist("users", "tg_user_id", tgUser.getId())) {
                dbEngine.update(
                    "users",
                    SQLCreator.SQL_USER_COLUMNS[0], tgUser.getUserName(),
                    "tg_user_id", tgUser.getId()
                );

                dbEngine.update(
                    "users",
                    SQLCreator.SQL_USER_COLUMNS[1], tgUser.getFirstName(),
                    "tg_user_id", tgUser.getId()
                );

                dbEngine.update(
                    "users",
                    SQLCreator.SQL_USER_COLUMNS[2], tgUser.getLastName(),
                    "tg_user_id", tgUser.getId()
                );

                dbEngine.update(
                    "users",
                    SQLCreator.SQL_USER_COLUMNS[3], chatInfo.getBirthdate() == null ?
                        "1800-01-01" : String.format("%d-%02d-%02d",
                                                     chatInfo.getBirthdate().getYear() == null ?
                                                         "1800" : chatInfo.getBirthdate().getYear(),
                                                     chatInfo.getBirthdate().getMonth(),
                                                     chatInfo.getBirthdate().getDay()),
                        "tg_user_id", tgUser.getId()
                    );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void updateUserIntoDb(MuChatter chatter, DatabaseEngine dbEngine, User tgUser, int columnIndex, Object value) {
        try {
            if(dbEngine.ifExist("users", "tg_user_id", tgUser.getId()))
                dbEngine.update("users", SQLCreator.SQL_USER_COLUMNS[columnIndex], value, "tg_user_id", tgUser.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateUserIntoDb(MuChatter chatter, DatabaseEngine dbEngine, User tgUser, int columnIndex, Object value, String suffix) {
        try {
            String table_name = "";

            if(suffix != null && !suffix.equals(""))
                table_name = String.format("user_%s", suffix);

            if(dbEngine.ifExist(table_name, "tg_user_id", tgUser.getId())) {
                String column = "";


                switch(suffix) {
                    case "settings":
                        column = SQLCreator.SQL_USER_SETTINGS_COLUMNS[columnIndex];
                        break;
                }

                dbEngine.update(table_name, column, value, "tg_user_id", tgUser.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean onSuccess(String message) {
        System.out.println(
        	DatabaseEngine.SUCCESS_COLOR + "[SUCC] " + DatabaseEngine.RESET_COLOR + message
        );
        return true;
    }
    
    @Override
    public boolean onPadding(String message) {
        System.out.println(
        	DatabaseEngine.RESET_COLOR + "       " + message
        );
        return true;
    }
	
	@Override
    public boolean onInfo(String message) {
        System.out.println(
        	DatabaseEngine.INFO_COLOR + "[INFO] " + DatabaseEngine.RESET_COLOR + message
        );
        return true;
    }

    @Override
    public boolean onWarning(String message) {
    	System.out.println(
			DatabaseEngine.WARNING_COLOR + "[WARN] " + DatabaseEngine.RESET_COLOR + message
		);
		return true;
    }

    @Override
    public boolean onError(String message) {
        System.out.println(
        	DatabaseEngine.ERROR_COLOR + "[ERR ] " + DatabaseEngine.RESET_COLOR + message
        );
        if(dbEngine.config != null)
				onPadding(
					String.format("SQL Database URL: %s", dbEngine.config.sql_addr)
				);
			onPadding(
				String.format("Error Message: %s", dbEngine.last_exception.getMessage())
			);
        return true;
    }
}
