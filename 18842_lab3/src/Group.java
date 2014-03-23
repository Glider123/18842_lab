import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.LinkedList;
import java.util.Queue;

public class Group {
	private static final String NAME = "name";
	private static final String MEMBERS = "members";
	
	private String groupName;
	private ArrayList<String> members;
	private ClockService vectorClk;
	private String procName;
	private ArrayList<TimeStampedMessage> receiveLst;
	private PriorityQueue<TimeStampedMessage> receiveQueue;
	private LinkedList<TimeStampedMessage> holdBackQueue;
	private Queue<TimeStampedMessage> ackQueue;
	

	int size;
	int index;

	// constructor
	public Group(HashMap<String, ArrayList<String>> groupMap, String procName)
	{

		Object groupNameObj = (Object)groupMap.get(NAME);
		String groupName = (String) groupNameObj;
		this.groupName = groupName;
		this.procName = procName;
		this.members = new ArrayList<String>();
		this.receiveLst = new ArrayList<TimeStampedMessage>();
		this.receiveQueue = new PriorityQueue<TimeStampedMessage>();
		this.holdBackQueue = new LinkedList<TimeStampedMessage>();
		this.ackQueue = new LinkedList<TimeStampedMessage>();
		
		constructTSVector(groupMap.get(MEMBERS));
	}
	
	// get the holdback queue
	public LinkedList<TimeStampedMessage> getHoldBackQueue()
	{
		return this.holdBackQueue;
	}
	
	public int getSize()
	{
		return this.size;
	}
	
	public void addACKQueue(TimeStampedMessage msg)
	{
		ackQueue.add(msg);
	}
	
	public Queue<TimeStampedMessage> getACKQueue()
	{
		return this.ackQueue;
	}
	
	// contruct the vector clock for the group
	private void constructTSVector(ArrayList<String> groupMembers)
	{
		int size = groupMembers.size();
		int index = 0;
		
		for(int count = 0; count < groupMembers.size(); count++)
		{
			if(groupMembers.get(count).equals(procName))
				index = count;
		}
		
		this.vectorClk = new Vector(size, index);
		this.size = size;
		this.index = index;
	}
	
	public int getMemIndex(String mem)
	{
		if(mem == null || mem.length() == 0)
			return -1;
		
		for(int count = 0; count < members.size(); count++)
		{
			if(members.get(count).equals(mem))
				return count;
		}
		
		return -1;
	}
	
	// get the receive list
	public ArrayList<TimeStampedMessage> getReceiveLst()
	{
		return this.receiveLst;
	}
	
	
	// get the priority queue
	public PriorityQueue<TimeStampedMessage> getReceiveQueue()
	{
		return this.receiveQueue;
	}
	
	// get the clock Service
	public ClockService getClockService()
	{
		return this.vectorClk;
	}
	
	// add the member to the group
	public void addMember(String mem)
	{
		members.add(mem);
	}
	
	// get the members
	public ArrayList<String> getMembers()
	{
		return this.members;
	}
	
	// get the group name
	public String getGroupName()
	{
		return this.groupName;
	}
	
	public String toString()
	{
		return members.toString() + "\n"
				+ "size\t: " + size + "\n"
				+ "index\t: " + index + "\n"
				+ "procName\t: " + procName + "\n";
	}
}
