/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.ForkJoinPool;
import protocol.Constants;
import server.controller.Controller;

/**
 *
 * @author Bernardo
 */
public class Handler implements Runnable {
    private SocketChannel channel;
    private boolean exit = false;
    private final ByteBuffer messageReceived = ByteBuffer.allocateDirect(Constants.MAX_LENGTH);
    //private String message;
    private Controller controller = new Controller();
    private final Server server;
    private String reply;
    
    Handler(SocketChannel clientChannel, Server server) {
        channel = clientChannel;
        this.server = server;
    }
    
    @Override
    public void run() {
        
        try {
            String rawMessage = verifyMessage(extractFromBuffer(messageReceived));
            try {
                String[] message = rawMessage.split(Constants.DELIMITER);
                reply = "";
                switch (message[0]) {
                    case "QUIT":
                        disconnect();
                        exit = true;
                        break;
                    case "START":
                        controller.startGame();
                        reply += "Game started:" + Constants.NEW_LINE;
                        reply += stateToString();
                        server.reply(channel);
                        break;
                    default:
                        if (controller.gameStarted()) {
                            try {
                                if(controller.attempt(message[0].toCharArray())) {
                                    reply += "Game finished as followed:" + Constants.NEW_LINE;
                                }
                                reply += stateToString();
                            } catch (Exception e) {
                                reply += "Incorrect amount of characters.";
                            }

                        } else {
                            reply += "Game hasn't started yet.";
                        }

                        server.reply(channel);
                }
            } catch(Exception e) {
                e.printStackTrace();
                System.err.println("Error in clienthandler run, aborting.");
                exit = true;
            } 
        } catch(IOException e) {
            System.out.println(e.getMessage());
            reply = e.getMessage();
            server.reply(channel);
        }   
    }
    
    public void receiveMessage() throws IOException {
        messageReceived.clear();
        int readBytes;
        readBytes = channel.read(messageReceived);
        if (readBytes == -1) {
            throw new IOException("Client has closed the connection.");
        }
        ForkJoinPool.commonPool().execute(this);
    }
    
    public void sendMessage() throws IOException { 
        channel.write(prepareMessage(reply));
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
    
    private String stateToString() {
        StringJoiner joinedMessage = new StringJoiner(Constants.NEW_LINE);
        String[]state = controller.getGameState();
        for (String element : state) {
            joinedMessage.add(element);
        }
        return joinedMessage.toString();
    }
    
    public void disconnect() throws IOException {
        channel.close();
    }
}
