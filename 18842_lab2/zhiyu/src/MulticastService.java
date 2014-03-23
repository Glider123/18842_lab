import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.yaml.snakeyaml.Yaml;

public class MulticastService {

	private static final String GROUP = "groups";
	private static final String NAME = "name";
	private static final String MEMBERS = "members";
	private static final int VECTOR = 1;
	
	
	private MessagePasser msgPasser;
	private HashMap<String, Group> groupMap;
	private String procName;
	private int seqNumber;
	private int crashPoint;
	private Lock revLock;
	
	
	class DeliverService extends Thread{
		public void run()
		{
			while(true)
			{
				TimeStampedMessage tsMsg = (TimeStampedMessage) msgPasser.receive();
				
				if(tsMsg != null)
				{
					TimeStampedMessage oriMsg = (TimeStampedMessage) tsMsg.get_data();
					String groupName = oriMsg.getGroupName();
					Group grp = groupMap.get(groupName);
					

					
					// flooding Algorithm
					if(!grp.getReceiveLst().contains(oriMsg))
					{
						grp.getReceiveLst().add(oriMsg);
						
						if(!oriMsg.get_source().equals(procName))
						{
							// multicast
							TimeStampedMessage wrapMsg = createWrapMessage(oriMsg);
							try 
							{
								multicast(wrapMsg, groupName);
							}
							catch (IOException e) 
							{
								e.printStackTrace();
							}
						}
						// Causal ordering using vector timestamps
						revLock.lock();
						grp.getHoldBackQueue().add(oriMsg);
						deliverMessage(grp, oriMsg);
						revLock.unlock();
						//grp.getReceiveQueue().add(oriMsg);
					}
				}
			}
		}
		
		private void deliverMessage(Group grp, TimeStampedMessage msg)
		{
			String src = msg.get_source();
			String groupName = msg.getGroupName();
			int index = getSrcIndex(src, groupName);
			if(index == -1)
			{
				grp.getReceiveQueue().add(msg);
				grp.getHoldBackQueue().remove(msg);
				System.out.println("this happens in index = -1");
				return;
			}
			if(src.equals(procName))
			{
				grp.getReceiveQueue().add(msg);
				grp.getHoldBackQueue().remove(msg);
				return;
			}
			int[] iVector = grp.getClockService().getVectorTimestamp();
			
			// used remove list to calm down concurrent modification exception
			ArrayList<TimeStampedMessage> removeLst = new ArrayList<TimeStampedMessage>();
			
					
			for(TimeStampedMessage jMsg : grp.getHoldBackQueue())
			{
				int[] jVector = jMsg.getVectorTS();
				
				if(shouldDeliver(iVector, jVector, index))
				{
					//grp.getHoldBackQueue().remove(jMsg);
					grp.getReceiveQueue().add(jMsg);
					//System.out.println(jMsg);
					removeLst.add(jMsg);
					
					//update timestamp
					grp.getClockService().getVectorTimestamp()[index] += 1;
					System.out.println("this happens");
					System.out.println(grp.getClockService().getVectorTimestamp());
				}
			}
			
			// remove the msg from the holdbackqueue
			for(TimeStampedMessage rmMsg : removeLst)
			{
				grp.getHoldBackQueue().remove(rmMsg);
			}
		}
		
		private boolean shouldDeliver(int[] iVector, int[] jVector, int index)
		{
			if(iVector == null && jVector == null)
				return true;
			
			if(iVector == null || jVector == null)
				return false;
			
			if(iVector.length != jVector.length)
				return false;
			
			for(int count = 0; count < iVector.length; count++)
			{
				if(count == index)
				{
					if(iVector[index] + 1 != jVector[index])
						return false;
				}
				else if (iVector[count] < jVector[count])
					return false;
			}
			
			return true;
		}
		
		private int getSrcIndex(String src, String groupName)
		{
			ArrayList<String> members = groupMap.get(groupName).getMembers();
			System.out.println("src is " + src);
			
			for(int count = 0; count < members.size(); count++)
			{
				if(members.get(count).equals(src))
					return count;
				System.out.println(members.get(count));
			}
			return -1;
		}
	}
	
	public MulticastService(String configFile, String procName)
	{
		this.groupMap = new HashMap<String, Group>();
		this.procName = procName;
		initialGroupMap(configFile, procName);
		this.msgPasser = new MessagePasser(configFile, procName);
		this.crashPoint = Integer.MAX_VALUE;
		DeliverService deliverWorker = new DeliverService();
		this.revLock = new ReentrantLock();
		deliverWorker.start();
	}
	
	public void setCrashPoint(int crashPoint)
	{
		this.crashPoint = crashPoint;
	}
	
	public void multicastSend(TimeStampedMessage tsMsg) throws IOException
	{
		String groupName = tsMsg.getGroupName();
		
		if(!groupMap.containsKey(groupName))
		{
			System.err.println("proc " + procName + "does not include in the group " + groupName);
			System.exit(1);
		}
		
		Group targetGroup = groupMap.get(groupName);
		ClockService groupClkSvc = targetGroup.getClockService();
		groupClkSvc.updateTimestamp();
		tsMsg.set_seqNum(seqNumber++);
		
		tsMsg.getTimeStamp().setTimeStamp(VECTOR, groupClkSvc.getVectorTimestamp(), 0);
		
		TimeStampedMessage wrapMsg = createWrapMessage(tsMsg);
		multicast(wrapMsg, groupName);
	}
	
	private void multicast(TimeStampedMessage tsMsg, String groupName) throws IOException
	{
		Group targetGroup = groupMap.get(groupName);
		
		ArrayList<String> members = targetGroup.getMembers();
		int count = 0;
		
		for(String mem : members)
		{
			if(this.crashPoint < count)
				break;
			tsMsg.set_dest(mem);
			msgPasser.send(tsMsg);
			//System.out.println(tsMsg);
		}
	}
	
	private TimeStampedMessage createWrapMessage(TimeStampedMessage tsMsg)
	{
		TimeStampedMessage wrapMessage = new TimeStampedMessage();
		wrapMessage.set_data(tsMsg);
		wrapMessage.set_source(procName);
		wrapMessage.set_kind("flooding");
		wrapMessage.setGroupName(tsMsg.getGroupName());
		wrapMessage.getTimeStamp().setTimeStamp(VECTOR, tsMsg.getVectorTS(), 0);
		return wrapMessage;
	}
	
	
	public void multicastReceive()
	{
		//TimeStampedMessage tsMsg = groupMap.get(groupName).getReceiveQueue().poll();
		//return tsMsg;
		
		for(String groupName : groupMap.keySet())
		{
			System.out.println("*****************************" + groupName + "*****************************");
			PriorityQueue<TimeStampedMessage> revQueue = 
					groupMap.get(groupName).getReceiveQueue();
			revLock.lock();
			if(revQueue.isEmpty())
				System.out.println("No message in the group");
			while(!revQueue.isEmpty())
			{
				TimeStampedMessage tsMsg = revQueue.poll();
				System.out.println(tsMsg);
			}
			System.out.println("****************************************************************");
			revLock.unlock();
			
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private void initialGroupMap(String configFile, String procName)
	{
		Yaml yaml = new Yaml();

		/* fill the item in the configuration file in the map */
		try 
		{
			File pFile = new File(configFile);
			InputStream input = new FileInputStream(pFile);
			Object obj = yaml.load(input);
			LinkedHashMap<String, ArrayList<HashMap<String, ArrayList<String>>>> map = 
					(LinkedHashMap<String, ArrayList<HashMap<String, ArrayList<String>>>>)obj;
			for(HashMap<String, ArrayList<String>> gMap : map.get(GROUP))
				fillInGroupMap(gMap);
		} 
		catch (FileNotFoundException e) 
		{
			System.err.println("cannot find the config file");
			System.exit(1);
		}
		catch (NullPointerException e)
		{
			System.err.println("the format of yaml is wrong at least one field is empty");
			System.exit(1);
		}
	}
	
	private void fillInGroupMap(HashMap<String, ArrayList<String>> map)
	{
		Object groupNameObj = (Object)map.get(NAME);
		String groupName = (String) groupNameObj;
		ArrayList<String> memLst = map.get(MEMBERS);
		Group group = new Group(map, procName);
		
		for(String member : memLst)
		{
			group.addMember(member);
			if(member.equals(procName))
			{
				groupMap.put(groupName, group);
			}
		}
		
	}
	
	/*
	public static void main(String[] args) throws IOException
	{
		MulticastService mService = new MulticastService("config.txt", "alice");
		
		TimeStampedMessage tsMsg = new TimeStampedMessage();
		tsMsg.set_data("a");
		tsMsg.setGroupName("Group1");
		tsMsg.set_source("alice");
		tsMsg.set_kind("a");
		mService.multicastSend(tsMsg);
	}
	*/
}
