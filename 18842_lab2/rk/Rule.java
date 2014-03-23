/*
 * This class is used for making Rule type arraylist.
 */

public class Rule {
	String action;
	String source;
	String dest;
	String kind;
	int seqn;
	String duplicate;

	public Rule() {
	}

	public Rule(String action, String source, String dest, String kind,
			int seqn, String dup) {
		this.action = action;
		this.source = source;
		this.dest = dest;
		this.kind = kind;
		this.seqn = seqn;
		this.duplicate = dup;

	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public int getSeqn() {
		return seqn;
	}

	public void setSeqn(int seqn) {
		this.seqn = seqn;
	}

	public String getDuplicate() {
		return duplicate;
	}

	public void setDuplicate(String dup) {
		this.duplicate = dup;
	}
}
