package com;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        final int CLIENT_ID = Integer.parseInt(args[0]);
        int randomServerIndex = (int) (Math.random() * (4 - 1));
        final int SERVER_PORT = Info.getServerPorts()[randomServerIndex];

        try {
            MessageParser parser = new MessageParser();
            Socket s = new Socket("localhost", SERVER_PORT);
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());
            DataInputStream din = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            String send = parser.compose("ENQUIRE");
            send = parser.compose("READ-a.txt");
            if (CLIENT_ID == 1) {
                send = parser.compose("READ-a.txt");
            }
            dout.writeBytes(send);
            s.shutdownOutput();

            String msg = new String(din.readAllBytes());
            System.out.println("From client " + CLIENT_ID + ": " + msg);
            s.close();
        } catch (Exception e) {
            System.out.println("Exception happened in client at port: " + CLIENT_ID + "\n" + e.toString());

        }
    }

}
