package ru.voodoo420.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public class Client {
    public static void main(String[] args) throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start();
        networkStarter.await();

        if (args.length == 0) {
            System.out.println("No specified parameters. List of parameters:\ncp - copy file to server");
        } else if (args.length == 1) {
            if (args[0].equals("ls")) {
                Network.getInstance().sendMessage("ls");
            }
        } else if (args.length == 2) {
            if (args[0].equals("cp")) {
                Path path = Paths.get(args[1]);
                if(Files.exists(path)){
                    uploadFile(path);
                } else {
                    System.out.println(args[1] + " not exists");
                }
            }
        }
    }

    private static void uploadFile(Path path) throws IOException {
        FileSender.sendFile(path, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
                System.out.println(path.getFileName() + " not uploaded");
                Network.getInstance().stop();
            }
            if (future.isSuccess()) {
                System.out.println(path.getFileName() + " uploaded");
                Network.getInstance().stop();
            }
        });
    }
}
