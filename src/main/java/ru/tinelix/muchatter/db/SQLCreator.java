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
            "birth_date DATE NOT NULL, " +
            "reg_date DATE NOT NULL, " +
            "interests VARCHAR(255), " +
            "hobbys VARCHAR(255), " +
            "city VARCHAR(40), " +
            "irc_nickname VARCHAR(30), " +
            "xmpp_address VARCHAR(255), " +
            "matrix_address VARCHAR(255), " +
            "ovk_address VARCHAR(255)" +
        ")";

    public static final String SQL_CREATE_USER_BLOCKLISTS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS user_blocklists (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "tg_user_id BIGINT NOT NULL, " +
            "reason VARCHAR(255) NOT NULL, " +
            "ban_date DATE NOT NULL, " +
            "duration TIME, " +
            "CONSTRAINT user_blocklists_fk " +
            "FOREIGN KEY (tg_user_id) REFERENCES users (tg_user_id)" +
        ")";

    public static final String SQL_CREATE_USER_STATS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS user_stats (" +
            "tg_user_id BIGINT PRIMARY KEY, " +
            "score BIGINT NOT NULL, " +
            "level INT NOT NULL, " +
            "messages_count BIGINT NOT NULL, " +
            "levels_act_date DATE NOT NULL, " +
            "CONSTRAINT user_stats_fk " +
            "FOREIGN KEY (tg_user_id) REFERENCES users (tg_user_id)" +
        ")";

    public static final String SQL_CREATE_USER_REPUTATIONS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS user_reps (" +
            "tg_from_id BIGINT PRIMARY KEY, " +
            "tg_to_id BIGINT NOT NULL, " +
            "rating INT NOT NULL, " +
            "reason VARCHAR(250) NOT NULL, " +
            "CONSTRAINT user_reputations_fk " +
            "FOREIGN KEY (tg_from_id) REFERENCES users (tg_user_id)" +
        ")";

    public static final String SQL_CREATE_USER_SETTINGS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS user_settings (" +
            "tg_user_id BIGINT PRIMARY KEY NOT NULL, " +
            "ui_language VARCHAR(8) NOT NULL, " +
            "timezone INT NOT NULL, " +
            "levels BOOLEAN NOT NULL, " +
            "reps BOOLEAN NOT NULL, " +
            "CONSTRAINT user_settings_fk " +
            "FOREIGN KEY (tg_user_id) REFERENCES users (tg_user_id)" +
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
            "public_name VARCHAR(180) " +
        ")";

    public static final String SQL_CREATE_GROUP_WARNINGS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS group_warnings (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "tg_group_id BIGINT NOT NULL, " +
            "from_tg_id BIGINT NOT NULL, " +
            "to_tg_id BIGINT NOT NULL, " +
            "duration TIME " +
        ")";

    public static final String SQL_CREATE_GROUP_STATS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS group_stats (" +
            "tg_group_id BIGINT PRIMARY KEY, " +
            "tg_user_id BIGINT, " +
            "score BIGINT NOT NULL, " +
            "level INT NOT NULL, " +
            "messages_count BIGINT NOT NULL, " +
            "levels_act_date DATE NOT NULL, " +
            "CONSTRAINT group_stats_fk " +
            "FOREIGN KEY (tg_group_id) REFERENCES groups (tg_group_id)" +
        ")";

    public static final String SQL_CREATE_GROUP_SETTINGS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS group_settings (" +
            "tg_group_id BIGINT PRIMARY KEY NOT NULL, " +
            "ui_language VARCHAR(8) NOT NULL, " +
            "timezone INT NOT NULL, " +
            "linked_channel_id BIGINT NOT NULL, " +
            "comments_only BOOLEAN NOT NULL, " +
            "block_by_user_rep BIGINT NOT NULL, " + // less or equal
            "irc_bridge BOOLEAN NOT NULL, " +
            "xmpp_bridge BOOLEAN NOT NULL, " +
            "matrix_bridge BOOLEAN NOT NULL, " +
            "warns_amount INT NOT NULL, " +
            "levels BOOLEAN NOT NULL, " +
            "CONSTRAINT group_settings_fk " +
            "FOREIGN KEY (tg_group_id) REFERENCES groups (tg_group_id)" +
        ")";

    public static final String SQL_CREATE_GROUP_BRIDGES_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS group_bridges (" +
            "tg_group_id BIGINT PRIMARY KEY, " +
            "irc_server_id BIGINT, " +
            "irc_channel VARCHAR(30), " +
            "xmpp_group_id VARCHAR(255), " +
            "matrix_group_id VARCHAR(255), " +
            "CONSTRAINT group_bridges_fk " +
            "FOREIGN KEY (tg_group_id) REFERENCES groups (tg_group_id)" +
        ")";

    public static final String SQL_CREATE_GROUP_CAPTCHAS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS group_captchas (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "tg_group_id BIGINT NOT NULL, " +
            "question VARCHAR(255) NOT NULL, " +
            "hint VARCHAR(50) NOT NULL," +
            "exactly_answer VARCHAR(30) NOT NULL, " +
            "answer1 VARCHAR(30), " +
            "answer2 VARCHAR(30), " +
            "answer3 VARCHAR(30), " +
            "answer4 VARCHAR(30), " +
            "duration TIME NOT NULL, " +
            "accepted BIGINT NOT NULL, " +
            "declined BIGINT NOT NULL " +
        ")";

    public static final String SQL_CREATE_SPAM_FILTERS_TABLE = "" +
        "CREATE TABLE IF NOT EXISTS spam_filters (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL," +
            "tg_group_id BIGINT, " +
            "content_regex TEXT NOT NULL, " +
            "reason VARCHAR(255) NOT NULL, " +
            "CONSTRAINT group_spamfilters_fk " +
            "FOREIGN KEY (tg_group_id) REFERENCES groups (tg_group_id)" +
        ")";

    public static final String[] SQL_USER_COLUMNS = {
         "tg_nickname", "first_name", "last_name", "birth_date",
         "interests", "hobbys", "city", "irc_nickname", "matrix_address",
         "ovk_address"
    };

    public static final String[] SQL_USER_SETTINGS_COLUMNS = {
         "ui_language", "timezone", "levels", "reps"
    };

}
