package com;

public class MessageParser {

    public MessageParse decompose(String recv) {
        recv = Utils.strip(recv);
        MessageParse mp = new MessageParse();
        int colonInd = recv.indexOf(":");
        String type = recv.substring(0, colonInd);
        if (type.equals("C")) {
            int hyphenInd = recv.indexOf("-");
            mp.fromServer = false;
            mp.message = recv.substring(colonInd + 1);
            if (hyphenInd != -1) {
                int commaInd = recv.indexOf(",");
                if (commaInd != -1) {
                    mp.message = recv.substring(colonInd + 1, commaInd);
                    mp.file = recv.substring(hyphenInd + 1);
                    mp.contents = recv.substring(commaInd + 1, hyphenInd);
                } else {
                    mp.message = recv.substring(colonInd + 1, hyphenInd);
                    String file = recv.substring(hyphenInd + 1);
                    mp.file = file;
                }

            }
        } else {
            mp.fromServer = true;
            int hyphenInd = recv.indexOf("-");
            int firstComma = recv.indexOf(",");
            int secondComma = recv.indexOf(",", firstComma + 1);
            int timestamp = Integer.parseInt(recv.substring(firstComma + 1, secondComma));
            int process = Integer.parseInt(recv.substring(secondComma + 1, colonInd));
            String msg = recv.substring(colonInd + 1);
            if (hyphenInd != -1) {
                mp.file = recv.substring(hyphenInd + 1);
                msg = recv.substring(colonInd + 1, hyphenInd);
            }
            mp.message = msg;
            mp.process = process;
            mp.timestamp = timestamp;
        }

        return mp;
    }

    public String compose(String message) {
        String comp = "C:" + message;
        return comp;
    }

    public String compose(String message, String file) {
        String comp = "C:" + message + "-" + file;
        return comp;
    }

    public String compose(String message, String contents, String file) {
        String comp = "C:" + message + "," + contents + "-" + file;
        return comp;
    }

    public String compose(String message, int timestamp, int process, String file) {
        String comp = "S," + timestamp + "," + process + ":" + message + "-" + file;
        return comp;
    }
}

class MessageParse {
    int timestamp;
    int process;
    String file;
    String message;
    String contents;
    boolean fromServer;
}