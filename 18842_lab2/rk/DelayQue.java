public class DelayQue {

	private String sourcename;
	private String destname;
	private TimeStampedMessage mess;

	public DelayQue(String sourcename, String destname, TimeStampedMessage m) {
		this.sourcename = sourcename;
		this.destname = destname;
		this.mess = m;
	}

	public String getSource() {
		return sourcename;
	}

	public String getDest() {
		return destname;
	}

	public TimeStampedMessage getMessage() {
		return mess;
	}
}
