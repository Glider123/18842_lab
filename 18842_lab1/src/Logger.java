import java.util.*;
import java.util.concurrent.locks.*;

public class Logger {
	private static final String CLOCK_TYPE = "clockType";
	
	private String configFileName;
	private String localHostName;
	
	private ArrayList<TimeStampedMessage> recvBuffer;
	private MessagePasser msgPasser;
	private Lock recvBufferLock;
	
	public Logger (String configFileName, String localHostName) {
		this.configFileName = configFileName;
		this.localHostName = localHostName;
		recvBuffer = new ArrayList<TimeStampedMessage>();
		recvBufferLock = new ReentrantLock();
	}
	
	public void run() {
		// create a new message passer
		msgPasser = new MessagePasser(configFileName, localHostName);
		
		// read clock type from config file
		String clockTp = null;
		try
		{
			clockTp = msgPasser.getYamlMap().get(CLOCK_TYPE).get(0).get("type");
		}
		catch(Exception e)
		{
			System.out.println("cannot find clock type");
			System.exit(1);
		}
		
		// initiate clock service
		ClockService vectorClk;
		if(clockTp.equals("logical"))
			vectorClk = new Logical(msgPasser);
		else
			vectorClk = new Vector(msgPasser);
		
		msgPasser.setClockService(vectorClk);
		
		// receive messages
		while (true) {
			// lock receive buffer
			recvBufferLock.lock();
			
			// receive message from message passer and store it in receive buffer
			Message msg = msgPasser.receive();
			
			// add none null TimeStampedMessage to the ArrayList
			if(msg != null) {
				Object data = msg.get_data();
				if (data != null) {
					recvBuffer.add((TimeStampedMessage)data);
				}
			}
			
			// unlock receive buffer
			recvBufferLock.unlock();
		}
	}
	
	public void print() {
		// lock receive buffer
		recvBufferLock.lock();
		
		// get all messages in receive buffer
		ArrayList<TimeStampedMessage> msgs = new ArrayList<TimeStampedMessage>(recvBuffer);
		recvBuffer.clear();
		
		// unlock receive buffer
		recvBufferLock.unlock();
		
		// compare and print all messages
		if (msgs.isEmpty()) {
			System.out.println("No message received!");
			return ;
		}
		if (msgs.get(0).getTimeStampType() == TimeStamp.LOGICAL) {
			printLogicalMessage(msgs);
		} else if (msgs.get(0).getTimeStampType() == TimeStamp.VECTOR){
			printVectorMessage(msgs);
		} else {
			System.out.println("Invalid clock type! Unable to compare!");
			printAllMessages(msgs);
			return ;
		}
	}
	
	private void printLogicalMessage(ArrayList<TimeStampedMessage> msgs) {
		// sort all messages
		Collections.sort(msgs);
		
		// print all messages
		printAllMessages(msgs);
		
		// print all message's relationship, for logical clock, it is inaccurate
		System.out.println("Relationship (Using logical clock, not accurate):");
		for (int i = 0; i < msgs.size(); i++) {
			for (int j = i + 1; j < msgs.size(); j++) {
				int result = msgs.get(i).compareTo(msgs.get(j));
				if (result == TimeStamp.LOGICAL_LESSTHAN) {
					System.out.println("message " + i + " -> " + "message " + j);
				} else if (result == TimeStamp.LOGICAL_GREATERTHAN) {
					System.out.println("message " + i + " <- " + "message " + j);
				} else if (result == TimeStamp.LOGICAL_EQUAL) {
					System.out.println("message " + i + " || " + "message " + j);
				} else {
					continue;
				}
			}
		}
		System.out.println();
	}
	
	private void printVectorMessage(ArrayList<TimeStampedMessage> msgs) {
		// print all messages
		printAllMessages(msgs);
		
		// print all message's relationship
		System.out.println("Relationship (Using vector clock):");
		for (int i = 0; i < msgs.size(); i++) {
			for (int j = i + 1; j < msgs.size(); j++) {
				int result = msgs.get(i).compareTo(msgs.get(j));
				if (result == TimeStamp.VECTOR_LESSTHAN) {
					System.out.println("message " + i + " -> " + "message " + j);
				} else if (result == TimeStamp.VECTOR_GREATERTHAN) {
					System.out.println("message " + i + " <- " + "message " + j);
				} else if (result == TimeStamp.VECTOR_UNEQUAL) {
					System.out.println("message " + i + " || " + "message " + j);
				} else {
					continue;
				}
			}
		}
		System.out.println();
	}
	
	private void printAllMessages(ArrayList<TimeStampedMessage> msgs) {
		System.out.println("All Logged Messages:");
		for (TimeStampedMessage msg: msgs) {
			System.out.println(msg.toString());
		}
	}
}
