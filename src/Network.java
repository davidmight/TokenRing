/**
 * @author david
 */


import java.io.*;
import java.util.*; 
import java.io.IOException;

/**
 * 
 * This class builds and then controls the token ring network.
 * It does this by keeping a arraylist of both instances of RingNode
 * and the threads that run them.
 * After that it makes use of a terminal based menu to control
 * the network
 *
 */

public class Network {
	public static final int MAX_BUFFER = 1024;
	
	private static BufferedReader inputStream = 
		new BufferedReader(new InputStreamReader(System.in));
	
	public static void main (String args[]){
		int numNodes = -1;
		int ports = 9001;
		int choice = -1;
		
		System.out.println("Please enter the number of nodes:");
		numNodes = readNonNegativeInt();
		
		ArrayList<Thread> nodesThreads = new ArrayList<Thread>();
		ArrayList<RingNode> nodes = new ArrayList<RingNode>();
		RingNode temp;
		for(int j=0; j<numNodes; j++){
			temp = new RingNode(j+1, ports);
			nodes.add(temp);
			ports++;
		}
		//initialize all the addresses between the ports 
		initRingConn(nodes, numNodes);
		Thread r;
		//assign a thread to each node
		for(int i=0; i<numNodes; i++){
			r = new Thread(nodes.get(i));
			nodesThreads.add(r);
		}
		
		for(int k=0; k<numNodes; k++){
			nodesThreads.get(k).start();
		}
		
		int senderid, receiverid, deleteid;
		String msg;
		do{
			System.out.println("Menu:");
			System.out.println("(0) Send a message.");
			System.out.println("(1) Add a node.");
			System.out.println("(2) Delete a node.");
			System.out.println("(3) Quit.");
			System.out.println();
			System.out.println("Please enter menu choice: ");
			//Calls a method that will take in a integer in the console
			choice = readNonNegativeInt();
			switch (choice){
				case 0:
					System.out.println("Please enter the sender's id:");
					senderid = readNonNegativeInt();
					System.out.println("Please enter the receiver's id:");
					receiverid = readNonNegativeInt();
					System.out.println("Please enter the message:");
					msg = readString();
					
					nodes.get(senderid-1).giveMessageToSend(msg, (nodes.get(receiverid-1).getSocketPort()));
					break;
				case 1:
					nodes = addNode(nodes, numNodes, ports);
					numNodes++;
					ports++;
					System.out.println(numNodes);
					break;
				case 2:
					System.out.println("Please enter the node's id:");
					deleteid = readNonNegativeInt();
					deleteNode(deleteid, nodes, numNodes);
					numNodes--;
					break;
				case 3:
					for(int l=0; l<numNodes; l++){
						nodes.get(l).switchReceiving();
					}
					System.out.println("Program quitting.");
					break;
				default:
					System.out.println("Not a valid menu choice.");
					System.out.println();
					break;
			}
		}while(choice != 1);
		
	}
	
	public static ArrayList<RingNode> addNode(ArrayList<RingNode> nodes, int numNodes, int port){
		RingNode lastNode, next, node;
		Thread newNode;
		
		lastNode = nodes.get(numNodes-1);
		next = new RingNode(lastNode.id+1, port);
		next.nextNodePort = lastNode.nextNodePort;
		lastNode.nextNodePort = next.getSocketPort();
		//System.out.println(lastNode.nextNodePort);
		//System.out.println(next.nextNodePort);
		nodes.add(next);
		newNode = new Thread(nodes.get(numNodes));
		
		for(int i=0; i<numNodes+1; i++){
			node = nodes.get(i);
			System.out.println(node.nextNodePort);
			System.out.println(node.port);
			System.out.println(node.id);
		}
		
		newNode.start();
		
		return nodes;
	}
	
	public static void deleteNode(int nodeid, ArrayList<RingNode> nodes, int numNodes){
		nodes.get(nodeid-2).nextNodePort = nodes.get(nodeid-1).nextNodePort;
		nodes.get(nodeid-1).switchReceiving();
		nodes.remove(nodeid);
	}
	
	public static void initRingConn(ArrayList<RingNode> nodes, int numNodes){
		RingNode node = null;
		RingNode nextNode = null;
		
		nodes.get(0).setMonitor();
		node = nodes.get(numNodes-1);
		nextNode = nodes.get(0);
		node.nextNodePort = nextNode.getSocketPort();
		node = nextNode;
		for(int i=1; i<numNodes; i++){
			nextNode = nodes.get(i);
			node.nextNodePort = nextNode.getSocketPort();
			node = nextNode;
		}
	}
	
	
	public static int readNonNegativeInt(){
		int val = readInt();
		while (val < 0) {
			System.out.println("Not a non-negative integer: " + val);
			System.out.println("Enter a non-negative integer:");
			val = readInt();
		} // end of while
		return val;
	}
	
	//Takes in a integer value in the console
	public static int readInt(){

		String line = null;
		int val = 0;
		while(true){
			try {
				line = inputStream.readLine(); // can throw an IOException
				val = Integer.parseInt(line);  // can throw a NumberFormatException
				break; // break out of enclosing while 
			}
			catch (IOException e) {
				System.err.println("Unexpected IO ERROR: " + e);
				System.exit(1); // exit program as an unexpected error occurred
			}
			catch (NumberFormatException e) {
				System.err.println("Not a valid integer: " + line);
			}
		} // end of while
		return val; // break will cause a jump to here
	}
	
	public static String readString(){
		String line = "";
		while(true){
			try{
				line = inputStream.readLine();
				break;
			}
			catch (IOException e){
				System.err.println("Unexpected IO ERROR: " + e);
				System.exit(1);
			}
		}
		return line;
	}
}
