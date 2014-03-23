

public class Vector extends ClockService{
	private static final String CONFIG_HEADER = "configuration";
	private static final int VECTOR = 1;
	
	private int[] timestampVector; // the vector clock
	private int procIndex; // the proc using the vector clock

	/**
	 * constructor
	 * @param msgPasser the msgPasser to add to 
	 */
	public Vector(MessagePasser msgPasser)
	{
		super(msgPasser);
		
		int vectorSize = msgPasser.getYamlMap().get(CONFIG_HEADER).size();
		this.timestampVector = new int[vectorSize];
		this.procIndex = msgPasser.getProcIndex();
	}

	@Override
	public void updateTimestamp() {
		timestampVector[procIndex]++;
	}

	@Override
	public int getClkType() {
		return VECTOR;
	}
	
	@Override
	public void updateRevTimestamp(TimeStampedMessage tsMsg)
	{		
		int[] revVector = tsMsg.getVectorTS();
		
		if(tsMsg != null)
		{
			for(int i = 0; i < revVector.length; i++)
				timestampVector[i] = Math.max(timestampVector[i], revVector[i]);
		}
	}
	
	@Override
	public int[] getVectorTimestamp()
	{
		return this.timestampVector;
	}
	
	@Override
	public int getLogicTimestamp()
	{
		return 0;
	}
}
