package edu.uchicago.cs.ucare.dmck.interceptor;

import java.io.*;
import java.util.Hashtable;
import org.apache.log4j.Logger;

public class InterceptionLayer {
  private static final Logger LOG = Logger.getLogger(InterceptionLayer.class);
  private static String ipcDir = "/tmp/ipc";

  private static Hashtable<String, Integer> respTable = new Hashtable<String, Integer>();

  private static int latestSenderStep = 0;
  private static int latestReceiverStep = 0;
  private static String senderSequencer = "";
  private static String receiverSequencer = "";

  public static void interceptPaxosEvent(
      long sender, long recv, String verb, String payload, String usrval) {
    long eventId = getHashId(sender, recv, verb, payload);

    String filename = "cassPaxos-" + eventId + "-" + getTimestamp();

    String content = "";
    content += "sender=" + sender + "\n";
    content += "recv=" + recv + "\n";
    content += "verb=" + verb + "\n";
    content += "payload=" + payload + "\n";
    content += "usrval=" + usrval + "\n";
    content += "eventId=" + eventId + "\n";

    writePacketToFile(filename, content);

    LOG.info(
        "[DMCK] Intercept a Paxos message event. sender: "
            + sender
            + " recv: "
            + recv
            + " verb: "
            + verb
            + " payload: "
            + payload
            + "usrval:"
            + usrval
            + " filename: "
            + ipcDir
            + "/"
            + filename);

    commitFile(filename);
    waitForAck(filename);
  }

  public static void updateState(long sender, String type, int key, String ballot) {
    String filename = "cassUpdate-" + sender + "-" + getTimestamp() + "-" + type;

    String content = "";
    content += "sender=" + sender + "\n";
    content += "type=" + type + "\n";
    content += "key=" + key + "\n";
    content += "ballot=" + ballot + "\n";

    writePacketToFile(filename, content);

    LOG.info(
        "[DMCK] Intercept a state update in node: "
            + sender
            + " type: "
            + type
            + " key: "
            + key
            + " ballot: "
            + ballot
            + " filename: "
            + ipcDir
            + "/"
            + filename);

    commitFile(filename);
  }

  public static void updateState2(long sender, String type, String ballot, int key, int value) {
    String filename = "cassUpdate-" + sender + "-" + getTimestamp() + "-" + type;

    String content = "";
    content += "sender=" + sender + "\n";
    content += "type=" + type + "\n";
    content += "ballot=" + ballot + "\n";
    content += "key=" + key + "\n";
    content += "value=" + value + "\n";

    writePacketToFile(filename, content);

    LOG.info(
        "[DMCK] Intercept a state update in node: "
            + sender
            + " type: "
            + type
            + " ballot: "
            + ballot
            + " key: "
            + key
            + " value: "
            + value
            + " filename: "
            + ipcDir
            + "/"
            + filename);

    commitFile(filename);
  }

  public static void updateResponseState(long receiver, String type, boolean response) {
    String filename = "cassResponseUpdate-" + receiver + "-" + getTimestamp() + "-" + type;

    if (!respTable.containsKey(type)) {
      respTable.put(type, 0);
    }
    respTable.put(type, response ? respTable.get(type) + 1 : respTable.get(type));

    String content = "";
    content += "recv=" + receiver + "\n";
    content += "type=" + type + "\n";
    content += "response=" + respTable.get(type) + "\n";

    writePacketToFile(filename, content);

    LOG.info(
        "[DMCK] Intercept a state update in node: "
            + receiver
            + " type: "
            + type
            + " response: "
            + response
            + " filename: "
            + ipcDir
            + "/"
            + filename);

    commitFile(filename);
  }

  public static void updateWorkloadAccomplishement(int id, boolean isApplied) {
    String filename = "cassWorkloadUpdate-" + id;

    String content = "";
    content += "id=" + id + "\n";
    content += "isApplied=" + isApplied + "\n";

    writePacketToFile(filename, content);
    commitFile(filename);
  }

  private static void writePacketToFile(String filename, String content) {
    File file = new File(ipcDir + "/new/" + filename);

    try {
      file.createNewFile();
      FileWriter writer = new FileWriter(file);
      writer.write(content);
      writer.flush();
      writer.close();
    } catch (Exception e) {
      LOG.error("[DMCK] Error in writing state content to file.");
    }
  }

  private static void commitFile(String filename) {
    try {
      Runtime.getRuntime()
          .exec("mv " + ipcDir + "/new/" + filename + " " + ipcDir + "/send/" + filename);
    } catch (Exception e) {
      LOG.error("[DMCK] Error in committing file.");
    }
  }

  private static boolean waitForAck(String filename) {
    File f = new File(ipcDir + "/ack/" + filename);
    boolean dmckResponse = false;

    while (!f.exists()) {
      try {
        Thread.sleep(0, 100);
      } catch (InterruptedException ie) {
        ie.printStackTrace();
      }
    }

    if (f.exists()) {
      try {
        BufferedReader br = new BufferedReader(new FileReader(f));
        for (String line; (line = br.readLine()) != null; ) {
          if (line.contains("execute=true")) {
            dmckResponse = true;
          }
          if (line.contains("dmckStep")) {
            int step = Integer.parseInt(line.split("=")[1]);
            while (step - latestSenderStep != 1) {
              try {
                // Sleep for 5ms if the senderSequencer does not match
                Thread.sleep(5);
              } catch (InterruptedException ie) {
                ie.printStackTrace();
              }
            }
          }
        }
      } catch (Exception e) {
        LOG.error("[DMCK] Error in reading ack file.");
      }
    }

    // Remove DMCK enabling message
    try {
      Runtime.getRuntime().exec("rm " + ipcDir + "/ack/" + filename);
    } catch (Exception e) {
      LOG.error("[DMCK] Error in deleting ack file.");
    }

    return dmckResponse;
  }

  // Interception Layer to enable receiving message event
  public static void enableReceiving(int sender, int recv, String verb) {
    LOG.info("[DMCK] Receive message " + verb + " from node-" + sender + " at node-" + recv);
    boolean isExpectedFile = false;
    File path = new File(ipcDir + "/ack");
    String expectedFilename = "";

    while (!isExpectedFile) {
      if (path.listFiles().length > 0) {
        expectedFilename = "recv-" + recv + "-" + (latestReceiverStep + 1);
        for (File file : path.listFiles()) {
          if (file.getName().equals(expectedFilename)) {
            LOG.info("[DMCK] See expected filename=" + expectedFilename);
            try {
              BufferedReader br = new BufferedReader(new FileReader(file));
              boolean correctSender = false;
              boolean correctVerb = false;
              for (String line; (line = br.readLine()) != null; ) {
                if (line.contains("sendNode")) {
                  int s = Integer.parseInt(line.split("=")[1]);
                  correctSender = sender == s;
                  LOG.info("s=" + s + " vs sender=" + sender + " --> " + correctSender);
                } else if (line.contains("verb")) {
                  String v = line.split("=")[1];
                  correctVerb = verb.equals(v);
                  LOG.info("v=" + v + " vs verb=" + verb + " --> " + correctVerb);
                }
              }
              if (correctSender && correctVerb) {
                LOG.info("[DMCK] Confirmed expected file!");
                isExpectedFile = true;
                break;
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      } else {
        try {
          // Sleep for 5ms if the receiverSequencer does not match
          Thread.sleep(5);
        } catch (InterruptedException ie) {
          ie.printStackTrace();
        }
      }
    }
    // Remove DMCK enabling message
    try {
      LOG.info("[DMCK] Remove file=" + path.getAbsolutePath() + "/" + expectedFilename);
      Runtime.getRuntime().exec("rm " + path.getAbsolutePath() + "/" + expectedFilename);
    } catch (Exception e) {
      LOG.error("[DMCK] Error in deleting ack file.");
    }

    LOG.info(
        "[DMCK] Enable receiving message " + verb + " from node-" + sender + " at node-" + recv);
  }

  // Sender Sequencer Counter
  public static void incrementSenderSequencer() {
    latestSenderStep++;
    senderSequencer += latestSenderStep + "-";
    LOG.info("[DMCK] Sender Sequencer=" + senderSequencer);
  }

  // Receiver Sequencer Counter
  public static void incrementReceiverSequencer() {
    latestReceiverStep++;
    receiverSequencer += latestReceiverStep + "-";
    LOG.info("[DMCK] Receiver Sequencer=" + receiverSequencer);
  }

  private static long getHashId(long sender, long recv, String verb, String payload) {
    long prime = 31;
    long hash = 1;
    hash = prime * hash + recv;
    hash = prime * hash + sender;
    hash = prime * hash + verb.hashCode();
    if (verb.equals("PAXOS_PROPOSE_RESPONSE") || verb.equals("PAXOS_PREPARE_RESPONSE")) {
      // int response = payload.toLowerCase().contains("response=true") ? 1 : 0;
      // hash = prime * hash + response;
    } else {
      hash = prime * hash + payload.hashCode();
    }

    return hash;
  }

  private static long getTimestamp() {
    return System.currentTimeMillis() % 100000;
  }
}
