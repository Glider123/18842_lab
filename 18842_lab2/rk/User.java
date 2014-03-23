/*
 * This class is used for making an User type arraylist.
 */
public class User {
	String name;
	String ip;
	int port;

	public User() {
	}

	public User(String name, String ip, int port) {
		this.name = name;
		this.ip = ip;
		this.port = port;
	}

	public String getNameByPort(int port) {
		if (port == this.port)
			return this.name;
		return "";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
