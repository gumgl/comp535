package MyPackage;

import socs.network.node.Router;

public class MyTimer extends Thread
{
	public final static int CHECK_INTERVAL = 5; // in seconds
	public final static int SEND_INTERVAL = 3; // in seconds
	Router router = null;
	
	int timer = 0;
	
	public MyTimer(Router r)
	{
		router = r;
	}
	
	@Override
	public void run()
	{
		while(true)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			timer ++;
			if(timer % SEND_INTERVAL == 0)
			{
				synchronized (router) {
					router.sendAliveMessages();
				}
			}
		
			if(timer % CHECK_INTERVAL == 0)
			{
				synchronized (router) {
					router.checkIsAlive();
				}
			}
		}
	}
}
