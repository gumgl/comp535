package socs.network.node;

import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import MyPackage.ExternalRouterConnection;
import MyPackage.NoAvailableSlotsForConnectionException;
import MyPackage.ServerListenerThread;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  
  //The 4 connections
  private ArrayList<ExternalRouterConnection> neighbors = new ArrayList<ExternalRouterConnection>();
  
  ServerListenerThread listener;

  public Router(Configuration config) throws IOException
  {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    lsd = new LinkStateDatabase(rd);
    
    MyRouterSetup();
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  @SuppressWarnings("unused")
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight)
  {
	  ExternalRouterConnection createdConnection = null;
	  try
	  {
		Socket connection = new Socket(InetAddress.getByName(processIP), processPort);
		createdConnection = new ExternalRouterConnection(this, connection, simulatedIP);
		createdConnection.start();
	  }
	  catch (IOException e)
	  {
		  if(neighbors.size() > 0)
		  {
			  neighbors.remove(neighbors.size()-1);
		  }
		  System.out.println("Attach Failed");
	  }
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart()
  {
	  for(ExternalRouterConnection externalConnection : neighbors)
	  {
		  externalConnection.setRouterStatus(RouterStatus.INIT);
		  externalConnection.sendHELLOmessage();
	  }
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors()
  {
	  if(neighbors.size() == 0)
	  {
		  System.out.println("There are no neighbors.");
	  }
	  else
	  {
		  for (ExternalRouterConnection externalRouterConnection : neighbors)
		  {
			  externalRouterConnection.printConnectionIP();
		  }
	  }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit()
  {
	 //we dont want to have multiple theads working with the neighbors arraylist
	 ArrayList<ExternalRouterConnection> connectionsToRemove = new ArrayList<ExternalRouterConnection>();
	  
	 for (ExternalRouterConnection externalRouterConnection : neighbors)
	 {
		 connectionsToRemove.add(externalRouterConnection);
	 }
	 
	  
	 for (ExternalRouterConnection externalRouterConnection : connectionsToRemove)
	 {
		 externalRouterConnection.interrupt();
		 try
		 {
			 externalRouterConnection.join();
		 }
		 catch (InterruptedException e)
		 {
			 //do nothing
		 }
	  }
	 
	  neighbors.clear();
	  
	  listener.interrupt();
	  try
	  {
		listener.join();
	  }
	  catch (InterruptedException e)
	  {
		//do nothing
	  }
  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
          break;
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
          System.out.println("Invalid Command.");
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  
  private void MyRouterSetup() throws IOException
  {
	  int randomPortNumber = (int)(Math.random()*25000)+2000;
	  
	  listener = new ServerListenerThread(this, randomPortNumber);
	  listener.start();
	  
	  System.out.println("Router port number: "+randomPortNumber);
	  System.out.println("Router simlated IP address: "+rd.simulatedIPAddress);
  }
  
  
  public void AddNeighbor(ExternalRouterConnection connection) throws NoAvailableSlotsForConnectionException
  {
	  if(neighbors.size() < 4)
	  {
		  neighbors.add(connection);
	  }
	  else
	  {
		  throw new NoAvailableSlotsForConnectionException();
	  }
  }
  
  
  public void RemoveNeighbor(ExternalRouterConnection connection)
  {
	  neighbors.remove(connection);
  }
  
  
  public String getSimulatedIP()
  {
	  return rd.simulatedIPAddress;
  }

}
