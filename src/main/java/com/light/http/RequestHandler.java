package com.light.http;

import com.light.http.exceptions.ServerInternalException;
import com.light.http.requests.Request;
import com.light.http.requests.RequestParser;
import com.light.http.responses.FileResponse;
import com.light.http.responses.NotFoundResponse;
import com.light.http.responses.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created on 2018/4/21.
 */
@Slf4j
public class RequestHandler implements Runnable {

    private SocketChannel channel;
    private Selector selector;

    public RequestHandler(SocketChannel channel, Selector selector) {
        this.channel = channel;
        this.selector = selector;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        Request request = null;
        Response response = null;
        try {
            //parseRequest
            request = RequestParser.parseRequest(channel);
            if (request == null) {
                return;
            }

            String relativelyPath = System.getProperty("user.dir");
            String basePath = relativelyPath+"\\web";

            String requestURI = request.getRequestURI();
            File file = new File(basePath + requestURI);
            log.info(file.getAbsolutePath());

            if (!file.exists()) {
                response = new NotFoundResponse();
            } else {
                response = new FileResponse(HttpStatus.OK_200, file);
            }

        } catch (IOException e) {
            response = new Response(HttpStatus.INTERNAL_SERVER_ERROR_500);
            log.error("parseRequest failed", e);
        }

        attachResponse(response);
        assert request != null;
        log.info("{} \"{}\" {} {} ms", request.getMethod(), request.getRequestURI(), response.getStatus(), System.currentTimeMillis() - start);
    }

    private void attachResponse(Response response) {
        try {
            channel.register(selector, SelectionKey.OP_WRITE, response);
            selector.wakeup();
        } catch (ClosedChannelException e) {
            log.error("通道已关闭", e);
        }
    }
}
