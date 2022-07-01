package com;

import java.util.ArrayList;

/*
 * Mutual Exclusion class based on the Ricart-Agrawala algorithm with
 * Roucairol and Carvalho optimization
 */
public class Mutex {
    int numProcesses;
    ArrayList<Integer> deffered;
    Integer timestamp;

    public Mutex(int numProcesses) {
        this.numProcesses = numProcesses;
        deffered = new ArrayList<>();
    }

    public boolean determine(int processId, int timestamp) {
        if (this.timestamp > timestamp) {
            // SEND REPLY
            return true;
        } else {
            this.deffered.add(processId);
            return false;
        }
    }

    public ArrayList<Integer> getAllDeferred() {
        ArrayList<Integer> procs = new ArrayList<>(deffered);
        return procs;
    }

}