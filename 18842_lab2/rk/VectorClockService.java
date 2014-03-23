import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class VectorClockService extends ClockService {
	private TimeStamp next;
	private int processNo;
	private static Semaphore semaphore = new Semaphore(1);

	public VectorClockService(int processNo, int size) {
		next = new VectorTimeStamp(size);
		this.processNo = processNo;
		((ArrayList<Integer>) next.getTime()).set(processNo, 1);
	}

	@Override
	public void updateTime(TimeStamp curr) {
		// TODO Auto-generated method stub
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<Integer> newnext = new ArrayList<Integer>(
				(ArrayList<Integer>) curr.getTime());
		ArrayList<Integer> orinext = new ArrayList<Integer>(
				(ArrayList<Integer>) next.getTime());
		for (int i = 0; i < newnext.size(); i++) {
			int update = Math.max(orinext.get(i), newnext.get(i));
			newnext.set(i, update);
		}
		next.setTime(newnext);
		semaphore.release();
	}

	@Override
	public TimeStamp setTime() {
		// TODO Auto-generated method stub
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TimeStamp curr = new VectorTimeStamp(next);
		ArrayList<Integer> newnext = new ArrayList<Integer>(
				(ArrayList<Integer>) curr.getTime());
		Integer update = new Integer(newnext.get(processNo).intValue() + 1);
		newnext.set(processNo, update);
		System.out.println(processNo);
		next.setTime(newnext);
		semaphore.release();
		return curr;
	}

	@Override
	public void updateTimeByOne() {
		// TODO Auto-generated method stub
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<Integer> orinext = new ArrayList<Integer>(
				(ArrayList<Integer>) next.getTime());
		int update = orinext.get(processNo)+1;
		orinext.set(processNo, update);
		next.setTime(orinext);
		semaphore.release();
	}

}
