package database;

import static org.junit.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		List<String> result = testSQL("select Registration, Manufacturer, Model from Car where colour = 'Orange' and year_of_registration = '2002' and model = 'Mondeo';");		
		softly.assertThat(result).as("Only one car should be returned").hasSize(2);		
		softly.assertThat(result.get(1)).contains("BK52VJC	Ford	Mondeo");
		
		result = testSQL("select * from Car where colour = 'Orange' and year_of_registration = '2002' and model = 'Mondeo';");		
		softly.assertThat(result).as("Only one car should be returned").hasSize(2);
		int columnCount = database.getTables().get("CAR").getColumns().size();
		softly.assertThat(result.get(1).split("\t", 0)).hasSize(columnCount);
		
		result = testSQL("select * from car;");		
		softly.assertThat(result).as("All 1000 cars should be returned").hasSize(1001);
	}
	
	@Test
	public void testDelete() {
		loadData();
		List<String> result = testSQL("delete from Car where colour = 'Orange' and year_of_registration = '2002';");
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).isEqualTo("Deleted 5 rows from table CAR");
		
		result = testSQL("delete from Car;");
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);		
		softly.assertThat(result.get(0)).isEqualTo("Deleted 995 rows from table CAR");
	}
	
	@Test
	public void testInsert() {
		List<String> result = testSQL("insert into Car (Registration, Manufacturer, Model, Colour, year_of_registration, seats, engine_size, taxed) values ('BK52VJC', 'Ford', 'Mondeo', 'Orange', '2002', '5', '1.8', 'true');");
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		Table table = database.getTables().get("CAR");
		softly.assertThat(table.getAllRows()).hasSize(1);
		softly.assertThat(table.getRowString(table.getRowByIndex(0))).isEqualTo("0	BK52VJC	Ford	Mondeo	Orange	2002	5	1.8	true");
		
		result = testSQL("insert into Car values ('1', 'DF17HJB', 'Ford', 'Transit', 'Pink', '2017', '3', '2.4', 'true');");
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		softly.assertThat(table.getAllRows()).hasSize(2);
		softly.assertThat(table.getRowString(table.getRowByIndex(1))).isEqualTo("1	DF17HJB	Ford	Transit	Pink	2017	3	2.4	true");
	}
	
	@Test
	public void testUpdate() {
		loadData();
		List<String> result = testSQL("update Car set colour = 'Red', taxed = 'false' where colour = 'Orange' and year_of_registration = '2002';");
		softly.assertThat(result).as("Only one car should be returned").hasSize(1);
		softly.assertThat(result.get(0)).isEqualTo("Updated 5 rows in table CAR");
		
		result = testSQL("update Car set colour = 'Red';");
		softly.assertThat(result).as("Only one line should be returned").hasSize(1);
		softly.assertThat(result.get(0)).isEqualTo("Updated 1000 rows in table CAR");
		
		result = testSQL("select * from Car where colour = 'Red';");
		softly.assertThat(result).as("All 1000 cars should have been turned red").hasSize(1001);
	}
	
	@Test
	public void testLeftJoin() {
		loadData();
		List<String> result = testSQL("select forename, surname, registration, manufacturer, model from owner left join car on owner.car_id = car.id;");
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
		List<String> result = testSQL("select forename, surname, registration, manufacturer, model from owner right join car on car.id = owner.car_id;");		
		assertEquals("1001 results should be returned", 1001, result.size());
		
		softly.assertThat(result.get(250)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(500)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(750)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(1000)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
	}
	
	@Test
	public void testInnerJoin() {
		loadData();
		List<String> result = testSQL("select forename, surname, registration, manufacturer, model from owner inner join car on car.id = owner.car_id;");
		assertEquals("5 results should be returned", 5, result.size());
		
		softly.assertThat(result.get(1)).isEqualTo("Joe	Bloggs	LE65RGD	Ford	Focus");
		softly.assertThat(result.get(2)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(3)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
		softly.assertThat(result.get(4)).isEqualTo("Jane	Doe	TN68LUX	Ford	Ka");
	}
	
	@Test
	public void testFullJoin() {
		loadData();
		List<String> result = testSQL("select forename, surname, registration, manufacturer, model from owner full join car on car.id = owner.car_id;");
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
		List<String> result = testSQL("select forename, surname, registration, manufacturer, model from owner left join car on owner.car_id = car.id where forename = 'John' and manufacturer = 'Ford';");
		assertEquals("3 results should be returned", 3, result.size());

		softly.assertThat(result.get(1)).isEqualTo("John	Smith	RX06SYB	Ford	Fiesta");
		softly.assertThat(result.get(2)).isEqualTo("John	Doe	US54LJR	Ford	Transit");
	}
	
	@Test
	public void testSelectWithOrderBy() {
		loadData();
		List<String> result = testSQL("select registration, manufacturer, model from car order by registration;");
		assertEquals("1001 results should be returned", 1001, result.size());
		softly.assertThat(result.get(1)).isEqualTo("AB04WBF	Ford	Focus");
		softly.assertThat(result.get(1000)).isEqualTo("ZZ69VYE	Ford	Fusion");
		
		result = testSQL("select registration, manufacturer, model from car order by model, registration;");
		assertEquals("1001 results should be returned", 1001, result.size());
		softly.assertThat(result.get(1)).isEqualTo("AC04ZCS	Ford	Fiesta");
		softly.assertThat(result.get(1000)).isEqualTo("ZY57CKF	Ford	Transit");
		
		result = testSQL("select * from car where colour = 'Red' order by model, registration;");
		assertEquals("119 results should be returned", 119, result.size());
		String header = "ID	REGISTRATION	MANUFACTURER	MODEL	COLOUR	YEAR_OF_REGISTRATION	SEATS	ENGINE_SIZE	TAXED";
		softly.assertThat(result.get(0)).isEqualTo(header);
		softly.assertThat(result.get(1)).contains("AP64KMV");
		softly.assertThat(result.get(118)).contains("YP05UYK");
	}
	
	@Test
	public void testOperators() {
		loadData();
		
		List<String> result = testSQL("select registration, manufacturer, model, engine_size from car where engine_size > '2.8';");
		softly.assertThat(result).as("Test number of cars with engine size > 2.8").hasSize(80);
		
		result = testSQL("select registration, manufacturer, model, engine_size from car where engine_size >= '2.90';");
		softly.assertThat(result).as("Test number of cars with engine size >= 2.90").hasSize(80);
		
		result = testSQL("select registration, manufacturer, model, engine_size from car where engine_size < '1.2';");
		softly.assertThat(result).as("Test number of cars with engine size < 1.2").hasSize(74);
		
		result = testSQL("select registration, manufacturer, model, engine_size from car where engine_size <= '1.1';");
		softly.assertThat(result).as("Test number of cars with engine size <= 1.10").hasSize(74);
		
		result = testSQL("select registration, manufacturer, model, engine_size from car where engine_size != '1';");
		softly.assertThat(result).as("Test number of cars with engine size != 1").hasSize(972);
		
		result = testSQL("select registration, manufacturer, model, engine_size from car where model like 'si';");
		softly.assertThat(result).as("Test number of cars with si in the model").hasSize(279);
		
		result = testSQL("select registration, manufacturer, model, engine_size from car where model > 'Fusion';");
		softly.assertThat(result).as("Test > operator on non-numeric data type").hasSize(1);
		
		result = testSQL("select registration, manufacturer, model, engine_size from car where engine_size like '2.56';");
		softly.assertThat(result).as("Test LIKE operator on numeric data type").hasSize(1);
	}
	
	@Test
	public void testNumericValues() {
		loadData();
		List<String> result = testSQL("select registration, manufacturer, model, engine_size from car where engine_size = 3;");
		softly.assertThat(result).hasSize(29);
		
		result = testSQL("select registration, manufacturer, model, engine_size from car where engine_size > 2.9;");
		softly.assertThat(result).hasSize(29);
	}
	
	@Test
	public void testInvalidSQL() {
		loadData();
		List<String> result = testSQL("select invalid_column_name from car where invalid_column_name > '2.8';");
		softly.assertThat(result).as("Invalid column names should return no results").hasSize(0);
		
		result = testSQL("invalid_instruction registration from car where engine_size > '2.8';");
		softly.assertThat(result).as("Invalid instructions should return no results").hasSize(0);
		
		result = testSQL("select registration invalid_keyword car where engine_size > '2.8';");
		softly.assertThat(result).as("Invalid keywords should return no results").hasSize(0);
		
		result = testSQL("invalid_instruction registration invalid_keyword car where engine_size > '2.8';");
		softly.assertThat(result).as("Invalid keywords should return no results").hasSize(0);
	}
	
	@Test
	public void testCreateTable() {
		String tableName = "ADDRESS";
		String houseNumber = "HOUSE_NUMBER";
		String street = "STREET";
		String town = "TOWN";
		String postcode = "POSTCODE";
		
		List<String> result = testSQL("create table address;");
		softly.assertThat(database.getTables().keySet()).contains(tableName);
		softly.assertThat(database.getTables().get(tableName).getName()).isEqualTo(tableName);
		database.removeTable(tableName);
		
		result = testSQL("create table address (id long, house_number varchar(4), street varchar(16), town varchar(16), postcode varchar(4));");
		softly.assertThat(database.getTables().keySet()).contains(tableName);
		softly.assertThat(database.getTables().get(tableName).getName()).isEqualTo(tableName);
		softly.assertThat(database.getTables().get(tableName).getColumns()).hasSize(5);
		assertThat(database.getTables().get(tableName).getColumns().get("ID")).isNotNull();
		softly.assertThat(database.getTables().get(tableName).getColumns().get("ID").getDataType()).isEqualTo(DataType.LONG);
		assertThat(database.getTables().get(tableName).getColumns().get(houseNumber)).isNotNull();
		softly.assertThat(database.getTables().get(tableName).getColumns().get(houseNumber).getDataType()).isEqualTo(DataType.VARCHAR);
		softly.assertThat(database.getTables().get(tableName).getColumns().get(houseNumber).getLength()).isEqualTo(8);
		assertThat(database.getTables().get(tableName).getColumns().get(street)).isNotNull();
		softly.assertThat(database.getTables().get(tableName).getColumns().get(street).getDataType()).isEqualTo(DataType.VARCHAR);
		softly.assertThat(database.getTables().get(tableName).getColumns().get(street).getLength()).isEqualTo(32);
		assertThat(database.getTables().get(tableName).getColumns().get(town)).isNotNull();
		softly.assertThat(database.getTables().get(tableName).getColumns().get(town).getDataType()).isEqualTo(DataType.VARCHAR);
		softly.assertThat(database.getTables().get(tableName).getColumns().get(town).getLength()).isEqualTo(32);
		assertThat(database.getTables().get(tableName).getColumns().get(postcode)).isNotNull();
		softly.assertThat(database.getTables().get(tableName).getColumns().get(postcode).getDataType()).isEqualTo(DataType.VARCHAR);
		softly.assertThat(database.getTables().get(tableName).getColumns().get(postcode).getLength()).isEqualTo(8);
	}
	
	private List<String> testSQL(String queryString){
		LOGGER.info("SQL Test : {}", queryString);
		Query query = new Query(SQLInterpreter.interpret(SQLLexer.readQuery(queryString), database));
		List<String> result = query.execute();
		result.forEach(line -> LOGGER.debug(line));
		LOGGER.info("Returned {} results", result.size());
		return result;
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
