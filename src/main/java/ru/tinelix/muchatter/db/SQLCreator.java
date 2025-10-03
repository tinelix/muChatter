package ru.tinelix.muchatter.db;

public class SQLCreator {
    public static final String SQL_CREATE_VERSIONS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS versions (" +
            "bot_version VARCHAR(25) PRIMARY KEY, " +
            "db_version INT NOT NULL, " +
            "update_time DATE NOT NULL" +
        ")";

    public static final String SQL_CREATE_USERS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS users (" +
            "tg_user_id BIGINT PRIMARY KEY, " +
            "tg_nickname VARCHAR(180)," +
            "first_name VARCHAR(60) NOT NULL, " +
            "last_name VARCHAR(60), " +
            "reg_region VARCHAR(40)," +
            "reg_date DATE NOT NULL," +
            "irc_nick VARCHAR(30) " +
        ")";

    public static final String SQL_CREATE_USER_BLOCKLISTS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS user_blocklists (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "tg_user_id BIGINT NOT NULL, " +
            "reason VARCHAR(255) NOT NULL, " +
            "ban_date DATE NOT NULL, " +
            "duration TIME " +
        ")";

    public static final String SQL_CREATE_USER_SCORES_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS user_scores (" +
            "tg_user_id BIGINT PRIMARY KEY, " +
            "score BIGINT NOT NULL, " +
            "level INT NOT NULL, " +
            "messages_count BIGINT NOT NULL, " +
            "levels_act_date DATE NOT NULL" +
        ")";

    public static final String SQL_CREATE_CHANNELS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS channels (" +
            "tg_channel_id BIGINT PRIMARY KEY, " +
            "name VARCHAR(120) NOT NULL, " +
            "public_name VARCHAR(180) " +
        ")";

    public static final String SQL_CREATE_GROUPS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS groups (" +
            "tg_group_id BIGINT PRIMARY KEY, " +
            "name VARCHAR(120) NOT NULL, " +
            "public_name VARCHAR(180), " +
            "linked_channel_id BIGINT NOT NULL, " +
            "comments_only BOOLEAN NOT NULL, " +
            "irc_channel VARCHAR(30), " +
            "warns_amount INT NOT NULL, " +
            "levels BOOLEAN NOT NULL" +
        ")";

    public static final String SQL_CREATE_GROUP_WARNINGS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS group_warnings (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "tg_group_id BIGINT NOT NULL, " +
            "from_tg_id BIGINT NOT NULL, " +
            "to_tg_id BIGINT NOT NULL, " +
            "duration TIME " +
        ")";

    public static final String SQL_CREATE_GROUP_SCORES_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS group_scores (" +
            "tg_group_id BIGINT PRIMARY KEY, " +
            "tg_user_id BIGINT, " +
            "score BIGINT NOT NULL, " +
            "level INT NOT NULL, " +
            "messages_count BIGINT NOT NULL, " +
            "levels_act_date DATE NOT NULL" +
        ")";

    public static final String SQL_CREATE_INTELLICAPTCHA_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS intellicaptcha_warnings (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "tg_group_id BIGINT NOT NULL, " +
            "question VARCHAR(255) NOT NULL, " +
            "hint VARCHAR(50) NOT NULL," +
            "exactly_answer VARCHAR(30) NOT NULL, " +
            "answer1 VARCHAR(30), " +
            "answer2 VARCHAR(30), " +
            "answer3 VARCHAR(30), " +
            "answer4 VARCHAR(30), " +
            "duration TIME NOT NULL" +
        ")";

    public static final String SQL_CREATE_ENTITIES_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS entities (" +
            "tg_entities_id BIGINT PRIMARY KEY NOT NULL, " +
            "ui_language VARCHAR(8) NOT NULL, " +
            "timezone INT NOT NULL " +
        ")";

    public static final String SQL_CREATE_SPAM_BLOCKLISTS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS spam_blocklists (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL," +
            "tg_group_id BIGINT, " +
            "content TEXT NOT NULL, " +
            "reason VARCHAR(255) NOT NULL " +
        ")";
}
