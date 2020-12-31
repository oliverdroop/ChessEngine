package database;

public enum PersistenceType {
	IMMEDIATE,
	DELAYED,
	NEVER;
	
	private long delay = 1000;
	
	PersistenceType(){
		
	}
	
	PersistenceType(long delay) {
		this.delay = delay;
	}
	
	public void setDelay(int millisecondDelay) {
		this.delay = millisecondDelay;
	}

	public long getDelay() {
		return delay;
	}
}
