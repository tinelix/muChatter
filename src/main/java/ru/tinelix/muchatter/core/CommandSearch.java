package ru.tinelix.muchatter.core;

import java.util.HashMap;
import java.lang.Class;
import java.lang.Object;
import java.lang.reflect.Field;

import ru.tinelix.muchatter.commands.HelloCommand;
import ru.tinelix.muchatter.commands.ProfileCommand;

import ru.tinelix.muchatter.core.BotCommand;
import ru.tinelix.muchatter.core.MuChatter;
import ru.tinelix.muchatter.db.DatabaseEngine;

public class CommandSearch {

    public static BotCommand find(MuChatter chatter, DatabaseEngine dbEngine, String cmdText) {
        if(CommandSearch.getCommandNames().containsKey(cmdText)) {
            String cmdName = CommandSearch.getCommandNames().get(cmdText);
            return loadCommand(cmdName, dbEngine, chatter);
        }

        return null;
    }

    public static BotCommand findByCallback(MuChatter chatter, DatabaseEngine dbEngine, String cbData) {
        if(CommandSearch.getCallbacks().containsKey(cbData.split("_")[0])) {
            String cmdName = CommandSearch.getCallbacks().get(cbData.split("_")[0]);
            return loadCommand(cmdName, dbEngine, chatter);
        }

        return null;
    }

    public static BotCommand loadCommand(String cmdName, DatabaseEngine dbEngine, MuChatter chatter) {
        try {
            Class<?> clazz = Class.forName(
                String.format("ru.tinelix.muchatter.commands.%sCommand", cmdName)
            );

            if(clazz != null) {
                Field cmdNameField = clazz.getField("COMMAND_NAME");

                if(cmdNameField != null) {

                    String classCmdName = (String) cmdNameField.get(null);

                    Object instance =
                        clazz.getDeclaredConstructor(MuChatter.class, DatabaseEngine.class)
                            .newInstance(chatter, dbEngine);

                    if(cmdName.equals(classCmdName))
                        return (BotCommand) instance;
                }
            }
        } catch (Exception e) {}

        return null;
    }

    public static HashMap<String, String> getCommandNames() {
        HashMap<String, String> commands = new HashMap<String, String>();

        commands.put("/start",      HelloCommand.COMMAND_NAME);
        commands.put("/hello",      HelloCommand.COMMAND_NAME);
        commands.put("/profile",    ProfileCommand.COMMAND_NAME);

        return commands;
    }

    public static HashMap<String, String> getCallbacks() {
        HashMap<String, String> commands = new HashMap<String, String>();

        commands.put(ProfileCommand.EDIT_PROFILE_CALLBACK,          ProfileCommand.COMMAND_NAME);
        commands.put(ProfileCommand.FILL_PROFILE_TEXTAREA_CALLBACK, ProfileCommand.COMMAND_NAME);

        return commands;
    }

}
