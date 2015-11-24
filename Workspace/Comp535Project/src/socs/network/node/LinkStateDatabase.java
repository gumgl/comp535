package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import MyPackage.LSAholder;

import java.util.Vector;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();
  ArrayList<String> lsaList = new ArrayList<String>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
    lsaList.add(l.linkStateID);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  public String getShortestPath(String destinationIP)
  {
    HashMap<String, LSAholder> map = new HashMap<String, LSAholder>();
    
    for(String lsaID : lsaList)
    {
    	map.put(lsaID, new LSAholder(_store.get(lsaID)));
    }
    
    LSAholder holder = map.get(rd.simulatedIPAddress);
    holder.getShortestPath(map, destinationIP);
    
    if(holder.hasConnectionToTarget)
    {
    	return rd.simulatedIPAddress + holder.shortestPath;
    }
    else
    {
    	return "There are no paths to "+destinationIP;
    }
  }
  
  
  public String getShortestPathDebug(String destinationIP)
  {
    HashMap<String, LSAholder> map = new HashMap<String, LSAholder>();
    
    for(String lsaID : lsaList)
    {
    	map.put(lsaID, new LSAholder(_store.get(lsaID)));
    }
    
    LSAholder holder = map.get(rd.simulatedIPAddress);
    holder.getShortestPathDebug(map, destinationIP, "");
    
    System.out.println();
    
    if(holder.hasConnectionToTarget)
    {
    	return rd.simulatedIPAddress + holder.shortestPath;
    }
    else
    {
    	return "There are no paths to "+destinationIP;
    }
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portNum = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portNum).append(",").
                append(ld.tosMetrics).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }
  
  public LSA getLSA(String ip)
  {
	  return _store.get(ip);
  }
  
  public void storeLSA(String ip, LSA value)
  {
	  _store.put(ip, value);
	  
	  if(!lsaList.contains(value.linkStateID))
	  {
		  lsaList.add(value.linkStateID);
	  }
  }
  
  public void fillLSAarray(Vector<LSA> lsaArray)
  {
	  for(String lsaID : lsaList)
	  {
		  lsaArray.add(_store.get(lsaID));
	  }
  }
  
  public boolean updateDatabase(Vector<LSA> lsaArray)
  {
	  LSA ourLSA = null;
	  boolean hasChanged = false;
	  
	  for(LSA lsa : lsaArray)
	  {
		  ourLSA = _store.get(lsa.linkStateID);
		  if(ourLSA == null)
		  {
			  storeLSA(lsa.linkStateID,lsa);
			  
			  if(!lsaList.contains(lsa.linkStateID))
			  {
				  lsaList.add(lsa.linkStateID);
			  }
			  hasChanged = true;
		  }
		  else if(lsa.lsaSeqNumber > ourLSA.lsaSeqNumber)
		  {
			  _store.put(lsa.linkStateID,lsa);
			  hasChanged = true;
		  }
	  }
	  
	  return hasChanged;
  }
  
  
  public void removeConnection(String thisID, String connectionID)
  {
	  _store.get(thisID).removeLink(connectionID);
  }
  
  
  

}
