package com.light;

import com.light.io.Server;

import java.io.IOException;

/**
 * Created on 2018/4/27.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.run(args);
    }
}
