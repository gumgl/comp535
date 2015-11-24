package MyPackage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;
import socs.network.node.Router;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;

public class ExternalRouterConnection extends Thread
{
	private Socket socket;
	private ObjectOutputStream socketOutput;
	private ObjectInputStream socketInput;
	
	private final Router router;
	
	private RouterDescription externalRouterDescription = new RouterDescription();
	public RouterStatus myRouterStatus;
	private int pathLength = -1;
	public boolean myConnection = false;
	public int routerConnectionID = 0;
	
	public boolean receivedIsAlive = true;
	public boolean connected = true;
	
	
	public ExternalRouterConnection(Router r, Socket socket, String simulatedIPadress, int length, int connetionID) throws IOException
	{
		this.socket = socket;
		this.pathLength = length;
		router = r;
		externalRouterDescription.simulatedIPAddress = simulatedIPadress;
		routerConnectionID = connetionID;
		if(simulatedIPadress != null)
		{
			myConnection = true;
		}
		
		try
		{
			router.AddNeighbor(this);
		
			socketOutput = new ObjectOutputStream(this.socket.getOutputStream());
			socketOutput.flush(); //sometimes there are leftover data
	    
			socketInput = new ObjectInputStream(this.socket.getInputStream());
		
		
			System.out.println("A connection has been made");
		}
		catch(NoAvailableSlotsForConnectionException exception)
		{
			System.out.println("There are no more available slots for connection. Attach Failed.");
			socket.close();
		}
	}
	
	
	//send HELLO message to the otehr side of the connection
	public void sendHELLOmessage()
	{
		try
		{
			SOSPFPacket packet = new SOSPFPacket();
			packet.neighborID = router.getSimulatedIP();
			packet.routerID = router.getSimulatedIP();
			packet.lsaArray = new Vector<LSA>();
			packet.pathLength = this.pathLength;
			router.fillLSAarray(packet.lsaArray);
			
			socketOutput.writeObject(packet);
			socketOutput.flush(); //push the data through
		}
		catch (IOException e)
		{
			System.out.println("Failed to send message.");
		}
	}
	
	public void sendDisconnectMessage() throws IOException
	{
		SOSPFPacket packet = new SOSPFPacket();
		packet.sospfType = 3; //disconnect message
		
		socketOutput.writeObject(packet);
		socketOutput.flush();
	}
	
	public void sendIsAliveACK()
	{
		try
		{
			SOSPFPacket packet = new SOSPFPacket();
			packet.sospfType = 2; //is alive request
		
			socketOutput.writeObject(packet);
			socketOutput.flush();
		}
		catch(Exception e)
		{
			//do nothing
		}
	}
	
	public void sendIsAliveRequest()
	{
		try
		{
			SOSPFPacket packet = new SOSPFPacket();
			packet.sospfType = 4; //is alive request
		
			socketOutput.writeObject(packet);
			socketOutput.flush();
		}
		catch(Exception e)
		{
			//do nothing
		}
	}
	
	public void sendLSAupdate() throws IOException
	{
		SOSPFPacket packet = new SOSPFPacket();
		packet.neighborID = router.getSimulatedIP();
		packet.routerID = router.getSimulatedIP();
		packet.lsaArray = new Vector<LSA>();
		packet.pathLength = this.pathLength;
		packet.sospfType = 1;
		router.fillLSAarray(packet.lsaArray);
		
		/*System.out.println("sending:");
		for(LSA lsa : packet.lsaArray)
		{
			System.out.println(lsa.linkStateID + ":" + lsa.links.toString());
		}*/
		
		socketOutput.flush();
		socketOutput.reset();
		socketOutput.writeObject(packet);
		socketOutput.flush(); //push the data through
	}
	
	
	//the thread reads all messages that we receive.
	@Override
	public void run()
	{
		SOSPFPacket receivedMessage;
		
		while(connected)
	    {
	      try
	      {
	    	  receivedMessage = (SOSPFPacket)socketInput.readObject();
	    	  
	    	  //if HELLO message
	    	  if(receivedMessage.sospfType == 0)
	    	  {
	    		  receivedHELLO(receivedMessage);
	    	  }
	    	  else if(receivedMessage.sospfType == 1)
	    	  {
	    		  receiveLSAupdate(receivedMessage);
	    	  }
	    	  else if(receivedMessage.sospfType == 2)
	    	  {
	    		  receivedIsAlive = true;
	    	  }
	    	  else if(receivedMessage.sospfType == 3)
	    	  {
	    		  receivedDisconnect();
	    	  }
	    	  else if(receivedMessage.sospfType == 4)
	    	  {
	    		  sendIsAliveACK();
	    	  }
	    	  else
	    	  {
	    		  System.out.println("Received some message.");
	    	  }
	      }
	      catch(IOException exception)
	      {
	    	  connected = false;
	      }
	      catch(ClassNotFoundException classNotFoundException)
	      {
	    	  System.out.println("Received a message that could not be understood.");
	      }
	      catch(NullPointerException exception)
	      {
	    	  return;
	      }
	    }
	}
	
	
	//what to do when we receive the HELLO message
	private void receivedHELLO(SOSPFPacket receivedMessage) throws IOException
	{
		String receivedIPaddress = receivedMessage.neighborID;
		
		externalRouterDescription.simulatedIPAddress = receivedIPaddress;
		
		router.updateLinkStateDatabase(receivedMessage.lsaArray);
		
		
		if(myRouterStatus == null)
		{
			setRouterStatus(RouterStatus.INIT);
			
			if(this.pathLength == -1)
			{
				pathLength = receivedMessage.pathLength;
				router.addLSAlink(receivedIPaddress, receivedMessage.pathLength);
			}
			
			System.out.println("Received HELLO from "+receivedIPaddress+";");
			System.out.println("set "+receivedIPaddress+" state to INIT;");
			
			sendHELLOmessage();
		}
		else if(myRouterStatus == RouterStatus.INIT)
		{
			setRouterStatus(RouterStatus.TWO_WAY);
			
			if(this.pathLength == -1)
			{
				pathLength = receivedMessage.pathLength;
				router.addLSAlink(receivedIPaddress, receivedMessage.pathLength);
			}
			
			System.out.println("Received HELLO from "+receivedIPaddress+";");
			System.out.println("set "+receivedIPaddress+" state to TWO_WAY;");
			
			
			router.broadCastLSAupdate(routerConnectionID);
			//router.printLSA();
			
			sendHELLOmessage();
		}
		else
		{
			router.connectionFinished();
		}
	}
	
	private void receiveLSAupdate(SOSPFPacket message) throws IOException
	{
		
		if(router.updateLinkStateDatabase(message.lsaArray))
		{
			router.broadCastLSAupdate(routerConnectionID);
		}
	}
	
	private void receivedDisconnect() throws IOException
	{
		if(getIPaddress()!=null)
		{
			System.out.println("Lost connection with "+getIPaddress());
		}
		
		socketInput.close();
		socketOutput.close();
		socket.close();
		connected = false;
		router.RemoveNeighbor(this);
		router.removeLSDConnection(getIPaddress());
		router.broadCastLSAupdate(-1);
	}
	
	
	public void setRouterStatus(RouterStatus status)
	{
		myRouterStatus = status;
	}
	
	
	@Override
	public void interrupt()
	{		
		try
		{
			socketInput.close();
			socketOutput.close();
			socket.close();
		}
		catch(IOException ioException)
		{
			System.out.println("Failed to properly close the connection.");
		}
	}
	
	
	//prints the simulated IP of the other router
	public void printConnectionIP()
	{
		if(externalRouterDescription.simulatedIPAddress == null)
		{
			System.out.println("xxx.xxx.xxx.xxx");
		}
		else
		{
			if(myConnection)
			{
				System.out.print("-> ");
			}
			System.out.println(externalRouterDescription.simulatedIPAddress);
		}
	}
	
	public String getIPaddress()
	{
		return externalRouterDescription.simulatedIPAddress;
	}
	
	public void disconnect() throws IOException
	{
		socket.close();
		socketOutput.close();
		socketInput.close();
	}
}
