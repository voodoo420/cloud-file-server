package ru.voodoo420.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.util.concurrent.CountDownLatch;

public class Server {
    private static final int port = 1210;

    private static final Server instance = new Server();

    public static Server getInstance() {
        return instance;
    }

    private Channel currentChannel;

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public static void main(String[] args) throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> {
            try {
                Server.getInstance().run(networkStarter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        networkStarter.await();
//        instance.run();
    }

    public void run(CountDownLatch countDownLatch) throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline()
//                                    .addLast(new StringEncoder())
                                    .addLast(new CommandsHandler());
                            currentChannel = ch;
                        }
                    });
            ChannelFuture future = b.bind(port).sync();
            System.out.println("server started");
            countDownLatch.countDown();
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
