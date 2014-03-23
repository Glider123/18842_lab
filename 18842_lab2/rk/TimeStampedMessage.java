import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class TimeStampedMessage extends Message {
	private TimeStamp timeStamp;
	private boolean log;

	public TimeStampedMessage() {
		super();
		timeStamp = null;
	}

	public TimeStampedMessage(String dest, String kind, Object data, String log) {
		super(dest, kind, data);
		timeStamp = null;
		if (log.equals("y"))
			this.log = true;
		else
			this.log = false;
	}

	public void setTimeStamp(TimeStamp timeStamp) {
		this.timeStamp = timeStamp;
	}

	public TimeStamp getTimeStamp() {
		return timeStamp;
	}

	public boolean getLog() {
		return log;
	}
}
