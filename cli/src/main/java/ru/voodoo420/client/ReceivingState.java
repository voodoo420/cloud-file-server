package ru.voodoo420.client;

public enum ReceivingState {
    WAITING,
    FILE_NAME_LENGTH,
    FILE_NAME,
    FILE_LENGTH,
    FILE
}
