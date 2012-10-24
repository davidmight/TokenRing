/**
 * @author david
 */

import java.io.Serializable;

/**
 * 
 * A class which represents a data frame on the
 * token ring network.It is serializable which means I can
 * flatten it and reuse later. This essentially means that the object
 * exists beyond the lifetime of the virtual machine.
 *
 */

public class DataFrame implements Serializable {
	private static final long serialVersionUID = -7309788301614787772L;
	//true if it is a token frame
	boolean token = false;
	//true if it is a amp frame
	boolean amp = false;
	//true if it is a Claim token frame
	boolean ct = false;
	//true if the frame has the token frame
	boolean hasToken = false;
	//true if the frame has passed the monitor station
	boolean monitor = false;
	//true if a frame reached its intended recipient
	boolean frameStatus = false;
	//Destination port of the frame 
	int destination_addr;
	//Source port of the frame
	int source_addr;
	//Message the frame is transmitting (if any)
	String message;
	
	//create either a Token or amp frame
	public DataFrame(){
		
	}
	
	//create a claim token frame
	public DataFrame(int source){
		ct = true;
		source_addr = source;
	}
	
	//create a data frame
	public DataFrame(int destination, int source, String msg){
		this.destination_addr = destination;
		this.source_addr = source;
		this.message = msg;
	}
	
	public void setAsToken(){
		token = true;
	}
	
	public void setAsAmp(){
		amp = true;
	}
	
	//swap the addresses when acknowledging a frame
	public void swapAddresses(){
		int temp;
		temp = destination_addr;
		destination_addr = source_addr;
		source_addr = temp;
		
	}
	
	public boolean getFrameStatus(){
		return frameStatus;
	}
	
	public void acknowledge(){
		frameStatus = true;
	}
	
	public void setTokenBit(boolean con){
		hasToken = con;
	}
	
	public void setMonitorBit(boolean con){
		monitor = con;
	}
	
	public int getDes(){
		return this.destination_addr;
	}
	
	public int getSource(){
		return this.source_addr;
	}
	
	public String getMsg(){
		return this.message;
	}
}
