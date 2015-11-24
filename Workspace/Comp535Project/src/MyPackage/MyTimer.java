package MyPackage;

import socs.network.node.Router;

public class MyTimer extends Thread
{
	Router router = null;
	
	int sendMessageTime = 3;
	int checkTime = 5;
	
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
			
			sendMessageTime--;
			checkTime--;
		
			if(sendMessageTime == 0)
			{
				sendMessageTime = 3;
				router.sendIsAliveMessages();
			}
		
			if(checkTime == 0)
			{
				checkTime = 5;
				router.checkIsAlive();
			}
		}
	}
}
