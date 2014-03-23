import java.io.Serializable;
import java.util.Arrays;


public class TimeStamp implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 433205825966037455L;
	public static final int LOGICAL = 0;
	public static final int VECTOR = 1;
	
	public static final int LOGICAL_LESSTHAN = -1;
	public static final int LOGICAL_EQUAL = 0;
	public static final int LOGICAL_GREATERTHAN = 1;
	
	public static final int VECTOR_LESSTHAN = -1;
	public static final int VECTOR_EQUAL = 0;
	public static final int VECTOR_GREATERTHAN = 1;
	public static final int VECTOR_UNEQUAL = 2;
	
	private int[] vectorTS; // the vector clock
	private int logicalTS; // the logical clock
	 
	private int type; // the type of the clock
	
	/**
	 * get the type of the clock
	 * @return the time of the cock
	 */
	public int getType()
	{
		return this.type;
	}
	
	/**
	 * set the type of the clock
	 * @param type the type of the clock
	 */
	public void setType(int type)
	{
		this.type = type;
	}
	
	/**
	 * constructor
	 */
	public TimeStamp()
	{
		
	}
	
	/**
	 * constructor
	 * @param type
	 */
	public TimeStamp(int type)
	{
		this.type = type;
	}
	
	/**
	 * get the logical clock
	 * @return the locigal clock
	 */
	public int getLogicalClock() {
		return logicalTS;
	}
	
	/**
	 * get the vector clock
	 * @return the vector clock
	 */
	public int[] getVectorClock() {
		return vectorTS;
	}
	
	/**
	 * set the time stamp
	 * @param type the clock type
	 * @param vectorTS the vector time stamp
	 * @param logicalTS the logical time stamp
	 */
	public void setTimeStamp(int type, int[] vectorTS, int logicalTS)
	{
		this.type = type;
		if (type == LOGICAL) {
			this.logicalTS = logicalTS;
		}
		else {
			this.vectorTS = Arrays.copyOf(vectorTS, vectorTS.length);
		}
	}
	
	/**
	 * compare logical time stamp
	 * @param timeStamp the logical time stamp
	 * @return smaller or concurrent
	 */
	public int compareLogicalClock(TimeStamp timeStamp) {
		if (logicalTS < timeStamp.getLogicalClock()) {
			return LOGICAL_LESSTHAN;
		} else if (logicalTS == timeStamp.getLogicalClock()) {
			return LOGICAL_EQUAL;
		} else {
			return LOGICAL_GREATERTHAN;
		}
	}
	
	/**
	 * compare vector clock
	 * @param timestamp te vector clock
	 * @return smaller or concurrent
	 */
	public int compareVectorClock(TimeStamp timestamp) {
		int[] otherVectorTS = timestamp.getVectorClock();
		int result = VECTOR_EQUAL;	
		for (int i = 0; i < vectorTS.length; i++) {
			if (vectorTS[i] < otherVectorTS[i]) {
				if (result == VECTOR_EQUAL) {
					result = VECTOR_LESSTHAN;
				} else if (result == VECTOR_LESSTHAN) {
					continue;
				} else {
					return VECTOR_UNEQUAL;
				}
			} else if (vectorTS[i] > otherVectorTS[i]) {
				if (result == VECTOR_EQUAL) {
					result = VECTOR_GREATERTHAN;
				} else if (result == VECTOR_GREATERTHAN) {
					continue;
				} else {
					return VECTOR_UNEQUAL;
				}
			} else {
				continue;
			}
		}
		return result;
	}
	
}
