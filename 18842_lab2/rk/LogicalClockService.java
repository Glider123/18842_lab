import java.util.concurrent.Semaphore;

public class LogicalClockService extends ClockService {
	private TimeStamp next;
	private static Semaphore semaphore = new Semaphore(1);

	public LogicalClockService() {
		next = new LogicalTimeStamp();
	}

	public void updateTime(TimeStamp curr) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Integer newnext = new Integer(((Integer) curr.getTime()).intValue() + 1);
		next.setTime(newnext);
		semaphore.release();
	}

	public TimeStamp setTime() {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TimeStamp curr = new LogicalTimeStamp(next);
		Integer newnext = new Integer(((Integer) curr.getTime()).intValue() + 1);
		next.setTime(newnext);
		semaphore.release();
		return curr;
	}

	@Override
	public void updateTimeByOne() {
		// TODO Auto-generated method stub
		Integer newnext = new Integer(((Integer) next.getTime()).intValue() + 1);
		next.setTime(newnext);
	}

}
