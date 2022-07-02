package com;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;

public class Client {
    public static void main(String[] args) {
        final int CLIENT_ID = Integer.parseInt(args[0]);
        ArrayList<String> files = new ArrayList<>();
        try {
            int initServerPort = Generator.getRandomServer();
            Socket enquire = new Socket("localhost", initServerPort);
            MessageParser parser = new MessageParser();
            String enq = parser.compose("ENQUIRE");
            DataOutputStream dout = new DataOutputStream(enquire.getOutputStream());
            DataInputStream din = new DataInputStream(enquire.getInputStream());
            dout.writeBytes(enq);
            enquire.shutdownOutput();
            String enquireResponse = new String(din.readAllBytes());
            String[] f = enquireResponse.split(",");
            for (String fi : f) {
                files.add(fi.strip());
            }
            enquire.close();

            int numOps = Generator.getRandomNumberOps();

            for (int i = 0; i < numOps; i++) {
                String operation = Generator.getRandomOperation();
                String file = Generator.getRandomFile(files);

                if (operation.equals("READ")) {
                    Socket s = new Socket("localhost", Generator.getRandomServer());
                    DataOutputStream dop = new DataOutputStream(s.getOutputStream());
                    DataInputStream dip = new DataInputStream(s.getInputStream());
                    String comp = parser.compose("READ", file);
                    dop.writeBytes(comp);
                    s.shutdownOutput();
                    String recv = new String(dip.readAllBytes());
                    System.out.println("\n\nFROM SERVER AT CLIENT (READ) " + CLIENT_ID + ": " + recv + "\n\n");
                    s.close();
                } else {

                    int[] serverArr = Info.getServerPorts();
                    String time = System.currentTimeMillis() + "";
                    for (int j = 0; j < serverArr.length; j++) {

                        int serverPort = serverArr[j];
                        Socket s = new Socket("localhost", serverPort);
                        DataOutputStream dop = new DataOutputStream(s.getOutputStream());
                        DataInputStream dip = new DataInputStream(s.getInputStream());
                        String comp = parser.compose("WRITE", CLIENT_ID + "&" + time + "\n", file);
                        dop.writeBytes(comp);
                        s.shutdownOutput();
                        s.close();

                    }

                }

                Thread.sleep(Generator.generateRandomSleepTime());
            }

        } catch (Exception e) {
            System.out.println("Exception happened in client at port: " + CLIENT_ID + "\n" + e.toString());

        }
    }

}

class Generator {

    public static String getRandomOperation() {// ENQUIRE, READ, WRITE
        int randomOperationIndex = (int) (Math.random() * (3 - 1));
        if (randomOperationIndex == 0) {
            return "WRITE";
        } else {
            return "READ";
        }
    }

    public static int getRandomNumberOps() {
        return (int) (Math.random() * (50 - 1));
    }

    public static String getRandomStringToWrite() {
        byte[] arr = new byte[40];
        new Random().nextBytes(arr);
        String l = new String(arr, Charset.forName("ASCII"));
        String res = "xyz";
        for (char c : l.toCharArray()) {
            if (Character.isAlphabetic(c)) {
                res += c;
            }
        }
        return res + "\n";
    }

    public static String getRandomFile(ArrayList<String> files) {
        int randomOperationIndex = (int) (Math.random() * (files.size() + 1 - 1));
        return files.get(randomOperationIndex);
    }

    public static int getRandomServer() {
        return Info.getServerPorts()[(int) (Math.random() * (4 - 1))];
    }

    public static int generateRandomSleepTime() {
        return (int) (Math.random() * (10 - 1));
    }
}
