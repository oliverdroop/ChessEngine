package database;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(BlockJUnit4ClassRunner.class)
public class SQLInterpreterTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLInterpreterTest.class);
	
	private static SQLInterpreter interpreter = new SQLInterpreter();
	
	@Test
	public void test1() {
		
		LOGGER.info(interpreter.executeQuery("select * from cars where id = '1'"));
	}
}
