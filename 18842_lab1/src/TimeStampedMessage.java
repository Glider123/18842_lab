import java.util.Arrays;


public class TimeStampedMessage extends Message implements Comparable<TimeStampedMessage>{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3820719632244310987L;
	private static final int LOGICAL = 0;
	
	private TimeStamp timeStamp;
	
	/**
	 * constructor
	 */
	public TimeStampedMessage()
	{
		super();
		this.timeStamp = new TimeStamp();
	}
	
	/**
	 * constructor
	 * @param msg the message
	 * @param clkService the clock service
	 */
	public TimeStampedMessage(Message msg, ClockService clkService)
	{
		super(msg);
		this.timeStamp = new TimeStamp(clkService.getClkType());
		
		if(clkService.getClkType() == LOGICAL)
		{
			int logicalTS = clkService.getLogicTimestamp();
			timeStamp.setTimeStamp(clkService.getClkType(), null, logicalTS);
		}
		else
		{
			int[] vectorTS = clkService.getVectorTimestamp();
			timeStamp.setTimeStamp(clkService.getClkType(), vectorTS, 0);
		}
	}
	
	/**
	 * constructor for copy
	 * @param tsMsg the tsMsg to copy to
	 */
	public TimeStampedMessage(TimeStampedMessage tsMsg)
	{
		this.set_dest(tsMsg.get_dest());
		this.set_kind(tsMsg.get_kind());
		this.set_data(tsMsg.get_data());
		this.set_source(tsMsg.get_source());
		this.set_duplicate(tsMsg.get_duplicate());
		this.set_portNum(tsMsg.get_portNum());
		this.set_ip(tsMsg.get_ip());
		this.set_seqNum(tsMsg.get_seqNum());
		
		this.timeStamp = tsMsg.getTimeStamp();
	}
	
	
	public TimeStamp getTimeStamp()
	{
		return this.timeStamp;
	}
	
	public int getTimeStampType()
	{
		return timeStamp.getType();
	}
	
	public int[] getVectorTS()
	{
		return timeStamp.getVectorClock();
	}
	
	public int getLogicalTS()
	{
		return timeStamp.getLogicalClock();
	}
	
	/**
	 * overide function for toString
	 */
	public String toString()
	{
		String ts;
		if(timeStamp.getType() == LOGICAL)
		{
			ts = "" + timeStamp.getLogicalClock();
		} else
		{
			ts = Arrays.toString(timeStamp.getVectorClock());
		}

		String result = "\nsrc\t: " + this.get_source() + "\n"
				+ "dest\t: " + this.get_dest() + "\n"
				+ "kind\t: " + this.get_kind() + "\n"
				+ "data\t: " + this.get_data() + "\n"
				+ "seq num\t: " + this.get_seqNum() + "\n"
				+ "dup\t: " + this.get_duplicate() + "\n"
			    + "time stamp\t: " + ts + "\n";
		
		return result;
	}
	
	public int compareTo(TimeStampedMessage msg) {
		// compare timestamp according to timestamp's type
		if (timeStamp.getType() == LOGICAL) {
			return timeStamp.compareLogicalClock(msg.getTimeStamp());
		} else {
			return timeStamp.compareVectorClock(msg.getTimeStamp());
		}
	}
}
