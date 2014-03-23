/*
 * The Message class has creates a message which is sent to the processes.
 * The sequence number keeps on increasing. The Message class implements 
 * Serializable and hence it can be sent over a network. This class also 
 * checks for matchrules while sending and receiving.
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
	String dest;
	String source;
	String kind;
	int seqn;
	Object data;
	Boolean dupe;

	static int counter = 0;

	public Message() {
		this.seqn = counter++;
		this.dupe = false;
	}

	public Message(String dest, String kind, Object data) {
		this.dest = dest;
		this.kind = kind;
		this.data = data;
		this.seqn = counter++;
		this.dupe = false;
	}

	/*
	 * public Message(Message orig) {
	 * 
	 * this.source = orig.source;
	 * 
	 * this.dest = orig.dest;
	 * 
	 * this.kind = orig.kind;
	 * 
	 * this.data = orig.data;
	 * 
	 * this.seqn = orig.seqn;
	 * 
	 * this.dupe = true;
	 * 
	 * System.out.println("Seq Num is "+ this.seqn);
	 * 
	 * }
	 */
	public static Object Copy(Object a) throws IOException,
			ClassNotFoundException {
		// Message another = (Message) a;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(a);
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bis);
		Object copied = in.readObject();
		return copied;

	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setDuplicate(Boolean dupe) {
		this.dupe = dupe;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public Boolean getDuplicate() {
		return this.dupe;
	}

	public String getDest() {
		return this.dest;
	}

	public String getSource() {
		return this.source;
	}

	public String getKind() {
		return this.kind;
	}

	public int getSequenceNumber() {
		return seqn;
	}

	public Object getData() {
		return data;
	}

	public String matchRules(ArrayList<Rule> rules) {
		int length = rules.size();
		if (length == 0)
			return "";
		for (int i = 0; i < length; i++) {
			if (matchRule(rules.get(i))) {
				return rules.get(i).action;
			}
		}
		return "";
	}

	private Boolean matchRule(Rule theRule) {
		Boolean[] judge = new Boolean[5];
		int[] judgeno = new int[5];
		Boolean result = true;

		for (int i = 0; i < 5; i++) {
			judge[i] = false;
		}

		for (int i = 0; i < 5; i++) {
			judgeno[i] = 0;
		}

		if (!theRule.source.equals("")) {
			judgeno[0] = 1;
			if (theRule.source.equals(source)) {
				judge[0] = true;
			}
		}

		if (!theRule.dest.equals("")) {
			judgeno[1] = 1;
			if (theRule.dest.equals(dest)) {
				judge[1] = true;
			}
		}

		if (!theRule.kind.equals("")) {
			judgeno[2] = 1;
			if (theRule.kind.equals(kind)) {
				judge[2] = true;
			}
		}

		if (theRule.seqn != -1) {
			judgeno[3] = 1;
			if (theRule.seqn == seqn) {
				judge[3] = true;
			}
		}

		if (!theRule.duplicate.equals("")) {
			judgeno[4] = 1;
			if ((theRule.duplicate.equals("true") && (dupe == true))
					|| (theRule.duplicate.equals("false") && (dupe == false))) {
				judge[3] = true;
			}
		}

		for (int i = 0; i < 5; i++) {
			if (judgeno[i] == 1) {
				result = result && judge[i];
			}
		}

		return result;
	}
}
