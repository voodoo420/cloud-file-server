package ru.voodoo420.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ReceivingHandler extends ChannelInboundHandlerAdapter {

    private TransferState currentState = TransferState.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        if (buf.toString(CharsetUtil.UTF_8).equals("ls")) {
            String pathString = System.getProperty("user.dir");
            Path path = Paths.get(pathString);
            Stream<Path> files = Files.list(path);
            files.forEach((file) -> ctx.writeAndFlush(file.toString() + "\n"));
            ctx.close();
        } else {
            while (buf.readableBytes() > 0) {
                if (currentState == TransferState.IDLE) {
                    byte read = buf.readByte();
                    if (read == (byte) 25) {
                        currentState = TransferState.NAME_LENGTH;
                        receivedFileLength = 0L;
                        System.out.println("STATE: Start file receiving");
                    } else {
                        System.out.println("ERROR: Invalid first byte - " + read);
                    }
                }

                if (currentState == TransferState.NAME_LENGTH) {
                    if (buf.readableBytes() >= 4) {
                        System.out.println("STATE: Get filename length");
                        nextLength = buf.readInt();
                        currentState = TransferState.NAME;
                    }
                }

                if (currentState == TransferState.NAME) {
                    if (buf.readableBytes() >= nextLength) {
                        byte[] fileName = new byte[nextLength];
                        buf.readBytes(fileName);
                        System.out.println("STATE: Filename received - _" + new String(fileName, StandardCharsets.UTF_8));
                        out = new BufferedOutputStream(new FileOutputStream("_" + new String(fileName)));
                        currentState = TransferState.FILE_LENGTH;
                    }
                }

                if (currentState == TransferState.FILE_LENGTH) {
                    if (buf.readableBytes() >= 8) {
                        fileLength = buf.readLong();
                        System.out.println("STATE: File length received - " + fileLength);
                        currentState = TransferState.FILE;
                    }
                }

                if (currentState == TransferState.FILE) {
                    while (buf.readableBytes() > 0) {
                        out.write(buf.readByte());
                        receivedFileLength++;
                        if (fileLength == receivedFileLength) {
                            currentState = TransferState.IDLE;
                            System.out.println("File received");
                            out.close();
                            break;
                        }
                    }
                }
            }
        }

        if (buf.readableBytes() == 0) {
            System.out.println("buf.readableBytes() == 0");
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
