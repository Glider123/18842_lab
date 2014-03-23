import java.util.ArrayList;

public class VectorTimeStamp extends TimeStamp {
	private ArrayList<Integer> vectorTime;

	public VectorTimeStamp() {
		vectorTime = new ArrayList<Integer>();
	}

	public VectorTimeStamp(int size) {
		vectorTime = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			vectorTime.add(0);
		}
	}

	public VectorTimeStamp(TimeStamp lt) {
		if (lt instanceof VectorTimeStamp) {
			vectorTime = new ArrayList<Integer>(
					(ArrayList<Integer>) lt.getTime());
		} else {
			System.out.println("Wrong type of time stamp!");
		}
	}

	@Override
	public void setTime(Object a) {
		this.vectorTime = (ArrayList<Integer>) a;
	}

	@Override
	public Object getTime() {
		// TODO Auto-generated method stub
		return vectorTime;
	}

	public String toStringTime() {
		// TODO Auto-generated method stub
		String result = "[";
		result += vectorTime.get(0).toString();
		for (int i = 1; i < vectorTime.size(); i++) {
			result = result + ", " + vectorTime.get(i).toString();
		}
		result += "]";
		return result;
	}

	public int compareTo(TimeStamp another) {
		// TODO Auto-generated method stub
		// -1 means before, 0 means concurrent, 1 means after
		ArrayList<Integer> thislist = vectorTime;
		ArrayList<Integer> anotherlist = (ArrayList<Integer>) another.getTime();
		int length = thislist.size();
		boolean same = true;
		int relationship = thislist.get(0) < anotherlist.get(0) ? -1 : thislist
				.get(0) > anotherlist.get(0) ? 1 : 0;
		for (int i = 1; i < length; i++) {
			int newrela = thislist.get(i) < anotherlist.get(i) ? -1 : thislist
					.get(i) > anotherlist.get(i) ? 1 : relationship;
			if (newrela != relationship && relationship != 0) {
				same = false;
				break;
			}
			relationship = newrela;
		}
		if (same)
			return relationship;
		else
			return 0;
	}
}
