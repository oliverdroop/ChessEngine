package database;

import java.io.File;
import java.util.Random;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class DatabaseTest {
	
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
			Car car = createCar();
			if (table == null) {				
				 table = parser.getApplicableTable(car);
			}
			table.addRow(parser.parse(car));
		}
		int rowLength = table.getRowLength();
		int expectedDatabaseLength = rowLength * count;
		
		String message = ("Expected data length to be " + expectedDatabaseLength);
		softly.assertThat(table.getData().length).as(message).isEqualTo(expectedDatabaseLength);
		
		String fileName = rootDirectory + File.separator + table.getName() + ".ddbt";
		table.save(fileName);
		table.load(fileName);
		
		softly.assertThat(table.getData().length).as(message).isEqualTo(expectedDatabaseLength);
	}
	
	private Car createCar() {
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
		if (c == 'O' || c == 'I') {
			c = getRandomCapitalLetter();
		}
		return c;
	}
}
