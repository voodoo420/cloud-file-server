package ru.voodoo420.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileOperations {
    private static final byte LS = 10;
    private static final byte CP = 11;
    private static final byte RM = 12;
    private static final byte CPFS = 13;

    private static final Channel channel = Network.getInstance().getCurrentChannel();
    private static ByteBuf buf;

    public static void copyFileFromServer(String fileName) {
        sendFirstByte(CPFS);
        sendFileName(fileName);
    }

    public static void showFiles() {
        sendFirstByte(LS);
    }

    public static void sendFile(String fileName, boolean isMoving) throws IOException {
        if (checkFileExisting(fileName)) {
            Path path = Paths.get(fileName);
            FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

            sendFirstByte(CP);
            sendFileName(fileName);

            buf = ByteBufAllocator.DEFAULT.directBuffer(8);
            buf.writeLong(Files.size(path));
            channel.writeAndFlush(buf);

            ChannelFuture transferOperationFuture = channel.writeAndFlush(region, channel.newProgressivePromise());
            transferOperationFuture.addListener(getProgressiveListener(isMoving, path));
        } else {
            System.out.println(fileName + " not exists");
            System.exit(0);
        }
    }

    public static void removeFile(String fileName) {
        sendFirstByte(RM);
        sendFileName(fileName);
    }

    private static boolean checkFileExisting(String fileName) {
        Path path = Paths.get(fileName);
        return Files.exists(path);
    }

    private static void sendFirstByte(byte b) {
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(b);
        channel.writeAndFlush(buf);
    }

    private static void sendFileName(String filename) {
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);

        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(filenameBytes.length);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);
    }

    private static ChannelProgressiveFutureListener getProgressiveListener(boolean isMoving, Path path) throws IOException {
        List<Integer> progressList = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50, 60, 70, 60, 90));
        return new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                int progressInt = Math.round((float) progress / total * 100);
                if (progressList.contains(progressInt)) {
                    if (progressInt == 10 || progressInt == 20 || progressInt == 30 || progressInt == 40
                            || progressInt == 50 || progressInt == 60 || progressInt == 70 || progressInt == 80
                            || progressInt == 90) {
                        float uploaded = (float) progress / 1000 / 1000;
                        System.out.println("\u001b[35m" + String.format("%.1f", uploaded) + "mb uploaded - " + progressInt + "%");
                    }
                    progressList.remove((Object) progressInt);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("\u001b[32m" + path.getFileName().toString() + " uploaded successfully");
                    if (isMoving) Files.delete(path);
                } else System.out.println("\u001b[31m" + path.getFileName().toString() + " not uploaded");
                System.exit(0);
            }
        };
    }
}
