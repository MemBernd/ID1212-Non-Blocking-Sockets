/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controller;

import java.io.IOException;
import server.model.GameState;

/**
 *
 * @author Bernardo
 */
public class Controller {
    private GameState game = new GameState();
    
    public String[] getGameState() {
        return game.getbuiltState();
    }
    
    public boolean attempt(char[] attempt) throws Exception {
        return game.finishes(attempt);
    }
    
    public void startGame() throws IOException {
        game.initializeGame();
    }
    
    public boolean gameStarted() {
        return game.gameStarted();
    }
}
