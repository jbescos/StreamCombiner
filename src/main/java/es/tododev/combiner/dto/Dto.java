package es.tododev.combiner.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "data")
public class Dto {

	private long timestamp;
	private double amount;
	
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(amount);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Dto other = (Dto) obj;
		if (Double.doubleToLongBits(amount) != Double.doubleToLongBits(other.amount))
			return false;
		if (timestamp != other.timestamp)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Dto [timestamp=" + timestamp + ", amount=" + amount + "]";
	}
	
	
}
