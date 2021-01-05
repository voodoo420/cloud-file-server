package ru.voodoo420.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSender {
    private static final byte CP = 11;
    private static final Channel channel = Server.getInstance().getCurrentChannel();

    public static void sendFile(String fileName, boolean isMoving, ChannelFutureListener finishListener) throws IOException {
        if (checkFileExisting(fileName)) {

            Path path = Paths.get(fileName);
            FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));

            ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeByte(CP);
            channel.writeAndFlush(buf);

            byte[] fileNameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);

            buf = ByteBufAllocator.DEFAULT.directBuffer(4);
            buf.writeInt(fileNameBytes.length);
            channel.writeAndFlush(buf);

            buf = ByteBufAllocator.DEFAULT.directBuffer(fileNameBytes.length);
            buf.writeBytes(fileNameBytes);
            channel.writeAndFlush(buf);

            buf = ByteBufAllocator.DEFAULT.directBuffer(8);
            buf.writeLong(Files.size(path));
            channel.writeAndFlush(buf);

            ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
            if (finishListener != null) {
                transferOperationFuture.addListener(finishListener);
            }
        } else {
            System.out.println(fileName + " not exists");
            System.exit(0);
        }

    }

    private static boolean checkFileExisting(String fileName) {
        Path path = Paths.get(fileName);
        return Files.exists(path);
    }
}
