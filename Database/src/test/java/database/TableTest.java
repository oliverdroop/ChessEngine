package database;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Pair;

@RunWith(BlockJUnit4ClassRunner.class)
public class TableTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TableTest.class);
	
	private static Database database;
	
	private String manufacturer = "Ford";
	
	private String[] models = new String[] {"Ka", "Fiesta", "Fusion", "Focus", "Mondeo", "Kuga", "Transit"};
	
	private String[] colours = new String[] {"White", "Black", "Silver", "Red", "Blue", "Yellow", "Purple", "Orange", "Green"};
	
	private Random random = new Random();
	
	private static String rootDirectory = System.getProperty("user.dir");
	
	@Rule
	public JUnitSoftAssertions softly = new JUnitSoftAssertions();
	
	@BeforeClass
	public static void setUp() {
		String directory = rootDirectory;
		directory += File.separator;
		directory += "src" + File.separator;
		directory += "test" + File.separator;
		directory += "resources" + File.separator;
		database = new Database(directory, false);
	}
	
	@Test
	public void testSequentialWrite() {
		int count = 1000;
		ObjectParser parser = new ObjectParser(database);
		Table table = null;
		for(int i = 0; i < count; i++) {
			Car car = createRandomCar();
			if (table == null) {				
				 table = parser.getApplicableTable(car);
			}
			table.addRow(parser.parse(car));
		}
		int rowLength = table.getRowLength();
		int expectedDatabaseLength = rowLength * count;
		
		String message = ("Expected data length to be " + expectedDatabaseLength);
		softly.assertThat(table.getData().length).as(message).isEqualTo(expectedDatabaseLength);
		
		try {
			Path tempDirectoryPath = Files.createTempDirectory("temp");
			String fileName = tempDirectoryPath.toString() + File.separator + table.getName() + ".ddbt";
			
			table.save(fileName);
			table.load(fileName);
			
			softly.assertThat(table.getData().length).as(message).isEqualTo(expectedDatabaseLength);

			Files.delete(new File(fileName).toPath());
			Files.delete(tempDirectoryPath);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testGetByteMatchedRows() {		
		Table table = setUpMatchedRowsTestTable();
		
		Map<String, Pair<Operator, byte[]>> propertyValueMap = new HashMap<>();
		propertyValueMap.put("COLOUR", Operator.EQUAL.pairWith("Black".getBytes()));
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 3 black cars").hasSize(3);
		
		propertyValueMap = new HashMap<>();
		propertyValueMap.put("ENGINE_SIZE", Operator.EQUAL.pairWith(ByteBuffer.allocate(8).putDouble(1.25).array()));
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 2 cars with engineSize 1.25").hasSize(2);
		
		propertyValueMap = new HashMap<>();
		propertyValueMap.put("MODEL", Operator.EQUAL.pairWith("Fiesta".getBytes()));
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 4 Fiestas").hasSize(4);
		
		propertyValueMap.put("COLOUR", Operator.EQUAL.pairWith("Black".getBytes()));
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 3 Black Fiestas").hasSize(3);
		
		propertyValueMap.put("ENGINE_SIZE", Operator.EQUAL.pairWith(ByteBuffer.allocate(8).putDouble(1.25).array()));
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 2 Black Fiestas with engineSize 1.25").hasSize(2);
		
		propertyValueMap.put("TAXED", Operator.EQUAL.pairWith(new byte[] {1}));
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 1 taxed Black Fiesta with engineSize 1.25").hasSize(1);
		
		propertyValueMap.put("SEATS", Operator.EQUAL.pairWith(new byte[] {2}));
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find no taxed Black 1.25 Fiestas with 2 seats").isEmpty();
	}
	
	@Test
	public void testGetStringMatchedRows() {
		Table table = setUpMatchedRowsTestTable();
		
		Map<String, Pair<Operator, String>> propertyStringMap = new HashMap<>();
		propertyStringMap.put("COLOUR", Operator.EQUAL.pairWith("Black"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 3 black cars").hasSize(3);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("ENGINE_SIZE", Operator.EQUAL.pairWith("1.25"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 2 cars with engineSize 1.25").hasSize(2);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("MODEL", Operator.EQUAL.pairWith("Fiesta"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 4 Fiestas").hasSize(4);
		
		propertyStringMap.put("COLOUR", Operator.EQUAL.pairWith("Black"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 3 Black Fiestas").hasSize(3);
		
		propertyStringMap.put("ENGINE_SIZE", Operator.EQUAL.pairWith("1.25"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 2 Black Fiestas with engineSize 1.25").hasSize(2);
		
		propertyStringMap.put("TAXED", Operator.EQUAL.pairWith("true"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 1 taxed Black Fiesta with engineSize 1.25").hasSize(1);
		
		propertyStringMap.put("SEATS", Operator.EQUAL.pairWith("2"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find no taxed Black 1.25 Fiestas with 2 seats").isEmpty();
	}
	
	@Test
	public void testAddDuplicatePrimaryKey() {
		setUp();
		Car car = createCar("D730DAV", "Ford", "Sierra", "Blue", 1986, (byte) 5, 1.4, false);
		car.setId(0L);
		ObjectParser parser = new ObjectParser(database);		
		Table table = parser.getApplicableTable(car);
		table.setAutoGenerateKey(false);
		
		table.addRow(parser.parse(car));
		car.setId(1L);
		softly.assertThat(table.countRows()).as("Table %s should contain only 1 row", table.getName()).isEqualTo(1);
		table.addRow(parser.parse(car));
		softly.assertThat(table.countRows()).as("Table %s should contain 2 rows", table.getName()).isEqualTo(2);
		table.addRow(parser.parse(car));
		softly.assertThat(table.countRows()).as("Table %s should contain 2 rows", table.getName()).isEqualTo(2);

		table.setAutoGenerateKey(true);
		table.addRow(parser.parse(car));
		softly.assertThat(table.countRows()).as("Table %s should contain 3 rows", table.getName()).isEqualTo(3);
		Map<String, Pair<Operator, String>> propertyStringMap = new HashMap<>();
		propertyStringMap.put("ID", Operator.EQUAL.pairWith("1"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Only 1 row should be returned with id of 1").hasSize(1);
		propertyStringMap.put("ID", Operator.EQUAL.pairWith("2"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Only 1 row should be returned with id of 2").hasSize(1);
	}
	
	@Test
	public void testDeleteRows() {
		Table table = setUpMatchedRowsTestTable();
		Map<String, Pair<Operator, String>> propertyStringMap = new HashMap<>();
		propertyStringMap.put("MODEL", Operator.EQUAL.pairWith("Focus"));
		ObjectParser parser = new ObjectParser(database);
		Car focus = (Car)parser.unparse(table.getStringMatchedRows(propertyStringMap).get(0), table);
		byte[] focusId = DataType.getBytes(focus.getId());
		
		softly.assertThat(table.countRows()).as("Table %s should have a size of 5 when the test starts", table.getName()).isEqualTo(5);		
		table.deleteRow(focusId);
		softly.assertThat(table.countRows()).as("Table %s should have a size of 4 after the first removal", table.getName()).isEqualTo(4);
		
		propertyStringMap.remove("MODEL");
		propertyStringMap.put("TAXED", Operator.EQUAL.pairWith("false"));
		table.deleteStringMatchedRows(propertyStringMap);
		softly.assertThat(table.countRows()).as("Table %s should have a size of 3 after the second removal", table.getName()).isEqualTo(3);
		
		propertyStringMap.remove("TAXED");
		propertyStringMap.put("COLOUR", Operator.EQUAL.pairWith("Black"));
		table.deleteStringMatchedRows(propertyStringMap);
		softly.assertThat(table.countRows()).as("Table %s should have a size of 1 after the third removal", table.getName()).isEqualTo(1);
		
		byte[] finalId = DataType.getBytes(3L);
		table.deleteRow(finalId);
		softly.assertThat(table.countRows()).as("Table %s should have a size of 0 after the final removal", table.getName()).isEqualTo(0);
	}
	
	@Test
	public void testUpdateRows() {
		Table table = setUpMatchedRowsTestTable();
		Map<String, Pair<Operator, String>> propertyStringMap = new HashMap<>();
		propertyStringMap.put("MODEL", Operator.EQUAL.pairWith("Focus"));
		
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Only one Focus should exist in database").hasSize(1);
		Map<String,String> updateMap = new HashMap<>();
		updateMap.put("MODEL", "Fiesta");
		table.updateStringMatchedRows(propertyStringMap, updateMap);
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("No Focuses should exist in database").hasSize(0);
		
		propertyStringMap.remove("MODEL");
		propertyStringMap.put("ENGINE_SIZE", Operator.EQUAL.pairWith("1.6"));
		updateMap.put("MODEL", "Fiesta RS");
		updateMap.put("COLOUR", "Yellow");
		updateMap.put("SEATS", "2");
		
		table.updateStringMatchedRows(propertyStringMap, updateMap);
		
		propertyStringMap = new HashMap<>();
		for(String column : updateMap.keySet()) {
			propertyStringMap.put(column, Operator.EQUAL.pairWith(updateMap.get(column)));
		}
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Two updated cars should exist with the specified properties").hasSize(2);
	}
	
	@Test
	public void testSortRows() {
		Table table = setUpMatchedRowsTestTable();
		Column columnReg = table.getColumns().get("REGISTRATION");
		List<byte[]> rows = table.sortRows(columnReg, true, Arrays.asList(table.getAllRows()));
		
		assertEquals(5, rows.size());
		softly.assertThat(table.getValueString(columnReg, rows.get(0))).as("Test sort by registration").isEqualTo("AF20BGD");
		
		Column columnColour = table.getColumns().get("COLOUR");
		
		rows = table.sortRows(columnColour, true, rows);
		softly.assertThat(table.getValueString(columnReg, rows.get(3))).as("Test sort by colour").isEqualTo("AF20BGD");
		
		rows = table.sortRows(columnColour, true, Arrays.asList(table.getAllRows()));
		softly.assertThat(table.getValueString(columnReg, rows.get(0))).as("Test sort by colour").isEqualTo("LR20PNM");
	}
	
	@Test
	public void testOperators() {
		Table table = setUpMatchedRowsTestTable();
		Map<String, Pair<Operator, String>> propertyStringMap = new HashMap<>();

		propertyStringMap.put("ENGINE_SIZE", Operator.GREATER.pairWith("0.9"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Test engine size greater than 0.9").hasSize(5);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("ENGINE_SIZE", Operator.GREATER.pairWith("1.25"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Test engine size greater than 1.25").hasSize(3);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("ENGINE_SIZE", Operator.GREATER_EQUAL.pairWith("1.40"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Test engine size greater than or equal to 1.4").hasSize(3);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("ENGINE_SIZE", Operator.LESS.pairWith("1.40"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Test engine size less than 1.4").hasSize(2);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("ENGINE_SIZE", Operator.LESS_EQUAL.pairWith("1.40"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Test engine size less than or equal to 1.4").hasSize(3);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("ENGINE_SIZE", Operator.NOT_EQUAL.pairWith("1.40"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Test engine size not 1.4").hasSize(4);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("ENGINE_SIZE", Operator.EQUAL.pairWith("1.40"));
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Test engine size equals 1.4").hasSize(1);
	}
	
	@Test
	public void testAddColumn() {
		Table table = setUpMatchedRowsTestTable();
		int newColumnLength = 16;
		int dataLength = table.getData().length;
		int rowCount = table.countRows();
		int rowLength = table.getRowLength();
		Column newColumn = new Column("OWNER_NAME", DataType.VARCHAR, newColumnLength / DataType.VARCHAR.getLength());
		table.addColumn(newColumn);
		
		softly.assertThat(table.getRowLength()).as("Row length appears not to have been updated").isEqualTo(rowLength + newColumnLength);
		softly.assertThat(table.countRows()).as("Row count should not have changed").isEqualTo(rowCount);
		softly.assertThat(table.getData().length).as("Data length should have been updated").isEqualTo((rowCount * newColumnLength) + dataLength);
		softly.assertThat(table.getColumns().get(newColumn.getName())).as("New column doesn't match added column").isEqualTo(newColumn);
		softly.assertThat(table.getColumns().get(newColumn.getName()).getDataType()).as("Wrong data type on new column").isEqualTo(DataType.VARCHAR);
		for(int i = 0; i < rowCount; i++) {
			int startOfAddition = i * table.getRowLength() + rowLength;
			int endOfAddition = i * table.getRowLength() + rowLength + newColumnLength;
			for(int iByte = startOfAddition; i < endOfAddition; i++) {
				softly.assertThat(table.getData()[iByte]).as("Newly-added data should be blank").isEqualTo(Byte.parseByte("0"));
			}
		}
		softly.assertThat(table.getValueString(table.getColumns().get("ID"), table.getRowByIndex(3))).isEqualTo("3");
		softly.assertThat(table.getValueString(table.getColumns().get("REGISTRATION"), table.getRowByIndex(3))).isEqualTo("AF20BGD");
		softly.assertThat(table.getValueString(table.getColumns().get("MANUFACTURER"), table.getRowByIndex(3))).isEqualTo("Ford");
		softly.assertThat(table.getValueString(table.getColumns().get("MODEL"), table.getRowByIndex(3))).isEqualTo("Fiesta");
		softly.assertThat(table.getValueString(table.getColumns().get("COLOUR"), table.getRowByIndex(3))).isEqualTo("Blue");
		softly.assertThat(table.getValueString(table.getColumns().get("YEAR_OF_REGISTRATION"), table.getRowByIndex(3))).isEqualTo("2020");
		softly.assertThat(table.getValueString(table.getColumns().get("SEATS"), table.getRowByIndex(3))).isEqualTo("5");
		softly.assertThat(table.getValueString(table.getColumns().get("ENGINE_SIZE"), table.getRowByIndex(3))).isEqualTo("1.6");
		softly.assertThat(table.getValueString(table.getColumns().get("TAXED"), table.getRowByIndex(3))).isEqualTo("true");
	}
	
	@Test
	public void testResizeColumn() {
		Table table = setUpMatchedRowsTestTable();
		Column column = table.getColumns().get("REGISTRATION");
		table.resizeColumn(column.getName(), 4);
		
		byte[] row = table.getRowByIndex(3);
		softly.assertThat(table.getValueString(table.getColumns().get("ID"), row)).isEqualTo("3");
		softly.assertThat(table.getValueString(table.getColumns().get("REGISTRATION"), row)).isEqualTo("AF20");
		softly.assertThat(table.getValueString(table.getColumns().get("MANUFACTURER"), row)).isEqualTo("Ford");
		softly.assertThat(table.getValueString(table.getColumns().get("MODEL"), row)).isEqualTo("Fiesta");
		softly.assertThat(table.getValueString(table.getColumns().get("COLOUR"), row)).isEqualTo("Blue");
		softly.assertThat(table.getValueString(table.getColumns().get("YEAR_OF_REGISTRATION"), row)).isEqualTo("2020");
		softly.assertThat(table.getValueString(table.getColumns().get("SEATS"), row)).isEqualTo("5");
		softly.assertThat(table.getValueString(table.getColumns().get("ENGINE_SIZE"), row)).isEqualTo("1.6");
		softly.assertThat(table.getValueString(table.getColumns().get("TAXED"), row)).isEqualTo("true");
		
		table = setUpMatchedRowsTestTable();
		column = table.getColumns().get("REGISTRATION");
		table.resizeColumn(column.getName(), 12);
		
		row = table.getRowByIndex(3);
		softly.assertThat(table.getValueString(table.getColumns().get("ID"), row)).isEqualTo("3");
		softly.assertThat(table.getValueString(column, row)).isEqualTo("AF20BGD");
		softly.assertThat(table.getValueBytes(column, row)).isEqualTo(new byte[] {65, 70, 50, 48, 66, 71, 68, 0, 0, 0, 0, 0});
		softly.assertThat(table.getValueString(table.getColumns().get("MANUFACTURER"), row)).isEqualTo("Ford");
		softly.assertThat(table.getValueString(table.getColumns().get("MODEL"), row)).isEqualTo("Fiesta");
		softly.assertThat(table.getValueString(table.getColumns().get("COLOUR"), row)).isEqualTo("Blue");
		softly.assertThat(table.getValueString(table.getColumns().get("YEAR_OF_REGISTRATION"), row)).isEqualTo("2020");
		softly.assertThat(table.getValueString(table.getColumns().get("SEATS"), row)).isEqualTo("5");
		softly.assertThat(table.getValueString(table.getColumns().get("ENGINE_SIZE"), row)).isEqualTo("1.6");
		softly.assertThat(table.getValueString(table.getColumns().get("TAXED"), row)).isEqualTo("true");
	}
	
	private Table setUpMatchedRowsTestTable() {
		setUp();
		int year = 2020;
		ObjectParser parser = new ObjectParser(database);
		Car car1 = createCar("LR20PNM", "Ford", "Fiesta", "Black", year, (byte)5, 1.4, true);
		Table table = parser.getApplicableTable(car1);
		table.setLastGeneratedKey(null);
		table.setData(null);
		table.setClassName("database.Car");
		table.addRow(parser.parse(car1));
		Car car2 = createCar("EN20TFU", "Ford", "Fiesta", "Black", year, (byte)5, 1.25, true);
		table.addRow(parser.parse(car2));
		Car car3 = createCar("RB20XFW", "Ford", "Focus", "Blue", year, (byte)5, 1.6, true);
		table.addRow(parser.parse(car3));
		Car car4 = createCar("AF20BGD", "Ford", "Fiesta", "Blue", year, (byte)5, 1.6, true);
		table.addRow(parser.parse(car4));
		Car car5 = createCar("BG20BNQ", "Ford", "Fiesta", "Black", year, (byte)5, 1.25, false);
		table.addRow(parser.parse(car5));
		return table;
	}
	
	private Car createRandomCar() {
		Car car = new Car();
		int year = random.nextInt(18) + 2002;
		car.setRegistration(createRandomRegistration(year));
		car.setManufacturer(manufacturer);
		car.setModel(models[random.nextInt(models.length)]);
		car.setColour(colours[random.nextInt(colours.length)]);
		car.setYearOfRegistration(year);
		car.setSeats((byte)((random.nextInt(2) * 3) + 2));
		car.setEngineSize((double)((Math.round(random.nextDouble() * 20) + 10) / (double) 10));
		car.setTaxed(random.nextBoolean());
		return car;		
	}
	
	private Car createCar(String registration, String manufacturer, String model, String colour, int year, byte seats, double engineSize, boolean taxed) {
		Car car = new Car();
		car.setRegistration(registration);
		car.setManufacturer(manufacturer);
		car.setModel(model);
		car.setColour(colour);
		car.setYearOfRegistration(year);
		car.setSeats(seats);
		car.setEngineSize(engineSize);
		car.setTaxed(taxed);
		return car;		
	}
	
	private String createRandomRegistration(int year) {
		StringBuilder regBuilder = new StringBuilder();
		regBuilder.append(getRandomCapitalLetter());
		regBuilder.append(getRandomCapitalLetter());
		regBuilder.append(getYearCode(year));
		regBuilder.append(getRandomCapitalLetter());
		regBuilder.append(getRandomCapitalLetter());
		regBuilder.append(getRandomCapitalLetter());
		return regBuilder.toString();
	}
	
	private String getYearCode(int year) {
		boolean lateHalf = random.nextBoolean();
		year -= 2000;
		if (lateHalf) {
			year += 50;
		}
		String out = String.valueOf(year);
		if (out.length() < 2) {
			out = "0" + out;
		}
		return out;
	}
	
	private char getRandomCapitalLetter() {
		Random random = new Random();
		char c = (char) (byte)(random.nextInt(26) + 65);
		if (c == 'O' || c == 'I' || c == 'Q') {
			c = getRandomCapitalLetter();
		}
		return c;
	}
}
