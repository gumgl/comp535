package socs.network;

import java.io.IOException;

import socs.network.node.Router;
import socs.network.util.Configuration;

public class Main {

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("usage: program conf_path");
      System.exit(1);
    }
    Router r;
	try
	{
		r = new Router(new Configuration(args[0]));
		r.terminal();
	}
	catch (IOException e)
	{
		System.out.println("Error creating server: "+e.getMessage());
		System.out.println("Your router was destroyed destroyed");
	}
	
	System.out.println("Router Destroyed");
  }
}
