package project.cs249.src.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static LocalDateTime now = LocalDateTime.now();
	public static void info(Class<?> logClass, String message){
		if (message != null) {
			System.out.println(dtf.format(now)+" [INFO] " + logClass.getName() + ": " + message);
		}
	}

	public static void error(Class<?> logClass, String message){
		if (message != null) {
			System.err.println(dtf.format(now)+" [ERROR] " + logClass.getName() + ": " + message);
		}
	}
}