package database;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(BlockJUnit4ClassRunner.class)
public class SQLInterpreterTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLInterpreterTest.class);
	
	private static SQLInterpreter interpreter = new SQLInterpreter();
	
	private static String rootDirectory = System.getProperty("user.dir");
	
	private static Database database;
	
	@BeforeClass
	public static void setUp() {
		String directory = rootDirectory;
		directory += File.separator;
		directory += "src" + File.separator;
		directory += "test" + File.separator;
		directory += "resources" + File.separator;
		database = new Database(directory);
		
	}
	
	@Test
	public void test1() {
		
		String query = "select * from Car where colour = 'Orange';";
		String path = rootDirectory;
		path += File.separator;
		path += "src" + File.separator;
		path += "test" + File.separator;
		path += "resources" + File.separator;
		for(Table t : database.getTables().values()) {
			t.load(path + t.getName() + ".ddbt");
		}

		LOGGER.info(interpreter.executeQuery(query, database));
	}
}
