package database;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class TableTest {
	
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
		database = new Database(directory);
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
		
		Map<String, byte[]> propertyValueMap = new HashMap<>();
		propertyValueMap.put("colour", "Black".getBytes());
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 2 black cars").hasSize(2);
		
		propertyValueMap = new HashMap<>();
		propertyValueMap.put("engineSize", ByteBuffer.allocate(8).putDouble(1.25).array());
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 1 car with engineSize 1.25").hasSize(1);
		
		propertyValueMap = new HashMap<>();
		propertyValueMap.put("model", "Fiesta".getBytes());
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 3 Fiestas").hasSize(3);
		
		propertyValueMap.put("colour", "Black".getBytes());
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 2 Black Fiestas").hasSize(2);
		
		propertyValueMap.put("engineSize", ByteBuffer.allocate(8).putDouble(1.25).array());
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find 1 Black Fiesta with engineSize 1.25").hasSize(1);
		
		propertyValueMap.put("seats", new byte[] {2});
		softly.assertThat(table.getByteMatchedRows(propertyValueMap)).as("Expected to find no Black 1.25 Fiestas with 2 seats").isEmpty();
	}
	
	@Test
	public void testGetStringMatchedRows() {
		Table table = setUpMatchedRowsTestTable();
		
		Map<String, String> propertyStringMap = new HashMap<>();
		propertyStringMap.put("colour", "Black");
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 2 black cars").hasSize(2);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("engineSize", "1.25");
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 1 car with engineSize 1.25").hasSize(1);
		
		propertyStringMap = new HashMap<>();
		propertyStringMap.put("model", "Fiesta");
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 3 Fiestas").hasSize(3);
		
		propertyStringMap.put("colour", "Black");
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 2 Black Fiestas").hasSize(2);
		
		propertyStringMap.put("engineSize", "1.25");
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find 1 Black Fiesta with engineSize 1.25").hasSize(1);
		
		propertyStringMap.put("seats", "2");
		softly.assertThat(table.getStringMatchedRows(propertyStringMap)).as("Expected to find no Black 1.25 Fiestas with 2 seats").isEmpty();
	}
	
	private Table setUpMatchedRowsTestTable() {
		int year = 2020;
		ObjectParser parser = new ObjectParser(database);
		Car car1 = createCar(createRandomRegistration(year), "Ford", "Fiesta", "Black", year, (byte)5, 1.4);
		Table table = parser.getApplicableTable(car1);
		table.setData(null);
		table.addRow(parser.parse(car1));
		Car car2 = createCar(createRandomRegistration(year), "Ford", "Fiesta", "Black", year, (byte)5, 1.25);
		table.addRow(parser.parse(car2));
		Car car3 = createCar(createRandomRegistration(year), "Ford", "Focus", "Blue", year, (byte)5, 1.6);
		table.addRow(parser.parse(car3));
		Car car4 = createCar(createRandomRegistration(year), "Ford", "Fiesta", "Blue", year, (byte)5, 1.6);
		table.addRow(parser.parse(car4));
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
		return car;		
	}
	
	private Car createCar(String registration, String manufacturer, String model, String colour, int year, byte seats, double engineSize) {
		Car car = new Car();
		car.setRegistration(registration);
		car.setManufacturer(manufacturer);
		car.setModel(model);
		car.setColour(colour);
		car.setYearOfRegistration(year);
		car.setSeats(seats);
		car.setEngineSize(engineSize);
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
