package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.SQLException;
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
		database = new Database(getDataDirectory(), false);		
	}
	
	@Test
	public void testSelect() {
		loadData();
		String queryString = "select Registration, Manufacturer, Model from Car where colour = 'Orange' and year_of_registration = '2002' and model = 'Mondeo';";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one car should be returned").hasSize(2);		
		softly.assertThat(result.get(1)).contains("BK52VJC	Ford	Mondeo");
		
		queryString = "select * from Car where colour = 'Orange' and year_of_registration = '2002' and model = 'Mondeo';";
		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Only one car should be returned").hasSize(2);
		int columnCount = database.getTables().get("CAR").getColumns().size();
		softly.assertThat(result.get(1).split("\t", 0)).hasSize(columnCount);
		
		queryString = "select * from car;";
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("All 1000 cars should be returned").hasSize(1001);
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
		
		softly.assertThat(result).as("Only one car should be returned").hasSize(1);
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
		
		softly.assertThat(result).as("All 1000 cars should have been turned red").hasSize(1001);
	}
	
	@Test
	public void testLeftJoin() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner left join car on owner.car_id = car.id;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("6 results should be returned", 6, result.size());
		
		softly.assertThat(result.get(1)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(2)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(3)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(4)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
		softly.assertThat(result.get(5)).isEqualTo("Elvis	Presley			");
	}
	
	@Test
	public void testRightJoin() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner right join car on car.id = owner.car_id;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));

		assertEquals("1001 results should be returned", 1001, result.size());
		
		softly.assertThat(result.get(250)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(500)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(750)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(1000)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
	}
	
	@Test
	public void testInnerJoin() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner inner join car on car.id = owner.car_id;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		//result.forEach(line -> LOGGER.info(line));
		LOGGER.info(ResultFormatter.formatResult(result));
		
		assertEquals("5 results should be returned", 5, result.size());
		
		softly.assertThat(result.get(1)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(2)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(3)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(4)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
	}
	
	@Test
	public void testFullJoin() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner full join car on car.id = owner.car_id;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("1002 results should be returned", 1002, result.size());
		
		softly.assertThat(result.get(1)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(2)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(3)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(4)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
		softly.assertThat(result.get(5)).isEqualTo("Elvis	Presley			");
		softly.assertThat(result.get(6)).isEqualTo("		XG63PJS	Ford	Focus");
	}
	
	@Test
	public void testLeftJoinWithConditions() {
		loadData();
		String queryString = "select forename, surname, registration, manufacturer, model from owner left join car on owner.car_id = car.id where forename = 'John' and manufacturer = 'Ford';";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("3 results should be returned", 3, result.size());

		softly.assertThat(result.get(1)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(2)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
	}
	
	@Test
	public void testSelectWithOrderBy() {
		loadData();
		String queryString = "select registration, manufacturer, model from car order by registration;";
		
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("1001 results should be returned", 1001, result.size());
		softly.assertThat(result.get(1)).isEqualTo("AB04WBF	Ford	Focus");
		softly.assertThat(result.get(1000)).isEqualTo("ZZ69VYE	Ford	Fusion");
		
		queryString = "select registration, manufacturer, model from car order by model, registration;";
		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("1001 results should be returned", 1001, result.size());
		softly.assertThat(result.get(1)).isEqualTo("AC04ZCS	Ford	Fiesta");
		softly.assertThat(result.get(1000)).isEqualTo("ZY57CKF	Ford	Transit");
		
		queryString = "select * from car where colour = 'Red' order by model, registration;";
		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		assertEquals("119 results should be returned", 119, result.size());
		String header = "ID	REGISTRATION	MANUFACTURER	MODEL	COLOUR	YEAR_OF_REGISTRATION	SEATS	ENGINE_SIZE	TAXED";
		softly.assertThat(result.get(0)).isEqualTo(header);
		softly.assertThat(result.get(1)).contains("AP64KMV");
		softly.assertThat(result.get(118)).contains("YP05UYK");
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
		
		softly.assertThat(result).as("Test number of cars with engine size > 2.8").hasSize(80);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size >= '2.90';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size >= 2.90").hasSize(80);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size < '1.2';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size < 1.2").hasSize(74);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size <= '1.1';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size <= 1.10").hasSize(74);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size != '1';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with engine size != 1").hasSize(972);
		
		queryString = "select registration, manufacturer, model, engine_size from car where model like 'si';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test number of cars with si in the model").hasSize(279);
		
		queryString = "select registration, manufacturer, model, engine_size from car where model > 'Fusion';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test > operator on non-numeric data type").hasSize(1);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size like '2.56';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Test LIKE operator on numeric data type").hasSize(1);
	}
	
	@Test
	public void testNumericValues() {
		loadData();
		String queryString;
		Query query;
		List<String> result;
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size = 3;";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).hasSize(29);
		
		queryString = "select registration, manufacturer, model, engine_size from car where engine_size > 2.9;";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).hasSize(29);
	}
	
	@Test
	public void testInvalidSQL() {
		loadData();
		String queryString;
		Query query;
		List<String> result;
		
		queryString = "select invalid_column_name from car where invalid_column_name > '2.8';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Invalid column names should return no results").hasSize(0);
		
		queryString = "invalid_instruction registration from car where engine_size > '2.8';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Invalid instructions should return no results").hasSize(0);
		
		queryString = "select registration invalid_keyword car where engine_size > '2.8';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Invalid keywords should return no results").hasSize(0);
		
		queryString = "invalid_instruction registration invalid_keyword car where engine_size > '2.8';";		
		query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		result = query.execute();
		result.forEach(line -> LOGGER.info(line));
		
		softly.assertThat(result).as("Invalid keywords should return no results").hasSize(0);
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
