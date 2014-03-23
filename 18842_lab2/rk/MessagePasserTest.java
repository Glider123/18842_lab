/*
 * This is the driver program which has the main method.
 * It is an interactive application where you can send a message to other
 * process. The receiver gets the message in real time. 
 * 
 * Group 17: Lu Qu and Darsh Shah
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MessagePasserTest {

	private static class Receiver implements Runnable {
		MessagePasser mpass;
		BufferedReader br;

		public Receiver(MessagePasser mpass,BufferedReader br) {
			this.mpass = mpass;
			this.br = br;
		}

		@Override
		public void run() {
			TimeStampedMessage mess;
			try {
				while (true) {
					mess = mpass.receive();
					String str = null;
					if (mess != null) {
						System.out.println("*******MESSAGE*****");
						System.out.println("Send from: " + mess.getSource());
						System.out.println("Content: "
								+ mess.getData().toString());
						System.out.println("SeqNum: "
								+ mess.getSequenceNumber());
						System.out.println("TimeStamp: "
								+ mess.getTimeStamp().toStringTime());
						System.out.println("*******MESSAGE*****");
						System.out.println("*******SEND TO LOGGER?*****");
						try {
							str = br.readLine();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (str.equals("y")){
							TimeStampedMessage mess2 = new TimeStampedMessage("logger", "log", mess, "n");
							mpass.sendToLog(mess2);
						}
						System.out
								.println("What do you want to do? [Type in 's' to send, 'a' to add a empty event, or 'e' to end].");
						
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void runApplication(String a, String b) throws IOException {
		MessagePasser mpass = new MessagePasser(a, b);

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		new Thread(new Receiver(mpass, br)).start();
		String str = null;
		String s1 = null;
		String s2 = null;
		String s3 = null;
		String s4 = null;
		String src = b;
		while (true) {
			System.out
					.println("What do you want to do? [Type in 's' to send, 'a' to add a empty event, or 'e' to end, or 'w' to skip].");
			try {
				str = br.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (str.equals("s")) {
				System.out.println("[SEND]name?type?content?logged(y/n)?");
				s1 = br.readLine();
				s2 = br.readLine();
				s3 = br.readLine();
				s4 = br.readLine();
				TimeStampedMessage mess = new TimeStampedMessage(s1, s2, s3, s4);
				mpass.send(mess);
			} else if (str.equals("e")) {
				System.out.println("Exit Sucessfully");
				System.exit(0);
			} else if (str.equals("a")) {
				ClockService cs = mpass.clockService;
				cs.updateTimeByOne();
			} else if (str.equals("w")){
				
			}
			else {
				System.out.println("BAD INPUT!");

			}
		}
	}

	private static class LoggerRec implements Runnable {
		Logger mpass;

		public LoggerRec(Logger mpass) {
			this.mpass = mpass;
		}

		@Override
		public void run() {
			TimeStampedMessage mess;
			try {
				while (true) {
					mess = mpass.receive();
					if (mess != null) {
						System.out.println("*******LoggerMessage*****");
						System.out.println("Send from: " + mess.getSource());
						System.out.println("Received by: " + mess.getDest());
						System.out.println("Content: "
								+ mess.getData().toString());
						System.out.println("TimeStamp: "
								+ mess.getTimeStamp().toStringTime());
						System.out.println("*******LoggerMessage*****");
						System.out
								.println("What do you want to do? [Type in 'r' to read log or 'e' to end].");

					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void runLogger(String a, String b) throws IOException {
		Logger mpass = new Logger(a, b);
		new Thread(new LoggerRec(mpass)).start();

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String str = null;
		String src = b;
		while (true) {
			System.out
					.println("What do you want to do? [Type in 'r' to read log or 'e' to end].");
			try {
				str = br.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (str.equals("r")) {
				mpass.display();
			} else if (str.equals("e")) {
				System.out.println("Exit Sucessfully");
				System.exit(0);
			} else {
				System.out.println("BAD INPUT!");

			}
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException {

		System.out.println("args are - " + args[0] + " and " + args[1]);
		if (args[1].equals("logger")) {
			runLogger(args[0], args[1]);
		} else {
			runApplication(args[0], args[1]);
		}

	}
}
