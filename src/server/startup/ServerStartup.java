/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.startup;

import server.net.Server;
/**
 *
 * @author Bernardo
 */
public class ServerStartup {
    
    public static void main(String[] args) {
        new Server(args);
    }
}
