package database;

import java.util.List;

public class Owner {
	
	private long id;
	
	private String forename;
	
	private String surname;
	
	private long carId;
	
	//private List<Car> cars;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getForename() {
		return forename;
	}

	public void setForename(String forename) {
		this.forename = forename;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public long getCarId() {
		return carId;
	}

	public void setCarId(long carId) {
		this.carId = carId;
	}
	
	

//	public List<Car> getCars() {
//		return cars;
//	}
//
//	public void setCars(List<Car> cars) {
//		this.cars = cars;
//	}	
}
