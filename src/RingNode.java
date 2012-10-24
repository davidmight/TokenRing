/**
 * @author david
 */


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * An individual node on the token ring network.
 * Implements runnable as it is meant to be executed by a thread.
 * Once it is ran it will first wait to receive a packet (unless it is 
 * the monitor, in that case it will inject the token).
 * When it receives a packet it unpacks it into the DataFrame class
 * and examines it. 
 * If it is the token it moves it on unless it has a message
 * to send.
 * Otherwise it checks if it is meant for it, if it isn't it just sends 
 * it on.
 * The node keeps on receiving frames while the boolean receiving is true.
 *
 */

public class RingNode implements Runnable {
	
	public static final int MAX_BUFFER = 1024;
	public static final int TOKEN_BUFFER = 6;
	
	int id;
	int port;
	InetAddress address;
	DatagramSocket socket = null;
	int nextNodePort;
	boolean monitor = false;
	boolean receiving = true;
	boolean hasMessageToSend = false;
	String message = "";
	int destin;
	Timer timer = new Timer();
	
	RingNode(int id, int port){
		try {
			this.id = id;
			this.port = port;
			socket = new DatagramSocket(port);
			address = socket.getLocalAddress();
			
		} catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void setMonitor(){
		monitor = true;
	}
	
	public int getSocketPort(){
		return port;
	}
	
	public void giveMessageToSend(String msg, int destination){
		hasMessageToSend = true;
		message = msg;
		destin = destination;
	}
	
	public void sendMessage(){
		DataFrame frame = new DataFrame(destin, port, message);
		frame.setTokenBit(true);
		hasMessageToSend = false;
		sendFrame(frame);
	}
	
	//send a frame with the DatagramPacket through the socket
	public void sendFrame(DataFrame frame){
			
		DatagramPacket packet = null;
		byte[] buf = new byte[MAX_BUFFER];
		
		//set the monitor bit to false if it isn't the monitor otherwise set it to true
		if(!monitor){frame.setMonitorBit(false);}else{frame.setMonitorBit(true);}
			
		try{
			ByteArrayOutputStream fis = new ByteArrayOutputStream();
			ObjectOutputStream is = new ObjectOutputStream(fis);
			is.writeObject(frame);
			is.flush();
			buf = fis.toByteArray();
			packet = new DatagramPacket(buf, buf.length, 
					address, nextNodePort);
			socket.send(packet);
		}catch(IOException ex){
			ex.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	//Make a new token frame send it on
	public void makeToken(){
		DataFrame tokenFrame = new DataFrame();
		tokenFrame.setAsToken();
		sendFrame(tokenFrame);
	}
	
	public void makeAmp(){
		DataFrame ampFrame = new DataFrame();
		ampFrame.setAsAmp();
		sendFrame(ampFrame);
	}
	
	public void makeCt(){
		DataFrame ctFrame = new DataFrame(port);
		sendFrame(ctFrame);
	}
	
	//stop the node from receiving
	public void switchReceiving(){
		
		try {
			receiving = !receiving;
			//socket.close();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	int checkFrame(DataFrame frame){
		if(frame.token == true){
			return 0;
		}else if(frame.amp == true){
			return 1;
		}else if(frame.ct == true){
			return 2;
		}
		return 3;
	}
	
	public void run(){
		DatagramPacket packet = null;
		byte[] buffer;
		
		try {
			if(monitor){makeToken();makeAmp();}
			System.out.println("Node " + id + " has started");
			while(receiving){
				buffer = new byte[MAX_BUFFER];
				packet = new DatagramPacket(buffer, buffer.length);
				socket.setSoTimeout(5000);
				socket.receive(packet);
				buffer = packet.getData();
				
				ByteArrayInputStream fis = new ByteArrayInputStream(buffer);
				ObjectInputStream in = new ObjectInputStream(fis);
				DataFrame frame = (DataFrame)in.readObject();
				
				switch(checkFrame(frame)){
					case 0:
						//if(monitor){System.out.println("Token has passed the monitor.");}
						if(hasMessageToSend){
							sendMessage();
						}else{sendFrame(frame);}
						break;
						
					case 1:
						if(monitor){makeAmp();
						}else{
							//timer.cancel();
							//timer.schedule(new ClaimTask(), 10000);
							//set Timeout
							//if reaches certain time there is no longer a monitor
							//send a claim token frame
							//makeCt();
							sendFrame(frame);
						}
						break;
						
					//If the claim token's claim is greater than your own
					//let it otherwise replace it
					//if it's yours become monitor
					//purge ring
					case 2:
						if(frame.getSource() > port){
							sendFrame(frame);
						}else if(frame.getSource() == port){
							monitor = true;
						}else{frame.source_addr = port; sendFrame(frame);}
						break;
						
					case 3:
						//If has passed the monitor twice an error has occured
						if(!monitor ||
							(monitor && frame.monitor != true)){
							//if(monitor){System.out.println("Data Link Frame has passed the monitor.");}
							if(frame.getDes() == port){
								//The node has received a confirmation that previous message it sent has been received.
								if(frame.getFrameStatus()){
									System.out.println("Node " + id + " acknowledges their frame reached the destination");
									System.out.println("Node " + id + " has released the token");
									makeToken();
								}else{
									//The message is meant for this node, acknowledge and send it back
									System.out.println("Node " + id + " has received the following message.");
									System.out.println("Received: " + frame.getMsg());
									System.out.println("From: "+packet.getAddress()+":"+frame.getSource());
									frame.swapAddresses();
									frame.acknowledge();
									sendFrame(frame);
								}
							}else{sendFrame(frame);}
						}else{
							System.out.println("The frame has passed the monitor twice.");
							makeToken();
						}
						break;
				}
			}
		//If after 5 seconds nothing has been received, timeout
		//If the node is the monitor it can reinsert the token
		}catch (SocketTimeoutException E) {
			System.out.println("ST: Hit timeout !");
			if(monitor){makeToken();}
			run();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
	}
	class ClaimTask extends TimerTask {
		public void run(){
			makeCt();
			timer.cancel();
		}
	}
}
