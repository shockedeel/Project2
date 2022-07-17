package com;

public class Info {
    private static int[] SERVER_PORTS = { 5555, 5556, 5557 };
    private static int[] CLIENT_PORTS = { 8568, 8569, 8570, 8571, 8572 };// to be used for client init

    public static int[] getServerPorts() {
        return SERVER_PORTS;
    }

    public static int[] getClientPorts() {
        return CLIENT_PORTS;
    }
}
