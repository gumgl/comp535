package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import MyPackage.ExternalRouterConnection;
import MyPackage.MyTimer;
import MyPackage.NoAvailableSlotsForConnectionException;
import MyPackage.ServerListenerThread;


public class Router {
	
	public static final boolean ALIVE_DEBUG = false;

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  
  //The 4 connections
  private ArrayList<ExternalRouterConnection> neighbors = new ArrayList<ExternalRouterConnection>();
  
  ServerListenerThread listener;
  
  int connectionsToFinish = 0;
  
  MyTimer myTimer;

  public Router(Configuration config) throws IOException
  {
	  synchronized (this) {
		rd.simulatedIPAddress = config.getString("socs.network.router.ip");
		lsd = new LinkStateDatabase(rd);
		
		MyRouterSetup();
		myTimer = new MyTimer(this);
		myTimer.start();
	  }
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP)
  {
	  synchronized (this) {
		  System.out.println(lsd.getShortestPath(destinationIP));
	  }
  }
  
  private void processDetectDebug(String destinationIP)
  {
	  synchronized (this) {
		  System.out.println(lsd.getShortestPathDebug(destinationIP));
	  }
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
 * @throws IOException 
   */
  private void processDisconnect(short portNumber) throws IOException
  {
	  synchronized (this) {
		  for(ExternalRouterConnection connection : neighbors)
		  {
			  if(connection.routerConnectionID == portNumber)
			  {
				  disconnect(connection);
				  break;
			  }
		  }
	  }
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
	  synchronized (this) {
		  ExternalRouterConnection createdConnection = null;
		  try
		  {
			Socket connection = new Socket(InetAddress.getByName(processIP), processPort);
			createdConnection = new ExternalRouterConnection(this, connection, simulatedIP, weight, nextConnectionID());
			createdConnection.start();
			
			lsd.getLSA(this.getSimulatedIP()).addLink(simulatedIP, processPort, weight);
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
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart()
  {
	  synchronized (this) {
		  for(ExternalRouterConnection externalConnection : neighbors)
		  {
			  if(externalConnection.myConnection)
			  {
				  connectionsToFinish++;
				  externalConnection.setRouterStatus(RouterStatus.INIT);
				  externalConnection.sendHELLOmessage();
			  }
		  }
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
	  
	  synchronized (this)
	  {
		int size = neighbors.size();
	  	this.processAttach(processIP, processPort, simulatedIP, weight);
	  	if(neighbors.size() > size)
	  	{
		  	neighbors.get(size).setRouterStatus(RouterStatus.INIT);
		  	neighbors.get(size).sendHELLOmessage();
	  	}
	  }

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors()
  {
	  synchronized (this) {
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
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit()
  {
	 System.exit(0);
  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      while (true) {
        System.out.print("G>> ");
        System.out.flush();
        String command = br.readLine();
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("detectDebug ")) {
              String[] cmdLine = command.split(" ");
              processDetectDebug(cmdLine[1]);
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
        } else if (command.startsWith("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else if (command.equals("printLSD")) {
            printLSA();
        } else {
          //invalid command
          System.out.println("Invalid Command.");
        }
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  
  private void MyRouterSetup() throws IOException
  {
	  synchronized (this) {
		  int randomPortNumber = (int)(Math.random()*25000)+2000;
		  
		  listener = new ServerListenerThread(this, randomPortNumber);
		  listener.start();
		  
		  System.out.println("Router port number: "+randomPortNumber);
		  System.out.println("Router simlated IP address: "+rd.simulatedIPAddress);
	  }
  }
  
  
  public void AddNeighbor(ExternalRouterConnection connection) throws NoAvailableSlotsForConnectionException
  {
	  synchronized (this) {
		  if(neighbors.size() < 4)
		  {
			  neighbors.add(connection);
			  lsd.getLSA(getSimulatedIP()).lsaSeqNumber++;
		  }
		  else
		  {
			  throw new NoAvailableSlotsForConnectionException();
		  }
	  }
  }
  
  
  public void RemoveNeighbor(ExternalRouterConnection connection)
  {
	  synchronized (this) {
		  neighbors.remove(connection);
	  }
  }
  
  
  public String getSimulatedIP()
  {
	  return rd.simulatedIPAddress;
  }
  
  public void fillLSAarray(Vector<LSA> lsaArray)
  {
	  synchronized (this) {
		  lsd.fillLSAarray(lsaArray);
	  }
  }
  
  public boolean updateLinkStateDatabase(Vector<LSA> lsaArray)
  {
	  synchronized(this)
	  {
		  return lsd.updateDatabase(lsaArray);
	  }
  }
  
  public void addLSAlink(String id, int connectionWeight)
  {
	  synchronized (this) {
		  LSA temp = lsd.getLSA(getSimulatedIP());
		  temp.links.add(new LinkDescription(id, 0, connectionWeight));
		  temp.lsaSeqNumber++;
	  }
  }
  
  public int nextConnectionID()
  {
	  synchronized (this) {
		  boolean[] ports = new boolean[4];
		  for(ExternalRouterConnection connection : neighbors)
		  {
			  ports[connection.routerConnectionID] = true;
		  }
		  
		  for(int i=0; i<4; i++)
		  {
			  if(!ports[i])
			  {
				  return i;
			  }
		  }
		  
		  return 0;
	  }
  }
  
  
  public void broadCastLSAupdate(int senderID) throws IOException
  {
	  synchronized (this) {
		  for(ExternalRouterConnection connection : neighbors)
		  {
			  if(connection.myRouterStatus != null && connection.routerConnectionID != senderID)
			  {
				  connection.sendLSAupdate();
			  }
		  }
	  }
  }
  
  public void printLSA()
  {
	  synchronized (this) {
		  System.out.println("LSA database:");
		  for(String lsaID: lsd.lsaList)
		  {
			  System.out.println(lsaID+":"+lsd._store.get(lsaID).links.toString());
		  }
		  System.out.println("count: " + lsd.lsaList.size());
	  }
  }
  
  public void connectionFinished() throws IOException
  {
	  synchronized (this) {
		  connectionsToFinish--;
		  if(connectionsToFinish == 0)
		  {
			  broadCastLSAupdate(0);
		  }
	  }
  }
  
  public void disconnect(ExternalRouterConnection connection) throws IOException
  {
	  synchronized (this) {
		  connection.sendDisconnectMessage();
		  connection.disconnect();
		  neighbors.remove(connection);
		  lsd.removeConnection(this.getSimulatedIP(), connection.getIPaddress());
		  broadCastLSAupdate(-1);
	  }
  }
  
  public void removeLSDConnection(String ipAddress)
  {
	  synchronized (this) {
		  lsd.removeConnection(this.getSimulatedIP(), ipAddress);
	  }
  }
  
  public void sendAliveMessages()
  {
	  synchronized (this) {
		  if (ALIVE_DEBUG && neighbors.size() > 0)
			  System.out.print("X");
		  for(ExternalRouterConnection connection : neighbors)
		  {
			  connection.sendAliveMessage();
		  }
	  }
  }
  
  public void checkIsAlive()
  {
	  synchronized (this) {
		  ArrayList<ExternalRouterConnection> toRemove = new ArrayList<ExternalRouterConnection>();
		  // Because we cannot remove during a for each loop
		  
		  for(ExternalRouterConnection connection : neighbors)
		  {
			  if (System.currentTimeMillis() - connection.lastAliveMessage > MyTimer.CHECK_INTERVAL * 1000)
			  {
				  String ip = connection.getIPaddress();
				  if(ip == null)
				  {
					  System.out.println("Lost connection with some router.");
				  }
				  else
				  {
					  System.out.println("Connection with "+ip+" lost due to time out.");
					  lsd.removeConnection(this.getSimulatedIP(), connection.getIPaddress());
				  }
				  toRemove.add(connection);
			  }
		  }
		  
		  if(!toRemove.isEmpty())
		  {
			  for(ExternalRouterConnection connection : toRemove)
			  {
				  neighbors.remove(connection);
			  }
				  try {
					broadCastLSAupdate(-1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		  }
	  }
  }
}
