package ru.voodoo420.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public class Client {
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
            if (args[0].equals("ls")) sendMessage("ls");
            else printErrorMessage();
        } else if (args.length == 2) {
            if (args[0].equals("cp")) {
                Path path = Paths.get(args[1]);
                if (Files.exists(path)) uploadFile(path);
                else System.out.println(args[1] + " not exists");
            } else printErrorMessage();
        } else printErrorMessage();
    }

    private static void printErrorMessage() {
        System.out.println("List of parameters:\n" +
                "cp file.name   - copy file to server\n" +
                "ls             - list of files");
        System.exit(0);
    }

    private static void uploadFile(Path path) throws IOException {
        FileSender.sendFile(path, Network.getInstance().getCurrentChannel());
    }

    private static void sendMessage(String message) throws IOException {
        Network.getInstance().sendMessage(message);
    }
}
