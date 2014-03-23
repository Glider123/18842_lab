import java.io.Serializable;

public abstract class TimeStamp implements Serializable {
	public abstract void setTime(Object a);

	public abstract Object getTime();

	public abstract String toStringTime();

	public abstract int compareTo(TimeStamp another);
}
