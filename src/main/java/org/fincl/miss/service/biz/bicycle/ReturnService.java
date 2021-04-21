package org.fincl.miss.service.biz.bicycle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fincl.miss.server.annotation.RPCService;
import org.fincl.miss.server.annotation.RPCServiceGroup;
import org.fincl.miss.server.message.MessageHeader;
import org.fincl.miss.service.biz.bicycle.common.CommonUtil;
import org.fincl.miss.service.biz.bicycle.common.CommonVo;
import org.fincl.miss.service.biz.bicycle.common.Constants;
import org.fincl.miss.service.biz.bicycle.common.QRLogVo;
import org.fincl.miss.service.biz.bicycle.service.BicycleRentMapper;
import org.fincl.miss.service.biz.bicycle.service.BicycleRentService;
import org.fincl.miss.service.biz.bicycle.service.CommonService;
import org.fincl.miss.service.biz.bicycle.vo.PeriodicStateReportsRequestVo;
import org.fincl.miss.service.biz.bicycle.vo.RentHistVo;
import org.fincl.miss.service.biz.bicycle.vo.QRReturnResultRequestVo;
import org.fincl.miss.service.biz.bicycle.vo.QRReturnDistResultRequestVo;
import org.fincl.miss.service.biz.bicycle.vo.QRReturnResponseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@RPCServiceGroup(serviceGroupName = "반납")
@Service
public class ReturnService  {

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	CommonService commonService;
	
	@Autowired
	BicycleRentService bikeService;
	
	@Autowired
    private BicycleRentMapper bicycleMapper;

    // 반납 
    @RPCService(serviceId = "Bicycle_03", serviceName = "반납 Request", description = "반납 Request")
    public QRReturnResponseVo waiting(QRReturnResultRequestVo vo)
    {
    	//double latitude, longitude = 0.0d;
    								  
    	logger.debug("################### QR BIKE_Return_RESULT_SUCCESS");
    	logger.debug("QRReturnResultRequestVo vo : {}" , vo);
        
        
        MessageHeader m = vo.getMessageHeader();
        logger.debug(" MessageHeader m:: {}" , m);
        
        QRReturnResponseVo responseVo = new QRReturnResponseVo();
        
        CommonVo com = new CommonVo();
        com.setBicycleId(vo.getBicycleId());
        
        //MOUNT ID 가 어떻게 될지 몰라 비워둠
       // com.setRockId(vo.getBeaconId());
       
        Map<String, String> ourBikeMap = new HashMap<String, String>();
        ourBikeMap = bikeService.chkOurBike(com);	//ENTRPS_CD : ENT_001(vick) , ENT_002(witcom)  , ENT_003(atech) ,BIKE_SE_CD
		 
        String	 nBikeSerial;
        QRLogVo QRLog = new QRLogVo();
        if(ourBikeMap != null)
        {
			//add 자전거 번호 가져오기 2018.09.01
			String  bikeNo = ourBikeMap.get("BIKE_NO");
		 	nBikeSerial = bikeNo.substring(2,bikeNo.length());
		 	String  ENTRPS_CD = ourBikeMap.get("ENTRPS_CD");
		 	com.setCompany_cd("CPN_" + ENTRPS_CD.substring(4,ENTRPS_CD.length()));
		 	String	bike_SE_CD = ourBikeMap.get("BIKE_SE_CD");
		// 	com.setBikeStusCd(bike_SE_CD);
		 	
		 	logger.debug("QR_436F ##### => bike {} ,state {} , company {} ,bike_se_cd {}  ",vo.getBicycleId(),vo.getBicycleState(),com.getCompany_cd(),bike_SE_CD);
		 	
		 	//QRLogVo QRLog = new QRLogVo();
		 	
		 	QRLog.setBicycleId(vo.getBicycleId());
		 	QRLog.setBicycleNo(bikeNo);
		 	QRLog.setBicycleState(vo.getBicycleState());
		 	QRLog.setBeaconid(vo.getBeaconId());
		 	QRLog.setLock(vo.getLockState());
		 	
		 	QRLog.setBiketype(vo.getElecbicycle());
		 	
		 	QRLog.setDev_BATT(String.valueOf(Integer.parseInt(vo.getBattery(), 16)));
		 	QRLog.setBeacon_BATT(String.valueOf(Integer.parseInt(vo.getBeaconBattery(), 16)));
		 	QRLog.setBike_BATT(String.valueOf(Integer.parseInt(vo.getElectbatt(), 16)));
		 	
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
			QRLog.setQr_frame("반납 완료");
		 	
		 	bikeService.insertQRLog(QRLog);
        }
        else
        {
			 logger.error("INVALID 자전거 ID ");
			 com.setBikeStusCd("BKS_001");	//자전거 상태가 장애로 변경 
			 com.setBikeBrokenCd("ELB_006");
			 responseVo.setErrorId(Constants.CODE.get("ERROR_FF"));
			 responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_FF"));
			 
			 responseVo = setFaiiMsg(responseVo, vo);
			 
			 return responseVo;
        }
        
        //밧데리....beacon 밧데리는 필요없음.
        if(!vo.getBattery().equals(null) && !vo.getBattery().equals("")
				&& !vo.getBeaconBattery().equals(null) && !vo.getBeaconBattery().equals(""))
		{
			logger.debug("##### BIKE RETURN ## BATTERY UPDATE START #####");
			
			/*
			PeriodicStateReportsRequestVo periodicStateReportsRequestVo = new PeriodicStateReportsRequestVo();
			periodicStateReportsRequestVo.setBattery(vo.getBattery());
			periodicStateReportsRequestVo.setBicycleId(vo.getBicycleId());
			*/
			
			int battery = Integer.parseInt(vo.getBattery(), 16);
     		String battery_info = null;
     		
     		battery_info = new CommonUtil().getBattery_Info(vo.getBattery());
     		Map<String, String> battery_map = new HashMap<String, String>();
     		battery_map.put("BATTERY", String.valueOf(battery));
     		battery_map.put("BATTERY_INFO", String.valueOf(battery_info));
     		battery_map.put("BICYCLE_ID", vo.getBicycleId());
     		
			commonService.updateBatteryInfo(battery_map);
			
			/*
			if(!vo.getBeaconBattery().equals(null) && !vo.getBeaconBattery().equals("")
					&& !vo.getBeaconId().equals(null) && !vo.getBeaconId().equals(""))
			{
				    				
				int beacon_battery = Integer.parseInt(vo.getBeaconBattery(), 16);
         		Map<String, String> beacon_battery_map = new HashMap<String, String>();
         		beacon_battery_map.put("BATTERY", String.valueOf(beacon_battery));
         		beacon_battery_map.put("BEACON_ID", String.valueOf(vo.getBeaconId()));

				commonService.updateBeaconBatteryInfo(beacon_battery_map);
			}
			
			*/
			//전기 자전거 밧데리 정보.. 2020.07.17 추가
			/* 새싹 따릉이 로 대치..2020.10.05
			if(vo.getElecbicycle().equals("01"))
     		{
     			logger.debug("##### PERIOD ELECTRONIC_BIKE {} ## BATTERY UPDATE END #####",vo.getBicycleId());
     			
	     		if(!vo.getElectbatt().equals(null) && !vo.getElectbatt().equals(""))
	     		{
	     			int beacon_battery = Integer.parseInt(vo.getElectbatt(), 16);
	         		Map<String, String> elec_battery_map = new HashMap<String, String>();
	         		elec_battery_map.put("ELEC_BATTERY", String.valueOf(beacon_battery));
	         		elec_battery_map.put("BICYCLE_ID", vo.getBicycleId());
	         		
					commonService.updateElecBatteryInfo(elec_battery_map);
	     		}
     		}
			*/
			
			logger.debug("##### BIKE RETURN ## BATTERY UPDATE END #####");
		}
        
        /*
        if(vo.getBeaconId().equals("00000000000000"))	//비콘 ID 없으면 임시 반납으로 가야함...
		{
			 
			logger.debug("BIKE RETURN ## beacon_id is NULL : auth failed {}",vo.getBeaconId());
			
			QRLog.setResAck("BID00");
 			bikeService.updateQRLog(QRLog);
 			
			responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));	//invalid 비콘
			responseVo = setFaiiMsg(responseVo, vo);
			responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
	  		
			return responseVo;
			 
		}
		*/
        
        // 자전거 대여정보 확인
        // 자전거 STATION 정보 죄회 수정 필요 BEACON ID로 
        RentHistVo info = bikeService.getForReturnUse(com);
        int overPay = 0;
        
        //Map<String, String> ReqInfo = new HashMap<String, String>();
        
    	//ReqInfo.put("BEACON_ID", String.valueOf(vo.getBeaconId()));
    	//ReqInfo.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
        
        if(info != null)
        {
        	info.setGPS_X(new CommonUtil().GetGPS(vo.getLatitude()));
    		info.setGPS_Y(new CommonUtil().GetGPS(vo.getLongitude()));
    		Map<String, Object> stationInfo = null;
    		
    	
    		logger.debug("BIKE RETURN ## BIKE IS  NORMAL_BIKE : TOTAL_RAK {}",vo.getBicycleId());
    			
			/*	비콬이 없으면 반납 붗가 처리 2020.01.10*/
			if(stationInfo == null)
			{
				if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
						 && !vo.getLongitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
				{
										
					String latitude = new CommonUtil().GetGPS(vo.getLatitude());
			        String longitude = new CommonUtil().GetGPS(vo.getLongitude());
					
					logger.debug("GPS INFO ##### => : {} , {} ",String.valueOf(latitude),String.valueOf(longitude));
					 
					//log 추가 
					 logger.error("Station List Find Error");
					 
					Map<String, String> GPS = new HashMap<String, String>();
					GPS.put("BIKE_LATITUDE", String.valueOf(latitude));
					GPS.put("BIKE_LONGITUDE", String.valueOf(longitude));
					GPS.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
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
						 logger.debug("##### BIKE RETURN ## GPS STATION FIND START! #####");
						 
						 for(HashMap<String, Object> stationTmp : stationInfo_List)
						 {
							 
							 if( Double.parseDouble(String.valueOf(stationTmp.get("DISTANCE_DATA")))   < Double.parseDouble(String.valueOf(stationTmp.get("DSTNC_LT"))))
							 {
								 stationInfo = stationTmp;
								 logger.debug("##### BIKE RETURN ## GPS STATION ID[" + String.valueOf(stationTmp.get("STATION_ID")) + "] ENTERED");
								 break;
							 }
							 else
							 {
								 logger.debug("##### BIKE RETURN ## GPS STATION ID[" + String.valueOf(stationTmp.get("STATION_ID")) 
										+ "] GPS NOT ENTERED GPS DISTANCE[" + String.valueOf(stationTmp.get("DISTANCE_DATA")) 
										+ "] STATION RANGE[" + String.valueOf(stationTmp.get("DSTNC_LT")) + "] #####");
							 }
						 }
						 
						 //2020.01.20 test 에서 gps 로 반납이 안되도록 함.
						 //logger.debug("##### BIKE RETURN ## GPS STATION FIND END! (TEST_FORCE TO BUT RETURN is FAILED)  #####");
						 
						 //stationInfo = null;
					 } 
				 }
			}	//stationInfo is null : gps 체크 안함....
     		
     		
		
     		if(stationInfo != null)
			{
     			com.setStationId(stationInfo.get("STATION_ID").toString());
     			com.setRockId(stationInfo.get("RACK_ID").toString());
     			info.setRETURN_STATION_ID(com.getStationId());
     			info.setRETURN_RACK_ID(com.getRockId());
     	
			}
     		else
     		{
     			
     			logger.error("stationInfo is NULL : beconID is not find NO RETURN : {} ", vo.getBeaconId());
     			
     			QRLog.setResAck("RBF");
     			bikeService.updateQRLog(QRLog);
     			
     			responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
     			responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));
     			responseVo = setFaiiMsg(responseVo, vo);
     			 
     			return responseVo;
     			
     		}

     		// 케스케이드 반납 확인
     		info.setCASCADE_YN("N");
     		    	
     		// 반납 시간
     		//20191016111015
     		SimpleDateFormat transFormat = new SimpleDateFormat("yyyyMMddHHmmss");
     		Date Return_dttm = null;
     		try {
				Return_dttm = transFormat.parse(vo.getRegDttm());
			} catch (ParseException e) {
				e.printStackTrace();
			}
     		
     		SimpleDateFormat RENTFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     		Date Rent_Dttm = null;
     		try {
				Rent_Dttm = RENTFormat.parse(info.getRENT_DTTM());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
     		
     		//long diff = Return_dttm.getTime() - Rent_Dttm.getTime();  //밀리 초
     		//int overTime = Math.round(diff/(1000*60));    //분단위로 변경
     		
     		
        	int sysTime = Integer.parseInt(info.getUSE_MI().toString());
        	
        	info.setRETURN_STATION_ID(com.getStationId());
        	info.setTRANSFER_YN("N");
        	info.setOVER_FEE_YN("N");
        	
         	Integer tem_USE_SEQ = 0;
         	
         	Map<String, String> GPS_DATA = new HashMap<String, String>();
         	GPS_DATA.put("RENT_SEQ", info.getRENT_SEQ());
         	
         	double ACC_DIST = 0.0;
         	
         	info.setUSE_DIST("0");	//gps 로 거리 하는 함수 막음.

        	info.setUSE_MI(sysTime+"");
        	
        	int weight = bikeService.getUserWeight(info.getUSR_SEQ());
        	
        	double co2 = (((double)ACC_DIST/1000)*0.232);
        	double cal = 5.94 * (weight==0?65:weight) *((double)ACC_DIST/1000) / 15;
        	

        	info.setREDUCE_CO2(co2+"");
        	info.setCONSUME_CAL(cal+"");
        		
        	//프리미엄 이용권 초과요금 적용 시간 가져오기
        	//int baseRentTime = Integer.parseInt((String)commonService.getBaseTime(info).get("BASE_TIME"));
        	// 프리미엄 이용권 자전거 기본대여시간 가져오기 (일반권 포함)_20160630_JJH_END
        	
        	int baseRentTime = 0;
        	// 초과 요금 대상
    		//if(overTime > baseRentTime)
        	/*
        	if(info.getBIKE_SE_CD().equals("BIK_001"))
        	{
        		baseRentTime = 20;
        	}
        	else
        	{
        		baseRentTime = 0;
        	}
        	*/
    		//if(sysTime > baseRentTime)
    		//{
        	logger.debug(" #####  server_time is baseRentTime {}  sysTime {}"  , baseRentTime, sysTime);
    		
    		
			Map<String, Object> fee = new HashMap<String, Object>();
			
			fee.put("ADD_FEE_CLS", info.getPAYMENT_CLS_CD());
			fee.put("BIKE_SE_CD", info.getBIKE_SE_CD());
			
			/*
			if(info.getBIKE_SE_CD().equals("BIK_001"))
        	{
				fee.put("ADD_FEE_CLS", "B");
        	}
        	else
        	{
        		fee.put("ADD_FEE_CLS", "K");
        	}
        	*/
			
			
			Map<String, Object> minPolicy = bikeService.getOverFeeMinPolicy(fee);	//TB_SVC_ADD_FEE  
			Map<String, Object> maxPolicy = bikeService.getOverFeeMaxPolicy(fee);
    			
			overPay = new CommonUtil().getPay(minPolicy, maxPolicy, sysTime);
  
			if(overPay >0)
			{
				info.setOVER_FEE_YN("Y");
			
				info.setOVER_FEE(overPay+"");
				baseRentTime = Integer.parseInt(minPolicy.get("OVER_STR_MI").toString()) -1;
				info.setOVER_MI(String.valueOf(sysTime - (baseRentTime)));
			}
    			/*
    		}
    		else
    		{
    		//	if(overTime < 0)
    				
    			logger.debug(" ##### systime_time is baseRentTime {} ,sysTime {}", baseRentTime ,sysTime);
    		}
    		*/
    		/*  추가만 하고 적용 안함..
    		String faultSeq = commonService.getFaultSeq(com);
    		
    		if(faultSeq != null)
    		{
    			if(commonService.hasNetFault(com))
    			{
    				commonService.deleteFaultInfo(com);
    				commonService.changeValidBike(com);
    				faultSeq = commonService.getFaultSeq(com);
    			}
    		}
        	*/
    		
        	// 반납 프로세스 실행
    		bikeService.parkingInfoDelete(com);
    		bikeService.procReturn(info);
        	
        }
        else
        {
        	//RentHistVo info = bikeService.getForReturnUse(com);
        	logger.error("RentHistVo is NOT EXIST : no_getForReturnUse ");
        	
        	Map<String, String> BeaconInfo = null;
        			//commonService.CheckBeacon_Station(ReqInfo);
        	
        	
        	if(BeaconInfo == null)
        	{
        		responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
        	
        		responseVo.setErrorId(Constants.CODE.get("ERROR_F6"));
        		responseVo = setFaiiMsg(responseVo, vo);
        	
        		QRLog.setResAck("RF:00");
        	
        		bikeService.updateQRLog(QRLog);
        	}
        	else
        	{
        		if(BeaconInfo.get("STATION_ID") != null || !BeaconInfo.get("STATION_ID").toString().equals(""))
        		{
        			BeaconInfo.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
            		//RAK_002, RACK_003
            		int RACKCnt = commonService.CheckBeacon_RACK(BeaconInfo);
            		if(RACKCnt == 0)
            		{
            		//	String BIKE_SE_CD = commonService.CheckQRBIKE_Info(BeaconInfo);
            			
            			logger.debug("##### STATION QR_RACK VIRTUAL_RACK CREATED  #####");
            			
            			if(ourBikeMap.get("BIKE_SE_CD").toString().equals("BIK_002"))
            			{
            				commonService.InsertQR_RACK_0(BeaconInfo);
            			}
            			else if(ourBikeMap.get("BIKE_SE_CD").toString().equals("BIK_003"))
            			{
            				commonService.InsertQR_RACK_99(BeaconInfo);
            			}
            		}
            		
            		Map<String, Object> stationInfo = null;
            		
            		/*
            		if(vo.getElecbicycle().equals("02"))	//새싹 따릉이....
            		{
            			logger.debug("BIKE RETURN ## BIKE IS  SHOOT_BIKE : CHECK_RAK_003_004 {}",vo.getBicycleId());
            			
            			//stationInfo = commonService.CheckBeaconID_Shoot(ReqInfo);
            		}
            		else
            		{
            		*/
            			logger.debug("BIKE RETURN ## BIKE IS  NORMAL_BIKE : TOTAL_RAK {}",vo.getBicycleId());
            			
            			//stationInfo = commonService.CheckBeaconID(ReqInfo);
            	//	}
    				
    	     		if(stationInfo != null)
    				{
    	     			com.setStationId(stationInfo.get("STATION_ID").toString());
    	     			com.setRockId(stationInfo.get("RACK_ID").toString());
    	     			info = new RentHistVo();
    	     			info.setRENT_BIKE_ID(vo.getBicycleId());
    	     			info.setRETURN_STATION_ID(com.getStationId());
    	     			info.setRETURN_RACK_ID(com.getRockId());
    	     			info.setCASCADE_YN("N");
    	        		info.setUSE_DIST("0");
    	        		
    	        		 Map<String, Object> parkingMap = commonService.checkParkingInfo(com);
    	        		
    	        		 if(parkingMap == null)	//주기 보고 시 파깈정보 없으면 ...
    	        		 {
    	        			 logger.debug("RETURN : ParkingInfo_insert !!!!!!! {} ",vo.getBicycleId());
    	        			 
    	        			 // 자전거 주차 정보 INSERT PARKING
    	        			 bikeService.insertPeriodParkingInfo(info);
    	        			 
    	        			 // 자전거 정보 UPDATE BIKE
    	        			 bicycleMapper.updateBike(info);
    	        			 
    	        			 
    	        			// 자전거 배치 이력 INSERT LOCATION_BIKE
    	        		
    	        			 //2019.03.20 update 추가 
    	        			 //bicycleMapper.rentBikeLocationUpdate(com);
    	                     // 자전거 배치 이력 INSERT LOCATION_BIKE
    	                     //bicycleMapper.insertBikeLocation(info);
    	                	 //bikeService.insertPeriodBikeLocation(info);
    	        				 
    	        		 }
    				}
        		}
        		
        		responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
        		responseVo.setSeqNum(vo.getSeqNum());
        		responseVo.setCommandId(Constants.CID_RES_RETURNBIKE);
        		responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_02"));
        		responseVo.setBicycleId(vo.getBicycleId());
        		
        		
        		
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
        		
        		QRLog.setResAck("RSUC");
        		bikeService.updateQRLog(QRLog);
        	}
        	return responseVo;
        }
        
        //app 설치 사용자 한정.
        if(info.getUSR_DEVICE_TYPE() != null) {
       
        	//this.rentSuccessPushMsg(pushVo);
        }
        
        QRLog.setResAck("SUC");
        bikeService.updateQRLog(QRLog);
        
        responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
        responseVo.setSeqNum(vo.getSeqNum());
        responseVo.setCommandId(Constants.CID_RES_RETURNBIKE);
        responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_02"));
        responseVo.setBicycleId(vo.getBicycleId());
        
        
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
    
    
    


	/*private void rentSuccessPushMsg(PushTargetVO pushVo) {
		
		if(pushVo.getUsrDeviceType().equals(IConstants.PUSH_TYPE_GCM)) {
			
		} else {
			
			
		}
		
	}*/


	// 반납 실패 메세지
    public QRReturnResponseVo setFaiiMsg(QRReturnResponseVo responseVo, QRReturnResultRequestVo vo ){
    	
    	responseVo.setFrameControl(Constants.FAIL_CMD_CONTROL_FIELD);
    	responseVo.setSeqNum(vo.getSeqNum());
    	responseVo.setCommandId(Constants.CID_RES_RETURNBIKE);
    	responseVo.setBicycleId(vo.getBicycleId());
   // 	responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_FF"));
    	
    	return responseVo;
    }
    
    @RPCService(serviceId = "Bicycle_17", serviceName = "반납거리 Request", description = "반납거리 Request")
    public QRReturnResponseVo distwaiting(QRReturnDistResultRequestVo vo)
    {
    	logger.debug("################### QR BIKE_Distance_Return_RESULT_SUCCESS");
    	logger.debug("QRReturnDistResultRequestVo vo : {}" , vo);
                
        MessageHeader m = vo.getMessageHeader();
        logger.debug(" MessageHeader m:: {}" , m);
        
        QRReturnResponseVo responseVo = new QRReturnResponseVo();
        
        CommonVo com = new CommonVo();
        com.setBicycleId(vo.getBicycleId());
        
        //MOUNT ID 가 어떻게 될지 몰라 비워둠
       // com.setRockId(vo.getBeaconId());
       
        Map<String, String> ourBikeMap = new HashMap<String, String>();
        ourBikeMap = bikeService.chkOurBike(com);	//ENTRPS_CD : ENT_001(vick) , ENT_002(witcom)  , ENT_003(atech) ,BIKE_SE_CD
		 
        String	 nBikeSerial;
        QRLogVo QRLog = new QRLogVo();
        if(ourBikeMap != null)
        {
			//add 자전거 번호 가져오기 2018.09.01
			String  bikeNo = ourBikeMap.get("BIKE_NO");
		 	nBikeSerial = bikeNo.substring(2,bikeNo.length());
		 	String  ENTRPS_CD = ourBikeMap.get("ENTRPS_CD");
		 	com.setCompany_cd("CPN_" + ENTRPS_CD.substring(4,ENTRPS_CD.length()));
		 	String	bike_SE_CD = ourBikeMap.get("BIKE_SE_CD");
		// 	com.setBikeStusCd(bike_SE_CD);
		 	
		 	logger.debug("QR_436F ##### => bike {} ,state {} , company {} ,bike_se_cd {}  ",vo.getBicycleId(),vo.getBicycleState(),com.getCompany_cd(),bike_SE_CD);
		 	
		// 	QRLogVo QRLog = new QRLogVo();
		 	
		 	QRLog.setBicycleId(vo.getBicycleId());
		 	QRLog.setBicycleNo(bikeNo);
		 	QRLog.setBicycleState(vo.getBicycleState());
		 	QRLog.setBeaconid(vo.getBeaconId());
		 	QRLog.setLock(vo.getLockState());
		 	QRLog.setBiketype(vo.getElecbicycle());
		 	
		 	QRLog.setDev_BATT(String.valueOf(Integer.parseInt(vo.getBattery(), 16)));
		 	QRLog.setBeacon_BATT(String.valueOf(Integer.parseInt(vo.getBeaconBattery(), 16)));
		 	QRLog.setBike_BATT(String.valueOf(Integer.parseInt(vo.getElectbatt(), 16)));
		 	
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
		 	QRLog.setFirm_fw(vo.getDistance());
			QRLog.setMessage(vo.getReqMessage());
			QRLog.setQr_frame("반납 완료");
		 	
		 	bikeService.insertQRLog(QRLog);
        }
        else
        {
			 logger.error("INVALID 자전거 ID ");
			 com.setBikeStusCd("BKS_001");	//자전거 상태가 장애로 변경 
			 com.setBikeBrokenCd("ELB_006");
			 responseVo.setErrorId(Constants.CODE.get("ERROR_FF"));
			 responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_FF"));
			 responseVo = setFaiiDistMsg(responseVo, vo);
			 
			 return responseVo;
        }
        
        //밧데리....
        if(!vo.getBattery().equals(null) && !vo.getBattery().equals("")
				&& !vo.getBeaconBattery().equals(null) && !vo.getBeaconBattery().equals(""))
		{
			logger.debug("##### BIKE RETURN ## BATTERY UPDATE START #####");
			
			/*
			PeriodicStateReportsRequestVo periodicStateReportsRequestVo = new PeriodicStateReportsRequestVo();
			periodicStateReportsRequestVo.setBattery(vo.getBattery());
			periodicStateReportsRequestVo.setBicycleId(vo.getBicycleId());
			*/
			
			int battery = Integer.parseInt(vo.getBattery(), 16);
     		String battery_info = null;
     		
     		battery_info = new CommonUtil().getBattery_Info(vo.getBattery());
     		Map<String, String> battery_map = new HashMap<String, String>();
     		battery_map.put("BATTERY", String.valueOf(battery));
     		battery_map.put("BATTERY_INFO", String.valueOf(battery_info));
     		battery_map.put("BICYCLE_ID", vo.getBicycleId());
     		
			commonService.updateBatteryInfo(battery_map);
			
			if(!vo.getBeaconBattery().equals(null) && !vo.getBeaconBattery().equals("")
					&& !vo.getBeaconId().equals(null) && !vo.getBeaconId().equals(""))
			{
				    				
				int beacon_battery = Integer.parseInt(vo.getBeaconBattery(), 16);
         		Map<String, String> beacon_battery_map = new HashMap<String, String>();
         		beacon_battery_map.put("BATTERY", String.valueOf(beacon_battery));
         		beacon_battery_map.put("BEACON_ID", String.valueOf(vo.getBeaconId()));

				commonService.updateBeaconBatteryInfo(beacon_battery_map);
			}
			
			//전기 자전거 밧데리 정보.. 2020.07.17 추가
			/* 새싹 따릉이 로 대치..2020.10.05
			if(vo.getElecbicycle().equals("01"))
     		{
     			logger.debug("##### PERIOD ELECTRONIC_BIKE {} ## BATTERY UPDATE END #####",vo.getBicycleId());
     			
	     		if(!vo.getElectbatt().equals(null) && !vo.getElectbatt().equals(""))
	     		{
	     			int beacon_battery = Integer.parseInt(vo.getElectbatt(), 16);
	         		Map<String, String> elec_battery_map = new HashMap<String, String>();
	         		elec_battery_map.put("ELEC_BATTERY", String.valueOf(beacon_battery));
	         		elec_battery_map.put("BICYCLE_ID", vo.getBicycleId());
	         		
					commonService.updateElecBatteryInfo(elec_battery_map);
	     		}
     		}
			*/
			
			logger.debug("##### BIKE RETURN ## BATTERY UPDATE END #####");
		}
        
        /*
        if(vo.getBeaconId().equals("00000000000000"))	//비콘 ID 없으면 임시 반납으로 가야함...
		{
			 
			logger.debug("BIKE RETURN ## beacon_id is NULL : auth failed {}",vo.getBeaconId());
			
			QRLog.setResAck("BID00");
 			bikeService.updateQRLog(QRLog);
 			
			responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));	//invalid 비콘
			responseVo = setFaiiDistMsg(responseVo, vo);
			responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
	  		
			return responseVo;
			 
		}
        */
        // 자전거 대여정보 확인
        // 자전거 STATION 정보 죄회 수정 필요 BEACON ID로 
        RentHistVo info = bikeService.getForReturnUse(com);
        info.setGPS_X(new CommonUtil().GetGPS(vo.getLatitude()));
		info.setGPS_Y(new CommonUtil().GetGPS(vo.getLongitude()));
        int overPay = 0;
        
        Map<String, String> ReqInfo = new HashMap<String, String>();
    	ReqInfo.put("BEACON_ID", String.valueOf(vo.getBeaconId()));
    	ReqInfo.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
        
        if(info != null)
        {	
        	//비콘으로 반납 정거장 확인....
        	//SELECT NOW_LOCATE_ID AS STATION_ID FROM TB_OPR_QR_BEACON WHERE BEACON_ID = #{BEACON_ID}
        	
        	Map<String, String> BeaconInfo = commonService.CheckBeacon_Station(ReqInfo);	
        	if(BeaconInfo != null || BeaconInfo.get("STATION_ID") != null || !BeaconInfo.get("STATION_ID").toString().equals(""))
        	{
        		BeaconInfo.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
        		//RAK_002, RACK_003
        		int RACKCnt = commonService.CheckBeacon_RACK(BeaconInfo);
        		if(RACKCnt == 0)
        		{
        		//	String BIKE_SE_CD = commonService.CheckQRBIKE_Info(BeaconInfo);
        			
        			logger.debug("##### STATION QR_RACK VIRTUAL_RACK CREATED  #####");
        			if(ourBikeMap.get("BIKE_SE_CD").toString().equals("BIK_002"))
        			{
        				commonService.InsertQR_RACK_0(BeaconInfo);
        			}
        			else if(ourBikeMap.get("BIKE_SE_CD").toString().equals("BIK_003"))
        			{
        				commonService.InsertQR_RACK_99(BeaconInfo);
        			}
        		}
        		
        		Map<String, Object> stationInfo;
        		/*
        		if(vo.getElecbicycle().equals("02"))	//새싹 따릉이....
        		{
        			logger.debug("BIKE RETURN ## BIKE IS  SHOOT_BIKE : CHECK_RAK_003_004 {}",vo.getBicycleId());
        			
        			//stationInfo = commonService.CheckBeaconID_Shoot(ReqInfo);
        		}
        		else
        		{
        			*/
        			logger.debug("BIKE RETURN ## BIKE IS  NORMAL_BIKE : TOTAL_RAK {}",vo.getBicycleId());
        			
        			stationInfo = commonService.CheckBeaconID(ReqInfo);
        	//	}
				/*	비콬이 없으면 반납 붗가 처리 2020.01.10
				if(stationInfo == null)
				{
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
							 logger.debug("##### BIKE RETURN ## GPS STATION FIND START! #####");
							 
							 for(HashMap<String, Object> stationTmp : stationInfo_List)
							 {
								 
								 if( Double.parseDouble(String.valueOf(stationTmp.get("DISTANCE_DATA")))   < Double.parseDouble(String.valueOf(stationTmp.get("DSTNC_LT"))))
								 {
									 stationInfo = stationTmp;
									 logger.debug("##### BIKE RETURN ## GPS STATION ID[" + String.valueOf(stationTmp.get("STATION_ID")) + "] ENTERED");
									 break;
								 }
								 else
								 {
									 logger.debug("##### BIKE RETURN ## GPS STATION ID[" + String.valueOf(stationTmp.get("STATION_ID")) 
											+ "] GPS NOT ENTERED GPS DISTANCE[" + String.valueOf(stationTmp.get("DISTANCE_DATA")) 
											+ "] STATION RANGE[" + String.valueOf(stationTmp.get("DSTNC_LT")) + "] #####");
								 }
							 }
							 
							 //2020.01.20 test 에서 gps 로 반납이 안되도록 함.
							 logger.debug("##### BIKE RETURN ## GPS STATION FIND END! (TEST_FORCE TO BUT RETURN is FAILED)  #####");
							 
							 stationInfo = null;
						 } 
					 }
				}	//stationInfo is null : gps 체크 안함....
	     		
	     		*/
			
	     		if(stationInfo != null)
				{
	     			com.setStationId(stationInfo.get("STATION_ID").toString());
	     			com.setRockId(stationInfo.get("RACK_ID").toString());
	     			info.setRETURN_STATION_ID(com.getStationId());
	     			info.setRETURN_RACK_ID(com.getRockId());
	     			info.setSTATION_USE_YN(stationInfo.get("STATION_USE_YN").toString());
	     			info.setSTATION_CLOSE_REASON(stationInfo.get("REASON").toString());
	     			info.setSTATION_CLOSE_DATE(stationInfo.get("UNUSE_STR_DTTM").toString());
				}
	     		else
	     		{
	     			
	     			logger.error("stationInfo is NULL : beconID is not find NO RETURN : {} ", vo.getBeaconId());
	     			
	     			QRLog.setResAck("RBF");
	     			bikeService.updateQRLog(QRLog);
	     			
	     			responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
	     			responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));
	     			responseVo = setFaiiDistMsg(responseVo, vo);
	     			 
	     			return responseVo;
	     			
	     		}
        	}
        	else
        	{
        		logger.error("stationInfo is NULL : beconID is not find NO RETURN : {} ", vo.getBeaconId());
     			
     			QRLog.setResAck("SBF");
     			bikeService.updateQRLog(QRLog);
     			
     			responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
     			responseVo.setErrorId(Constants.CODE.get("ERROR_FE"));	//invalid 정거장
     			responseVo = setFaiiDistMsg(responseVo, vo);
     			 
     			return responseVo;
        	}
     		
     		// 케스케이드 반납 확인
     		info.setCASCADE_YN("N");

        	
            
        	// 방전 error 검출
        	/**
        	 * 반납시 전달되는 방전여부는 자가발전여부를 의미.실제로는 주기적인 상태보고에서만 확인하기로 함.
        	 * 반납시 방전오류는 제거.
        	*/
//        	if(vo.getErrorId().equals("E4")){
//        		// 충전상태 이상 UPdate
//        		commonService.updateBatteryDischarge(com);
//        	}
     		
     		    	
     		// 반납 시간
     		//20191016111015
     		SimpleDateFormat transFormat = new SimpleDateFormat("yyyyMMddHHmmss");
     		Date Return_dttm = null;
     		try {
				Return_dttm = transFormat.parse(vo.getRegDttm());
			} catch (ParseException e) {
				e.printStackTrace();
			}
     		
     		SimpleDateFormat RENTFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     		Date Rent_Dttm = null;
     		try {
				Rent_Dttm = RENTFormat.parse(info.getRENT_DTTM());
			} catch (ParseException e) {
				e.printStackTrace();
			}
     		
     		long diff = Return_dttm.getTime() - Rent_Dttm.getTime();  //밀리 초
     		int overTime = Math.round(diff/(1000*60));    //분단위로 변경
     		
     		
        	int sysTime = Integer.parseInt(info.getUSE_MI().toString());
        	
        	info.setRETURN_STATION_ID(com.getStationId());
        	info.setTRANSFER_YN("N");
        	info.setOVER_FEE_YN("N");
        	
        	//double USE_DIST = 0.0;
        	/* gps 이력으로 하는 부분 막음 2020.03.12 
        	double USE_DIST = bikeService.getBikeMoveDist(com);
        	logger.debug("Bike USE DISTANCE {} , {} " ,vo.getBicycleId(), String.valueOf(USE_DIST));
        	
        	if(!vo.getLatitude().equals("00000000") && !vo.getLatitude().substring(0,6).equals("FFFFFF")
					 && !vo.getLongitude().equals("00000000") && !vo.getLongitude().subSequence(0, 6).equals("FFFFFF"))
			{
	        	Map<String, String> GPS_DATA = new HashMap<String, String>();
	        	String latitude = new CommonUtil().GetGPS(vo.getLatitude());
		        String longitude = new CommonUtil().GetGPS(vo.getLongitude());
	        	GPS_DATA.put("BIKE_LATITUDE", String.valueOf(latitude));
	        	GPS_DATA.put("BIKE_LONGITUDE", String.valueOf(longitude));
	        	GPS_DATA.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
	        	int cnt = bikeService.getBikeMoveDist_COUNT(GPS_DATA);
	        	
	        	logger.debug("getBikeMoveDist_COUNT {} " ,vo.getBicycleId(), cnt);
	        	
	        	if(cnt > 0)
	        	{
	        		USE_DIST = USE_DIST + bikeService.getBikeMoveDist_Last(GPS_DATA);
	        		logger.debug("Bike USE DISTANCE2 {} , {} " ,vo.getBicycleId(), String.valueOf(USE_DIST));
	        		
	        	}
			}
        	*/
        	
        	/*
        	Integer tem_USE_SEQ = 0;
         	
         	Map<String, String> GPS_DATA = new HashMap<String, String>();
         	GPS_DATA.put("RENT_SEQ", info.getRENT_SEQ());
         	
         	
         	double ACC_DIST = 0.0;
         	
         	Map<String, Object> bikeData = bikeService.getBikeMoveDist_COUNT(GPS_DATA);
         	
			if(bikeData != null)
			{
				tem_USE_SEQ = Integer.valueOf(String.valueOf(bikeData.get("USE_SEQ")));
				int USE_SEQ = tem_USE_SEQ;
				ACC_DIST = new Double(bikeData.get("ACC_DIST").toString()); //현재까지 누적 데이터 db에서 추출
        		logger.debug("getBikeMoveDist_COUNT BICYCLE_ID {}, USE_SEQ {}" ,vo.getBicycleId(), USE_SEQ);
        		
        		String latp = new CommonUtil().GetGPS(vo.getLatitude());
        		String lonp = new CommonUtil().GetGPS(vo.getLongitude());
        		GPS_DATA.put("BIKE_LATITUDE", String.valueOf(latp));
	         	GPS_DATA.put("BIKE_LONGITUDE", String.valueOf(lonp));
        		
	         	double dlatp =  Double.parseDouble( latp);
	         	double dlonp = Double.parseDouble( lonp);
        		if(dlatp != 0.0 && dlonp != 0.0)
        		{
        			double USE_DIST = bikeService.getBikeMoveDist_Last(GPS_DATA);  //db 데이터 최종과 현재 첫번째 데이터와 비교
        			ACC_DIST += USE_DIST;
        		}
        		logger.debug("GPS DISTANCE LAST DB ACC_DIST {} ", ACC_DIST);
        		
        		info.setUSE_DIST(ACC_DIST + "");
        	}
			else
				info.setUSE_DIST("0");	//gps 로 거리 하는 함수 막음.
				*/
        	//
        //	info.setUSE_DIST(USE_DIST+"");
        	
        	/*
        	if(overTime >=0)
        		info.setUSE_MI(overTime+"");
        	else
        	*/
			
			
			// 이동거리
        	int km = Integer.parseInt(vo.getDistance().substring(0,2), 16);
        	int me = Integer.parseInt(vo.getDistance().substring(2,4), 16);
        	int distance = (km*1000)+(me*10);
        	logger.debug(" #####  distance {} km {} me {}", distance, km, me);
			
        	info.setUSE_MI(sysTime+"");
        	info.setUSE_DIST(distance+"");
        	
        	int weight = bikeService.getUserWeight(info.getUSR_SEQ());
        	
        	//double co2 = (((double)ACC_DIST/1000)*0.232);
        	//double cal = 5.94 * (weight==0?65:weight) *((double)ACC_DIST/1000) / 15;
        	
        	double co2 = (((double)distance/1000)*0.232);
        	double cal = 5.94 * (weight==0?65:weight) *((double)distance/1000) / 15;
        	
        //	info.setREDUCE_CO2("0");
        //	info.setCONSUME_CAL("0");
        	
        	info.setREDUCE_CO2(co2+"");
        	info.setCONSUME_CAL(cal+"");
        	
        	// 자전거 기본 대여시간 분
        	// 프리미엄 이용권 자전거 기본대여시간 가져오기 (일반권 포함)_20160630_JJH_START
        	
        	//프리미엄 이용권 초과요금 적용 시간 가져오기
        	int baseRentTime = Integer.parseInt((String)commonService.getBaseTime(info).get("BASE_TIME"));
        	// 프리미엄 이용권 자전거 기본대여시간 가져오기 (일반권 포함)_20160630_JJH_END
        	
        	
        	// 초과 요금 대상
    		//if(overTime > baseRentTime)
    		if(sysTime > baseRentTime)
    	    		
    		{
    			logger.debug(" #####  server_time is baseRentTime {}  overTime {} ,sysTime {}"  ,baseRentTime, overTime ,sysTime);
    		
    		
    			Map<String, Object> fee = new HashMap<String, Object>();
    			//T-APP PATCH , 전기 자전거 추가요금
    			if(info.getPAYMENT_CLS_CD().equals("BIL_021") || info.getPAYMENT_CLS_CD().equals("BIL_022"))
    			{
    				fee.put("ADD_FEE_CLS", "D");
    			}
    			else
    			{
    				fee.put("ADD_FEE_CLS", info.getADD_FEE_CLS());
    			}
    			Map<String, Object> minPolicy = bikeService.getOverFeeMinPolicy(fee);	//TB_SVC_ADD_FEE  
    			Map<String, Object> maxPolicy = bikeService.getOverFeeMaxPolicy(fee);
    		
    			//overPay = new CommonUtil().getPay(minPolicy, maxPolicy, overTime);
    			overPay = new CommonUtil().getPay(minPolicy, maxPolicy, sysTime);
    
    			//if(!(info.getUSR_CLS_CD().equals("USR_002")))
        		//{
    				info.setOVER_FEE_YN("Y");
    				info.setOVER_FEE(overPay+"");
    				//info.setOVER_MI(String.valueOf(overTime-baseRentTime));
    				info.setOVER_MI(String.valueOf(sysTime-baseRentTime));
        		//}
    		}
    		else
    		{
    		//	if(overTime < 0)
    				
    			logger.debug(" ##### systime_time is baseRentTime {} ,sysTime {}", overTime ,sysTime);
    		}
    		
        	// 반납 프로세스 실행
    		bikeService.parkingInfoDelete(com);
    		bikeService.procReturn(info);
        	
        }
        else
        {
        	//RentHistVo info = bikeService.getForReturnUse(com);
        	logger.error("RentHistVo is NOT EXIST : no_getForReturnUse ");
        	
        	Map<String, String> BeaconInfo = commonService.CheckBeacon_Station(ReqInfo);
        	
        	if(BeaconInfo == null)
        	{
        		responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
        	
        		responseVo.setErrorId(Constants.CODE.get("ERROR_F6"));
        		responseVo = setFaiiDistMsg(responseVo, vo);
        	
        		QRLog.setResAck("RF:00");
        	
        		bikeService.updateQRLog(QRLog);
        	}
        	else
        	{
        		if(BeaconInfo.get("STATION_ID") != null || !BeaconInfo.get("STATION_ID").toString().equals(""))
        		{
        			BeaconInfo.put("BIKE_ID", String.valueOf(vo.getBicycleId()));
            		//RAK_002, RACK_003
            		int RACKCnt = commonService.CheckBeacon_RACK(BeaconInfo);
            		if(RACKCnt == 0)
            		{
            		//	String BIKE_SE_CD = commonService.CheckQRBIKE_Info(BeaconInfo);
            			
            			logger.debug("##### STATION QR_RACK VIRTUAL_RACK CREATED  #####");
            			
            			if(ourBikeMap.get("BIKE_SE_CD").toString().equals("BIK_002"))
            			{
            				commonService.InsertQR_RACK_0(BeaconInfo);
            			}
            			else if(ourBikeMap.get("BIKE_SE_CD").toString().equals("BIK_003"))
            			{
            				commonService.InsertQR_RACK_99(BeaconInfo);
            			}
            		}
            		
            		Map<String, Object> stationInfo;
            		
            		/*
            		if(vo.getElecbicycle().equals("02"))	//새싹 따릉이....
            		{
            			logger.debug("BIKE RETURN ## BIKE IS  SHOOT_BIKE : CHECK_RAK_003_004 {}",vo.getBicycleId());
            			
            			//stationInfo = commonService.CheckBeaconID_Shoot(ReqInfo);
            		}
            		else
            		{
            		*/
            		logger.debug("BIKE RETURN ## BIKE IS  NORMAL_BIKE : TOTAL_RAK {}",vo.getBicycleId());
            		stationInfo = commonService.CheckBeaconID(ReqInfo);
            	//	}
    				
    	     		if(stationInfo != null)
    				{
    	     			com.setStationId(stationInfo.get("STATION_ID").toString());
    	     			com.setRockId(stationInfo.get("RACK_ID").toString());
    	     			info = new RentHistVo();
    	     			info.setRENT_BIKE_ID(vo.getBicycleId());
    	     			info.setRETURN_STATION_ID(com.getStationId());
    	     			info.setRETURN_RACK_ID(com.getRockId());
    	     			info.setCASCADE_YN("N");
    	        		info.setUSE_DIST("0");
    	        		
    	        		 Map<String, Object> parkingMap = commonService.checkParkingInfo(com);
    	        		
    	        		 if(parkingMap == null)	//주기 보고 시 파깈정보 없으면 ...
    	        		 {
    	        			 logger.debug("RETURN : ParkingInfo_insert !!!!!!! {} ",vo.getBicycleId());
    	        			 
    	        			 // 자전거 주차 정보 INSERT PARKING
    	        			 bikeService.insertPeriodParkingInfo(info);
    	        			 
    	        			 // 자전거 정보 UPDATE BIKE
    	        			 bicycleMapper.updateBike(info);
    	        			 
    	        			 
    	        			// 자전거 배치 이력 INSERT LOCATION_BIKE
    	        		
    	        			 //2019.03.20 update 추가 
    	        			 //bicycleMapper.rentBikeLocationUpdate(com);
    	                     // 자전거 배치 이력 INSERT LOCATION_BIKE
    	                     //bicycleMapper.insertBikeLocation(info);
    	                	 //bikeService.insertPeriodBikeLocation(info);
    	        				 
    	        		 }
    				}
        		}
        		
        		responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
        		responseVo.setSeqNum(vo.getSeqNum());
        		responseVo.setCommandId(Constants.CID_RES_RETURNBIKE);
        		responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_02"));
        		responseVo.setBicycleId(vo.getBicycleId());

        		QRLog.setResAck("RSUC");
        		bikeService.updateQRLog(QRLog);
        	}
        	return responseVo;
        }
        
        //app 설치 사용자 한정.
        if(info.getUSR_DEVICE_TYPE() != null) {
       
        	//this.rentSuccessPushMsg(pushVo);
        }
        
        QRLog.setResAck("SUC");
        bikeService.updateQRLog(QRLog);
        
        responseVo.setFrameControl(Constants.SUCC_CMD_CONTROL_FIELD);
        responseVo.setSeqNum(vo.getSeqNum());
        responseVo.setCommandId(Constants.CID_RES_RETURNBIKE);
        responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_02"));
        responseVo.setBicycleId(vo.getBicycleId());
        
        
        return responseVo;
    	
    }
    
    
 // 반납 실패 메세지
    public QRReturnResponseVo setFaiiDistMsg(QRReturnResponseVo responseVo, QRReturnDistResultRequestVo vo )
    {
    	
    	responseVo.setFrameControl(Constants.FAIL_CMD_CONTROL_FIELD);
    	responseVo.setSeqNum(vo.getSeqNum());
    	responseVo.setCommandId(Constants.CID_RES_RETURNBIKE);
    	responseVo.setBicycleId(vo.getBicycleId());
   // 	responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_FF"));
    	
    	return responseVo;
    }
    
}
