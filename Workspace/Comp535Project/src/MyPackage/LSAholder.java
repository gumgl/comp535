package MyPackage;

import java.util.HashMap;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

public class LSAholder {
	
	LSA lsa;
	public boolean explored = false;
	public int distToTarget = -1;
	public boolean hasConnectionToTarget = false;
	
	public String shortestPath = "";
	
	public LSAholder(LSA lsa)
	{
		this.lsa = lsa;
	}
	
	public void getShortestPath(HashMap<String, LSAholder> map, String destination)
	{
		if(distToTarget > -1)
		{
			return;
		}
		
		if(destination.equals(lsa.linkStateID))
		{
			hasConnectionToTarget = true;
			distToTarget = 0;
			return;
		}
		
		explored = true;
		LSAholder temp;
		
		int smallestLength = Integer.MAX_VALUE;
		String path = "";
		
		for(LinkDescription link : lsa.links)
		{
			temp = map.get(link.linkID);
			
			if(temp != null && !temp.explored)
			{
				temp.getShortestPath(map,destination);
			
				if(link.tosMetrics + temp.distToTarget < smallestLength  && temp.hasConnectionToTarget)
				{
					hasConnectionToTarget = true;
					smallestLength = link.tosMetrics + temp.distToTarget;
					path = " ->("+link.tosMetrics+") "+link.linkID+temp.shortestPath;
				}
			}
		}
		
		distToTarget = smallestLength;
		shortestPath = path;
		explored = false;
	}
	
	
	public void getShortestPathDebug(HashMap<String, LSAholder> map, String destination, String offset)
	{
		System.out.println(offset+"<"+lsa.linkStateID+">");
		if(distToTarget > -1)
		{
			System.out.println(offset+"  -> you already know my value!");
			return;
		}
		
		if(destination.equals(lsa.linkStateID))
		{
			System.out.println(offset+"  -> you found me!");
			hasConnectionToTarget = true;
			distToTarget = 0;
			return;
		}
		
		explored = true;
		LSAholder temp;
		
		int smallestLength = Integer.MAX_VALUE;
		String path = "";
		
		for(LinkDescription link : lsa.links)
		{
			temp = map.get(link.linkID);
			
			if(temp != null && !temp.explored)
			{
				temp.getShortestPathDebug(map,destination, offset+"  ");
			
				if(link.tosMetrics + temp.distToTarget < smallestLength  && temp.hasConnectionToTarget)
				{
					hasConnectionToTarget = true;
					smallestLength = link.tosMetrics + temp.distToTarget;
					path = " ->("+link.tosMetrics+") "+link.linkID+temp.shortestPath;
				}
			}
		}
		
		distToTarget = smallestLength;
		shortestPath = path;
		
		System.out.println(offset+"  -> Shortest path: "+path);
		System.out.println(offset+"  -> Shortest length: "+distToTarget);
		
		explored = false;
	}

}
