package ru.tinelix.muchatter.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import ru.tinelix.muchatter.db.DatabaseEngine;
import ru.tinelix.muchatter.db.SQLCreator;
import ru.tinelix.muchatter.core.interfaces.LogColorFormatter;

public class SQLProcessor implements LogColorFormatter {
	
	private DatabaseEngine dbEngine;
	private Connection conn;
	public int last_error_code;
	public String last_error_desc;

	public Set<String> validTables = new HashSet<>(
		Arrays.asList(
			"versions",
			"users", "channels", "groups", "warns",
			"chat_settings", "user_settings",
			"chat_stats", "user_stats",
			"captchas", "spam_filters",
			"timers", "tickets", "bridges",
			"blacklist"
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
             stmt.executeUpdate(SQLCreator.SQL_CREATE_USER_SCORES_TABLE);

             // Channels
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_CHANNELS_TABLE);

			 // Groups
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUPS_TABLE);
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUP_WARNINGS_TABLE);
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_GROUP_SCORES_TABLE);
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_INTELLICAPTCHA_TABLE);

			 // Entities
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_ENTITIES_TABLE);

			 // Spamfilters
			 stmt.executeUpdate(SQLCreator.SQL_CREATE_SPAM_BLOCKLISTS_TABLE);

			 onSuccess("Created 11 tables successfully.");
        } catch (SQLException e) {
			dbEngine.last_exception = e;
			onError("Cannot create tables. Please try again.");
			e.printStackTrace();
			last_error_code = -1;
			return -3;
        }
        return 0;
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
