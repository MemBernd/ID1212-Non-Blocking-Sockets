/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.controller.Controller;

/**
 *
 * @author Bernardo
 */
public class Server {
    private int port = 54321;
    private static final int LINGER = 3000;
    private static final int TIMEOUT_LONG = 1200000;
    
    private Selector selector;
    private ServerSocketChannel serverChannel;
    
    public Server(String[] args) {
        parsePort(args);
        serve();
    }
    
    private void serve() {
        try {
            initialize();
            while (true) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                SelectionKey key;
                while(iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        startNewClient(key);
                    } else if(key.isReadable()) {
                        receive(key);
                    } else if(key.isWritable()) {
                        send(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void receive(SelectionKey key) throws IOException {
        Handler handler = (Handler) key.attachment();
        try {
            handler.receiveMessage();
        } catch (IOException e) {
            handler.disconnect();
            key.cancel();
        }
    }
    
    public void reply(SocketChannel client, String msg) {
        client.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }
    
    private void send(SelectionKey key) throws IOException {
        Handler handler = (Handler) key.attachment();
        try {
        handler.sendMessage();
        key.interestOps(SelectionKey.OP_READ);
        } catch(IOException e) {
            handler.disconnect();
            key.cancel();
        }
    }
    
    private void startNewClient(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, new Handler(clientChannel, this));
        clientChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER);
    }
    
    private void initialize() throws IOException {
       selector = Selector.open();
       serverChannel = ServerSocketChannel.open();
       serverChannel.configureBlocking(false);
       serverChannel.bind(new InetSocketAddress(port));
       serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
    
    
    private void parsePort(String[] args) {
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default.");
            }
        }
    }
}
