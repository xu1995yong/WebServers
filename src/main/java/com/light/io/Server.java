package com.light.io;

import com.light.http.RequestHandler;
import com.light.http.responses.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created on 2018/3/31.
 */
@Slf4j
public class Server {

    private ServerContext serverContext;
    private Selector selector;

    /**
     * 启动服务器
     *
     * @param args 格式:start [address:port]
     * @throws IOException
     */
    public void run(String[] args) throws IOException {
        init(args);
        start();
    }

    private void init(String[] args) throws UnknownHostException {
        long start = System.currentTimeMillis();

        //初始化server context，包括ip、port等信息
        initContext(args);
        //扫描controller
         //   initController(controllerPacakgePaths);
        //初始化serversocket，开始接受socket连接
        initServer();

        long end = System.currentTimeMillis();
        log.info("服务器启动 http:/{}:{}/ 耗时:{}ms", serverContext.getIp().getHostAddress(),
                serverContext.getPort(), end - start);
    }

    private void initContext(String[] args) throws UnknownHostException {
        //parse command line arguments
//        if (args.length < 1 || !args[0].equals("start")) {
//            log.info("Usage: start [address:port]");
//            System.exit(1);
//        }

        InetAddress ip = null;
        int port = 0;

        if (args.length == 2 && args[1].matches(".+:\\d+")) {
            String[] addressAndPort = args[1].split(":");
            ip = InetAddress.getByName(addressAndPort[0]);
            port = Integer.valueOf(addressAndPort[1]);
        } else {
            ip = InetAddress.getByName("127.0.0.1");
            port = 8080;
        }

        ServerContext context = new ServerContext();
        context.setIp(ip);
        context.setPort(port);
        this.serverContext = context;
    }


    private void initServer() {
        ServerSocketChannel serverSocketChannel;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(serverContext.getIp(), serverContext.getPort()));
            serverSocketChannel.configureBlocking(false);
            this.selector = Selector.open();
            serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() throws IOException {
        while (true) {
            try {
                if (this.selector.select(500) == 0)
                    continue;
            } catch (IOException e) {
                e.printStackTrace();
            }

            Set<SelectionKey> readyKeys = this.selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
                iterator.remove();
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        //System.out.println("Start read method");
        SocketChannel client = (SocketChannel) key.channel();
        ThreadPool.execute(new RequestHandler(client, selector));
        //TODO keep-alive
        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);//目前一个请求对应一个socket连接，没有实现keep-alive
        //System.out.println("End read method");
    }

    private void write(SelectionKey key) throws IOException {
        //System.out.println("Start write method");
        SocketChannel client = (SocketChannel) key.channel();
        Response response = (Response) key.attachment();
        ByteBuffer byteBuffer = response.getResponseBuffer();
        if (byteBuffer.hasRemaining()) {
            client.write(byteBuffer);
        }
        if (!byteBuffer.hasRemaining()) {
            key.cancel();
            client.close();
        }
        //System.out.println("End write method");
    }

}
