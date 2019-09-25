package org.apache.cassandra.testing;

import com.google.gson.Gson;
import org.apache.cassandra.net.MessageOut;

public class Serializer<T> {
  public String toJsonStr(MessageOut<T> obj) {
    Gson gson = new Gson();
    System.out.println(gson.toJson(obj));
    return gson.toJson(obj);
  }

  public MessageOut<T> toObject(String json) {
    Gson gson = new Gson();
    System.out.println(json);
    return gson.fromJson(json, MessageOut.class);
  }
}
