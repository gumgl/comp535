package socs.network.message;

import java.io.Serializable;
import java.util.LinkedList;

public class LSA implements Serializable {

  //IP address of the router originate this LSA
  public String linkStateID;
  public int lsaSeqNumber = Integer.MIN_VALUE;

  public LinkedList<LinkDescription> links = new LinkedList<LinkDescription>();

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(linkStateID + ":").append(lsaSeqNumber + "\n");
    for (LinkDescription ld : links) {
      sb.append(ld);
    }
    sb.append("\n");
    return sb.toString();
  }
  
  public void addLink(String id, int portNumber, int connectionWeight)
  {
	  lsaSeqNumber++;
	  links.add(new LinkDescription(id, portNumber, connectionWeight));
  }
  
  public void removeLink(String id)
  {
	  
	  for(LinkDescription link : links)
	  {
		  if(link.linkID.equals(id))
		  {
			  lsaSeqNumber++;
			  links.remove(link);
			  break;
		  }
	  }
  }
}
