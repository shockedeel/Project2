package com;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Client {
    public static void main(String[] args) {
        final int CLIENT_ID = Integer.parseInt(args[0]);
        ArrayList<String> files = new ArrayList<>();
        try {
            Thread.sleep(1000);
            int initServerPort = Generator.getRandomServer();
            Socket enquire = new Socket("localhost", initServerPort);
            MessageParser parser = new MessageParser();
            String enq = parser.compose("ENQUIRE");
            DataOutputStream dout = new DataOutputStream(enquire.getOutputStream());
            DataInputStream din = new DataInputStream(enquire.getInputStream());
            dout.writeBytes(enq);
            enquire.shutdownOutput();
            String enquireResponse = Utils.readAllBytes(din);
            String[] f = enquireResponse.split(",");
            for (String fi : f) {
                files.add(Utils.strip(fi));
            }
            enquire.close();
            System.out.println(files.toString());
            Semaphore sem = new Semaphore(1);
            ClientSideServer clientServer = new ClientSideServer(CLIENT_ID, sem);
            ClientRequestGenerator clientRequestGenerator = new ClientRequestGenerator(sem, files);
            ClientToClientService c2c = new ClientToClientService(sem, CLIENT_ID);
            c2c.start();
            clientServer.start();
            Thread.sleep(2000);
            clientRequestGenerator.start();

        } catch (Exception e) {
            System.out.println("Exception happened in client at port: " + CLIENT_ID + "\n" + e.toString());

        }
    }

}

class ClientSideServer extends Thread {

    Semaphore sem;
    int port;
    int portIndex;
    ServerSocket sock;
    ServerInfo servInfo;

    public ClientSideServer(int portIndex, Semaphore sem) throws IOException {
        this.portIndex = portIndex;
        this.servInfo = ServerInfo.getInstance();
        this.sem = sem;
        this.port = Info.getClientPorts()[portIndex];

        this.sock = new ServerSocket(port);

    }

    @Override
    public void run() {
        MessageParser parser = new MessageParser();

        try {
            System.out.println("Booting up client server");
            while (true) {

                Socket s = sock.accept();
                System.out.println("Accepted conn...");
                DataInputStream dis = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                String msg = Utils.readAllBytes(dis);

                MessageParse m = parser.decompose(msg);
                sem.acquire();

                sem.release();
                System.out.println(m.message);
                if (m.message.equals("REPLY")) {
                    sem.acquire();
                    servInfo.clock.receiveMessageRule(m.timestamp);
                    servInfo.requestMap.get(m.file).requests.peek().numReplies++;

                    sem.release();
                } else if (m.message.equals("TERMINATE")) {
                    sem.acquire();
                    servInfo.numFinishedClients++;
                    if (servInfo.numFinishedClients >= 5) {
                        sem.release();
                        break;

                    }
                    sem.release();
                } else if (m.message.equals("REQUEST")) {

                    sem.acquire();

                    servInfo.clock.receiveMessageRule(m.timestamp);

                    if (!servInfo.requestMap.containsKey(m.file)) {// Local queue doesn't have this file
                        servInfo.requestMap.put(m.file, new ServerQueue());
                    }

                    ServerQueue q = servInfo.requestMap.get(m.file);
                    Request top = q.requests.peek();
                    Request foreign = new Request(m.file, "WRITE", m.timestamp, null);
                    foreign.process = m.process;

                    if (top == null || (top.timestamp > m.timestamp)
                            || (top.timestamp == m.timestamp && m.process < portIndex)) {// SEND RESPONSE TOTAL
                                                                                         // ORDERING
                        System.out.println("Response Queued...");
                        q.replyQueue.add(foreign);
                    } else {// DEFER
                        System.out.println("Deferred Response...");
                        q.deferred.add(foreign);
                    }
                    sem.release();

                }

                s.close();

            }

        } catch (Exception e) {
            System.out.println("Error occured at port: " + this.port + "\n" + e.toString());

        } finally {
            try {
                sock.close();
            } catch (Exception e) {
                System.out.println("Couldn't close socket: " + e.toString());
            }
        }

    }

}

class ClientRequestGenerator extends Thread {
    Semaphore sem;
    ServerInfo servInfo;
    ArrayList<String> fileList;

    ClientRequestGenerator(Semaphore sem, ArrayList<String> fileList) {
        this.sem = sem;
        this.servInfo = ServerInfo.getInstance();
        this.fileList = fileList;
    }

    @Override
    public void run() {
        try {
            MessageParser parser = new MessageParser();
            int numOps = 50;
            ArrayList<Request> operationList = new ArrayList<>();
            for (int i = 0; i < numOps; i++) {
                String req = Generator.getRandomOperation();
                String file = Generator.getRandomFile(this.fileList);
                Request request = new Request(file, req, 0, null);
                operationList.add(request);

            }

            for (int i = 0; i < operationList.size(); i++) {
                Request op = operationList.get(i);
                if (op.request.equals("READ")) {

                    sem.acquire();
                    int timestamp = servInfo.clock.getClock();
                    Request req = new Request(op.file, "READ", timestamp, null);
                    if (!servInfo.requestMap.containsKey(req.file)) {
                        servInfo.requestMap.put(req.file, new ServerQueue());
                        servInfo.requestMap.get(req.file).requests.add(req);
                    } else {
                        servInfo.requestMap.get(req.file).requests.add(req);
                    }

                    sem.release();
                } else if (op.request.equals("WRITE")) {
                    sem.acquire();
                    int timestamp = servInfo.clock.getClock();
                    Request req = new Request(op.file, "WRITE", timestamp, null);

                    if (!servInfo.requestMap.containsKey(req.file)) {
                        servInfo.requestMap.put(req.file, new ServerQueue());
                        servInfo.requestMap.get(req.file).requests.add(req);
                    } else {
                        servInfo.requestMap.get(req.file).requests.add(req);
                    }
                    sem.release();
                } else if (op.request.equals("TERMINATE")) {
                    sem.acquire();
                    servInfo.numFinishedClients++;
                    if (servInfo.numFinishedClients >= 5) {
                        sem.release();
                        System.out.println("terminating...");
                        // break;
                    }
                    sem.release();

                }
                Thread.sleep(Generator.generateRandomSleepTime());

            }

            // // TERMINATION CLIENT SIDE
            // int[] clientPorts = Info.getClientPorts();
            // for (int i = 0; i < clientPorts.length; i++) {
            // Socket s = new Socket("localhost", clientPorts[i]);
            // DataOutputStream dfin = new DataOutputStream(s.getOutputStream());
            // String m = parser.compose("TERMINATE");
            // dfin.writeBytes(m);
            // s.shutdownOutput();
            // s.close();

            // }
            // //TERMINATION SERVER SIDE
            // int[] serverPorts = Info.getServerPorts();
            // for (int i = 0; i < serverPorts.length; i++) {
            // Socket s = new Socket("localhost", serverPorts[i]);
            // DataOutputStream dfin = new DataOutputStream(s.getOutputStream());
            // String m = parser.compose("TERMINATE");
            // dfin.writeBytes(m);
            // s.shutdownOutput();
            // s.close();
            // }
        } catch (Exception e) {
            System.out.println("Exeption in client request generation service: " + e.toString());
        }
    }
}

class ClientToClientService extends Thread {
    Semaphore sem;
    int portIndex;
    ArrayList<Integer> serverPorts;
    ServerInfo servInfo;
    String dir;
    MessageParser parser;

    ClientToClientService(Semaphore sem, int portIndex) {

        this.sem = sem;
        this.portIndex = portIndex;
        this.servInfo = ServerInfo.getInstance();
        this.parser = new MessageParser();
        serverPorts = new ArrayList<>();
        for (int i = 0; i < Info.getClientPorts().length; i++) {
            if (i == portIndex)
                continue;
            serverPorts.add(Info.getClientPorts()[i]);
        }

    }

    private String fulfillRead(String file) throws IOException {

        int servPort = Generator.getRandomServer();
        System.out.println(servPort);
        Socket s = new Socket("localhost", servPort);
        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
        DataInputStream din = new DataInputStream(s.getInputStream());
        String send = this.parser.compose("READ", file);
        dout.writeBytes(send);
        s.shutdownOutput();

        String msg = Utils.readAllBytes(din);
        s.shutdownInput();
        s.close();
        return msg;
    }

    private void fulfillWrite(String file) throws IOException, InterruptedException {
        int[] servers = Info.getServerPorts();
        String time = System.currentTimeMillis() + "";

        for (int i = 0; i < servers.length; i++) {

            int serverPort = servers[i];
            System.out.println(serverPort);
            Socket s = new Socket("localhost", serverPort);
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());
            DataInputStream din = new DataInputStream(s.getInputStream());
            String contents = portIndex + "&" + time + "\n";
            String send = this.parser.compose("WRITE", contents, file);
            dout.writeBytes(send);
            s.shutdownOutput();
            Utils.readAllBytes(din);
            s.close();

        }

    }

    @Override
    public void run() {
        int numProcessed = 0;
        try {
            MessageParser parser = new MessageParser();
            while (true) {

                sem.acquire();

                if (servInfo.numFinishedClients >= 5) {
                    System.out.println("LEAVING...");
                    sem.release();
                    break;
                }
                if (numProcessed == 50) {
                    servInfo.numFinishedClients++;
                    numProcessed = Integer.MAX_VALUE;
                    for (int port : this.serverPorts) {
                        Socket s = new Socket("localhost", port);
                        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                        String send = parser.compose("TERMINATE");
                        dout.writeBytes(send);
                        s.shutdownOutput();
                        s.close();

                    }

                    for (int i = 0; i < Info.getServerPorts().length; i++) {
                        Socket s = new Socket("localhost", Info.getServerPorts()[i]);
                        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                        String send = parser.compose("TERMINATE");
                        dout.writeBytes(send);
                        s.shutdownOutput();
                        s.close();
                    }
                    if (servInfo.numFinishedClients >= 5) {
                        Socket s = new Socket("localhost", Info.getClientPorts()[this.portIndex]);
                        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                        String send = parser.compose("TERMINATE");
                        dout.writeBytes(send);
                        s.shutdownOutput();
                        s.close();
                        break;
                    }
                }

                for (String key : servInfo.requestMap.keySet()) {// The map of all files and their respective queues and
                                                                 // information
                    ServerQueue sq = servInfo.requestMap.get(key);
                    while (!sq.replyQueue.isEmpty()) {
                        Request req = sq.replyQueue.poll();
                        servInfo.clock.sendMessageRule();

                        String comp = parser.compose("REPLY", servInfo.clock.getClock(), portIndex, req.file);
                        int destPort = Info.getClientPorts()[req.process];
                        Socket s = new Socket("localhost", destPort);
                        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                        dout.writeBytes(comp);
                        s.shutdownOutput();
                        s.close();
                    }

                    if (sq.sentRequestForTop == false && sq.requests.peek() != null) {
                        sq.sentRequestForTop = true;
                        Request top = sq.requests.peek();
                        top.numReplies++;// default we reply to ourselves
                        for (int port : this.serverPorts) {
                            System.out.println("Requesting for file: " + top.file + " to server: " + port);
                            Socket s = new Socket("localhost", port);
                            DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                            servInfo.clock.sendMessageRule();
                            String comp = parser.compose("REQUEST", top.timestamp, portIndex, top.file);
                            dout.writeBytes(comp);
                            s.shutdownOutput();
                            s.close();
                        }
                    } else {
                        Request top = sq.requests.peek();

                        if (top != null && top.numReplies == 5) {// RECV RESPONSES FROM ALL CLIENTS
                            numProcessed++;
                            sq.requests.poll();
                            sq.sentRequestForTop = false;
                            System.out.println("Received all responses from servers, good to go!");
                            System.out.println(top.request);
                            if (top.request.equals("READ")) {
                                String line = fulfillRead(top.file);
                                System.out.println("From Server to Client" + portIndex + ": " + line);

                            } else if (top.request.equals("WRITE")) {
                                fulfillWrite(top.file);
                            }
                            while (!sq.requests.isEmpty()) {
                                numProcessed++;
                                System.out.println(sq.requests.toString());
                                System.out.println("optimization used...");
                                Request op = sq.requests.poll();
                                if (op.request.equals("READ")) {
                                    System.out.println("reading...");
                                    String line = fulfillRead(op.file);
                                    System.out.println("From Server to Client" + portIndex + ": " + line);

                                } else if (op.request.equals("WRITE")) {
                                    System.out.println("writing...");
                                    fulfillWrite(op.file);
                                    System.out.println("done writing");
                                }
                            }

                            // PROCESS DEFERRED RESPONSES
                            if (sq.deferred.isEmpty()) {
                                System.out.println("no deferred");
                            }
                            while (!sq.deferred.isEmpty()) {
                                System.out.println("Processing deferred responses...");
                                Request req = sq.deferred.poll();
                                servInfo.clock.sendMessageRule();
                                String comp = parser.compose("REPLY", servInfo.clock.getClock(), portIndex, req.file);
                                Socket s = new Socket("localhost", Info.getClientPorts()[req.process]);
                                DataOutputStream doutDef = new DataOutputStream(s.getOutputStream());
                                doutDef.writeBytes(comp);
                                s.close();
                            }

                        }

                    }
                }
                if (servInfo.numFinishedClients >= 5) {
                    sem.release();
                    break;
                }

                sem.release();
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.out.println("Exception caught in ServerToServerService: \n" + e.toString());
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
        int ret = (int) (Math.random() * (100 - 1));
        System.out.println(ret);
        return ret;
    }
}
