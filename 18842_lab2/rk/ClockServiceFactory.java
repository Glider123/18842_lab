public class ClockServiceFactory {
	private static ClockService cs;

	public static ClockService setType(String type, int processNo, int usersNum) {
		System.out.println(type);
		if (type.equals("vector"))
			cs = new VectorClockService(processNo, usersNum);
		else if (type.equals("logical"))
			cs = new LogicalClockService();
		else
			System.out
					.println("Configuration file error: no such type of clock service.");
		return cs;
	}

}
