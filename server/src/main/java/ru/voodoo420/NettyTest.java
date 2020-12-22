package ru.voodoo420;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyTest {
    public static void main(String[] args) {
        try {
            new NettyTest().run();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();     //обработка соединения
        EventLoopGroup workerGroup = new NioEventLoopGroup();   //обработка пайплайна
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(new NettyTestHandler());
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(1210).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
