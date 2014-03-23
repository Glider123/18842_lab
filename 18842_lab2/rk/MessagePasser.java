/*
 * The MessagePasser class will take input a config file along with the process 
 * name. The config file is read using the MPhelper class. The sendRules and 
 * receiveRules are stored in Arraylists and are checked whenever a new msg is 
 * sent or received. The processes communicate using TCP connection. The receiver
 * runs in a seperate thread.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Queue;

public class MessagePasser {
	BlockingQueue<TimeStampedMessage> recbuf = new LinkedBlockingDeque<TimeStampedMessage>();
	Map<String, Socket> socketmap = new HashMap<String, Socket>();
	volatile ArrayList<User> users = new ArrayList<User>();
	volatile ArrayList<Rule> sendRules = new ArrayList<Rule>();
	volatile ArrayList<Rule> receiveRules = new ArrayList<Rule>();
	volatile ArrayList<String> clockType = new ArrayList<String>();
	ArrayList<DelayQue> sendQ = new ArrayList<DelayQue>();
	ArrayList<DelayQue> receiveQ = new ArrayList<DelayQue>();
	MPhelper helper = new MPhelper();
	static String config_filename;
	static String localname;
	static int processNo;
	ClockService clockService;

	public MessagePasser() {
	}

	public MessagePasser(String configuration_filename, String local_name)
			throws IOException {
		this.localname = local_name;
		this.config_filename = configuration_filename;
		this.processNo = 0;
		helper.parseConfigFile(config_filename, users, sendRules, receiveRules,
				clockType);
		for (int i = 0; i < users.size(); i++) {
			if (this.localname.equals(users.get(i).getName())) {
				this.processNo = i;
				System.out.println("Process Number is" + this.processNo);
				break;
			}
		}
		clockService = ClockServiceFactory.setType(clockType.get(0), processNo,
				users.size() - 1);
		// Start accepting connection
		ServerSocket receiveserver = setConnection(users, local_name, helper,
				socketmap);

		new Thread(new Receiver(receiveserver, receiveRules)).start();
		System.out.println("Server thread successfully started.");
	}

	/* method send to log */
	public void sendToLog(TimeStampedMessage message) {
		String destname = "logger";
		message.setDuplicate(false);
		message.setSource(localname);
		if (!socketmap.containsKey(destname)) {
			User thedest = null;
			for (int i = 0; i < users.size(); i++) {
				if (destname.equals(users.get(i).name)) {
					thedest = users.get(i);
				}
			}
			Socket s;
			try {
				s = new Socket(thedest.getIp(), thedest.getPort());
				socketmap.put(destname, s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Dest not found");
				e.printStackTrace();
			}
		}
		Socket s = socketmap.get(destname);
		ObjectOutputStream sendstream;
		try {
			sendstream = new ObjectOutputStream(s.getOutputStream());
			sendstream.writeObject(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("ERROR CLOSE");
				System.exit(1);
			}
			socketmap.remove(destname);
			System.out.println("Cannot send to " + destname + " anymore+.+");
		}

	}

	/* end of method send to log */
	/* method send(Message) */
	public void send(TimeStampedMessage message) {
		String destname = message.getDest();
		message.setDuplicate(false);
		message.setSource(localname);
		message.setTimeStamp(clockService.setTime());

		if (!socketmap.containsKey(destname)) {
			User thedest = null;
			for (int i = 0; i < users.size(); i++) {
				if (destname.equals(users.get(i).name)) {
					thedest = users.get(i);
				}
			}
			Socket s;
			try {
				s = new Socket(thedest.getIp(), thedest.getPort());
				socketmap.put(destname, s);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Dest not found");
				e.printStackTrace();
			}
		}

		Socket s = socketmap.get(destname);
		helper.parseConfigFile(config_filename, users, sendRules, receiveRules,
				clockType);
		// send a copy to logger
		String seaction = message.matchRules(sendRules);
		if (message.getLog() == true) {
			System.out.println("WILL SEND TO LOGGER");
			TimeStampedMessage mess2 = new TimeStampedMessage("logger", "log", message, "n");
			sendToLog(mess2);
		}
		if (seaction.equals("")) {
			ObjectOutputStream sendstream;
			try {
				sendstream = new ObjectOutputStream(s.getOutputStream());
				sendstream.writeObject(message);

				for (int i = 0; i < sendQ.size(); i++) {
					if ((sendQ.get(i).getSource().equals(message.getSource()))
							&& (sendQ.get(i).getDest()
									.equals(message.getDest()))) {
						sendstream = new ObjectOutputStream(s.getOutputStream()); // Ask
																					// LU
																					// if
																					// new
																					// ObjectOutputString
																					// is
																					// required
																					// everytime
						sendstream.writeObject(sendQ.get(i).getMessage()); // remove
																			// if
																			// source
																			// and
																			// destination
																			// match
						sendQ.remove(i);
						i--;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				try {
					s.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					System.out.println("ERROR CLOSE");
					System.exit(1);
				}
				socketmap.remove(destname);
				System.out
						.println("Cannot send to " + destname + " anymore+.+");
			}

		} else if (seaction.equals("drop")) {/* TODO */
		} else if (seaction.equals("duplicate")) {
			ObjectOutputStream sendstream;
			try {
				sendstream = new ObjectOutputStream(s.getOutputStream());
				sendstream.writeObject(message);
				message.setDuplicate(true);
				sendstream = new ObjectOutputStream(s.getOutputStream());
				sendstream.writeObject(message);
				if (message.getLog() == true) {
					System.out.println("WILL SEND TO LOGGER");
					TimeStampedMessage mess2 = new TimeStampedMessage("logger", "log", message, "n");
					sendToLog(mess2);
				}
				for (int i = 0; i < sendQ.size(); i++) {
					if ((sendQ.get(i).getSource().equals(message.getSource()))
							&& (sendQ.get(i).getDest()
									.equals(message.getDest()))) {
						sendstream = new ObjectOutputStream(s.getOutputStream()); // Ask
																					// LU
																					// if
																					// new
																					// ObjectOutputString
																					// is
																					// required
																					// everytime
						sendstream.writeObject(sendQ.get(i).getMessage()); // remove
																			// if
																			// source
																			// and
																			// destination
																			// match
						sendQ.remove(i);
						i--;
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				try {
					s.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					System.out.println("ERROR CLOSE");
					System.exit(1);
				}
				socketmap.remove(destname);
				System.out
						.println("Cannot send to " + destname + " anymore+.+");
			}
		} else if (seaction.equals("delay")) {
			sendQ.add(new DelayQue(message.getSource(), message.getDest(),
					message)); // Added to delay queue
		}/* TODO */// Add to the DelayedQueue
		else {
			System.out.println("Send action read error!");
		}

	}

	/* End of method send(Message) */
	/* method receive() */
	public TimeStampedMessage receive() throws InterruptedException {
		return recbuf.take();
	}

	/* method display */
	public void display() {
	}

	/* End of method display */
	/* End of method receive() */
	/* method setConnection */
	ServerSocket setConnection(ArrayList<User> users, String local_name,
			MPhelper helper, Map<String, Socket> socketmap) {
		ServerSocket listener = null;
		if (!helper.containName(users, local_name)) {
			return listener;
		}
		// Itself as a server
		System.out.println(helper.getPort(users, local_name));
		try {
			listener = new ServerSocket(helper.getPort(users, local_name));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return listener;
	}

	/* End of method setConnection */
	class Receiver implements Runnable {
		ServerSocket receiveserver;
		Socket receivesocket;
		ArrayList<Rule> receiverules;

		public Receiver(ServerSocket receiveserver, ArrayList<Rule> rr) {
			this.receiveserver = receiveserver;
			this.receiverules = rr;
		}

		@Override
		public void run() {
			while (true) {
				try {
					receivesocket = receiveserver.accept();
					new Thread(
							new newReceiveThread(receivesocket, receiverules))
							.start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/* End of class Receiver */
	/* Begin of newReveiceThread */
	class newReceiveThread implements Runnable {
		Socket receivesocket;
		ArrayList<Rule> receiverules;
		ObjectInputStream is;
		String rname;

		public newReceiveThread(Socket receivesocket, ArrayList<Rule> rr) {
			this.receivesocket = receivesocket;
			this.receiverules = rr;
			rname = null;
		}

		@Override
		public void run() {
			System.out.println("Server accepted connection.");
			while (true) {
				try {
					helper.parseConfigFile(config_filename, users, sendRules,
							receiveRules, clockType);
					TimeStampedMessage recm;
					is = new ObjectInputStream(receivesocket.getInputStream());
					recm = (TimeStampedMessage) is.readObject();
					if (rname == null)
						rname = recm.getSource();
					String recaction = recm.matchRules(receiverules);
					if (recaction.equals("")) {
						TimeStamp receiveStamp = recm.getTimeStamp();
						clockService.updateTime(receiveStamp);
						recm.setTimeStamp(clockService.setTime());
						recbuf.add(recm);

						for (int i = 0; i < receiveQ.size(); i++) {
							if ((receiveQ.get(i).getSource().equals(recm
									.getSource()))
									&& (receiveQ.get(i).getDest().equals(recm
											.getDest()))) {
								recbuf.add(receiveQ.get(i).getMessage());
								receiveQ.remove(i);
								i--;
							}
						}

					} else if (recaction.equals("drop")) {/* TODO */
					} else if (recaction.equals("duplicate")) {
						TimeStampedMessage rerecm;
						TimeStamp receiveStamp = ((TimeStampedMessage) recm)
								.getTimeStamp();
						clockService.updateTime(receiveStamp);
						((TimeStampedMessage) recm).setTimeStamp(clockService
								.setTime());
						rerecm = (TimeStampedMessage) Message.Copy(recm);
						((TimeStampedMessage) rerecm).setTimeStamp(clockService
								.setTime());
						recbuf.add(recm);
						recbuf.add(rerecm);
						for (int i = 0; i < receiveQ.size(); i++) {
							if ((receiveQ.get(i).getSource().equals(recm
									.getSource()))
									&& (receiveQ.get(i).getDest().equals(recm
											.getDest()))) {
								recbuf.add(receiveQ.get(i).getMessage());
								receiveQ.remove(i);
								i--;
							}
						}
					} else if (recaction.equals("delay")) {
						// Message rerecm2 = new Message(recm); // Ask LU if new
						// Message is required or recm can be passed directly
						TimeStamp receiveStamp = ((TimeStampedMessage) recm)
								.getTimeStamp();
						clockService.updateTime(receiveStamp);
						((TimeStampedMessage) recm).setTimeStamp(clockService
								.setTime());
						receiveQ.add(new DelayQue(recm.getSource(), recm
								.getDest(), recm)); // Added to delay queue
					} else {
						System.out.println("Receive action read error!");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					try {
						this.receivesocket.close();
						System.out.println("User " + rname + " exit!");
						return;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						System.out.println("ERROR CLOSE SOCKET2");
						System.exit(1);
					}
					socketmap.remove(rname);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	/* End of newReceiveThread */
}
