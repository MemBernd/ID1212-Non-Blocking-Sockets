/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.FileHandling;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Bernardo
 */
public class FileHandler {
    private static final Path path = Paths.get("");
    private static final String file = "words.txt";
    private BufferedReader reader ;
    
    
    
    public String getWord() throws FileNotFoundException, IOException {
        List<String> words = new ArrayList<String>();
        String line;
        reader = new BufferedReader( new FileReader(path.toAbsolutePath().resolve(file).toString()));
        line = reader.readLine();
        while (line != null) {
            words.add(line);
            line = reader.readLine();
        }
        
        return words.get(new Random().nextInt(words.size()));
    }
}
