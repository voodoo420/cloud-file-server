package ru.voodoo420.server;

public enum TransferState {
    IDLE,
    NAME_LENGTH,
    NAME,
    FILE_LENGTH,
    FILE
}
