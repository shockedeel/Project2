package com;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class Server {
    public static void main(String[] args) throws IOException {
        int portIndex = Integer.parseInt(args[0]);
        final int SERVER_PORT = Info.getServerPorts()[portIndex];
        Semaphore sem = new Semaphore(1);

        ServerService serv = new ServerService(SERVER_PORT, portIndex, sem);
        ServerToServerService sts = new ServerToServerService(sem, portIndex);
        serv.start();
        sts.start();

    }
}

class ServerToServerService extends Thread {
    Semaphore sem;
    int portIndex;
    ArrayList<Integer> serverPorts;
    ServerInfo servInfo;

    ServerToServerService(Semaphore sem, int portIndex) {

        this.sem = sem;
        this.portIndex = portIndex;
        this.servInfo = ServerInfo.getInstance();
        serverPorts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (i == portIndex)
                continue;
            serverPorts.add(Info.getServerPorts()[i]);
        }

    }

    @Override
    public void run() {

        try {
            MessageParser parser = new MessageParser();
            while (true) {
                sem.acquire();
                for (String key : servInfo.requestMap.keySet()) {
                    ServerQueue sq = servInfo.requestMap.get(key);
                    while (!sq.replyQueue.isEmpty()) {
                        Request req = sq.replyQueue.poll();
                        servInfo.clock.sendMessageRule();
                        String comp = parser.compose("REPLY", servInfo.clock.getClock(), portIndex, req.file);
                        int destPort = Info.getServerPorts()[req.process];
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
                        for (int port : serverPorts) {
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
                        if (top != null && top.numReplies == 3) {// RECV RESPONSES FROM ALL SERVERS
                            sq.requests.poll();
                            sq.sentRequestForTop = false;
                            System.out.println("Received all responses from servers, good to go!");

                            DataOutputStream dout = new DataOutputStream(top.s.getOutputStream());

                            dout.writeBytes("YOUR REQUEST HAS BEEN PROCESSED");
                            top.s.shutdownOutput();
                        }

                    }
                }
                sem.release();
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.out.println("Exception caught in ServerToServerService: \n" + e.toString());
        }
    }

}

class ServerService extends Thread {
    ServerSocket sock;
    Map<Integer, State> stateMap;
    String directory;
    int portIndex;
    String dir;
    Semaphore sem;
    ServerInfo servInfo;

    public ServerService(int port, int portIndex, Semaphore semaphore) throws IOException {
        this.sock = new ServerSocket(port);
        this.portIndex = portIndex;
        this.sem = semaphore;
        this.servInfo = ServerInfo.getInstance();
        determineDir();

    }

    private void determineDir() {
        if (portIndex == 0) {
            this.dir = "s1";
        } else if (portIndex == 1) {
            this.dir = "s2";
        } else if (portIndex == 2) {
            this.dir = "s3";
        }
    }

    @Override
    public void run() {
        MessageParser parser = new MessageParser();
        System.out.println("Running ServerService at port: " + sock.getLocalPort());

        try {
            while (true) {

                Socket s = sock.accept();
                DataInputStream dis = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());
                String msg = new String(dis.readAllBytes());
                MessageParse m = parser.decompose(msg);
                if (m.fromServer == false) {// RESPONDING TO ALL CLIENTS
                    if (m.message.equals("ENQUIRE")) {
                        String res = fulfillEnquire();
                        dout.writeBytes(res);
                        s.close();
                    } else if (m.message.equals("READ")) {
                        sem.acquire();
                        int timestamp = servInfo.clock.getClock();
                        Request req = new Request(m.file, "READ", timestamp, s);
                        if (!servInfo.requestMap.containsKey(req.file)) {
                            servInfo.requestMap.put(req.file, new ServerQueue());
                            servInfo.requestMap.get(req.file).requests.add(req);
                        } else {
                            servInfo.requestMap.get(req.file).requests.add(req);
                        }
                        System.out.println(servInfo.requestMap.get(req.file).requests.size());
                        sem.release();
                    }
                } else {// MESSAGE CAME FROM ANOTHER SERVER
                    System.out.println("Message from another server...: " + m.message);
                    if (m.message.equals("REPLY")) {
                        sem.acquire();
                        servInfo.clock.receiveMessageRule(m.timestamp);
                        servInfo.requestMap.get(m.file).requests.peek().numReplies++;
                        s.close();
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
                        if (top == null || top.timestamp > m.timestamp) {// SEND RESPONSE
                            System.out.println("Response Queued...");
                            q.replyQueue.add(foreign);
                        } else {// DEFER
                            System.out.println("Deferred Response...");
                            q.deferred.add(foreign);
                        }
                        sem.release();
                    }

                }

            }
        } catch (Exception e) {

            System.out.println("Exception occured in ServerService " + sock.getLocalPort() + ":\n" + e.toString());
        }
    }

    private String fulfillEnquire() throws IOException {
        ArrayList<String> files = new ArrayList<>();
        Files.list(new File(this.dir).toPath())
                .limit(10)
                .forEach(path -> {
                    files.add(path.getFileName().toString());
                });
        return files.toString().replace("[", "").replace("]", "");
    }

}

class ServerQueue {
    Queue<Request> requests;
    boolean sentRequestForTop;
    Queue<Request> deferred;
    Queue<Request> replyQueue;

    public ServerQueue() {
        requests = new LinkedList<>();
        deferred = new LinkedList<>();
        replyQueue = new LinkedList<>();
        sentRequestForTop = false;
    }
}

class ServerInfo {
    private static ServerInfo info = null;
    LamportsClock clock;
    Map<String, ServerQueue> requestMap;// File, Queue with that files info

    private ServerInfo() {
        this.clock = new LamportsClock();
        this.requestMap = new HashMap<>();
    }

    public static ServerInfo getInstance() {
        if (info == null) {
            info = new ServerInfo();
        }
        return info;
    }

}

class State {
    int port;

}

class Request {
    int timestamp;
    String file;
    String request;
    int numReplies;
    Socket s;
    int process;

    public Request(String file, String request, int timestamp, Socket s) {
        this.file = file;
        this.request = request;
        this.timestamp = timestamp;
        this.numReplies = 0;
        this.s = s;
    }
}