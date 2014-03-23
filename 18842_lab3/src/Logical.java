
public class Logical extends ClockService{
	private static final int LOGICAL = 0;
	int timestamp;
	
	/* the logical clock */
	public Logical(MessagePasser msgPasser)
	{
		super(msgPasser);
		
		this.timestamp = 0;
	}
	
	/* override fundtions */
	@Override
	public void updateTimestamp() 
	{
		this.timestamp++;
	}
	
	@Override
	public int getClkType()
	{
		return LOGICAL;
	}
	
	@Override
	public void updateRevTimestamp(TimeStampedMessage tsMsg)
	{
		int revTimeStamp = tsMsg.getLogicalTS();
		this.timestamp = Math.max(revTimeStamp, this.timestamp);
	}
	
	@Override
	public int getLogicTimestamp()
	{
		return this.timestamp;
	}
	
	@Override
	public int[] getVectorTimestamp()
	{
		return null;
	}
}
