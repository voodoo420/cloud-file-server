package ru.voodoo420.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperations {
    private static final byte LS = 10;
    private static final byte CP = 11;
    private static final byte RM = 12;

    private static final Channel channel = Network.getInstance().getCurrentChannel();
    private static ByteBuf buf;

    public static void showFiles() {
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(LS);
        channel.writeAndFlush(buf);
    }

    public static void sendFile(String fileName, boolean isMoving) throws IOException {
        if (checkFileExisting(fileName)) {
            Path path = Paths.get(fileName);
            FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeByte(CP);
            channel.writeAndFlush(buf);

            byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
            sendFileName(filenameBytes);

            buf = ByteBufAllocator.DEFAULT.directBuffer(8);
            buf.writeLong(Files.size(path));
            channel.writeAndFlush(buf);

            ChannelFuture transferOperationFuture = channel.writeAndFlush(region, channel.newProgressivePromise());
            transferOperationFuture.addListener(new ChannelProgressiveFutureListener() {
                @Override
                public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                    float uploaded = (float) progress / 1000 / 1000;
                    float progressFloat = (float) progress / total * 100;
                    System.out.println("\u001b[35m" + String.format("%.1f", uploaded) + "mb uploaded - " + String.format("%.0f", Math.floor(progressFloat)) + "%");
                }

                @Override
                public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                    if (future.isSuccess()) {
                        System.out.println("\u001b[32m" + path.getFileName().toString() + " uploaded successfully");
                        if (isMoving) Files.delete(Paths.get(fileName));
                    } else System.out.println("\u001b[31m" + path.getFileName().toString() + " not uploaded");
                    System.exit(0);
                }
            });
        } else {
            System.out.println(fileName + " not exists");
            System.exit(0);
        }

    }

    public static void removeFile(String fileName) {
        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(RM);
        channel.writeAndFlush(buf);

        byte[] filenameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        sendFileName(filenameBytes);
    }

    private static boolean checkFileExisting(String fileName) {
        Path path = Paths.get(fileName);
        return Files.exists(path);
    }

    private static void sendFileName(byte[] filenameBytes) {
        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
        buf.writeInt(filenameBytes.length);
        channel.writeAndFlush(buf);

        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        channel.writeAndFlush(buf);
    }
}
