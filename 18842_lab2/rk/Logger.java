import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Logger extends MessagePasser {
	ArrayList<ArrayList<TimeStampedMessage>> log = new ArrayList<ArrayList<TimeStampedMessage>>();
	Map<String, Integer> nameIndex = new HashMap<String, Integer>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	public Logger(String configuration_filename, String local_name)
			throws IOException {
		this.localname = local_name;
		this.config_filename = configuration_filename;
		helper.parseConfigFile(config_filename, users, sendRules, receiveRules,
				clockType);
		// Start accepting connection
		ServerSocket receiveserver = setConnection(users, local_name, helper,
				socketmap);
		new Thread(new Receiver(receiveserver, receiveRules)).start();
		System.out.println("Server thread successfully started.");
		int length = super.users.size();
		for (int i = 0; i < length - 1; i++) {
			nameIndex.put(users.get(i).getName(), i);
			log.add(new ArrayList<TimeStampedMessage>());
		}
	}

	public void display() {
		readLock.lock();
		TimeStampedMessage e=null;
		System.out
				.println("---------------------Display current log---------------------");
		ArrayList<Iterator> itrs = new ArrayList<Iterator>();
		int length = log.size();
		for (int i = 0; i < length; i++) {
			itrs.add(log.get(i).iterator());
		}
		for (int i = 0; i < length; i++) {
			if (log.get(i).size() != 0)
				System.out.println("|**|___These are messages sent from "
						+ log.get(i).get(0).getSource() + "___|**|");
			while (itrs.get(i).hasNext()) {
				e = (TimeStampedMessage) itrs.get(i).next();
				System.out.print(e.getTimeStamp().toStringTime() + "-->");
			}
			System.out.print('\n');
			itrs.set(i, log.get(i).iterator());
			if (e!=null && e.getTimeStamp() instanceof LogicalTimeStamp)
				System.out.println("Logical Time Stamps are not easy to be compared.");
			else if (e!=null && e.getTimeStamp() instanceof VectorTimeStamp){
			System.out.println("|**|_________In Detail________|**|");
			while (itrs.get(i).hasNext()) {
				TimeStampedMessage a = (TimeStampedMessage) itrs.get(i).next();
				for (int j = i + 1; j < length; j++) {
					ArrayList<TimeStampedMessage> temp = new ArrayList<TimeStampedMessage>();
					while (itrs.get(j).hasNext()) {
						TimeStampedMessage b = (TimeStampedMessage) itrs.get(j)
								.next();
						if (a.getTimeStamp().compareTo(b.getTimeStamp()) == 0) {
							if (temp.size() != 0) {
								System.out.print(a.getTimeStamp()
										.toStringTime()
										+ " is sent after ("
										+ temp.get(0).getSource()
										+ "to"
										+ temp.get(0).getDest()
										+ ") "
										+ temp.get(0).getTimeStamp()
												.toStringTime());
							}
							for (int t = 0; t < temp.size(); t++) {
								System.out.print("--> ("
										+  temp.get(t).getSource()
										+ "to"
										+ temp.get(t).getDest()
										+ ") "
										+ temp.get(t).getTimeStamp()
												.toStringTime());
							}
							if (temp.size() != 0)
								System.out.print('\n');
							temp.clear();
							System.out.println(a.getTimeStamp().toStringTime()
									+ " is concurrent with (" + b.getSource()
									+ "to" + b.getDest()+") " + b.getTimeStamp().toStringTime());
						} else if (a.getTimeStamp().compareTo(b.getTimeStamp()) == -1) {
							if (temp.size() != 0) {
								System.out.print(a.getTimeStamp()
										.toStringTime()
										+ " is sent after ("
										+ temp.get(0).getSource()
										+ "to"
										+ temp.get(0).getDest()
										+ ") "
										+ temp.get(0).getTimeStamp()
												.toStringTime());
							}
							for (int t = 0; t < temp.size(); t++) {
								System.out.print("--> ("
										+  temp.get(t).getSource()
										+ "to"
										+ temp.get(t).getDest()
										+ ") "
										+ temp.get(t).getTimeStamp()
												.toStringTime());
							}
							if (temp.size() != 0)
								System.out.print('\n');
							temp.clear();
							System.out.print(a.getTimeStamp().toStringTime()
									+ " is sent before (" + b.getSource()
									+ "to" + b.getDest() + ") " + b.getTimeStamp().toStringTime());
							while (itrs.get(j).hasNext()) {
								b = (TimeStampedMessage) itrs.get(j).next();
								System.out.print("--> ("
										+  b.getSource()
										+ "to"
										+ b.getDest()
										+ ") "
										+ b.getTimeStamp().toStringTime());
							}
							System.out.print('\n');
							break;
						} else if (a.getTimeStamp().compareTo(b.getTimeStamp()) == 1) {
							temp.add(b);
							if (!itrs.get(j).hasNext()){
								if (temp.size() != 0) {
									System.out.print(a.getTimeStamp()
											.toStringTime()
											+ " is sent after ("
											+ temp.get(0).getSource()
											+ "to"
											+ temp.get(0).getDest()
											+ ") "
											+ temp.get(0).getTimeStamp()
													.toStringTime());
								}
								for (int t = 0; t < temp.size(); t++) {
									System.out.print("--> ("
											+  temp.get(t).getSource()
											+ "to"
											+ temp.get(t).getDest()
											+ ") "
											+ temp.get(t).getTimeStamp()
													.toStringTime());
								}
								if (temp.size() != 0)
									System.out.print('\n');
								temp.clear();
							}
						} else {
							System.out.println("Bad compare!");
						}
					}
					itrs.set(j, log.get(j).iterator());
				}
			}
			}
		}
		System.out
				.println("---------------------Display current log-----------------------");
		readLock.unlock();
	}

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
					TimeStampedMessage recm;
					is = new ObjectInputStream(receivesocket.getInputStream());
					recm = (TimeStampedMessage) is.readObject();
					if (rname == null)
						rname = recm.getSource();
					recm = (TimeStampedMessage)recm.getData();
					writeLock.lock();
					log.get(nameIndex.get(rname)).add(recm);
					recbuf.add(recm);
					writeLock.unlock();
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
