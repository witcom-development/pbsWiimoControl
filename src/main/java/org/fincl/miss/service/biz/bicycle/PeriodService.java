package org.fincl.miss.service.biz.bicycle;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fincl.miss.server.annotation.RPCService;
import org.fincl.miss.server.annotation.RPCServiceGroup;
import org.fincl.miss.server.scheduler.job.sms.SmsMessageVO;
import org.fincl.miss.server.scheduler.job.sms.TAPPMessageVO;
import org.fincl.miss.server.sms.SendType;
import org.fincl.miss.server.sms.SmsSender;
import org.fincl.miss.service.biz.bicycle.common.CommonUtil;
import org.fincl.miss.service.biz.bicycle.common.CommonVo;
import org.fincl.miss.service.biz.bicycle.common.Constants;
import org.fincl.miss.service.biz.bicycle.common.QRLogVo;
import org.fincl.miss.service.biz.bicycle.service.BicycleRentMapper;
import org.fincl.miss.service.biz.bicycle.service.BicycleRentService;
import org.fincl.miss.service.biz.bicycle.service.CommonService;
import org.fincl.miss.service.biz.bicycle.service.FileUpdateService;
import org.fincl.miss.service.biz.bicycle.service.impl.BicycleRentServiceImpl;
import org.fincl.miss.service.biz.bicycle.vo.BikeRentInfoVo;
import org.fincl.miss.service.biz.bicycle.vo.QRReturnRequestVo;
import org.fincl.miss.service.biz.bicycle.vo.QRReturnResponseVo;
import org.fincl.miss.service.biz.bicycle.vo.QRAdminMountingRequestVo;
import org.fincl.miss.service.biz.bicycle.vo.PeriodicStateReportsRequestVo;
import org.fincl.miss.service.biz.bicycle.vo.PeriodicStateReportsResponseVo;
import org.fincl.miss.service.biz.bicycle.vo.RentHistVo;
import org.fincl.miss.service.util.NaverGPSUtil;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@RPCServiceGroup(serviceGroupName = "??????")
@Service
public class PeriodService{

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private CommonService commonService;
	
	
	@Autowired
	private BicycleRentService bikeService;

	@Autowired
	private FileUpdateService fileService;
	
	@Autowired
    private BicycleRentMapper bicycleMapper;
 
	
	// ???????????? ?????? ?????? 
	 @RPCService(serviceId = "Bicycle_11", serviceName = "???????????? ???????????? Request", description = "???????????? ???????????? Request")
	 public PeriodicStateReportsResponseVo adminMove(PeriodicStateReportsRequestVo vo) {
		 
		 logger.debug("######################## 435F_Bicycle_11 ");
		 logger.debug("QR_PeriodicStateReportsRequestVo vo :::::::::::{} " , vo);
		 
		 
		 	
		// double latitude = 0.0d, longitude = 0.0d;
		 String latitude = null;
		 String longitude = null;
		 String	sBike_status=null;
		 String  BIKE_SE_CD=null;
		 
		 PeriodicStateReportsResponseVo responseVo = new PeriodicStateReportsResponseVo();
		 
		 CommonVo com = new CommonVo();
		 com.setBicycleId(vo.getBicycleId());
		 com.setTimeStamp(vo.getTimestamp());	//?????? 
		 com.setBikeId(vo.getBicycleId());
		 
		 Map<String, String> ourBikeMap = new HashMap<String, String>();
		 
		 //DEVICE.ENTRPS_CD,BIKE.BIKE_ID, BIKE.BIKE_NO ,BIKE.BIKE_SE_CD (BIKE_001)
		 ourBikeMap = bikeService.chkOurBike(com);	//ENTRPS_CD : ENT_001(vick) , ENT_002(witcom)  , ENT_003(atech)
		 
		 QRLogVo QRLog = new QRLogVo();
		 
		 String	 nBikeSerial;
		 
		 if(ourBikeMap != null)
		 {
			//add ????????? ?????? ???????????? 2018.09.01
			String  bikeNo = ourBikeMap.get("BIKE_NO");
		 	nBikeSerial = bikeNo.substring(2,bikeNo.length());
		 	
		 	String  ENTRPS_CD = ourBikeMap.get("ENTRPS_CD");	//ENT_003   DB :002
		 	BIKE_SE_CD = ourBikeMap.get("BIKE_SE_CD");
		 	
		 	com.setCompany_cd("CPN_" + BIKE_SE_CD.substring(4,BIKE_SE_CD.length()));
		 	
		 	logger.debug("QR_435F ##### => BIKE_SE_CD {} ,  biketype {} ,bike {} ,state {} ,usrType {} , company {} ,lock {}",BIKE_SE_CD,vo.getBikeType(),vo.getBicycleId(),vo.getBicycleState(),vo.getUsrType(),com.getCompany_cd(),vo.getLockState());
		 	 
		 	//Map<String, String> qrlog = new HashMap<String, String>();
		 	
	//	 	QRLogVo QRLog = new QRLogVo();
		 	
		 	QRLog.setBicycleId(vo.getBicycleId());
		 	QRLog.setBicycleNo(bikeNo);
		 	QRLog.setBicycleState(vo.getBicycleState());
		 	QRLog.setBeaconid(vo.getBeaconId());
		 	
		 	QRLog.setUserSeq(String.valueOf(Integer.parseInt(vo.getUsrseq())));
		 	QRLog.setUserType(vo.getUsrType());
		 	QRLog.setLock(vo.getLockState());
		 	QRLog.setBiketype(vo.getBikeType());
		 
		 	QRLog.setDev_BATT(String.valueOf(Integer.parseInt(vo.getBattery(), 16)));
		 	QRLog.setBeacon_BATT(String.valueOf(Integer.parseInt(vo.getBeaconbatt(), 16)));
		 	QRLog.setBike_BATT(String.valueOf(Integer.parseInt(vo.getElecbatt(), 16)));
		 	QRLog.setFirm_fw("0");
		 	QRLog.setModem_fw(vo.getModem_firmwareVs());
		 	
		 	if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
					 && !vo.getLongitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
		 	{
		 		QRLog.setXpos(new CommonUtil().GetGPS(vo.getLatitude()));
		 		QRLog.setYpos(new CommonUtil().GetGPS(vo.getLongitude()));
		 	}
		 	else
            {
		 		QRLog.setXpos("00000000");
		 		QRLog.setYpos("00000000"); 
		 		
		 	//	QRLog.setXpos(vo.getLatitude());
		 	//	QRLog.setYpos(vo.getLongitude());        
            }
		 	
		 	
		 	QRLog.setTimeStamp(vo.getTimestamp());
			QRLog.setMessage(vo.getReqMessage());
			QRLog.setQr_frame("?????? ??????");
		 	
		 	bikeService.insertQRLog(QRLog);
		 }
		 else
		 {
			 QRLog.setBicycleNo("FFFFFFFF");
			 
			 QRLog.setBicycleId(vo.getBicycleId());
			 QRLog.setLock(vo.getLockState());
			 QRLog.setBicycleState(vo.getBicycleState());
			 QRLog.setBeaconid(vo.getBeaconId());
			 QRLog.setDev_BATT(String.valueOf(Integer.parseInt(vo.getBattery(), 16)));
			 QRLog.setBeacon_BATT(String.valueOf(Integer.parseInt(vo.getBeaconbatt(), 16)));
			 QRLog.setBike_BATT(String.valueOf(Integer.parseInt(vo.getElecbatt(), 16)));
			 QRLog.setFirm_fw("0");
			 QRLog.setModem_fw(vo.getModem_firmwareVs());
			 QRLog.setMessage(vo.getReqMessage());
			 QRLog.setQr_frame("?????? ??????");
				
			 bikeService.insertQRLog(QRLog);
			 
			 logger.error("INVALID ????????? ID {}",vo.getBicycleId() );
			 com.setBikeStusCd("BKS_001");	//????????? ????????? ????????? ?????? 
			 com.setBikeBrokenCd("ELB_006");
			 responseVo.setErrorId(Constants.CODE.get("ERROR_FF"));
			 responseVo = setFaiiMsg(responseVo, vo);
			 
			 return responseVo;
		 }
		 
		 
		 if(!vo.getBattery().equals(null) && !vo.getBattery().equals(""))
		 {
			 logger.debug("##### PERIOD UPDATE ## BATTERY UPDATE START #####");
			 int battery = Integer.parseInt(vo.getBattery(), 16);
			 String battery_info = null;

			 battery_info = new CommonUtil().getBattery_Info(vo.getBattery());
     		
     		Map<String, String> battery_map = new HashMap<String, String>();
     		battery_map.put("BATTERY", String.valueOf(battery));
     		battery_map.put("BATTERY_INFO", String.valueOf(battery_info));
     		battery_map.put("BICYCLE_ID", vo.getBicycleId());
     		
     		commonService.updateBatteryInfo(battery_map);
     		//?????? ????????? BATT UPDATE ?????? ???????
     		logger.debug("##### PERIOD UPDATE ## BATTERY UPDATE END #####");
		 }
		 
		 logger.debug("BIKE STATE UPDATE ::::::::::::: ");
		 commonService.updatePeriodState(com);
		 
		 int Bike_Speed = Integer.parseInt( vo.getCurrent_speed(), 16 );
		 logger.debug("current Speed Is " + Bike_Speed);
		 
		 //00 01 5E 2A 53   usr_seq ?????? ?????????.
		// 0001944283
		 
		 
		 //logger.debug("LOG TEST : USR_SEQ " + result);
		//?????? ????????????????????? becon, ????????? ?????? ??????...
		//?????? ?????? ?????? ????????????
		 //????????? , ?????? ?????? ???????????? ??????.
		 //GPS ????????? GPS ????????? ?????????...
		 if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
				 && !vo.getLatitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
		 {
			 //BIKE GPS INSERT
			 //TB_OPR_QR_BIKE_GPS_HIST
			//gps
			 
			 
			 
			 latitude = new CommonUtil().GetGPS(vo.getLatitude());
			 longitude = new CommonUtil().GetGPS(vo.getLongitude());
			 
			 //logger.debug("GPS INFO ##### => : {} , {} ",String.valueOf(latitude),String.valueOf(longitude));
			 logger.debug("GPS INFO ##### => : {} , {} ",latitude,longitude);
			 
			 RentHistVo gps = new RentHistVo();
			 gps.setRETURN_RACK_ID(com.getRockId());
			 gps.setRENT_BIKE_ID(com.getBicycleId());
			 gps.setGPS_X(latitude);
			 gps.setGPS_Y(longitude);
			 
			 bicycleMapper.updateBikeGPS(gps);
			 
		 }
		 if(vo.getBicycleState().equals("00") || vo.getBicycleState().equals("FF"))
		 {
			 logger.error("INVALID BIKE STATE ");
			 com.setBikeStusCd("BKS_001");	//????????? ????????? ????????? ?????? 
			 com.setBikeBrokenCd("ELB_006");
			 responseVo.setErrorId(Constants.CODE.get("ERROR_FF"));
			 responseVo = setFaiiMsg(responseVo, vo);
		 
			 
			 return responseVo;
		 }
		 
		 if(vo.getBicycleState().equals("01") || vo.getBicycleState().equals("02") )	//??????/?????? ????????????
		 {
			 logger.debug("QR_BIKE is RETURN_STATE_PERIOD");
			
			 //????????? ??????...
			 Map<String, Object> stationInfo = null;
			 if(stationInfo == null)
			 {
				 if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
						 && !vo.getLatitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
				 {
					 
					 Map<String, String> GPS = new HashMap<String, String>();
					 
					 GPS.put("BIKE_LATITUDE", String.valueOf(latitude));
					 GPS.put("BIKE_LONGITUDE", String.valueOf(longitude));
					 GPS.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
					 //???????????? ?????? ?????? ?????? ?????? 
					 
					 stationInfo = commonService.CheckStation_ForGPS(GPS);
					 
					 if(stationInfo == null)
					 {
						 logger.error("Station List Find Error");
						 responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));
						 responseVo = setFaiiMsg(responseVo, vo);
				  		
						 return responseVo;
					 }
					 else
					 {
						 logger.debug("##### QR_PERIOD ## GPS STATION ID[" + String.valueOf(stationInfo.get("STATION_ID")) + "] ENTERED");
					 }
					 
				 }
				 
			 }
		 
			 if(stationInfo != null)
			 {
				 com.setStationId(stationInfo.get("STATION_ID").toString());
				 com.setRockId(stationInfo.get("RACK_ID").toString());
			 }
			 else
			 {
				 Map<String, Object> serVersion = fileService.getVersion(com);
				 
				 
				 logger.error("QR_PERIOD : RETURN_AREA IS NOT PLACE : BUT FORCE TO SUCCESS");
		  		
	  			 responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); //  f/w ?????? ???????????? ??????

	  			responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
	  			 
	  			 if(responseVo.getBle_fwupdate().equals(Constants.CODE.get("WIFI_UPDATE_00")))  //BLE UPDATE ??????????????? MODEM ?????? ??????
	  			 {
	  				 logger.debug("##### period status : MODEL firmware_time_update=> " + serVersion.get("FIRMWARE_MODEM_TIME_CAN_DOWN") + " firmware_bike_update=> " + serVersion.get("FIRMWARE_MODEM_BIKE_CAN_DOWN"));
	  				 
	  				 if(serVersion.get("FIRMWARE_MODEM_TIME_CAN_DOWN") != null )
	  				 {
	  					 double serverFw = Double.parseDouble(serVersion.get("FIRMWARE_MODEM_VER").toString());
	  					 
	  					 boolean fwUseYn = serVersion.get("FIRMWARE_MODEM_USE_YN").equals("Y");
	  					 
	  					 boolean fwTimeCanDown = serVersion.get("FIRMWARE_MODEM_TIME_CAN_DOWN").equals("Y");
	  					 boolean fwBikeCanDown = serVersion.get("FIRMWARE_MODEM_BIKE_CAN_DOWN").equals("Y");
	  					 double requsetFw  = Double.parseDouble(vo.getModem_firmwareVs().substring(0,2) + "." + vo.getModem_firmwareVs().substring(2, 4));
	  							 
	  					if(fwTimeCanDown && fwBikeCanDown)
	  					{
	  						if(fwUseYn)
	  						{
	  							if(requsetFw <  serverFw )	//????????? ?????????????????? ????????? ???????????? 
	  							{
	  								logger.debug("### YES : PERIOD UPDATE MODEL FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
	  								responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_01")); //  f/w ?????? ???????????? ??????
	  							}
	  							else
	  							{
	  								logger.debug("### NO2 : PERIOD UPDATE MODEL FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
	  								responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
	  							}
	  						}
	  						else	//2019.12.18 ?????? 
	  						{
	  							logger.debug("### NO3 : PERIOD UPDATE MODEL FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
	  							responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
	  						}
	  					}
	  					else
	  					{
	  						logger.debug("### NO4 : PERIOD UPDATE MODEL FIRMWARE UPDATE2 ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
	  						responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
	  					}
	  				 }
	  				 else
	  				 {	 
	  					 logger.debug("##### PERIOD UPDATE MODEL FIRMWARE UPDATE : FIRM VERSION IS NO GET#####");
	  			         responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
	  				 }
	  			 }
	  			 
	  //			 responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); //  f/w ?????? ???????????? ??????
	  			 
	    		 responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
	    		 responseVo.setSeqNum(vo.getSeqNum());
	    		 responseVo.setCommandId(Constants.CID_RES_REPORTOFBIKE);
	    		 responseVo.setBicycleState(vo.getBicycleState());
	    		 responseVo.setBicycleId(vo.getBicycleId());
	    		 
	    		 responseVo.setDayAndNight("00");
	    		 
	    		 
	    		 List<HashMap<String, String>> PeriodList = commonService.CheckPeriodTime();
     	  		 
	    		 if(PeriodList.size() == 0 )
	    		 {
	     	  			 logger.error("Period Information Find Error");
	     	  			 responseVo.setErrorId(Constants.CODE.get("ERROR_D7"));
	     	  			 responseVo = setFaiiMsg(responseVo, vo);
	     	           	
	     	  			 return responseVo;
	    		 }
	    		 else
	    		 {
	     	  			 for(HashMap<String, String> PeriodMap : PeriodList)
	     	  			 {
	     	  				 if(String.valueOf(PeriodMap.get("COM_CD")).equals("MSI_036"))
	     	  				 {
	     	  					 logger.debug("######################## MSI_036 " + String.valueOf(PeriodMap.get("ADD_VAL1")));
	     	  					 //responseVo.setReturnPeriod(String.valueOf(PeriodMap.get("ADD_VAL1")));
	     	  					 int Hour = Integer.parseInt(PeriodMap.get("ADD_VAL1"))/60;
	     	  					 int Minute = Integer.parseInt(PeriodMap.get("ADD_VAL1"))%60;
	     	  					responseVo.setPeriodHour(getToString(String.valueOf(Hour),2));
	     	  					 responseVo.setPeriodMinute(getToString(String.valueOf(Minute),2));
	     	  				 }
	     	  			 }
	     	  		 }
	    		 
	    		 
	    		 return responseVo;
			 }
		    
			 //	 String faultSeq = commonService.getFaultSeq(com);
			 /**
			  * ???????????? ??????????????? ???????????? ???????????? ????????? ????????? ??????,
			  * ?????? ????????? ??????.
			  * ?????? ????????? ????????? ??????.
			  */
		 
			 Map<String, Object> deviceInfo = commonService.checkBicycle(com);
			 if(deviceInfo == null)
			 {
				 logger.error("INVALID ????????? ID ");
			 
				 com.setBikeStusCd("BKS_001");	//????????? ????????? ????????? ?????? 
			 
				 com.setBikeBrokenCd("ELB_006");
			 
				 //callBrokenError(com);
			 
				 responseVo.setErrorId(Constants.CODE.get("ERROR_FF"));
				 responseVo = setFaiiMsg(responseVo, vo);
				 
				 responseVo.setBicycleState(vo.getBicycleState());
			 
				 return responseVo;
			 }
			 
			 int battery = Integer.parseInt(vo.getBattery(), 16);
			 if(battery >= 30)
			 {
				 //Battery Alarm ????????? 
				 Integer FAULT_SEQ = 0;
				 Integer FAULT_CNT = 0;
				 
				 Map<String, String> battery_map = new HashMap<String, String>();
				 battery_map.put("BICYCLE_ID", vo.getBicycleId());
	     			
				 FAULT_SEQ = commonService.selectBatteryInfo(battery_map);
	     			
				 if(FAULT_SEQ != null && FAULT_SEQ.intValue() > 0)
				 {
					 FAULT_CNT = commonService.selectBatteryDetl(FAULT_SEQ.intValue());
					 if(FAULT_CNT == 1)
					 {
						 commonService.deleteFaultInfo(com);
						// commonService.changeValidBike(com);
						 logger.debug("##### BIKE {} HAS BATTERY_FAULT DELETE",vo.getBicycleId());
					 }
					 else
					 {
						 logger.debug("##### BIKE {} HAS BATTERY_FAULT BUT HAS MORE FAULT {} NO CHANGE",vo.getBicycleId(),FAULT_CNT);
					 }
				 }
			 }
			 
		//	 logger.debug("deviceInfo : {} ",deviceInfo);	//??????.
		 
		 }	//????????? ?????? 01, 02 ?????? ?????? ??????.
		 
		 // ?????? error ??????
		 if(vo.getErrorId().equals("E4") || vo.getBattery().equals("03"))
		 {
			 // ???????????? ?????? UPdate
			 commonService.updateBatteryDischarge(com);
		 }
     	
      	/**
      	 * ?????? ??????????????? ??????...?????? ????????? ???????????? ???????????? ?????????...
      	 */
     	
 //    	Map<String, Object> parkingMap = commonService.checkParkingInfo(com);
     	
		sBike_status = commonService.checkdBikeStateInfo(com);
		 
		
     	//?????? ???????????? ????????????, ???????????? ?????? ?????? ??????....
     	if(vo.getBicycleState().equals("03") || vo.getBicycleState().equals("04") )
     	{
     		logger.debug("QR_BIKE is RENTING_PERIOD REPORT STATE {} LOCK {} BIKE_STATE {} ###################",vo.getBicycleState(),vo.getLockState(),sBike_status);
     		
     		/* ?????????????????? ?????? ?????? ???.
     		com.setUserSeq(String.valueOf(Integer.parseInt(vo.getUsrseq())));
			Map<String, Object> rentInfo = bikeService.getUseBikeInfoFULL(com);
     		
			if(rentInfo != null)
			{
	     		if(rentInfo.get("RENT_YN").equals("Y"))
	     		{
	     			
	     		//	String  end_time = rentInfo.get("END_DTTM").toString();
	     		//	logger.debug("QR_BIKE RENT_STATE : RETURN_TIME ",rentInfo.get("END_DTTM"));
	     			
	     			if(rentInfo.get("END_DTTM") != null && !rentInfo.get("END_DTTM").equals("") )
	     			{
	     				logger.debug("QR_BIKE RENTING_STATE : ");
	     				
	     			}
	     			else
	     				logger.debug("QR_BIKE REQUEST_RETURN_TIME {}",rentInfo.get("END_DTTM"));
	     			
	     		}
			}
			*/
     		//SESSAK ????????? ????????? GPS ??? ??????????????? ??????...
     		//2020.10.06
     		
     		if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
     				&& !vo.getLatitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
     		{
     			 //?????? ??????  ?????? 02 
     			 /* NAVER ??????*/
     			if(vo.getBicycleState().equals("03"))	//
     			{
    				   
    				    String NAVER_API_KEY ;
    				    String NAVER_SECRET_KEY;	

		     			//zgyaca9y6t    , JbaXg9LvYDkHHPmeGi2r0DsL20KhhvOIkfJMfEOd
		     			// ????????? ??????  : a88veypaz2 , kg5bLoJkxE1KcFUURqgjdyf5x0ztBMKRY46y4Med
		     			NAVER_API_KEY 			= "zgyaca9y6t";     			// ????????? ??????
		     			NAVER_SECRET_KEY			= "JbaXg9LvYDkHHPmeGi2r0DsL20KhhvOIkfJMfEOd";  // billing Key
		     			
		     			
    				   
		     			
		     			ObjectMapper mapper = new ObjectMapper(); 		  			//jackson json object
		     			NaverGPSUtil util = new NaverGPSUtil();  
		     			
		
		     			String XPos = new CommonUtil().GetGPS(vo.getLatitude());
		     			String YPos = new CommonUtil().GetGPS(vo.getLongitude());
		     			
		     			/*
		     			  			
		     			String info_String = "coords="+ YPos + "," + XPos +"&output=json&orders=legalcode";
		     			
		     			logger.debug("NAVER API_BASIC " + info_String);
		     			
		     			
		     			String info_result = util.GPSSend(info_String, NAVER_API_KEY, NAVER_SECRET_KEY);
		     			String Data_Result =null;
		     			JsonNode node;
		     			try 
		     			{	
		     				node = mapper.readTree(info_result);
		     				logger.debug("SESSAK GPS CHECK POINT 1 " + info_result);
		     				if(node.has("results"))
		     				{
		     					ArrayNode  memberArray = (ArrayNode) node.get("results");
		     					
		     					if(memberArray.isArray())
		     					{
		     						for(JsonNode jsonNode : memberArray)
		     						{
		     							logger.debug("SESSAK NODE " + jsonNode);
		     							logger.debug("SESSAK GPS CHECK POINT 2 = " + jsonNode.get("region").get("area3").get("name").asText().toString());
		     							Data_Result = jsonNode.get("region").get("area3").get("name").asText().toString();
		     						}
		     					}
		     					//logger.debug("NAVER GPS CHECK POINT 2 = " + jsonNode.get("region").get("area2").get("name").toString());
		     				}
		     				else 
		     				{
		     					logger.debug("SESSAK GPS CHECK POINT 3");
		     				}
		     			} 
		     			catch (JsonProcessingException e) 
		     			{
		     				logger.debug("SESSAK GPS JsonProcessingException");
		     			} 
		     			catch (IOException e) 
		     			{
		     				logger.debug("SESSAK GPS IOException");
		     			}
		     			catch (Exception e)
		     			{
		     				logger.debug("SESSAK GPS Exception");
		     				e.printStackTrace();
		     			}
		     				
		     			*/
		     			
		     			
		     			if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
    							 && !vo.getLatitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
    					 {
		     				
		     				//BikeRentInfoVo bikeInfo = bicycleMapper.getBikeInfo(com);
		     				com.setUserSeq(String.valueOf(Integer.parseInt(vo.getUsrseq())));
	     					//com.setRockId(bikeInfo.getRent_rack_id());
		     				Map<String, Object> rentInfo = bikeService.getUseBikeInfoFULL(com);
		     				
		     				if(rentInfo != null)
	     					{

	    						 latitude = new CommonUtil().GetGPS(vo.getLatitude());
	    						 longitude = new CommonUtil().GetGPS(vo.getLongitude());
	
	    						 logger.debug("GPS INFO ##### => : {} , {} ",latitude,longitude);
	
								 Map<String, String> GPS_DATA = null;
								 double USE_DIST = 0.0;
								 int		USE_SEQ = 0;
								 double		ACC_DIST = 0.0;
								 
								 
								 
								 logger.debug("QR_GPS_SIZE : rentseq : {}, latitude : {}, longitude : {}",rentInfo.get("RENT_SEQ").toString(), latitude , longitude);
								 Integer tem_USE_SEQ = 0;
					         	
								 GPS_DATA = new HashMap<String, String>();
								 GPS_DATA.put("RENT_SEQ", rentInfo.get("RENT_SEQ").toString());
					         	
					         	Map<String, Object> bikeData = bikeService.getBikeMoveDist_COUNT(GPS_DATA);
								if(bikeData != null)
								{
									tem_USE_SEQ = Integer.valueOf(String.valueOf(bikeData.get("USE_SEQ")));
									USE_SEQ = tem_USE_SEQ;
									ACC_DIST = new Double(bikeData.get("ACC_DIST").toString()); //???????????? ?????? ????????? db?????? ??????
					        		logger.debug("getBikeMoveDist_COUNT BICYCLE_ID {}, USE_SEQ {}" ,vo.getBicycleId(), USE_SEQ);
					        		
					        		
					        		GPS_DATA.put("BIKE_LATITUDE", String.valueOf(latitude));
						         	GPS_DATA.put("BIKE_LONGITUDE", String.valueOf(longitude));
					        		
					        		
					        		USE_DIST = bikeService.getBikeMoveDist_Last(GPS_DATA);  //db ????????? ????????? ?????? ????????? ???????????? ??????
					        		ACC_DIST += USE_DIST;
					        		logger.debug("GPS DISTANCE LAST DB ACC_DIST {} ", ACC_DIST);
					        		
					        		GPS_DATA = new HashMap<String, String>();
					        		
						         	GPS_DATA.put("RENT_SEQ", rentInfo.get("RENT_SEQ").toString());
						         	
						         	USE_SEQ++;
						         	GPS_DATA.put("BIKE_LATITUDE", String.valueOf(latitude));
						         	GPS_DATA.put("BIKE_LONGITUDE", String.valueOf(longitude));
						        	GPS_DATA.put("USE_SEQ", String.valueOf(USE_SEQ));
						        	GPS_DATA.put("BIKE_ID", vo.getBicycleId());
						        	GPS_DATA.put("USE_DIST", String.valueOf(USE_DIST));
						        	GPS_DATA.put("ACC_DIST", String.valueOf(ACC_DIST));
						        	
						        	logger.debug("QR_GPS_DATA  tolatp : {} , tolongfp : {} -> szlatp :{}  , szlongp : {}, USE_DIST : {}, ACC_DIST : {}"  , String.valueOf(bikeData.get("GPS_X")),String.valueOf(bikeData.get("GPS_Y")), latitude, longitude,String.valueOf(USE_DIST), String.valueOf(ACC_DIST));
						        	bikeService.insertRentGPSDATA(GPS_DATA);
						         	
						         	
					        	}
								else
								{
									USE_SEQ = 0;
					        		logger.debug("getBikeMoveDist_COUNT BICYCLE_ID {}, USE_SEQ {}" ,vo.getBicycleId(), USE_SEQ);
					        		logger.debug("GPS DISTANCE LAST DB ACC_DIST {} ", ACC_DIST);
					        		
					        		GPS_DATA = new HashMap<String, String>();
	
						        	GPS_DATA.put("BIKE_LATITUDE", String.valueOf(latitude));
						         	GPS_DATA.put("BIKE_LONGITUDE", String.valueOf(longitude));
						         	GPS_DATA.put("RENT_SEQ", rentInfo.get("RENT_SEQ").toString());
						         	
						         	USE_SEQ++;
						        	GPS_DATA.put("USE_SEQ", String.valueOf(USE_SEQ));
						        	GPS_DATA.put("BIKE_ID", vo.getBicycleId());
						        	GPS_DATA.put("USE_DIST", String.valueOf(USE_DIST));
						        	GPS_DATA.put("ACC_DIST", String.valueOf(ACC_DIST));
						        	
						        	logger.debug("QR_GPS_DATA  szlatp :{}  , szlongp : {}  USE_DIST : {}, ACC_DIST : {}"  , latitude, longitude,String.valueOf(USE_DIST), String.valueOf(ACC_DIST));
						        	bikeService.insertRentGPSDATA(GPS_DATA);
								}
	     					}
    						 
    					 }
		     		}	//biketype 02 ?????? ?????????...
     			
     		} //GPS ??? ??????....!!!!
     		
     		//?????? ?????? (?????????/?????????  ???????????? ???????????? ??? ??????)
     	//	if((vo.getBicycleState().equals("04") || vo.getBicycleState().equals("03")) && vo.getLockState().equals("01"))
     		if(vo.getBicycleState().equals("04") || vo.getBicycleState().equals("03"))
     		{
     			logger.debug("QR_BIKE is Status " + sBike_status);
     			
     			//????????? , ????????? ?????? lock 00  , ??????  01 
     			if(sBike_status.equals("BKS_012") && vo.getLockState().equals("00"))	//???????????? ????????????
     			{
     				logger.debug("QR_BIKE is RENTING_PERIOD AND LOCKSTATE IS ON BIKE IS WAIT PERIOD : ENFORE PROCESS #########################");
     				
     				/*
     		     	if(sBike_status != null)
     		     	{
     		     		//if(bike_status.equals("BKS_012") || bike_status.equals("BKS_016") ||bike_status.equals("BKS_017"))
     		     		if(sBike_status.equals("BKS_012") || sBike_status.equals("BKS_016") ||sBike_status.equals("BKS_017"))	//???????????? ?????? ????????? ?????? ??????...
     		     		{
     		       			Integer enfrc_return_hist_seq = 0;
     		     		
     		     			logger.debug("QR_START : TB_SVC_ENFRC_RETURN_PROCESSING is BLOCKING ");
     		     		}
     		     	}
     		     	*/
     				commonService.changeValidBike(com);
     				
     				logger.debug("QR_START : TB_SVC_ENFRC_RETURN_PROCESSING is BLOCKING ");
     				
     				QRLog.setResAck("SETRT");
     	 			bikeService.updateQRLog(QRLog);
     	 			
     				responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); //  f/w ?????? ???????????? ??????
     	    		 
     	    		responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); //  f/w ?????? ???????????? ??????
     	    	
     	    		responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
     	    		responseVo.setSeqNum(vo.getSeqNum());
     	    		responseVo.setCommandId(Constants.CID_RES_REPORTOFBIKE);
     	    		responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_02"));
     	    		responseVo.setBicycleId(vo.getBicycleId());
     	    		responseVo.setDayAndNight("00");
     	    		
     	    		List<HashMap<String, String>> PeriodList = commonService.CheckPeriodTime();
     	  		 
     	    		if(PeriodList.size() == 0 )
     	    		{
	     	  			 logger.error("Period Information Find Error");
	     	  			 responseVo.setErrorId(Constants.CODE.get("ERROR_D7"));
	     	  			 responseVo = setFaiiMsg(responseVo, vo);
	     	           	
	     	  			 return responseVo;
     	    		}
     	    		else
     	    		{
     	    			if(vo.getBicycleState().equals("03"))
     	    			{
     	    				for(HashMap<String, String> PeriodMap : PeriodList)
     	    				{
   	     	  				 
     	    					if(String.valueOf(PeriodMap.get("COM_CD")).equals("MSI_039"))
     	    					{
     	    						logger.debug("######################## MSI_039 " + String.valueOf(PeriodMap.get("ADD_VAL1")));
	     	    					//responseVo.setReturnPeriod(String.valueOf(PeriodMap.get("ADD_VAL1")));
     	    						int Hour = Integer.parseInt(PeriodMap.get("ADD_VAL1"))/60;
     	    						int Minute = Integer.parseInt(PeriodMap.get("ADD_VAL1"))%60;
     	    						responseVo.setPeriodHour(getToString(String.valueOf(Hour),2));
     	    						responseVo.setPeriodMinute(getToString(String.valueOf(Minute),2));
     	    					}
     	    				}
     	    			
     	    			}
     	    			else if(vo.getBicycleState().equals("04"))
     	    			{
     	    				for(HashMap<String, String> PeriodMap : PeriodList)
     	    				{
   	     	  				 
     	    					if(String.valueOf(PeriodMap.get("COM_CD")).equals("MSI_037"))
     	    					{
     	    						logger.debug("######################## MSI_037 " + String.valueOf(PeriodMap.get("ADD_VAL1")));
	     	    					//responseVo.setReturnPeriod(String.valueOf(PeriodMap.get("ADD_VAL1")));
     	    						int Hour = Integer.parseInt(PeriodMap.get("ADD_VAL1"))/60;
     	    						int Minute = Integer.parseInt(PeriodMap.get("ADD_VAL1"))%60;
     	    						responseVo.setPeriodHour(getToString(String.valueOf(Hour),2));
     	    						responseVo.setPeriodMinute(getToString(String.valueOf(Minute),2));
     	    					}
     	    				}
     	    			
     	    			}
	     	  			 
     	    		}
     	    		return responseVo;
     			}	//???????????? ???  BIS_012 ?????? ?????? ?????? ??????.
     		}
     		else if(vo.getLockState().equals("01"))
     		{
     			//?????? ???????????? ?????? ????????? ?????? ?????????
     			// ?????? ????????? ?????? ?????? ?????????  ?????? ID?????? ????????? ?????? ??????
     			 logger.debug("QR_BIKE is RENTING_PERIOD BUT LOCK STATE IS LOCK ON");	//?????? ?????? ????????? ??????...
     		}
     	//	 ?????? ???????????? ????????? ?????? ???????????? ?????? 4372
     		//BIKE_SE_CD  BIK_001 ????????? BIK_002 ????????? 
     		//??????..
     		/* 2021.06.07  ??????????????? ?????? ????????? ?????? ??????.
     		else if(vo.getLockState().equals("00") && vo.getSeqNum().equals("00"))
     		{
     			//??????????????? ???????????? ?????? - 2021.06.08
     			logger.debug("QR_BIKE IS RENTING_PERIOD BUT BIKE IS RETURN_STATE, LOCK_STATE is OPEN : DB_BIKE {}",sBike_status);
     			
     			BikeRentInfoVo bikeInfo = bicycleMapper.getBikeInfo(com);
     			if(bikeInfo == null)
     			{
     				//logger.debug("QR_BIKE IS RENTING_PERIOD BUT LOCK STATE AND PARKING DATA HAVEN'T DB");
     			}
     			else	//?????? ?????? ???????????? ?????? ???.....
     			{
     				
     				if(vo.getUsrType().equals("02"))
      				{
      					 QRLog.setResAck("AMOVE");
      					 bikeService.updateQRLog(QRLog);
      					 
      					 logger.debug("QR_BIKE IS RENTING_PERIOD AND USER TYPE IS ADMIN");
      					 com.setUserSeq(String.valueOf(Integer.parseInt(vo.getUsrseq())));
      					 com.setRockId(bikeInfo.getRent_rack_id());
      					 bikeService.procAdminMove(com);
      				}//????????? ????????? ?????? ??????...??????,??????,??????????????? ?????? ????????????...,????????????,??????
     				else if(sBike_status.equals("BKS_002")|| sBike_status.equals("BKS_005") || sBike_status.equals("BKS_007") || sBike_status.equals("BKS_016"))
     				{
     					
     					logger.debug("QR_BIKE IS RENTING_PERIOD BUT BIKE IS LOSTED STATUS {}",sBike_status);
     					
     				}
     				else
     				{
     					com.setUserSeq(String.valueOf(Integer.parseInt(vo.getUsrseq())));
     					com.setRockId(bikeInfo.getRent_rack_id());
     				 
     					logger.debug("QR_BIKE IS RENTING_PERIOD BUT HAVE PARKING INFO : bike_status {}",sBike_status);
     					
     					BikeRentInfoVo voucher = bikeService.getUseVoucherInfo(com);	//2020.02. ????????? ??????????????? ??????...
     				 
     					if(voucher == null)
     					{
     						logger.error("USR_SEQ[" + com.getUserSeq() + "] HAS NO RENT POSSIBLE VOUCHER");
     						QRLog.setResAck("VOUNO");
     						bikeService.updateQRLog(QRLog);
     						responseVo.setErrorId(Constants.CODE.get("ERROR_E5"));
     						responseVo = setFaiiMsg(responseVo, vo);
						 
     						return responseVo;
     					}
					 
     					Map<String, Object> useBike = bikeService.getUseBikeInfoFULL(com);
     					if(useBike != null)
     					{
     						logger.error("RENT PERIOD USR_SEQ[" + com.getUserSeq() + "] HAS RENT INFO NOT COMPLETE");
							
     						//add
     						String	rack_id = String.valueOf(useBike.get("RENT_RACK_ID"));
     						String	bike_id = String.valueOf(useBike.get("RENT_BIKE_ID"));
						
     						logger.debug("RENTING_PERIOD  getUseBikeInfoFULL CHECK_RENT usr_seq {} ,{},{} ",com.getUserSeq(),rack_id,bike_id);
						
						
     						if(rack_id == null || rack_id.equals("") || bike_id == null || bike_id.equals(""))
							{
								logger.error("Period RENTING  USR_SEQ set rentTableUpdate2 ");
								
								bicycleMapper.rentTableUpdate2(com);
								//BikeRentInfoVo bikeInfo = bicycleMapper.getBikeInfo(com);
							}
     						
     					}
     					else
     					{
						// ?????? ????????? ???????????? ?????? ?????? ????????? ?????? ?????? ????????????.
     						if(ourBikeMap.get("BIKE_SE_CD").equals("BIK_002"))
    						{
    							if(voucher.getRent_cls_cd().equals("RCC_001"))
    							{
    								voucher.setRent_cls_cd("RCC_002");
    							}
    						}
     						bikeService.reservationInsert(com, voucher);
     						logger.error("RENT PERIOD USR_SEQ[" + com.getUserSeq() + "] HAS NO RENT INFO");
     					}
	     			 
     					// ?????? ?????? 
     					Map<String, Object> rentInfo = commonService.reservationCheck(com);
	     			
     					if(rentInfo == null)
     					{
		     				QRLog.setResAck("RESNO");
		     	 			bikeService.updateQRLog(QRLog);
		     				logger.error("reservationCheck is null" );
		     	    		responseVo.setErrorId(Constants.CODE.get("ERROR_EF"));
		     	    		responseVo = setFaiiMsg(responseVo, vo);
		     	    		
		     	    		return responseVo;
     					}
     					
     					if(rentInfo.get("BIKE_SE_CD").equals("BIK_001"))
    					{
    						if(!voucher.getBike_voucher_cnt().equals("99"))
    						{
    							if((Integer.parseInt(voucher.getBike_use_cnt())) >= (Integer.parseInt(voucher.getBike_voucher_cnt())))
    							{
    								//?????? ??????
    								logger.error("USR_SEQ[" + com.getUserSeq() + "] HAS NO RENT POSSIBLE VOUCHER");
    		  						QRLog.setResAck("VOUNO3");
    		  						bikeService.updateQRLog(QRLog);
    		  						responseVo.setErrorId(Constants.CODE.get("ERROR_E5"));
    		  						responseVo = setFaiiMsg(responseVo, vo);
    										 
    		  						return responseVo;
    							}
    							else
    							{
    								//?????? ??????
    								bikeService.updateBikeCnt(voucher.getVoucher_seq());
    							}
    						}
    						else
    						{
    							//?????? ??????
    							//bikeService.updateBikeCnt(voucher.getVoucher_seq());
    						
    						}
    					}
    					else
    					{
    						
    						if(!voucher.getKick_voucher_cnt().equals("99"))
    						{
    							if((Integer.parseInt(voucher.getKick_use_cnt())) >= (Integer.parseInt(voucher.getKick_voucher_cnt())))
    							{
    								//?????? ??????
    								logger.error("USR_SEQ[" + com.getUserSeq() + "] HAS NO RENT POSSIBLE VOUCHER");
    		  						QRLog.setResAck("VOUNO4");
    		  						bikeService.updateQRLog(QRLog);
    		  						responseVo.setErrorId(Constants.CODE.get("ERROR_E5"));
    		  						responseVo = setFaiiMsg(responseVo, vo);
    										 
    		  						return responseVo;
    							}
    							else
    							{
    								//?????? ??????
    								bikeService.updateKickCnt(voucher.getVoucher_seq());
    							}
    						}
    						else
    						{
    							//?????? ??????
    							//bikeService.updateKickCnt(voucher.getVoucher_seq());
    						
    						}
    					
    					}
     					
	     			 
		     			if(!bikeService.rentProcUpdate(com, rentInfo))
		     			{
		     				logger.error("rentProcUpdate: invalid voucher:ERROR_E5" );
		     				
		     				QRLog.setResAck("RENFA");
							 bikeService.updateQRLog(QRLog);
		     				responseVo.setErrorId(Constants.CODE.get("ERROR_E5"));
		     				responseVo = setFaiiMsg(responseVo, vo);
		     	    		
		     				return responseVo;
		     			}
		     			else
		     			{
		     				if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
	    							 && !vo.getLatitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
	    					 {

	    						 latitude = new CommonUtil().GetGPS(vo.getLatitude());
	    						 longitude = new CommonUtil().GetGPS(vo.getLongitude());
	
	    						 logger.debug("GPS INFO ##### => : {} , {} ",latitude,longitude);
	
								 Map<String, String> GPS_DATA = null;
								 double USE_DIST = 0.0;
								 int		USE_SEQ = 0;
								 double		ACC_DIST = 0.0;
								 
								 
								 
								 logger.debug("QR_GPS_SIZE : rentseq : {}, latitude : {}, longitude : {}",rentInfo.get("RENT_SEQ").toString(), latitude , longitude);
								 Integer tem_USE_SEQ = 0;
					         	
								 GPS_DATA = new HashMap<String, String>();
								 GPS_DATA.put("RENT_SEQ", rentInfo.get("RENT_SEQ").toString());
					         	
					         	Map<String, Object> bikeData = bikeService.getBikeMoveDist_COUNT(GPS_DATA);
								if(bikeData != null)
								{
									tem_USE_SEQ = Integer.valueOf(String.valueOf(bikeData.get("USE_SEQ")));
									USE_SEQ = tem_USE_SEQ;
									ACC_DIST = new Double(bikeData.get("ACC_DIST").toString()); //???????????? ?????? ????????? db?????? ??????
					        		logger.debug("getBikeMoveDist_COUNT BICYCLE_ID {}, USE_SEQ {}" ,vo.getBicycleId(), USE_SEQ);
					        		
					        		
					        		GPS_DATA.put("BIKE_LATITUDE", String.valueOf(latitude));
						         	GPS_DATA.put("BIKE_LONGITUDE", String.valueOf(longitude));
					        		
					        		
					        		USE_DIST = bikeService.getBikeMoveDist_Last(GPS_DATA);  //db ????????? ????????? ?????? ????????? ???????????? ??????
					        		ACC_DIST += USE_DIST;
					        		logger.debug("GPS DISTANCE LAST DB ACC_DIST {} ", ACC_DIST);
					        		
					        		GPS_DATA = new HashMap<String, String>();
					        		
						         	GPS_DATA.put("RENT_SEQ", rentInfo.get("RENT_SEQ").toString());
						         	
						         	USE_SEQ++;
						         	GPS_DATA.put("BIKE_LATITUDE", String.valueOf(latitude));
						         	GPS_DATA.put("BIKE_LONGITUDE", String.valueOf(longitude));
						        	GPS_DATA.put("USE_SEQ", String.valueOf(USE_SEQ));
						        	GPS_DATA.put("BIKE_ID", vo.getBicycleId());
						        	GPS_DATA.put("USE_DIST", String.valueOf(USE_DIST));
						        	GPS_DATA.put("ACC_DIST", String.valueOf(ACC_DIST));
						        	
						        	logger.debug("QR_GPS_DATA  tolatp : {} , tolongfp : {} -> szlatp :{}  , szlongp : {}, USE_DIST : {}, ACC_DIST : {}"  , String.valueOf(bikeData.get("GPS_X")),String.valueOf(bikeData.get("GPS_Y")), latitude, longitude,String.valueOf(USE_DIST), String.valueOf(ACC_DIST));
						        	bikeService.insertRentGPSDATA(GPS_DATA);
						         	
						         	
					        	}
								else
								{
									USE_SEQ = 0;
					        		logger.debug("getBikeMoveDist_COUNT BICYCLE_ID {}, USE_SEQ {}" ,vo.getBicycleId(), USE_SEQ);
					        		logger.debug("GPS DISTANCE LAST DB ACC_DIST {} ", ACC_DIST);
					        		
					        		GPS_DATA = new HashMap<String, String>();
	
						        	GPS_DATA.put("BIKE_LATITUDE", String.valueOf(latitude));
						         	GPS_DATA.put("BIKE_LONGITUDE", String.valueOf(longitude));
						         	GPS_DATA.put("RENT_SEQ", rentInfo.get("RENT_SEQ").toString());
						         	
						         	USE_SEQ++;
						        	GPS_DATA.put("USE_SEQ", String.valueOf(USE_SEQ));
						        	GPS_DATA.put("BIKE_ID", vo.getBicycleId());
						        	GPS_DATA.put("USE_DIST", String.valueOf(USE_DIST));
						        	GPS_DATA.put("ACC_DIST", String.valueOf(ACC_DIST));
						        	
						        	logger.debug("QR_GPS_DATA  szlatp :{}  , szlongp : {}  USE_DIST : {}, ACC_DIST : {}"  , latitude, longitude,String.valueOf(USE_DIST), String.valueOf(ACC_DIST));
						        	bikeService.insertRentGPSDATA(GPS_DATA);
								}
	    					 }
		     					
		     				
		     				
		     				logger.debug("RENTING_PERIOD  rentProcUpdate usr_seq {} ",com.getUserSeq());
		     				
		     				QRLog.setResAck("RENT");
		     	 			bikeService.updateQRLog(QRLog);
		     	 			
		     				com.setStationId(String.valueOf(rentInfo.get("RENT_STATION_ID")));
		     				com.setUserSeq(String.valueOf(rentInfo.get("USR_SEQ")));
		     				Map<String, Object> msgInfo = bikeService.getRentMsgInfo(com);
		     				
		     				SmsMessageVO sms = new SmsMessageVO();
		     				sms.setTitle("????????????");
		     				sms.setType("S");
		     				
		     				
		     				if(msgInfo != null && msgInfo.get("DEST_NO") != null && !msgInfo.get("DEST_NO").equals(""))
		     				{
			     				//String destno = (String)msgInfo.get("DEST_NO");
			     				String destno = String.valueOf(msgInfo.get("DEST_NO"));
			     				if(destno != null && !destno.equals(""))
			     				{
			     					sms.setDestno(destno);
			     					SmsSender.sender(sms, SendType.SMS_028, 
			     						String.valueOf(msgInfo.get("BIKE_NO")),
			     						String.valueOf(msgInfo.get("STATION_NAME")),
			     						String.valueOf(msgInfo.get("HOUR")),
			     						String.valueOf(msgInfo.get("MINUTES")));
			     					
			     				}
		     				}
		     			}
     				}//?????????
     			}//?????? ???????????????
     		}//?????? 2?????? ?????? ????????? ??????
     		*/
     		
     		
     		logger.debug("RENTING_PERIOD BIKE STATE UPDATE ::::::::::::: ");
    	   
    		 
    		responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); //  f/w ?????? ???????????? ??????
    		
    //		responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); //  f/w ?????? ???????????? ??????
    	
    		responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
    		responseVo.setSeqNum(vo.getSeqNum());
    		responseVo.setCommandId(Constants.CID_RES_REPORTOFBIKE);
    		responseVo.setBicycleState(vo.getBicycleState());
    		responseVo.setBicycleId(vo.getBicycleId());
    		responseVo.setDayAndNight("00");
    		
    		
    		List<HashMap<String, String>> PeriodList = commonService.CheckPeriodTime();
	  		 
    		if(PeriodList.size() == 0 )
    		{
 	  			 logger.error("Period Information Find Error");
 	  			 responseVo.setErrorId(Constants.CODE.get("ERROR_D7"));
 	  			 responseVo = setFaiiMsg(responseVo, vo);
 	           	
 	  			 return responseVo;
    		}
    		else
    		{
    			if(vo.getBicycleState().equals("03"))
    			{
    				for(HashMap<String, String> PeriodMap : PeriodList)
    				{
	  				 
    					if(String.valueOf(PeriodMap.get("COM_CD")).equals("MSI_039"))
    					{
    						logger.debug("######################## MSI_039 " + String.valueOf(PeriodMap.get("ADD_VAL1")));
    					//responseVo.setReturnPeriod(String.valueOf(PeriodMap.get("ADD_VAL1")));
    						int Hour = Integer.parseInt(PeriodMap.get("ADD_VAL1"))/60;
    						int Minute = Integer.parseInt(PeriodMap.get("ADD_VAL1"))%60;
    						responseVo.setPeriodHour(getToString(String.valueOf(Hour),2));
    						responseVo.setPeriodMinute(getToString(String.valueOf(Minute),2));
    					}
    				}
    			
    			}
    			else if(vo.getBicycleState().equals("04"))
    			{
    				for(HashMap<String, String> PeriodMap : PeriodList)
    				{
	  				 
    					if(String.valueOf(PeriodMap.get("COM_CD")).equals("MSI_037"))
    					{
    						logger.debug("######################## MSI_037 " + String.valueOf(PeriodMap.get("ADD_VAL1")));
    					//responseVo.setReturnPeriod(String.valueOf(PeriodMap.get("ADD_VAL1")));
    						int Hour = Integer.parseInt(PeriodMap.get("ADD_VAL1"))/60;
    						int Minute = Integer.parseInt(PeriodMap.get("ADD_VAL1"))%60;
    						responseVo.setPeriodHour(getToString(String.valueOf(Hour),2));
    						responseVo.setPeriodMinute(getToString(String.valueOf(Minute),2));
    					}
    				}
    			
    			}
    		}
    		 
    		return responseVo;
     	
     	}	//??? ????????? 03,04 ?????? ?????? ????????? ??? ?????? ?????? 
     	
     	String faultSeq = commonService.getFaultSeq(com);
     	String bike_status = commonService.checkdBikeStateInfo(com);
     	
     	/* start 2019.05.23 ?????? ?????? ?????? ??? ??????...2021.06.08
     	
     	
     	if(bike_status != null)
     	{
       		if(bike_status.equals("BKS_012")||bike_status.equals("BKS_016") ||bike_status.equals("BKS_017"))	//????????????
     		{
       			Integer enfrc_return_hist_seq = 0;
     		
     			logger.debug("START : TB_SVC_ENFRC_RETURN_PROCESSING is AUTO NORMAL ");
     			
     			enfrc_return_hist_seq = commonService.checkForcedReturnInfo(com);
     			
     		//	logger.debug("START : TB_SVC_ENFRC_RETURN_PROCESSING is AUTO NORMAL {}",enfrc_return_hist_seq.intValue());
     			
     			if( enfrc_return_hist_seq != null && enfrc_return_hist_seq.intValue() != 0)
     			{
     				commonService.updateForcedReturnState(enfrc_return_hist_seq.intValue());
     			}
     			else
     				logger.debug("START : TB_SVC_ENFRC_RETURN_PROCESSING is NULL");
     		}
     		
     	}
     	*/
     	
     	/* end 2019.05.23 ?????? ?????? ?????? */
   //  	logger.debug("START : checkParkingInfo !!!!!!!!! ");
		 
		/**
		 * ????????? ???????????? ????????? ????????????,
		 * ????????? ????????? ?????? ??? ??????????????? ?????? ????????????.
		 * ????????? ????????? ???????????? ???????????? ?????? ???????????? ?????? ??? ?????? ??????.
		 * 
		 * (??????) Locker ?????? (F1) ?????????.
		 * ???????????? ??????(ELB_008)
		 * 
		 * ???????????? ??????????????? ???????????? ??????
		 * 1. ???????????? ?????? ??????
		 * 2. ?????? ?????? (BKS_003) ?????? ??????
		 * 
		 *  ???, ???????????? ???????????? ??? ?????? ??????????????? ???????????? ?????? ??????.
		 */
		if(faultSeq == null)//?????????, ?????? ??????..2020.11.20 c
		{	
		//	if(!bike_status.equals("BKS_005") && !bike_status.equals("BKS_009"))	//????????? , ????????????
			if(!bike_status.equals("BKS_005") && !bike_status.equals("BKS_002"))	//????????? , ?????? 
			{
				logger.debug("PERIOD : BIKE_ID: {} BIKE_STATUS: {}",vo.getBicycleId(),bike_status);
				
	     		commonService.changeValidBike(com);		//BKS_003' ????????? ?????? ??????
			}
	    }
		else
		{
			com.setUserSeq(faultSeq);
			
			
			if(commonService.hasNetFault(com))
			{
				commonService.deleteFaultInfo(com);
				if(!bike_status.equals("BKS_002"))	//?????? ?????????
					commonService.changeValidBike(com);
			}
	   }
		
		/**
		  * ??????????????? ???????????? ?????? ??????, ????????? ????????????.
		  * 1. ??????????????? ?????? ?????? ??????????????? ????????? ??? ????????????.
		  * (?????? Cascade ??? ?????? ????????? ??????????????? ??????????????? ??????)
		  * 2. ??????????????? ??????. ?????? ?????????????????? ???????????? ??????????????? ?????????????????????,
		  * ?????????????????? cascade??? ??????????????? ???????????? ????????? ????????????.
		  */
		
		/**
		 * ???????????? ??????.
		 */
		RentHistVo info = new RentHistVo();
		info.setRETURN_RACK_ID(com.getRockId());
		info.setRENT_BIKE_ID(com.getBicycleId());
		info.setUSE_DIST("0");
		info.setCASCADE_YN("N");
		
		/**
		 * ?????? ?????? ???????????? ?????? ????????? ??????????????? ???????????? ??????.?????? ??? ???????????? ?????? ??????.
		 */
		 Map<String, Object> parkingMap = commonService.checkParkingInfo(com);
		
		 if(parkingMap == null)	//?????? ?????? ??? ???????????? ????????? ...
		 {
			 logger.debug("START : PERIOD : ParkingInfo_insert !!!!!!! {} ",vo.getBicycleId());
			 
			 // ????????? ?????? ?????? INSERT PARKING
			 bikeService.insertPeriodParkingInfo(info);
			// ????????? ?????? ?????? INSERT LOCATION_BIKE
		
			 //2019.03.20 update ?????? 
			 bicycleMapper.rentBikeLocationUpdate(com);
             // ????????? ?????? ?????? INSERT LOCATION_BIKE
             bicycleMapper.insertBikeLocation(info);
//        	 bikeService.insertPeriodBikeLocation(info);
				 
		 }
		 else
		 {
			 //?????? ?????? ??? ????????? ?????? ?????????.....
			 //com.setRockId	->  ?????? ????????????????????? ????????? RACK_ID 
			 //rack_id ?????? ???????????? ????????? rack_id...
			 if(vo.getLockState().equals("01") && vo.getBicycleState().equals("02"))	//?????? ????????? ?????? 
			 {
				 if(!com.getRockId().equals(parkingMap.get("RACK_ID")))
				  {	//???????????? ????????? ????????? ID ??? ????????? ...
					 
					 logger.debug("START : PERIOD : ParkingInfo_location insert !!!!!!! {} {} {} ",vo.getBicycleId(),com.getRockId(),parkingMap.get("RACK_ID"));
					//?????? ?????? ????????? ????????? ????????? ??????.
					//bikeService.deleteDuplicatedParkingInfo(info); 
					 bikeService.parkingInfoDelete(com);
						//?????? ???????????? ?????? ???????????? ????????? ??????.
			//		bikeService.deleteParkingInfoOnly(info);
						
					bikeService.insertPeriodParkingInfo(info);
						// ????????? ?????? ?????? INSERT LOCATION_BIKE
					
						//2019.03.20 update ?????? 
					bicycleMapper.rentBikeLocationUpdate(com);
			             // ????????? ?????? ?????? INSERT LOCATION_BIKE
			        bicycleMapper.insertBikeLocation(info);

						 
					 
				  }//?????? ????????????, ?????? ????????? ??????...

		
			 }
			 /**
			  * ??????????????? ??????????????? ???????????? ????????? ????????? ??????..
			  * ???????????? Pass, ???????????? ????????????.
			  */
		 }
		 
		 
		 /**
		  * ???????????? ?????? ?????? ??????
		  */
		 if(vo.getBicycleState().equals(Constants.CODE.get("BIKE_STATE_02")))
		 {
			 /*

			 if(vo.getLockState().equals("00"))
			 {
	     			//?????? ???????????? ?????? ????????? ?????? ?????????
				 logger.debug("QR_BIKE is RETURN_PERIOD BUT LOCK STATE IS RETURN_STATE");
			 }
			 RentHistVo histInfo = bikeService.checkInvalidRentInfo(com);
			 if(histInfo != null)
			 {
				 logger.debug("PERIOD UPDATE ## INVALID RENT FOUND !! RETURN PROCESS START ");
				 
				 histInfo.setRETURN_STATION_ID(com.getStationId());
				 histInfo.setRETURN_RACK_ID(com.getRockId());
				 histInfo.setTRANSFER_YN("N");
				 //????????? ???????????? ??????(????????? ???????????? ?????? ????????? ?????????????????? ??????)
				 
				 //?????? ?????? ?????? ???????????? QR_LOG ??? ???????????? ?????? ?????? 2020.09.23
				 QRLog.setResAck("RTURN");
  	 			 bikeService.updateQRLog(QRLog);
  	 			
				 try
				 {
					 Date today = new Date();
					 bikeService.deleteRentInfo(histInfo);
					 bikeService.insertInvalidRentHist(histInfo);
					 String returnStationNo = String.valueOf(bicycleMapper.getStationNo(String.valueOf(info.getRETURN_RACK_ID())));
					 String returnStationName = String.valueOf(bicycleMapper.getStationName(String.valueOf(info.getRETURN_RACK_ID()))); // ?????????
					 //SMS??????.
					 if(histInfo.getUSR_MPN_NO() != null && !histInfo.getUSR_MPN_NO().equals(""))
					 {
						 try
						 {
							 
							 SimpleDateFormat sdf;
							 sdf = new SimpleDateFormat("MM???dd??? HH???mm???");
								
							 SmsMessageVO smsVo = new SmsMessageVO();
							 smsVo.setDestno(histInfo.getUSR_MPN_NO());
							 smsVo.setMsg(SendType.SMS_090, histInfo.getBIKE_NO(), String.valueOf(sdf.format(today)), returnStationName);       
							 SmsSender.sender(smsVo);
						 }
						 catch(Exception e)
						 {
							 
						 }
					 }
				 }
				 catch(Exception e)
				 {
				 
				 }
			 }
			 */
		 }
		 
		 //????????? ????????? ????????????  Y,Y ,Y ?????????. 2020.07.21
		 Map<String, Object> serverVersion = fileService.getVersion(com);
		 
		 //Columns: FIRMWARE_CLS_CD, FIRMWARE_VER, FIRMWARE_SEQ, USE_YN, TIME_CAN_DOWN, BIKE_CAN_DOWN
		 //   Row: FWD_001, 30.11, 37, Y, Y, N
		 
		 //logger.debug("##### period status : BLE firmware_time_update=> " + serverVersion.get("FIRMWARE_BLE_TIME_CAN_DOWN") + " firmware_bike_update=> " + serverVersion.get("FIRMWARE_BLE_BIKE_CAN_DOWN"));
		 /*
		 if(serverVersion.get("FIRMWARE_BLE_TIME_CAN_DOWN") != null )
		 {
			 double serverFw = Double.parseDouble(serverVersion.get("FIRMWARE_BLE_VER").toString());
			 
			 boolean fwUseYn = serverVersion.get("FIRMWARE_BLE_USE_YN").equals("Y");
			 
			 boolean fwTimeCanDown = serverVersion.get("FIRMWARE_BLE_TIME_CAN_DOWN").equals("Y");
			 boolean fwBikeCanDown = serverVersion.get("FIRMWARE_BLE_BIKE_CAN_DOWN").equals("Y");
			 double requsetFw  = Double.parseDouble(vo.getBle_firmwareVs().substring(0,2) + "." + vo.getBle_firmwareVs().substring(2, 4));
	//		 boolean chkUseStation = commonService.chkUseStation(com);	//2020.07.20 ??????.
		
			 if(fwTimeCanDown && fwBikeCanDown)
			 {
				 if(fwUseYn)
				 {
					 if(requsetFw <  serverFw )
					 {
						 logger.debug("### YES : PERIOD UPDATE BLE FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
						 responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_01")); //  f/w ?????? ???????????? ??????
						 responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); //  f/w ?????? ???????????? ??????
					 }
					 else
					 {
						 logger.debug("### NO1 : PERIOD UPDATE BLE FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
						 responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
						 
					 }
				 }
				 else	//add 2019.12.19 ??????..
				 {
					 logger.debug("### NO2 : PERIOD UPDATE BLE FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
					 responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
					 
				 }
			 }
			 else
			 {
				 logger.debug("### NO3 : PERIOD UPDATE BLE FIRMWARE UPDATE2 ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID")));
				 responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
			 }
		 }
		 else
		 { 
			 logger.debug("##### PERIOD UPDATE BLE FIRMWARE : FIRM VERSION IS NO GET#####");
			 responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
		 }
		 */
		 responseVo.setBle_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
		 
		 if(responseVo.getBle_fwupdate().equals(Constants.CODE.get("WIFI_UPDATE_00")))  //BLE UPDATE ??????????????? MODEM ?????? ??????
		 {
			 logger.debug("##### period status : MODEL firmware_time_update=> " + serverVersion.get("FIRMWARE_MODEM_TIME_CAN_DOWN") + " firmware_bike_update=> " + serverVersion.get("FIRMWARE_MODEM_BIKE_CAN_DOWN"));
			 
			 if(serverVersion.get("FIRMWARE_MODEM_TIME_CAN_DOWN") != null )
			 {
				 double serverFw = Double.parseDouble(serverVersion.get("FIRMWARE_MODEM_VER").toString());
				 
				 boolean fwUseYn = serverVersion.get("FIRMWARE_MODEM_USE_YN").equals("Y");
				 
				 boolean fwTimeCanDown = serverVersion.get("FIRMWARE_MODEM_TIME_CAN_DOWN").equals("Y");
				 boolean fwBikeCanDown = serverVersion.get("FIRMWARE_MODEM_BIKE_CAN_DOWN").equals("Y");
				 double requsetFw  = Double.parseDouble(vo.getModem_firmwareVs().substring(0,2) + "." + vo.getModem_firmwareVs().substring(2, 4));
		//		 boolean chkUseStation = commonService.chkUseStation(com);	//2020.07.20 ??????.
		//
		//		 logger.debug("##### period_status : MODEM_firmware fwUseYn :{} ,fwTimeCanDown{} , fwBikeCanDown {} , serverFw :{}, requestFw {} ",fwUseYn,fwTimeCanDown ,fwBikeCanDown,serverFw,requsetFw); 
				 
				if(fwTimeCanDown && fwBikeCanDown)
				{
					if(fwUseYn)
					{
						if(requsetFw <  serverFw )	//????????? ?????????????????? ????????? ???????????? 
				//		if(requsetFw !=  serverFw )	//????????? ?????????????????? ????????? ???????????? 
						{
							logger.debug("### YES : PERIOD UPDATE MODEL FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
							responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_01")); //  f/w ?????? ???????????? ??????
						}
						else
						{
							logger.debug("### NO2 : PERIOD UPDATE MODEL FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
							responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
						}
					}
					else	//2019.12.18 ?????? 
					{
						logger.debug("### NO3 : PERIOD UPDATE MODEL FIRMWARE UPDATE ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
						responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
					}
				}
				else
				{
					logger.debug("### NO4 : PERIOD UPDATE MODEL FIRMWARE UPDATE2 ###  BIKE IS  " + com.getCompany_cd() + "!! BIKE NO : " + String.valueOf(ourBikeMap.get("BIKE_NO")) + ", BIKE ID : " + String.valueOf(ourBikeMap.get("BIKE_ID"))); 
					responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
				}
			 }
			 else
			 {	 
				 logger.debug("##### PERIOD UPDATE MODEL FIRMWARE UPDATE : FIRM VERSION IS NO GET#####");
		         responseVo.setModem_fwupdate(Constants.CODE.get("WIFI_UPDATE_00")); // ???????????? ?????? ??????
			 }
		 }
		 
		// String volumeFlag = commonService.getDayAndNightFlag();	// ????????? ???/??? ?????? ?????? ????????? ??????_20170725_JJH
		 String PeriodSet =commonService.getPeriodSetInfo();
		 
		 
		 responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
		 responseVo.setSeqNum(vo.getSeqNum());
		 responseVo.setCommandId(Constants.CID_RES_REPORTOFBIKE);
		 responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_02"));
		 responseVo.setBicycleId(vo.getBicycleId());
		 responseVo.setDayAndNight(PeriodSet);
		 
		 
		 List<HashMap<String, String>> PeriodList = commonService.CheckPeriodTime();
	  		 
		 if(PeriodList.size() == 0 )
		 {
	  			 logger.error("Period Information Find Error");
	  			 responseVo.setErrorId(Constants.CODE.get("ERROR_D7"));
	  			 responseVo = setFaiiMsg(responseVo, vo);
	           	
	  			 return responseVo;
		 }
		 else
		 {
	  			 for(HashMap<String, String> PeriodMap : PeriodList)
	  			 {
	  				 if(String.valueOf(PeriodMap.get("COM_CD")).equals("MSI_036"))
	  				 {
	  					 logger.debug("######################## MSI_036 " + String.valueOf(PeriodMap.get("ADD_VAL1")));
	  					 //responseVo.setReturnPeriod(String.valueOf(PeriodMap.get("ADD_VAL1")));
	  					 int Hour = Integer.parseInt(PeriodMap.get("ADD_VAL1"))/60;
	  					 int Minute = Integer.parseInt(PeriodMap.get("ADD_VAL1"))%60;
	  					 responseVo.setPeriodHour(getToString(String.valueOf(Hour),2));
	  					 responseVo.setPeriodMinute(getToString(String.valueOf(Minute),2));
	  				 }
	  			 }
	  		 }
		 
		 return responseVo;
		 
	 }
	 
	 // ???????????? ???????????? ?????? ?????????
	 public PeriodicStateReportsResponseVo setFaiiMsg(PeriodicStateReportsResponseVo responseVo, PeriodicStateReportsRequestVo vo ){
		 
		 responseVo.setFrameControl(Constants.FAIL_CMD_CONTROL_FIELD);
		 responseVo.setSeqNum(vo.getSeqNum());
		 responseVo.setCommandId(Constants.CID_RES_REPORTOFBIKE);
		 responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_02"));
		 responseVo.setBicycleId(vo.getBicycleId());
		 
		 return responseVo;
	 }
	 
	 public void callBrokenError( CommonVo com){
		 com.setBikeStusCd("BKS_001");
		 com.setBikeBrokenCd("ELB_006");
		 
		 String faultSeq = commonService.getFaultSeq(com);
			if(faultSeq == null){
				/**
				 * ???????????? ????????? ?????? ??????.
				 */
				commonService.insertBrokenBikeErr(com);
				commonService.insertBrokenInvalidLocation(com);
				commonService.insertBrokenBikeReport(com);
		
			}else{
				/**
				 * ??????????????? ?????? ??????
				 * faultSeq ??????.
				 */
				com.setUserSeq(faultSeq);
				/**
				 * ??????????????? ???????????? ????????? SKIP
				 */
				if(!commonService.isInvalidLocationDtl(com)){
					commonService.insertBrokenInvalidLocation(com);
				}
				/**
				 * ?????? ????????????????????? ???????????? SKIP
				 */
				if(commonService.isBrokenReport(com) == 0 ){
					commonService.insertBrokenBikeReport(com);
				}
				
				// ????????? ????????? ??????????????? UPDATE BIKE
				commonService.changeBikeBreakDowon(com);
			}
	 }
	 
	 public void callBrokenLockerError( CommonVo com){
		 com.setBikeStusCd("BKS_001");
		 com.setBikeBrokenCd("ELB_006");
		 
		 String faultSeq = commonService.getFaultSeq(com);
			if(faultSeq == null){
				/**
				 * Locker ???????????? ?????? ??????.
				 */
				commonService.insertBrokenBikeErr(com);
				commonService.insertBrokenLocker(com);
				commonService.insertBrokenBikeReport(com);
		
			}else{
				/**
				 * ??????????????? ?????? ??????
				 * faultSeq ??????.
				 */
				com.setUserSeq(faultSeq);
				/**
				 * ??????????????? ???????????? ????????? SKIP
				 */
				if(!commonService.isBrokenLocker(com)){
					commonService.insertBrokenLocker(com);
				}
				/**
				 * ?????? ????????????????????? ???????????? SKIP
				 */
				if(commonService.isBrokenReport(com) == 0 ){
					commonService.insertBrokenBikeReport(com);
				}
				
				// ????????? ????????? ??????????????? UPDATE BIKE
				commonService.changeBikeBreakDowon(com);
			}
	 }
	 
	 public int versionToInt(String version){
		 
		 if(version == null){
			 version = "0000";
		 }else if(version.equals("0")){
			 version = "0000";
		 }else{
			 String[] array = version.split("\\.");
			 if(array.length == 0){
				 version = Integer.parseInt(version)<10?"0"+version:version;
				 version = version + "00";
				 
			 }else if(array.length == 1){
				 
				 version = Integer.parseInt(array[0])<10?"0"+array[0]:array[0];
				 version += "00";
			 }else{
				 version = Integer.parseInt(array[0])<10?"0"+array[0]:array[0];
				 version += Integer.parseInt(array[1])<10?"0"+array[1]:array[1];
			 }
		 }
		 return Integer.parseInt(version);
	 }
	 
	 
	 public static void main(String[] args) {
		String str = "1.1";
		System.out.println(new PeriodService().versionToInt(str));
		
		str = "1.01";
		System.out.println(new PeriodService().versionToInt(str));
		
		str = "1";
		System.out.println(new PeriodService().versionToInt(str));
		
		str = "10.1";
		System.out.println(new PeriodService().versionToInt(str));
		
		str = "10.11";
		System.out.println(new PeriodService().versionToInt(str));
		
		
	}
	
	 
	 
	 // ???????????? ?????? ?????? 436F cmd : 0x12
	 @RPCService(serviceId = "Bicycle_12", serviceName = "???????????? ???????????? Request", description = "???????????? ???????????? Request")
	 public QRReturnResponseVo autoReturn(QRReturnRequestVo vo) throws Exception
	 {
		 //double latitude, longitude = 0.0d;
		 logger.debug("################### ????????? ?????? ?????? ?????? Bicycle_12");
		 logger.debug("QRReturnConfirmRequestVo vo : {}" , vo);
	    	
		 QRReturnResponseVo responseVo = new QRReturnResponseVo();
	   
		 CommonVo com = new CommonVo();
		 com.setBicycleId(vo.getBicycleId());
		
		 //Beacon id ??????
		 Map<String, String> ReqInfo = new HashMap<String, String>();
		 ReqInfo.put("BEACON_ID", String.valueOf(vo.getBeaconId()));
		 ReqInfo.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
		 
		 Map<String, String> ourBikeMap = new HashMap<String, String>();
		 ourBikeMap = bikeService.chkOurBike(com);	//ENTRPS_CD : ENT_001(vick) , ENT_002(witcom)  , ENT_003(atech)
		 
		 String	 nBikeSerial;
		 if(ourBikeMap != null)
		 {
			//add ????????? ?????? ???????????? 2018.09.01
			String  bikeNo = ourBikeMap.get("BIKE_NO");
		 	nBikeSerial = bikeNo.substring(2,bikeNo.length());
		 	String  ENTRPS_CD = ourBikeMap.get("ENTRPS_CD");
		 	String BIKE_SE_CD = ourBikeMap.get("BIKE_SE_CD");
		 	com.setCompany_cd("CPN_" + BIKE_SE_CD.substring(4,BIKE_SE_CD.length()));
		 	
		 	logger.debug("QR_436F ##### => bike {} ,state {} , company {} ",vo.getBicycleId(),vo.getBicycleState(),com.getCompany_cd());
		 	 
		 	
		 	QRLogVo QRLog = new QRLogVo();
		 	QRLog.setBicycleId(vo.getBicycleId());
		 	QRLog.setBicycleNo(bikeNo);
		 	QRLog.setBicycleState(vo.getBicycleState());
		 	QRLog.setBeaconid(vo.getBeaconId());
		 	QRLog.setLock(vo.getLockState());
		 	QRLog.setBiketype(vo.getElecbicycle());
		
		 	if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
					 && !vo.getLongitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
		 	{
		 		QRLog.setXpos(new CommonUtil().GetGPS(vo.getLatitude()));
		 		QRLog.setYpos(new CommonUtil().GetGPS(vo.getLongitude()));
		 	}
		 	else
            {
		 		QRLog.setXpos(vo.getLatitude());
		 		QRLog.setYpos(vo.getLongitude());        
            }
		 	
		 	
		 	QRLog.setTimeStamp(vo.getRegDttm());
			QRLog.setMessage(vo.getReqMessage());
			QRLog.setQr_frame("?????? ??????");
			
		 	bikeService.insertQRLog(QRLog);
		 }
		 else
		 {
			 logger.error("INVALID ????????? ID ");
			 com.setBikeStusCd("BKS_001");	//????????? ????????? ????????? ?????? 
			 com.setBikeBrokenCd("ELB_006");
			 responseVo.setErrorId(Constants.CODE.get("ERROR_FF"));
			 responseVo = setFaiiMsg(responseVo, vo);
			 
			 return responseVo;
		 }
		 
		 /*
		 if(vo.getBeaconId().equals("00000000000000"))
		 {
			 
			 logger.debug("QR_436F [0x12] : beacon_id is NULL : auth failed {}",vo.getBeaconId());
			 
			 responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));
	  		 responseVo = setFaiiMsg(responseVo, vo);
	  		 responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
	  		
	  		 return responseVo;
			 
		 }
		 */
		 
		 Map<String, Object> stationInfo = null;//commonService.CheckBeaconID(ReqInfo);
		 
		 if(stationInfo == null)
		 {
			 /*
			 if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
					 && !vo.getLongitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
			 {
				 String latitude = new CommonUtil().GetGPS(vo.getLatitude());
				 String longitude = new CommonUtil().GetGPS(vo.getLongitude());
				 logger.debug("GPS INFO ##### => : {} , {} ",String.valueOf(latitude),String.valueOf(longitude));
				 
				 Map<String, String> GPS = new HashMap<String, String>();
				 GPS.put("BIKE_LATITUDE", String.valueOf(latitude));
				 GPS.put("BIKE_LONGITUDE", String.valueOf(longitude));
				 GPS.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
				 //???????????? ?????? ?????? ?????? ?????? 
				 List<HashMap<String, Object>> stationInfo_List = commonService.CheckStation_ForGPS(GPS);
				 
				 if(stationInfo_List.size() == 0 )
				 {
					 logger.error("Station List Find Error");
					 responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));
					 responseVo = setFaiiMsg(responseVo, vo);
			  		
					 return responseVo;
				 }
				 else
				 {
					 logger.debug("##### QR APRRVE ## GPS STATION FIND START! #####");
					 for(HashMap<String, Object> stationTmp : stationInfo_List)
					 {
						 
						 if( Double.parseDouble(String.valueOf(stationTmp.get("DISTANCE_DATA")))   < Double.parseDouble(String.valueOf(stationTmp.get("DSTNC_LT"))))
						 {
							 stationInfo = stationTmp;
							 logger.debug("##### QR APRRVE ## GPS STATION ID[" + String.valueOf(stationTmp.get("STATION_ID")) + "] ENTERED");
							 break;
						 }
						 else
						 {
							 logger.debug("##### QR APRRVE ## GPS STATION ID[" + String.valueOf(stationTmp.get("STATION_ID")) 
									+ "] GPS NOT ENTERED GPS DISTANCE[" + String.valueOf(stationTmp.get("DISTANCE_DATA")) 
									+ "] STATION RANGE[" + String.valueOf(stationTmp.get("DSTNC_LT")) + "] #####");
						 }
					 }
					 logger.debug("##### QR APRRVE ## GPS STATION FIND END! #####");
				 } 
			 }
			 */
			 
		 }
		
		
		
		 
		//STATION ?????? ?????? ?????? ?????? BEACON ID ??? ??????
 		//Map<String, Object> stationInfo = commonService.checkMount(com);
 		
		 if(stationInfo != null)
		 {
			 com.setStationId(stationInfo.get("STATION_ID").toString());
			 com.setRockId(stationInfo.get("RACK_ID").toString());
		 }
		 else
		 {
  			logger.error("beacon_id failed");
  			responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));
  			responseVo = setFaiiMsg(responseVo, vo);
  			
  			responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
  			
  			return responseVo;
		 }
 			 
		
 		//QR ?????? ?????? ??????
		 responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
		 responseVo.setSeqNum(vo.getSeqNum());
		 responseVo.setCommandId(Constants.CID_RES_AUTORETURNBIKE);
		 responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
		 responseVo.setBicycleId(vo.getBicycleId());
		
		 return responseVo;
		 
	 }
	 
	 // ???????????? ?????? ?????? ?????? 
	 public QRReturnResponseVo setFaiiMsg(QRReturnResponseVo responseVo, QRReturnRequestVo vo ){
		 
		 responseVo.setFrameControl(Constants.FAIL_CMD_CONTROL_FIELD);
		 responseVo.setSeqNum(vo.getSeqNum());
		 responseVo.setCommandId(Constants.CID_RES_AUTORETURNBIKE);
		 responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_FF"));
		 responseVo.setBicycleId(vo.getBicycleId());
		 
		 return responseVo;
	 }
	 
	 private String getToString(String str , int length)
	 {
		 int temp = 0;
		 if(str.length() < length)
		 {
			 temp = length - str.length();
			 for (int i = 0; i <  temp; i++)
			 {
				 str = "0"+str;
			 }
		 }
		 return str;
	}
	 
	 
	 
	 

}
