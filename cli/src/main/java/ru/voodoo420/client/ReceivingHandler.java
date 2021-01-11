package ru.voodoo420.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ReceivingHandler extends ChannelInboundHandlerAdapter {

    private static final byte CPFS = 13;
    private static final byte MESSAGE = 14;

    private State currentState = State.WAITING;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private final long[] progressBytes = new long[9];

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            //todo логирование
            if (currentState == State.WAITING) {
                byte firstByte = buf.readByte();
                if (firstByte == CPFS) {
                    currentState = State.RECEIVING_FILE_NAME_LENGTH;
                    receivedFileLength = 0L;
                    System.out.println("STATE: Start file receiving");
                } else if (firstByte == MESSAGE) {
                    currentState = State.RECEIVING_MESSAGE;
                } else {
                    System.out.println("ERROR: Invalid first byte - " + firstByte);
                }
            }

            if (currentState == State.RECEIVING_MESSAGE) {
                System.out.println("STATE: Start message receiving");
                ctx.fireChannelRead(buf.retain());
                currentState = State.WAITING;
                break;
            }

            if (currentState == State.RECEIVING_FILE_NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get filename length");
                    nextLength = buf.readInt();
                    currentState = State.RECEIVING_FILE_NAME;
                }
            }

            if (currentState == State.RECEIVING_FILE_NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("STATE: Filename received - " + new String(fileName, StandardCharsets.UTF_8));
                    //todo удалить "_"
                    out = new BufferedOutputStream(new FileOutputStream("_" + new String(fileName)));
                    currentState = State.RECEIVING_FILE_LENGTH;
                }
            }

            if (currentState == State.RECEIVING_FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    long part = fileLength / 10;
                    for (int i = 0; i < 8; i++) {
                        progressBytes[i] = part * (i + 1);
                    }
                    System.out.println("STATE: File length received - " + fileLength);
                    currentState = State.RECEIVING_FILE;
                }
            }

            if (currentState == State.RECEIVING_FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;

                    if (receivedFileLength == progressBytes[0] || receivedFileLength == progressBytes[1]
                            || receivedFileLength == progressBytes[2] || receivedFileLength == progressBytes[3]
                            || receivedFileLength == progressBytes[4] || receivedFileLength == progressBytes[5]
                            || receivedFileLength == progressBytes[6] || receivedFileLength == progressBytes[7]
                            || receivedFileLength == progressBytes[8]) {
                        //todo отдельный поток?
                        float downloaded = (float) receivedFileLength / 1000 / 1000;
                        float progressFloat = (float) receivedFileLength / fileLength * 100;
                        System.out.println("\u001b[35m" + String.format("%.1f", downloaded) + "mb downloaded - "
                                + String.format("%.0f", Math.floor(progressFloat)) + "%");
                    }

                    if (fileLength == receivedFileLength) {
                        currentState = State.WAITING;
                        System.out.println("\u001b[32mFile received successfully");
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
