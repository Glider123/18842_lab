import java.io.Serializable;


public class Message implements Serializable{

	/**
	 * random generated serialVersionUID
	 */
	private static final long serialVersionUID = 9161898865426657918L;
	private String dest; // the destation of the connection
	private String kind; // the kind of the message
	private Object data; // the data of the message
	private String source; // the source of the message
	private int seqNumber; // the seqNumber of the message
	private boolean dupe; // see if the message is duplicate
	private int portNum; // see the port number of the message to send
	private String ip; // the ip of the message to send
	
	/**
	 * initialize the message
	 */
	public Message()
	{
		this.dupe = false;
	}
	
	/**
	 * initialize the message
	 * @param msg the message to be sent
	 */
	public Message(Message msg)
	{
		this.dest = msg.get_dest();
		this.kind = msg.get_kind();
		this.data = msg.get_data();
		this.source = msg.get_source();
		this.dupe = msg.get_duplicate();
		this.portNum = msg.get_portNum();
		this.ip = msg.get_ip();
		this.seqNumber = msg.get_seqNum();
	}
	
	
	/**
	 * initialize the message
	 * @param dest the dest of the message
	 * @param kind the kind of the message
	 * @param data the data of the message
	 */
	public Message(String dest, String kind, Object data)
	{
		this.dest = dest;
		this.kind = kind;
		this.data = data;
		this.dupe = false;
	}
	
	/**
	 * for the system output
	 */
	public String toString()
	{
		String result = "\nsrc\t: " + source + "\n"
						+ "dest\t: " + dest + "\n"
						+ "kind\t: " + kind + "\n"
						+ "data\t: " + data + "\n"
						+ "seq num\t: " + seqNumber + "\n"
						+ "dup: " + dupe + "\n";
		
		return result;
	}
	
	/**
	 * get the ip of the message
	 * @return the ip of the message
	 */
	public String get_ip()
	{
		return this.ip;
	}
	
	/**
	 * set the ip of the message
	 * @param ip the ip of the message
	 */
	public void set_ip(String ip)
	{
		this.ip = ip;
	}
	
	/**
	 * set the port number of the message to send to
	 * @param portNum the port number of the message to send to
	 */
	public void set_portNum(int portNum)
	{
		this.portNum = portNum;
	}
	
	/**
	 * get the port number of the message to send to
	 * @return the port number of the message to send to
	 */
	public int get_portNum()
	{
		return this.portNum;
	}
	
	/**
	 * set the data of the message
	 * @param data the data of the message
	 */
	public void set_data(Object data)
	{
		this.data = data;
	}
	
	/**
	 * get the data of the message
	 * @return the data of the message
	 */
	public Object get_data()
	{
		return this.data;
	}
	
	/**
	 * set the destination of the message
	 * @param dest the destination of the message
	 */
	public void set_dest(String dest)
	{
		this.dest = dest;
	}
	
	/**
	 * get the destination of the message
	 * @return the destination of the message
	 */
	public String get_dest()
	{
		return this.dest;
	}
	
	/**
	 * set the kind of the message
	 * @param kind the kind of the message
	 */
	public void set_kind(String kind)
	{
		this.kind = kind;
	}
	
	/**
	 * get the kind of the message
	 * @return kind of the message
	 */
	public String get_kind()
	{
		return this.kind;
	}
	
	/**
	 * get the source of the message
	 * @return the source of the message
	 */
	public String get_source()
	{
		return this.source;
	}
	
	/**
	 * set the source of the message
	 * @param source the source of the message
	 */
	public void set_source(String source)
	{
		this.source = source;
	}
	
	/**
	 * set the sequence number of the message
	 * @param sequenceNumber the sequence number of the 
	 *        message
	 */
	public void set_seqNum(int sequenceNumber)
	{
		this.seqNumber = sequenceNumber;
	}
	
	/**
	 * get the sequence number of the message
	 * @return the sequence number of the message
	 */
	public int get_seqNum()
	{
		return this.seqNumber;
	}
	
	/**
	 * set the duplicate field of the message
	 * @param dupe the duplicate field of the message
	 */
	public void set_duplicate(Boolean dupe)
	{
		this.dupe = dupe;
	}
	
	/**
	 * get the duplicate field of the message
	 * @return the duplicate field of the message
	 */
	public boolean get_duplicate()
	{
		return this.dupe;
	}
}
