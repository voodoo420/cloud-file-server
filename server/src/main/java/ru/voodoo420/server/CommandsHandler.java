package ru.voodoo420.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class CommandsHandler extends ChannelInboundHandlerAdapter {
    private static final byte LS = 10;
    private static final byte CP = 11;
    private static final byte RM = 12;
    public static final byte CPFS = 13;
    private static final byte MESSAGE = 14;

    private State currentState = State.WAITING;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
//        System.out.println("refcount: " + buf.refCnt());
        while (buf.readableBytes() > 0 && buf.refCnt() > 0) {
            //todo логирование
            System.out.println("refcount: " + buf.refCnt());
            if (currentState == State.WAITING) {
                byte firstByte = buf.readByte();
                if (firstByte == CP) {
                    currentState = State.RECEIVING_FILE_TO_RECEIVE_NAME_LENGTH;
                    receivedFileLength = 0L;
                    System.out.println("STATE: Start file receiving");
                } else if (firstByte == LS) {
                    //после отправки списка: io.netty.util.IllegalReferenceCountException: refCnt: 0

                    buf.writeByte(MESSAGE);
                    ctx.writeAndFlush(buf);

                    Path path = Paths.get(System.getProperty("user.dir"));
                    Stream<Path> files = Files.list(path);
                    files.forEach((file) -> ctx.write(file.getFileName().toString() + "\n"));
                    ctx.flush();

                } else if (firstByte == RM) {
                    currentState = State.RECEIVING_FILE_TO_DELETE_NAME_LENGTH;
                } else if (firstByte == CPFS) {
                    currentState = State.RECEIVING_FILE_TO_SEND_NAME_LENGTH;
                } else {
                    System.out.println("ERROR: Invalid first byte - " + firstByte);
                }
            }

            if (currentState == State.RECEIVING_FILE_TO_DELETE_NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: [DELETING] Get filename length");
                    nextLength = buf.readInt();
                    currentState = State.RECEIVING_FILE_TO_DELETE_NAME;
                }
            }

            if (currentState == State.RECEIVING_FILE_TO_DELETE_NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileNameBytes = new byte[nextLength];
                    buf.readBytes(fileNameBytes);
                    String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);
                    Path path = Paths.get(fileName);
                    if (Files.exists(path)) {
                        Files.delete(path);
                        ctx.writeAndFlush(fileName + " deleted");
                    } else {
                        ctx.writeAndFlush(fileName + " not exists");
                    }
                    currentState = State.WAITING;
                    ctx.close();
                }
            }

            if (currentState == State.RECEIVING_FILE_TO_SEND_NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: [SENDING] Get filename length");
                    nextLength = buf.readInt();
                    currentState = State.RECEIVING_FILE_TO_SEND_NAME;
                }
            }

            if (currentState == State.RECEIVING_FILE_TO_SEND_NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileNameBytes = new byte[nextLength];
                    buf.readBytes(fileNameBytes);
                    String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);
                    Path path = Paths.get(fileName);
                    if (Files.exists(path)) {
                        FileSender.sendFile(path, ctx.channel(), future -> {
                            if (!future.isSuccess()) {
                                future.cause().printStackTrace();
                            }
                            if (future.isSuccess()) {
                                System.out.println("File sent successfully");
                            }
                        });
                    } else {
                        ctx.writeAndFlush(fileName + " not exists");
                    }
                    currentState = State.WAITING;
                }
            }

            if (currentState == State.RECEIVING_FILE_TO_RECEIVE_NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get filename length");
                    nextLength = buf.readInt();
                    currentState = State.RECEIVING_FILE_TO_RECEIVE_NAME;
                }
            }

            if (currentState == State.RECEIVING_FILE_TO_RECEIVE_NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("STATE: Filename received - " + new String(fileName, StandardCharsets.UTF_8));
                    //todo удалить "_"
                    out = new BufferedOutputStream(new FileOutputStream("_" + new String(fileName)));
                    currentState = State.RECEIVING_FILE_TO_RECEIVE_LENGTH;
                }
            }

            if (currentState == State.RECEIVING_FILE_TO_RECEIVE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("STATE: File length received - " + fileLength);
                    currentState = State.RECEIVING_FILE;
                }
            }

            if (currentState == State.RECEIVING_FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        currentState = State.WAITING;
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
