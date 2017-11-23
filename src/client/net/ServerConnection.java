/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.net;

import client.view.Commands;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Bernardo
 */
public class ServerConnection implements Runnable {
    private Socket socket;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private static final int TIMEOUT_LONG = 1200000;
    private static final int TIMEOUT_SHORT = 30000;
    private volatile boolean connected = false;
    private InetSocketAddress server;
    private Selector selector;
    private SocketChannel channel;
    private OutputHandler output;
    
    public void run() {
        try {
            initialize();
            while(connected) {
                selector.select();
                for(SelectionKey key : selector.selectedKeys()) {
                    selector.selectedKeys().remove(key);
                    if (!key.isValid()) {
                        continue;
                    }
                    if(key.isConnectable()) {
                        channel.finishConnect();
                        output.printMessage("Connected");
                    } else if (key.isReadable()) {
                        
                    } else if (key.isWritable()) {
                        
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void initialize() throws IOException {
        selector = Selector.open();
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(server);
        channel.register(selector, SelectionKey.OP_CONNECT);
        connected = true;
    }
    
    public void connect(String host, int port, OutputHandler output) throws IOException {
        this.output = output;
        server = new InetSocketAddress(host, port);
        new Thread(this).start();
    }
    
    public void disconnect() throws IOException {
        if (connected) {
            socket.close();
        }
        socket = null;
        connected = false;
    }
    
    public void startGame() {
        if (connected) {
            printWriter.println(Commands.START);
        }
    }
    
    public void sendAttempt(String attempt) {
        printWriter.println(attempt);
    }
    
}
