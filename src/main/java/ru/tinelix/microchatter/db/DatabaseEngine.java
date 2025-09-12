package ru.tinelix.microbot.db;

import java.io.FileInputStream;
import java.io.IOException;

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
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.tinelix.microbot.core.interfaces.LogColorFormatter;
import ru.tinelix.microbot.db.SQLProcessor;

public class DatabaseEngine implements LogColorFormatter {
	public DatabaseConfig config;
	public Connection conn;
	public Exception last_exception;
	public SQLProcessor sql_proc;
	
	Set<String> validTables = new HashSet<>(
		Arrays.asList(
			"televisions", "vacuums", "refrigerators", "phones", "computers", "users"
		)
	); 
	
	public static final String RESET_COLOR 		= "\u001B[0m";
    public static final String SUCCESS_COLOR 	= "\u001B[32m"; // Green
    public static final String WARNING_COLOR 	= "\u001B[33m"; // Yellow
    public static final String ERROR_COLOR 		= "\u001B[31m"; // Red
    public static final String INFO_COLOR      	= "\u001B[36m"; // Cyan
	
	public static class DatabaseConfig {
		public String postgresql_url;
		public String username;
		public String password;
	}

	public DatabaseEngine() {
		
		try {
			FileInputStream inputStream = new FileInputStream("config/db.json");
			ObjectMapper mapper = new ObjectMapper();
        		config = mapper.readValue(
				inputStream, DatabaseConfig.class
			);
		} catch(java.io.IOException | java.lang.NullPointerException e) {
			last_exception = e;
			onError("Please create 'config/db.json' file and try again.");
		}
	}

	public int connect() {
		try {
            conn = DriverManager.getConnection(
				config.postgresql_url, config.username, config.password
			);
			onSuccess("Database connected successfully.");
			sql_proc = new SQLProcessor(this);
			return 0;
		} catch(SQLException | java.lang.NullPointerException e) {
			last_exception = e;
			onError("Connection refused. Please try again.");
			return -1;
		}
	}
	
	public int checkSQLConnection() {
		try {
			if (conn == null || conn.isClosed()) {
				onError("Connection refused. Please try again.");
				return -1;
			}
        } catch(SQLException e) {
        	onError("Cannot check SQL connection. Please try again.");
        	return -2;
        }
        return 0;
	}
	
	public boolean ifExist(String table, int id) {
		int sql_conn = checkSQLConnection();
		if(sql_conn < 0)
			return false;
			
		if (!validTables.contains(table.toLowerCase())) {
            onError("Invalid table name provided for ifExist function.");
            return false;
        }
        
        String safeTableName = '"' + table.replace("\"", "\"\"") + '"';
        
        String sqlIfExist = "" +
			"SELECT EXISTS(SELECT 1 FROM " + safeTableName + " WHERE id = ?)";

		try (PreparedStatement pstmt = conn.prepareStatement(sqlIfExist)) {
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (SQLException e) {
			last_exception = e;
			onError("Cannot check values if exist. Please try again.");
			return false;
		}
		
		return false;
	}
	
	public boolean ifExist(String table, String column, String value) {
		int sql_conn = checkSQLConnection();
		if(sql_conn < 0)
			return false;
			
		if (!validTables.contains(table.toLowerCase())) {
            onError("Invalid table name provided for ifExist function.");
            return false;
        }
        
        String safeTableName = '"' + table.replace("\"", "\"\"") + '"';
        
        String sqlIfExist = "" +
			"SELECT EXISTS(SELECT 1 FROM " + safeTableName + 
			" WHERE " + column + " = ?)";

		try (PreparedStatement pstmt = conn.prepareStatement(sqlIfExist)) {
			pstmt.setString(1, value);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(1);
			}
		} catch (SQLException e) {
			last_exception = e;
			onError("Cannot check values if exist. Please try again.");
			return false;
		}
		
		return false;
	}
	
	public ResultSet select(
		String columns, String table, 
		String whereClause
	) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(columns).append(" FROM ").append(table);
        if (!whereClause.trim().isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        PreparedStatement pstmt = conn.prepareStatement(query.toString());
        return pstmt.executeQuery();
    }
    
    public ResultSet select(
		String columns, String table, 
		String whereClause, String orderByClause
	) throws SQLException {
		
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(columns).append(" FROM ").append(table);
        
        if (!whereClause.trim().isEmpty()) {
            query.append(" WHERE ").append(whereClause);
        }
        if (!orderByClause.trim().isEmpty()) {
        	orderByClause = orderByClause.replace("'","").replace("\"", "");	
        	
			String[] orderSplitted = orderByClause.split("_");
			if(orderSplitted.length > 1) {
				query.append(" ORDER BY ").append(orderSplitted[0]);
				if(orderSplitted[1].equals("asc")) {
					query.append(" ASC");
				} else {
					query.append(" DESC");
				}
			}
		}
        onInfo(query.toString());
        PreparedStatement pstmt = conn.prepareStatement(query.toString());
        return pstmt.executeQuery();
    }
    
    public boolean add(String table, String values) throws SQLException {
		
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(table).append(" VALUES ").append(values).append(";");
        
        if (values.isEmpty()) {
            return false;
        }
        
        onInfo(query.toString());
        PreparedStatement pstmt = conn.prepareStatement(query.toString());
        
        return pstmt.executeUpdate() > 0;
    }
    
    public int getEntryCount(String table) {
    	try {
			int count = 0;
			String sqlCountQuery = "SELECT COUNT(*) FROM " + table + ";";
			Statement statement = conn.createStatement();
				
			ResultSet resultSet = statement.executeQuery(sqlCountQuery);
			if (resultSet.next()) { 
				count = resultSet.getInt(1);
			}
			return count;
        } catch(SQLException e) {
        	return -1;
        }
    }
    
    public String convertHTTPParams(LinkedHashMap<String, Object> map, int req_type) {
    	StringBuilder sqlClause = new StringBuilder();
    	int map_size = map.size();
		int map_count = 0;
    	
    	switch(req_type) {
    		case 0: // WHERE
				for (var entry : map.entrySet()) {
					if(entry.getValue() instanceof Integer) {
						if((Integer) entry.getValue() == 0)
							continue;
							
						if(map_count > 0) 
							sqlClause.append(" AND ");
							
						if(entry.getKey().endsWith("_start")) {
							sqlClause.append(
								entry.getKey().replace("_start", "") + " >= "
							);
						} else if(entry.getKey().endsWith("_end")) {
							sqlClause.append(
								entry.getKey().replace("_end", "")  + " <= "
							);
						} else {
							sqlClause.append(
								entry.getKey() + " = "
							);
						}
					
						sqlClause.append("" + (Integer)entry.getValue());	
					} else if(entry.getValue() instanceof Boolean) {
						if(!((Boolean) entry.getValue()))
							continue;
							
						if(map_count > 0) 
							sqlClause.append(" AND ");
							
						sqlClause.append(
								entry.getKey() + " = "
						);
					
						sqlClause.append("TRUE");	
					} else if(entry.getValue() instanceof String) {
						if(((String)entry.getValue()).length() == 0)
							continue;
						
						if(map_count > 0) 
							sqlClause.append(" AND ");
						
						sqlClause.append("LOWER(" + entry.getKey() + ") = ");
						
						sqlClause.append("LOWER(\'" + 
							((String)entry.getValue())
								.replace("'", "").replace("\"", "") 
							+ "\')"
						);	
					}
					map_count++;
				}
				break;
			case 1:			// INSERT INTO VALUES
				if(map_size > 0) 
					sqlClause.append("(");
				
				for (var entry : map.entrySet()) {
					if(entry.getValue() instanceof Integer) {
						if(map_count > 0) 
							sqlClause.append(", ");
						
						sqlClause.append("" + (Integer)entry.getValue()); 
					} else if(entry.getValue() instanceof String) {
						if(map_count > 0) 
							sqlClause.append(", ");
							
						sqlClause.append(
							"'" + ((String)entry.getValue()).replace("'", "").replace("\"", "") + "'"
						); 
					} else if(entry.getValue() instanceof Boolean) {
						if(map_count > 0) 
							sqlClause.append(", ");
							
						sqlClause.append("" + ((Boolean)entry.getValue() ? "TRUE" : "FALSE") + ""); 
					}
					
					map_count++;
				}
				
				if(map_count > 0)
					sqlClause.append(")");
				break;
    	}
        
        return sqlClause.toString();
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
        if(config != null)
				onPadding(
					String.format("PostgreSQL URL: %s",  config.postgresql_url)
				);
			onPadding(
				String.format("Error Message: %s", last_exception.getMessage())
			);
        return true;
    }

}
