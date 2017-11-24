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
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import protocol.Constants;

/**
 *
 * @author Bernardo
 */
public class ServerConnection implements Runnable {
    private Socket socket;
    private static final int TIMEOUT_LONG = 1200000;
    private static final int TIMEOUT_SHORT = 30000;
    private volatile boolean connected = false;
    private InetSocketAddress server;
    private Selector selector;
    private SocketChannel channel;
    private OutputHandler output;
    private ByteBuffer messageReceived = ByteBuffer.allocateDirect(Constants.MAX_LENGTH);
    private final Queue<ByteBuffer> messageToSend = new ArrayDeque<>();
    private boolean sendNow = false;
    
    public void run() {
        try {
            initialize();
            while(connected) {
                if(sendNow) {
                    channel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    sendNow = false;
                }
                selector.select();
                for(SelectionKey key : selector.selectedKeys()) {
                    selector.selectedKeys().remove(key);
                    if (!key.isValid()) {
                        continue;
                    }
                    if(key.isConnectable()) {
                        channel.finishConnect();
                        outputMessage("Connected");
                    } else if (key.isReadable()) {
                        receiveMessage(key);
                    } else if (key.isWritable()) {
                        sendToServer(key);
                    }
                }
            }
        } catch (IOException e) {
            output.printMessage("Error while communicating");
        }
        if(connected) {
            try {
                disconnect();
            } catch(IOException e) {
                output.printMessage("Error");
            }
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
        channel.close();
        channel.keyFor(selector).cancel();
        connected = false;
    }
    
    public void startGame() {
        if (connected) {
            sendMessage(Commands.START.toString());
        }
    }
    
    public void sendAttempt(String attempt) {
        sendMessage(attempt);
    }
    
    private void sendMessage(String message) {
        synchronized(messageToSend) {
            messageToSend.add(prepareMessage(message));
        }
        sendNow = true;
        selector.wakeup();
    }
    
    private void receiveMessage(SelectionKey key) throws IOException {
        messageReceived.clear();
        int readBytes = channel.read(messageReceived);
        if (readBytes == -1) {
            throw new IOException("Lost connection");
        }
        try {
            outputMessage(verifyMessage(extractFromBuffer(messageReceived)));
        } catch (IOException e) {
            outputMessage(e.getMessage());
        }
    }
    
    
    
    private void sendToServer(SelectionKey key) {
        ByteBuffer message;
        try {
            synchronized(messageToSend) {
                while((message = messageToSend.poll()) != null) {
                    Thread.sleep(4000);
                    channel.write(message);
                    if(message.hasRemaining())
                        return;
                }
             key.interestOps(SelectionKey.OP_READ);   
            }
            
        } catch (IOException ex) {
            output.printMessage("Couldn't send.");
        } catch(CancelledKeyException e) {
            output.printMessage("Program quit while sending");
        } catch (InterruptedException ex) {
            Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    private String extractFromBuffer(ByteBuffer message) {
        message.flip();
        byte[] bytes = new byte[message.remaining()];
        message.get(bytes);
        return new String(bytes);
    }
    
    
    private ByteBuffer prepareMessage(String message) {
        StringJoiner joiner = new StringJoiner(Constants.LENGTH_DELIMITER);
        joiner.add(Integer.toString(message.length()));
        joiner.add(message);
        return ByteBuffer.wrap(joiner.toString().getBytes());
    }
    
    private String verifyMessage(String message) throws IOException {
        String[] msg = message.split(Constants.LENGTH_DELIMITER);
        if (msg.length != 2) 
            throw new IOException("Corrupted message");
        int length = Integer.parseInt(msg[0]);
        if (length != msg[1].length())
            throw new IOException("Length doesn't match");
        return msg[1];
    }
    
    private void outputMessage(String message) {
        Executor pool = ForkJoinPool.commonPool();
        pool.execute(new Runnable() {
            @Override
            public void run() {
                output.printMessage(message);
            }
        });
    }
    
}
