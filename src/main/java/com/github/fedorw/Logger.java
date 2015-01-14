package com.github.fedorw.log4f;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by fedor on 1/13/15.
 */
public class Logger {
    static Logger instance = new Logger();
    ServerSocket sock = null;
    private static int PORT = 5454;
    Set<Socket> clients = Collections.synchronizedSet(new HashSet<Socket>());

    public static void write(String s) {
        Set<Socket> broken = new HashSet<>();
        for (Socket c : instance.clients) {
            try {
                c.getOutputStream().write((s + "\n").getBytes());
                c.getOutputStream().flush();
            } catch (Exception e) {
                // remove disconnected or otherwise screwed up client connection from clientlist
                broken.add(c);
            }
        }
        for (Socket b : broken) {
            try { b.close(); } catch (Exception e) { }
            System.err.println("log4j client disconnected: "+b.hashCode());
            instance.clients.remove(b);
        }
    }

    public Logger() {
        sock = getSocket();
        System.err.println("****************************************");
        System.err.println(String.format("     log4f listening at port " + sock.getLocalPort()));
        System.err.println("****************************************");

        Runnable r = new Runnable() {
            public void run() {

                for (; ; ) {
                    try {
                        Socket client = sock.accept();
                        clients.add(client);
                        write("CLIENT CONNECTED "+client.hashCode());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    private ServerSocket getSocket() {
        String prop = System.getProperty("log4f.port");
        if (prop != null) {
            try {
                int port = Integer.parseInt(prop);
                return new ServerSocket(port);
            } catch (NumberFormatException | IOException e) {
                System.err.println("Cannot parse log4f.port '" + prop + "'");
                e.printStackTrace();
            }
        }
        // try to find port
        for (int port : range(PORT, 10)) {
            try {
                return new ServerSocket(port);
            } catch (IOException e) {
                continue; // try next port
            }
        }
        return null;
    }

    private int[] range(int start, int len) {
        int p[] = new int[len];
        for (int i = 0; i < len; i++) {
            p[i] = start + i;
        }
        return p;
    }

    public static void main(String args[]) {
        System.err.println("start");
        for (int i = 0; i < 100; i++) {
            Logger.write("hoi " + i);
            try {
                Thread.sleep(1 * 1000);
            } catch (Exception e) {
            }
        }
    }
