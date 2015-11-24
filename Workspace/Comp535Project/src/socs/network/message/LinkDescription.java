package socs.network.message;

import java.io.Serializable;

public class LinkDescription implements Serializable {
  public String linkID;
  public int portNum;
  public int tosMetrics;

  public String toString() {
    return linkID + ","  + portNum + "," + tosMetrics;
  }
  
  public LinkDescription()
  {
	  
  }
  
  public LinkDescription(String id, int portNumber, int connectionWeight)
  {
	  linkID = id;
	  portNum = portNumber;
	  tosMetrics = connectionWeight;
  }
}
