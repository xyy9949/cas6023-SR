package org.apache.cassandra.testing;

import com.google.gson.Gson;
import org.apache.cassandra.net.MessageOut;
import org.apache.cassandra.net.MessagingService;
import org.apache.cassandra.service.paxos.Commit;
import org.apache.cassandra.service.paxos.PrepareResponse;
import org.apache.cassandra.utils.FBUtilities;

import java.net.InetAddress;
import java.util.HashMap;

public class PaxosEvent {

  long sender;
  long recv;
  String verb;
  String payload;
  String usrval;

  public static final String ACK_PAYLOAD = "ACK";

  public PaxosEvent(long sender, long recv, String verb, String payload, String usrval) {
    this.sender = sender;
    this.recv = recv;
    this.verb = verb;
    this.payload = payload;
    this.usrval = usrval;
  }

  public long getSender() {
    return sender;
  }

  public long getRecv() {
    return recv;
  }

  public String getVerb() {
    return verb;
  }

  public String getPayload() {
    return payload;
  }

  public String getUsrval() {
    return usrval;
  }

  public static PaxosEvent createFromMessage(MessageOut message, InetAddress to) {
    String sourceAddr = FBUtilities.getBroadcastAddress().getHostAddress();
    String destinationAddr = to.getHostAddress();
    int sourceId = Integer.parseInt(sourceAddr.substring(sourceAddr.length() - 1)) - 1;
    int destinationId = Integer.parseInt(destinationAddr.substring(destinationAddr.length() - 1)) - 1;

    String verb = transformVerbToString(message.verb);

    HashMap<String, Object> payload = new HashMap<String, Object>();
    HashMap<String, Object> usrval = new HashMap<String, Object>();
    if (message.payload instanceof PrepareResponse) {
      PrepareResponse response = (PrepareResponse) message.payload;
      payload.put("response", response.promised);
      payload.put("inProgressCommitKey", response.inProgressCommit.key.hashCode());
      payload.put("inProgressCommitBallot", response.inProgressCommit.ballot.toString().substring(0, 24));
      payload.put("mostRecentCommitKey", response.mostRecentCommit.key.hashCode());
      payload.put("mostRecentCommitBallot", response.mostRecentCommit.ballot.toString().substring(0, 24));
    } else if (message.payload instanceof Commit) {
      Commit commit = (Commit) message.payload;
      payload.put("key", commit.key.hashCode());
      payload.put("ballot", commit.ballot.toString().substring(0, 24));
    } else if (message.payload instanceof Boolean) {
      Boolean response = (Boolean) message.payload;
      payload.put("response", response);
    }

    return new PaxosEvent(sourceId, destinationId, verb, payload.toString(), usrval.toString());
  }

  private static String transformVerbToString(MessagingService.Verb msgVerb) {
    String result = "";
    switch (msgVerb) {
      case PAXOS_PREPARE:
        result = "PAXOS_PREPARE";
        break;
      case PAXOS_PROPOSE:
        result = "PAXOS_PROPOSE";
        break;
      case PAXOS_COMMIT:
        result = "PAXOS_COMMIT";
        break;
      case PAXOS_PREPARE_RESPONSE:
        result = "PAXOS_PREPARE_RESPONSE";
        break;
      case PAXOS_PROPOSE_RESPONSE:
        result = "PAXOS_PROPOSE_RESPONSE";
        break;
      case PAXOS_COMMIT_RESPONSE:
        result = "PAXOS_COMMIT_RESPONSE";
        break;
      case READ:
        result = "READ";
        break;
      case READ_REQUEST_RESPONSE:
        result = "READ_REQUEST_RESPONSE";
        break;
      default:
        break;
    }
    return result;
  }

  public static String toJsonStr(PaxosEvent obj) {
    Gson gson = new Gson();
    //System.out.println(gson.toJson(obj));
    return gson.toJson(obj);
  }

  public static PaxosEvent toObject(String json) {
    Gson gson = new Gson();
    System.out.println(json);
    return gson.fromJson(json, PaxosEvent.class);
  }

  public boolean isAckEvent() {
    return payload.equals(ACK_PAYLOAD);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == this) return true;

    if(!(obj instanceof PaxosEvent)) return false;

    PaxosEvent e = (PaxosEvent) obj;
    return sender == e.sender  &&
        recv == e.recv &&
        verb.equals(e.verb) &&
        payload.equals(e.payload) &&
        usrval.equals(e.usrval);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (int)sender;
    result = 31 * result + verb.hashCode();
    result = 31 * result + (int)recv;
    result = 31 * result + payload.hashCode();
    result = 31 * result + usrval.hashCode();
    return result;
  }
}
