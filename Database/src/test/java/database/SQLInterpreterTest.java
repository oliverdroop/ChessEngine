package database;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	
	@Before
	public void setUp(){
		database = new Database(getDataDirectory());		
	}
	
	@Test
	public void testSelect() {
		loadData();
		String queryString = "select Registration, Manufacturer, Model from Car where colour = 'Orange' and year_of_registration = '2002' and model = 'Mondeo';";
		
		Query query = new Query(interpreter.readQuery(queryString), database);
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one car should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).contains("BK52VJC	Ford	Mondeo");
		
		queryString = "select * from Car where colour = 'Orange' and year_of_registration = '2002' and model = 'Mondeo';";
		
		query = new Query(interpreter.readQuery(queryString), database);
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one car should be returned").hasSize(1);
		int columnCount = database.getTables().get("CAR").getColumns().size();
		softly.assertThat(result.get(0).split("\t", 0)).hasSize(columnCount);
		
		queryString = "select * from car;";
		query = new Query(interpreter.readQuery(queryString), database);
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("All 1000 cars should be returned").hasSize(1000);
	}
	
	@Test
	public void testDelete() {
		loadData();
		String queryString = "delete from Car where colour = 'Orange' and year_of_registration = '2002';";
		
		Query query = new Query(interpreter.readQuery(queryString), database);
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).isEqualTo("Deleted 5 rows from table CAR");
		
		queryString = "delete from Car;";
		query = new Query(interpreter.readQuery(queryString), database);
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).isEqualTo("Deleted 995 rows from table CAR");
	}
	
	@Test
	public void testInsert() {
		String queryString = "insert into Car (Registration, Manufacturer, Model, Colour, year_of_registration, seats, engine_size, taxed) values ('BK52VJC', 'Ford', 'Mondeo', 'Orange', '2002', '5', '1.8', 'true');";
		
		Query query = new Query(interpreter.readQuery(queryString), database);
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		Table table = database.getTables().get("CAR");
		softly.assertThat(table.getAllRows()).hasSize(1);
		softly.assertThat(table.getRowString(table.getRowByIndex(0))).isEqualTo("0	BK52VJC	Ford	Mondeo	Orange	2002	5	1.8	true");
		
		queryString = "insert into Car values ('1', 'DF17HJB', 'Ford', 'Transit', 'Pink', '2017', '3', '2.4', 'true');";
		
		query = new Query(interpreter.readQuery(queryString), database);
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		softly.assertThat(table.getAllRows()).hasSize(2);
		softly.assertThat(table.getRowString(table.getRowByIndex(1))).isEqualTo("1	DF17HJB	Ford	Transit	Pink	2017	3	2.4	true");
	}
	
	@Test
	public void testUpdate() {
		loadData();
		String queryString = "update Car set colour = 'Red', taxed = 'false' where colour = 'Orange' and year_of_registration = '2002';";
		
		Query query = new Query(interpreter.readQuery(queryString), database);
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		softly.assertThat(result.get(0)).isEqualTo("Updated 5 rows in table CAR");
		
		queryString = "update Car set colour = 'Red';";
		
		query = new Query(interpreter.readQuery(queryString), database);
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		softly.assertThat(result.get(0)).isEqualTo("Updated 1000 rows in table CAR");
		
		queryString = "select * from Car where colour = 'Red';";
		
		query = new Query(interpreter.readQuery(queryString), database);
		result = query.execute();
		
		softly.assertThat(result).as("All 1000 cars should have been turned red").hasSize(1000);
	}
	
	@Test
	public void testJoin() {		
		loadData();
		String queryString = "select forename, surname from owner left join on owner.car_id = car.id; ";
		
		Query query = new Query(interpreter.readQuery(queryString), database);
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("4 results should be returned").hasSize(4);
		softly.assertThat(result.get(0)).isEqualTo("0	Joe	Bloggs	249	249	LE65RGD	Ford	Focus	Red	2015	5	2.7	true");
		softly.assertThat(result.get(1)).isEqualTo("1	John	Smith	499	499	RX06SYB	Ford	Fiesta	Red	2006	5	2.6	false");
		softly.assertThat(result.get(2)).isEqualTo("2	John	Doe	749	749	US54LJR	Ford	Transit	Blue	2004	5	2.3	true");
		softly.assertThat(result.get(3)).isEqualTo("3	Jane	Doe	999	999	TN68LUX	Ford	Ka	Green	2018	5	1.6	false");
	}
	
	private static String getDataDirectory() {
		String directory = rootDirectory;
		directory += File.separator;
		directory += "src" + File.separator;
		directory += "test" + File.separator;
		directory += "resources" + File.separator;
		return directory;
	}
	
	private static void loadData() {
		String path = getDataDirectory();
		for(Table t : database.getTables().values()) {
			t.load(path + t.getName() + ".ddbt");
		}
	}
}
