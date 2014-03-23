
public abstract class ClockService {

	/* constructor */
	public ClockService(MessagePasser msgPasser)
	{
		
	}
	
	/* abstract functions */
	public abstract void updateTimestamp();
	public abstract void updateRevTimestamp(TimeStampedMessage tsMsg);
	public abstract int getClkType();
	public abstract int getLogicTimestamp();
	public abstract int[] getVectorTimestamp();
}
