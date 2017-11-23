/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.model;

import java.io.IOException;
import java.util.Arrays;
import protocol.Constants;
import server.FileHandling.FileHandler;

/**
 *
 * @author Bernardo
 */
public class GameState {
    private char[] word;
    private char[] actualWord;
    private int attemptsLeft;
    private int score = 0;
    private char placeholder = '_';
    private boolean started = false;
    
    
    //very ugly structure; dividing into several methods
    public boolean finishes(char[] attempt) throws Exception {
        if (attempt.length == actualWord.length) {
            //if user tries to guess whole word
            for (int i = 0 ; i < actualWord.length; i++) {
                if (attempt[i] != actualWord[i]) {
                    attemptsLeft--;
                    if (attemptsLeft == 0) { 
                        started = false;
                        score --;
                        return true;
                    }
                    return false;
                }
            }
            started = false;
            score ++;
            word = actualWord;
            return true;
        } else if (attempt.length == 1) {
            //if one char is to find
            boolean found = false;
            for (int i = 0; i< actualWord.length; i++) {
                if ((word[i] == placeholder) && (actualWord[i] == attempt[0])) {
                    found = true;
                    word[i]=actualWord[i];
                }
            }
            if (!found) {
                attemptsLeft--;
                if (attemptsLeft == 0) { 
                    started = false;
                    score --;
                    return true;
                }
            }
            return false;
        } else {
            throw new Exception("amount of characters doesn't match");
        }
    }
    public String[] getbuiltState() {
        String[] state = new String[3];
        state[Constants.SCORE_INDEX] = "Score: " + Integer.toString(score);
        state[Constants.ATTEMPTS_INDEX] = "Attempts: " + Integer.toString(attemptsLeft);
        state[Constants.WORD_INDEX] = Arrays.toString(word);
        return state;
    }
    public void initializeGame() throws IOException {
        actualWord = getRandomWord().toCharArray();
        word = new char[actualWord.length];
        Arrays.fill(word, placeholder);
        attemptsLeft = actualWord.length;
        started = true;
    }
    
    
    private String getRandomWord() throws IOException {
        String word;
        FileHandler file = new FileHandler();
        word = file.getWord();
        return word;
    }
    
    public boolean gameStarted() {
        return started;
    }
}
