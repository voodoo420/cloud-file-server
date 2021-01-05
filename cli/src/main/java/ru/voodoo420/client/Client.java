package ru.voodoo420.client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class Client {
    private static final String COPY = "cp";
    private static final String LIST = "ls";
    private static final String REMOVE = "rm";
    private static final String MOVE = "mv";
    private static final String COPY_FROM_SERVER = "cpfs";

    public static void main(String[] args) throws Exception {
        startClient();
        processArgs(args);
    }

    private static void startClient() throws InterruptedException {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        networkStarter.await();
    }

    private static void processArgs(String[] args) throws IOException {
        if (args.length == 1) {
            if (args[0].equals(LIST)) FileOperations.showFiles();
            else printErrorMessage();
        } else if (args.length == 2) {
            switch (args[0]) {
                case COPY:
                    FileOperations.sendFile(args[1], false);
                    break;
                case REMOVE:
                    FileOperations.removeFile(args[1]);
                    break;
                case MOVE:
                    FileOperations.sendFile(args[1], true );
                    break;
                case COPY_FROM_SERVER:
                    FileOperations.copyFromServer(args[1], false);
                    break;
                default:
                    printErrorMessage();
                    break;
            }
        } else printErrorMessage();
    }

    private static void printErrorMessage() {
        System.out.println("List of parameters:\n" +
                "ls             - list of files\n" +
                "cp file.name   - copy file to server\n" +
                "rm file.name   - delete file from\n" +
                "mv file.name   - move file to server");
        System.exit(0);
    }
}
