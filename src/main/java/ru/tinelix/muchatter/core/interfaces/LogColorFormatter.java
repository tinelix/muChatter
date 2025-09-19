package ru.tinelix.muchatter.core.interfaces;

public interface LogColorFormatter {
    boolean onSuccess(String message);
    boolean onInfo(String message);
    boolean onWarning(String message);
    boolean onError(String message);
    boolean onPadding(String message);
}
