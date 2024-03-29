package MyPackage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
	private RouterStatus myRouterStatus;	
	
	
	public ExternalRouterConnection(Router r, Socket socket, String simulatedIPadress) throws IOException
	{
		this.socket = socket;
		router = r;
		externalRouterDescription.simulatedIPAddress = simulatedIPadress;
		
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
	
	
	//send a message to the other side of the connection
	public void sendMessage(String message)
	{
		try
		{
			socketOutput.writeObject(message);
			socketOutput.flush(); //push the data through
		}
		catch (IOException e)
		{
			System.out.println("Failed to send message.");
		}
	}
	
	
	//send HELLO message to the otehr side of the connection
	public void sendHELLOmessage()
	{
		try
		{
			socketOutput.writeObject("HELLO:"+router.getSimulatedIP());
			socketOutput.flush(); //push the data through
		}
		catch (IOException e)
		{
			System.out.println("Failed to send message.");
		}
	}
	
	
	//the thread reads all messages that we receive.
	@Override
	public void run()
	{
		String receivedMessage = "";
		
		while(true)
	    {
	      try
	      {
	    	  receivedMessage = (String)socketInput.readObject();
	    	  
	    	  //TODO: do stuff with the message
	    	  if(receivedMessage.startsWith("HELLO:"))
	    	  {
	    		  receivedHELLO(receivedMessage);
	    	  }
	    	  else
	    	  {
	    		  System.out.println("Received : "+receivedMessage);
	    	  }
	      }
	      catch(IOException exception)
	      {
	    	  if(externalRouterDescription.simulatedIPAddress != null && externalRouterDescription.simulatedIPAddress != "")
	    	  {
	    		  System.out.println("Connection lost with "+externalRouterDescription.simulatedIPAddress);
	    	  }
	    	  else
	    	  {
	    		  System.out.println("Connection lost with some router.");
	    	  }
	    	  
	    	  router.RemoveNeighbor(this);
	    	  
	    	  return; //this will close the thread
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
	
	
	//what to do when we receive th HELLO message
	private void receivedHELLO(String receivedMessage)
	{
		String receivedIPaddress = receivedMessage.substring(6);
		
		externalRouterDescription.simulatedIPAddress = receivedIPaddress;
		
		if(myRouterStatus == null)
		{
			setRouterStatus(RouterStatus.INIT);
			
			System.out.println("Received HELLO from "+receivedIPaddress+";");
			System.out.println("set "+receivedIPaddress+" state to INIT;");
			
			sendHELLOmessage();
		}
		else if(myRouterStatus == RouterStatus.INIT)
		{
			setRouterStatus(RouterStatus.TWO_WAY);
			
			System.out.println("Received HELLO from "+receivedIPaddress+";");
			System.out.println("set "+receivedIPaddress+" state to TWO_WAY;");
			
			sendHELLOmessage();
		}
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
			System.out.println(externalRouterDescription.simulatedIPAddress);
		}
	}
}
