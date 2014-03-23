public class LogicalTimeStamp extends TimeStamp {
	private Integer logicalTime;

	public LogicalTimeStamp() {
		this.logicalTime = new Integer(1);
	}

	public LogicalTimeStamp(TimeStamp lt) {
		if (lt instanceof LogicalTimeStamp) {
			logicalTime = new Integer((Integer) lt.getTime());
		} else {
			System.out.println("Wrong type of time stamp!");
		}
	}

	public Object getTime() {
		return logicalTime;
	}

	public void setTime(Object a) {
		this.logicalTime = (Integer) a;
	}

	public String toStringTime() {
		// TODO Auto-generated method stu
		return logicalTime.toString();
	}

	@Override
	public int compareTo(TimeStamp another) {
		// TODO Auto-generated method stub
		if (logicalTime < (Integer)another.getTime()){
			return -1;
		}else if (logicalTime == (Integer)another.getTime()){
			return 0;
		}
		return 1;
	}
}
