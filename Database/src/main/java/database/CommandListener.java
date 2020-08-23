package database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class CommandListener {
	public static void listen(Database database) {
		String command = listenOnce();
		while(command != null) {
			Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(command), database));
			System.out.println(ResultFormatter.formatResult(query.execute()));
			command = listenOnce();
		}
	}
	
	public static String listenOnce() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			StringBuilder output = new StringBuilder();
			while (br.ready() || output.length() == 0) {
				output.append(br.readLine());
			}
			return output.toString();
		} catch(IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
}
