package org.apache.cassandra.testing;

import org.apache.cassandra.net.MessageIn;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class TestingClient {

  private static final Logger logger = LoggerFactory.getLogger(TestingClient.class);

  private final String hostName;
  private final int portNumber;

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  // onFlight matches IO communicated PaxosEvent to internal event MessageIn
  private Map<PaxosEvent, SendingInfo> onFlight;

  public static TestingClient getInstance() {
    return instance;
  }

  private static TestingClient instance = new TestingClient("127.0.0.1", 4444);

  private TestingClient(String hostName, int portNumber) {
    this.hostName = hostName;
    this.portNumber = portNumber;

    this.onFlight = new HashMap<PaxosEvent, SendingInfo>();
  }

  // Cassandra node sending a message
  public void writeToSocket(MessageOut message, int id, InetAddress to) {
    PaxosEvent pe = PaxosEvent.createFromMessage(message, to);
    SendingInfo si =  new SendingInfo(message, id, to);
    onFlight.put(pe, si);
    out.println(PaxosEvent.toJsonStr(pe));
  }

  // Cassandra node received a message
  public void writeToSocket(MessageIn message, int senderOfAck, InetAddress to) {
    PaxosEvent pe = new PaxosEvent(senderOfAck, senderOfAck, message.verb.toString(), PaxosEvent.ACK_PAYLOAD, PaxosEvent.ACK_PAYLOAD);
    out.println(PaxosEvent.toJsonStr(pe));
  }

  public void writeToSocket(String str) {
    out.println(str);
  }

  public void connect() {
    logger.info("Inside testing client");
    try {
      socket = new Socket(hostName, portNumber);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      runInLoop();
    } catch (UnknownHostException e) {
      logger.error("Don't know about host " + hostName);
      System.exit(1);
    } catch (IOException e) {
     logger.error("Couldn't get I/O for the connection to " + hostName);
      System.exit(1);
    }
  }

  public void disconnect() {

  }

  public void runInLoop() {
    String fromServer;

    try {
      while ((fromServer = in.readLine()) != null) {
        if (fromServer.equals("END"))
          break;

        logger.info("---Received from server: " + fromServer);
        PaxosEvent received = PaxosEvent.toObject(fromServer);

        //if(!onFlight.containsKey(received)) {
        //  logger.info("Message not found");
        //}
        SendingInfo si = onFlight.remove(received);
        //logger.info("---Removed: " + si.message.toString());
        MessagingService.instance().sendMessage(si.message, si.id, si.to);
      }
    } catch (Exception e) {
      logger.error("Error in receive loop", e);
      System.exit(1);
    }
  }

  public class SendingInfo {
    MessageOut message;
    int id;
    InetAddress to;

    public SendingInfo(MessageOut message, int id, InetAddress to) {
      this.message = message;
      this.id = id;
      this.to = to;
    }

    public MessageOut getMessage() {
      return message;
    }

    public int getId() {
      return id;
    }

    public InetAddress getTo() {
      return to;
    }
  }
}