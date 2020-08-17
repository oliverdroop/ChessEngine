package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
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

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ResultTreeType;

@RunWith(BlockJUnit4ClassRunner.class)
public class SQLInterpreterTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SQLInterpreterTest.class);
	
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
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one car should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).contains("BK52VJC	Ford	Mondeo");
		
		queryString = "select * from Car where colour = 'Orange' and year_of_registration = '2002' and model = 'Mondeo';";
		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one car should be returned").hasSize(1);
		int columnCount = database.getTables().get("CAR").getColumns().size();
		softly.assertThat(result.get(0).split("\t", 0)).hasSize(columnCount);
		
		queryString = "select * from car;";
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("All 1000 cars should be returned").hasSize(1000);
	}
	
	@Test
	public void testDelete() {
		loadData();
		String queryString = "delete from Car where colour = 'Orange' and year_of_registration = '2002';";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).isEqualTo("Deleted 5 rows from table CAR");
		
		queryString = "delete from Car;";
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).isEqualTo("Deleted 995 rows from table CAR");
	}
	
	@Test
	public void testInsert() {
		String queryString = "insert into Car (Registration, Manufacturer, Model, Colour, year_of_registration, seats, engine_size, taxed) values ('BK52VJC', 'Ford', 'Mondeo', 'Orange', '2002', '5', '1.8', 'true');";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		Table table = database.getTables().get("CAR");
		softly.assertThat(table.getAllRows()).hasSize(1);
		softly.assertThat(table.getRowString(table.getRowByIndex(0))).isEqualTo("0	BK52VJC	Ford	Mondeo	Orange	2002	5	1.8	true");
		
		queryString = "insert into Car values ('1', 'DF17HJB', 'Ford', 'Transit', 'Pink', '2017', '3', '2.4', 'true');";
		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
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
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		softly.assertThat(result.get(0)).isEqualTo("Updated 5 rows in table CAR");
		
		queryString = "update Car set colour = 'Red';";
		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		softly.assertThat(result.get(0)).isEqualTo("Updated 1000 rows in table CAR");
		
		queryString = "select * from Car where colour = 'Red';";
		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		
		softly.assertThat(result).as("All 1000 cars should have been turned red").hasSize(1000);
	}
	
	@Test
	public void testLeftJoin() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner left join car on owner.car_id = car.id;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("5 results should be returned", 5, result.size());
		
		softly.assertThat(result.get(0)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(1)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(2)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(3)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
		softly.assertThat(result.get(4)).isEqualTo("Elvis	Presley			");
	}
	
	@Test
	public void testRightJoin() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner right join car on car.id = owner.car_id;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));

		assertEquals("1000 results should be returned", 1000, result.size());
		
		softly.assertThat(result.get(249)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(499)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(749)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(999)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
	}
	
	@Test
	public void testInnerJoin() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner inner join car on car.id = owner.car_id;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("4 results should be returned", 4, result.size());
		
		softly.assertThat(result.get(0)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(1)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(2)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(3)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
	}
	
	@Test
	public void testFullJoin() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner full join car on car.id = owner.car_id;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("1001 results should be returned", 1001, result.size());
		
		softly.assertThat(result.get(0)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(1)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(2)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(3)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
		softly.assertThat(result.get(4)).isEqualTo("Elvis	Presley			");
		softly.assertThat(result.get(5)).isEqualTo("		XG63PJS	Ford	Focus");
	}
	
	@Test
	public void testLeftJoinWithConditions() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner left join car on owner.car_id = car.id where forename = 'John' and manufacturer = 'Ford';";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("2 results should be returned", 2, result.size());

		softly.assertThat(result.get(0)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(1)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
	}
	
	@Test
	public void testSelectWithOrderBy() {
		loadData();
		String queryString = "select registration, manufacturer, model from car order by registration;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("1000 results should be returned", 1000, result.size());
		softly.assertThat(result.get(0)).isEqualTo("AB04WBF	Ford	Focus");
		softly.assertThat(result.get(999)).isEqualTo("ZZ69VYE	Ford	Fusion");
		
		queryString = "select registration, manufacturer, model from car order by model, registration;";
		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("1000 results should be returned", 1000, result.size());
		softly.assertThat(result.get(0)).isEqualTo("AC04ZCS	Ford	Fiesta");
		softly.assertThat(result.get(999)).isEqualTo("ZY57CKF	Ford	Transit");
	}
	
	@Test
	public void testOperators() {
		loadData();
		String queryString;
		Query query;
		List<String> result;
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size > '2.8';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size > 2.8").hasSize(79);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size >= '2.90';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size >= 2.90").hasSize(79);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size < '1.2';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size < 1.2").hasSize(73);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size <= '1.1';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size <= 1.10").hasSize(73);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size != '1';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size != 1").hasSize(971);
		
		queryString = "select registration, manufacturer, model, engine_size from car where model > 'Fusion';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("Test operator on non-numeric data type", 0, result.size());
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
