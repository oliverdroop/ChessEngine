package database;

public class Car {
	private long id;
	private String registration;
	private String manufacturer;
	private String model;
	private String colour;
	private int yearOfRegistration;
	private byte seats;
	private double engineSize;
	private boolean taxed;
	public String getRegistration() {
		return registration;
	}
	public void setRegistration(String registration) {
		this.registration = registration;
	}
	public String getManufacturer() {
		return manufacturer;
	}
	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public String getColour() {
		return colour;
	}
	public void setColour(String colour) {
		this.colour = colour;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public int getYearOfRegistration() {
		return yearOfRegistration;
	}
	public void setYearOfRegistration(int yearOfRegistration) {
		this.yearOfRegistration = yearOfRegistration;
	}
	public byte getSeats() {
		return seats;
	}
	public void setSeats(byte seats) {
		this.seats = seats;
	}
	public double getEngineSize() {
		return engineSize;
	}
	public void setEngineSize(double engineSize) {
		this.engineSize = engineSize;
	}
	public boolean isTaxed() {
		return taxed;
	}
	public void setTaxed(boolean taxed) {
		this.taxed = taxed;
	}
}
