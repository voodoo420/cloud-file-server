package ru.voodoo420.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ReceivingHandler extends ChannelInboundHandlerAdapter {
    private static final byte CP = 11;
    private ReceivingState currentState = ReceivingState.WAITING;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);

        while (buf.readableBytes() > 0) {
            if (currentState == ReceivingState.WAITING) {

                byte firstByte = buf.readByte();
//                System.out.println(firstByte);
                if (firstByte == CP) {
                    currentState = ReceivingState.FILE_NAME_LENGTH;
                    receivedFileLength = 0L;
                    System.out.println("STATE: Start file receiving");
                } else {
                    System.out.println("ERROR: Invalid first byte - " + firstByte);
                }
            }


            if (currentState == ReceivingState.FILE_NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get filename length");
                    nextLength = buf.readInt();
                    currentState = ReceivingState.FILE_NAME;
                }
            }

            if (currentState == ReceivingState.FILE_NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("STATE: Filename received - " + new String(fileName, StandardCharsets.UTF_8));
                    //todo убрать "_" перед именем
                    out = new BufferedOutputStream(new FileOutputStream("_" + new String(fileName)));
                    currentState = ReceivingState.FILE_LENGTH;
                }
            }

            if (currentState == ReceivingState.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("STATE: File length received - " + fileLength);
                    currentState = ReceivingState.FILE;
                }
            }

            if (currentState == ReceivingState.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        currentState = ReceivingState.WAITING;
                        System.out.println("File received");
                        out.close();
                        ctx.close();
                        break;
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}