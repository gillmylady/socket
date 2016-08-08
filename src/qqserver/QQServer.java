/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qqserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import javax.swing.*;

/**
 *
 * @author gillmylady
 */
public class QQServer {
    
    private static long sendFrenquency = 1000;                     //10 seconds
    private static final int port = 9002;                           //port for connection
    private static boolean updateFlag = false;                      //user names update 
    private static HashMap<String, PrintWriter> namesAndWriters = new HashMap<>();
    
    private static JFrame frame = new JFrame("Server");
    private static JTextArea logArea = new JTextArea("Log Area                                  ");
    private static JTextArea onlineNamesArea = new JTextArea("Online Names  ");
    private static JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(logArea), new JScrollPane(onlineNamesArea));
    
    
    public QQServer(){
        logArea.setEditable(false);
        onlineNamesArea.setEditable(false);
        
        frame.add(splitPane);
        
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
    public static void main(String[] args) throws Exception{
        
        new QQServer();
        printPrompt("Server starts");
        BroadcastOnlineNames sendOnlineNames = new BroadcastOnlineNames();
        sendOnlineNames.start();
        
        ServerSocket listener = new ServerSocket(port);
        try{
            while(true){
                new Handler(listener.accept()).start();
            }
        }finally{
            listener.close();
            sendOnlineNames.shutdown();
        }

    }
    
    public static void printDebug(String s){
        logArea.append("\n##debug:" + s);
    }
    
    public static void printPrompt(String s){
        logArea.append("\n" + s);
    }
    
    private static void sendOnlineNames(){
        /*if(updateFlag)
            printDebug("sendOnlineNames, updateFlag=true");
        else
            printDebug("sendOnlineNames, updateFlag=false");
        */
        if(namesAndWriters.isEmpty() || updateFlag == false)
            return;
        String onlineNames = "FRIENDS:";
        for(String name : namesAndWriters.keySet()){
            onlineNames = onlineNames + ";";
            onlineNames = onlineNames + name;
        }
        printDebug(onlineNames);
        setOnlineNamesArea();
        
        for(PrintWriter writer : namesAndWriters.values()){
            writer.println(onlineNames);
        }
        updateFlag = false;
    }
    
    private static void setOnlineNamesArea(){
        String onlineNames = "FRIENDS:";
        for(String name : namesAndWriters.keySet()){
            onlineNames = onlineNames + "\n";
            onlineNames = onlineNames + name;
        }
        onlineNamesArea.setText(onlineNames);
    }
    
    private static class BroadcastOnlineNames extends Thread{
        
        private boolean running = true;
        
        public BroadcastOnlineNames(){
            printDebug("broadcast names thread begin");
        }
        
        public synchronized void run(){
            while(running){
                try {
                    this.wait(sendFrenquency);
                } catch (InterruptedException ex) {
                    printDebug("InterruptedException");
                }
                sendOnlineNames();
            }
        }
        
        public synchronized void shutdown(){
            running = false;
        }
    }
    
    
    private static class Handler extends Thread{
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        
        public Handler(Socket socket){
            this.socket = socket;
            printDebug("one client comes");
        }
        
        public void run(){
            
            try{
                
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                while(true){
                    out.println("NAME");
                    String input = "";
                    input = in.readLine();
                    System.out.println(input);
                    if(input.length() == 0)
                        continue;
                    String[] info = input.split(":");
                    String s0 = "";
                    if(info.length > 0)
                        s0 = info[0];
                    name = "";
                    if(info.length > 1)
                        name = info[1];
                    
                    if(s0.equals("NAME") == false || name.length() == 0){
                        printDebug("name null!!!");
                        return;
                    }
                    synchronized (namesAndWriters) {
                        if(!namesAndWriters.containsKey(name)){
                            break;
                        }
                    }
                }

                namesAndWriters.put(name, out);
                printPrompt(name + " added!!!");
                setOnlineNamesArea();
                updateFlag = true;
                
                // send format: TYPE:FROM:TO:MSG
                out.println("NAME:SERVER:" + name + ":ADDED");
                            
                while(true){
                    String input = in.readLine();
                    printDebug("input: " + input);
                    String[] info = input.split(":");
                    String s0 = "";
                    String s1 = "";
                    String s2 = "";
                    String s3 = "";
                    if(info.length > 0)
                        s0 = info[0];
                    if(info.length > 1)
                        s1 = info[1];
                    if(info.length > 2)
                        s2 = info[2];
                    if(info.length > 3)
                        s3 = info[3];
                    printDebug("read one input: " + input);
                    //
                    if(s0.equals("CHAT") == false){
                        continue;
                    }
                    synchronized(namesAndWriters){
                        if(namesAndWriters.containsKey(s2) == false){
                            continue;
                        }
                        out.println(input);                     //send this msg back to himself, to display
                        namesAndWriters.get(s2).println(input);    //send this msg to the destination, to display
                    }
                }
                
                
            }catch(IOException e){
                System.out.println(e);
            }finally{
                if(name != null || out != null){
                    namesAndWriters.remove(name);
                    updateFlag = true;
                    //setOnlineNamesArea();
                }
                try{
                    socket.close();
                }catch(IOException e){
                    printDebug("IOException in the end");
                }
            }
        }
    }
}
