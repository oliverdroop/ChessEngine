package database;

import java.io.File;
import java.util.List;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
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
	
	@Rule
	public JUnitSoftAssertions softly = new JUnitSoftAssertions();
	
	@BeforeClass
	public static void setUpClass() {
		database = new Database(getDataDirectory());
		
	}
	
	@Before
	public void setUp() {
		String path = getDataDirectory();
		for(Table t : database.getTables().values()) {
			t.load(path + t.getName() + ".ddbt");
		}
	}
	
	@Test
	public void testSelect() {		
		String queryString = "select Registration, Manufacturer, Model from Car where colour = 'Orange' and year_of_registration = '2002' and model = 'Mondeo';";
		
		Query query = interpreter.buildQuery(queryString, database);
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one car should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).contains("BK52VJC	Ford	Mondeo");
	}
	
	@Test
	public void testDelete() {
		String queryString = "delete from Car where colour = 'Orange' and year_of_registration = '2002';";
		
		Query query = interpreter.buildQuery(queryString, database);
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).isEqualTo("Deleted 5 rows from table CAR");
	}
	
	private static String getDataDirectory() {
		String directory = rootDirectory;
		directory += File.separator;
		directory += "src" + File.separator;
		directory += "test" + File.separator;
		directory += "resources" + File.separator;
		return directory;
	}
}
