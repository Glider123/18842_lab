import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.LinkedList;

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
	
	////////////////////////////////////comment out////////////////////////////////////////
	int size;
	int index;
	///////////////////////////////////////////////////////////////////////////////////////
	
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
		
		constructTSVector(groupMap.get(MEMBERS));
	}
	
	public LinkedList<TimeStampedMessage> getHoldBackQueue()
	{
		return this.holdBackQueue;
	}
	
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
	
	public ArrayList<TimeStampedMessage> getReceiveLst()
	{
		return this.receiveLst;
	}
	
	public PriorityQueue<TimeStampedMessage> getReceiveQueue()
	{
		return this.receiveQueue;
	}
	
	public ClockService getClockService()
	{
		return this.vectorClk;
	}
	
	
	public void addMember(String mem)
	{
		members.add(mem);
	}
	
	public ArrayList<String> getMembers()
	{
		return this.members;
	}
	
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
