package de.dfki.cos.basys.p4p.platform.bdevws.incidentservice;

import java.util.Date;

//import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonFormat;

public class Incident {

	private String signalId;
		
	//@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'hh:mm:ss.SSSSSSS'Z'")
	private long timestamp;
	
	private String message;
	
	private double value;

	public String getSignalId() {
		return signalId;
	}

	public void setSignalId(String signalId) {
		this.signalId = signalId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public double getValue() {
		return value;
	}
	
	public void setValue(double value) {
		this.value = value;
	}
}
