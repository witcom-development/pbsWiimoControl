package org.fincl.miss.service.biz.bicycle.vo;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class QRReturnDistResultRequestVo extends BicycleVo {
    
	
	public final Map<String, Integer> requestFields = new LinkedHashMap<String, Integer>();
    {
        requestFields.put("bicycleState", 1);
        requestFields.put("bicycleId", 7);
        requestFields.put("elecbicycle", 1);
        requestFields.put("regDttm", 7);
        requestFields.put("beaconId", 7);
        requestFields.put("lockState", 1);
        requestFields.put("battery", 1);
        requestFields.put("beaconBattery", 1);
        
        requestFields.put("electbatt", 1);
        requestFields.put("latitude", 4);
        requestFields.put("longitude", 4);
        requestFields.put("distance", 2);	//거리...
        requestFields.put("errorId", 1);
    }
    
    private String bicycleState;
    private String bicycleId;
    private String elecbicycle;
    private String regDttm;
    private String beaconId;
    private String lockState;
    private String battery;
    private String beaconBattery;
    private String electbatt;
    private String latitude;
    private String longitude;
    private String distance;
    private String errorId;
    
    
	public String getBicycleState() {
		return bicycleState;
	}
	public void setBicycleState(String bicycleState) {
		this.bicycleState = bicycleState;
	}
	public String getBicycleId() {
		return bicycleId;
	}
	public void setBicycleId(String bicycleId) {
		this.bicycleId = bicycleId;
	}
	public String getElecbicycle() {
		return elecbicycle;
	}
	public void setElecbicycle(String elecbicycle) {
		this.elecbicycle = elecbicycle;
	}
	public String getRegDttm() {
		return regDttm;
	}
	public void setRegDttm(String regDttm) {
		this.regDttm = regDttm;
	}
	public String getBeaconId() {
		return beaconId;
	}
	public void setBeaconId(String beaconId) {
		this.beaconId = beaconId;
	}
	public String getLockState() {
		return lockState;
	}
	public void setLockState(String lockState) {
		this.lockState = lockState;
	}
	
    //private String battery;
	public String getBattery() {
		return battery;
	}
	public void setBattery(String battery) {
		this.battery = battery;
	}
	
    public String getBeaconBattery() {
		return beaconBattery;
	}
	public void setBeaconBattery(String beaconBattery) {
		this.beaconBattery = beaconBattery;
	}
	//private String electbatt;
	public String getElectbatt() {
		return electbatt;
	}
	public void setElectbatt(String electbatt) {
		this.electbatt = electbatt;
	}
	
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public String getDistance() {
		return distance;
	}
	
	public void setDistance(String distance) {
		this.distance = distance;
	}
	public String getErrorId() {
		return errorId;
	}
	public void setErrorId(String errorId) {
		this.errorId = errorId;
	}
}
