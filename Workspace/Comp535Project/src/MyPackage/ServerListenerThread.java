package MyPackage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import socs.network.node.Router;

public class ServerListenerThread extends Thread
{
	private int routerPortNumber = 0;
	private ServerSocket serverSocket = null;
	
	private final Router router;
	
	
	public ServerListenerThread(Router r, int routerPortNumber) throws IOException
	{
		this.routerPortNumber = routerPortNumber;
		router = r;
		serverSocket = new ServerSocket(routerPortNumber,4);
	}
	
	
	@Override
	public void run()
	{
		Socket connectionRequest;
		ExternalRouterConnection externalConnection;
		
		while(true)
		{
			try
			{
				connectionRequest = serverSocket.accept();
				externalConnection = new ExternalRouterConnection(router, connectionRequest);
				externalConnection.start();
			}
			catch(NoAvailableSlotsForConnectionException exception)
			{
				System.out.println("There are no more available slots for connection.");
			}
			catch (IOException e)
			{
				return;
			}
		}
	}
	
	@Override
	public void interrupt()
	{
		try
		{
			serverSocket.close();
		}
		catch (IOException e)
		{
			System.out.println("Failed to properly close the listener.");
		}
	}
}
