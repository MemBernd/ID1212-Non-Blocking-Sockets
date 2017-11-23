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
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.StringJoiner;
import protocol.Constants;
import server.controller.Controller;

/**
 *
 * @author Bernardo
 */
public class Handler implements Runnable {
    private SocketChannel channel;
    private boolean exit = false;
    private ByteBuffer messageReceived = ByteBuffer.allocateDirect(Constants.MAX_LENGTH);
    //private String message;
    private Controller controller = new Controller();
    
    Handler(SocketChannel clientChannel) {
        channel = clientChannel;
    }
    
    @Override
    public void run() {

        try {
            String[] message = extractFromBuffer(messageReceived).split(Constants.DELIMITER);
            String reply = "";
            switch (message[0]) {
                case "QUIT":
                    disconnect();
                    exit = true;
                    break;
                case "START":
                    controller.startGame();
                    reply += "Game started:" + Constants.NEW_LINE;
                    channel.write(prepareMessage(reply + stateToString()));
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

                    channel.write(prepareMessage(reply));
            }
        } catch(Exception e) {
            //e.printStackTrace();
            System.err.println("Error in clienthandler run, aborting.");
            exit = true;
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
    
    private String stateToString() {
        StringJoiner joinedMessage = new StringJoiner(Constants.NEW_LINE);
        String[]state = controller.getGameState();
        for (String element : state) {
            joinedMessage.add(element);
        }
        return joinedMessage.toString();
    }
    
    private void disconnect() throws IOException {
        channel.close();
    }
}
