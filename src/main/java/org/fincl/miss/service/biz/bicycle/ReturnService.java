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
		 	String BIKE_SE_CD = ourBikeMap.get("BIKE_SE_CD");
		 	com.setCompany_cd("CPN_" + BIKE_SE_CD.substring(4,BIKE_SE_CD.length()));
		// 	com.setBikeStusCd(bike_SE_CD);
		 	
		 	logger.debug("QR_436F ##### => bike {} ,state {} , company {} ,bike_se_cd {}  ",vo.getBicycleId(),vo.getBicycleState(),com.getCompany_cd(),BIKE_SE_CD);
		 	
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
		 		
		 	//	02181D1B 07A69BCD 
		 		vo.setLatitude("02181D1B");
		 		vo.setLongitude("07A69BCD");
		 		
		 	//	QRLog.setXpos(vo.getLatitude());
		 	//	QRLog.setYpos(vo.getLongitude());    
		 		QRLog.setXpos(new CommonUtil().GetGPS(vo.getLatitude()));
		 		QRLog.setYpos(new CommonUtil().GetGPS(vo.getLongitude()));
		 		
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
			
			int battery = Integer.parseInt(vo.getBattery(), 16);
     		String battery_info = null;
     		
     		battery_info = new CommonUtil().getBattery_Info(vo.getBattery());
     		Map<String, String> battery_map = new HashMap<String, String>();
     		battery_map.put("BATTERY", String.valueOf(battery));
     		battery_map.put("BATTERY_INFO", String.valueOf(battery_info));
     		battery_map.put("BICYCLE_ID", vo.getBicycleId());
     		
			commonService.updateBatteryInfo(battery_map);
			
			logger.debug("##### BIKE RETURN ## BATTERY UPDATE END #####");
		}
        
        // 자전거 대여정보 확인
        // 자전거 STATION 정보 죄회 수정 필요 BEACON ID로 
        RentHistVo info = bikeService.getForReturnUse(com);
        int overPay = 0;
        
        
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
					stationInfo = commonService.CheckStation_ForGPS(GPS);
					
					if(stationInfo == null )
					{
						 logger.error("Station List Find Error");
						 responseVo.setErrorId(Constants.CODE.get("ERROR_FD"));
						 responseVo = setFaiiMsg(responseVo, vo);
				  		
						 return responseVo;
					 }
					 else
					 {
						 logger.debug("##### BIKE RETURN ## GPS STATION FIND START! #####");
						 logger.debug("##### BIKE RETURN ## GPS STATION ID[" + String.valueOf(stationInfo.get("STATION_ID")) + "] ENTERED");;
							 
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

        	int sysTime = Integer.parseInt(info.getUSE_MI().toString());
        	
        	info.setRETURN_STATION_ID(com.getStationId());
        	info.setTRANSFER_YN("N");
        	info.setOVER_FEE_YN("N");
        	
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
        	
        	logger.debug(" #####  server_time is baseRentTime {}  sysTime {}"  , baseRentTime, sysTime);
    		
    		
			Map<String, Object> fee = new HashMap<String, Object>();
			
			fee.put("ADD_FEE_CLS", info.getPAYMENT_CLS_CD());
			fee.put("BIKE_SE_CD", info.getBIKE_SE_CD());
			
			Map<String, Object> minPolicy = bikeService.getOverFeeMinPolicy(fee);	//TB_SVC_ADD_FEE  
			//Map<String, Object> maxPolicy = bikeService.getOverFeeMaxPolicy(fee);
			baseRentTime = Integer.parseInt(minPolicy.get("OVER_STR_MI").toString());
			
			if(sysTime - baseRentTime > 0)
			{
				info.setOVER_FEE_YN("Y");		
				overPay = new CommonUtil().getPay(minPolicy, (sysTime- baseRentTime));
				//overPay = new CommonUtil().getPay(minPolicy, maxPolicy, (sysTime - baseRentTime));
				info.setOVER_FEE(overPay+"");
				baseRentTime = Integer.parseInt(minPolicy.get("OVER_STR_MI").toString());
				
				
				
				
				info.setOVER_MI(String.valueOf(sysTime - baseRentTime));
			}
			// 반납 프로세스 실행
			bikeService.parkingInfoDelete(com);
			bikeService.procReturn(info);
        	
		}
        else
        {
        	logger.error("RentHistVo is NOT EXIST : no_getForReturnUse ");
	        	
        	
    		responseVo.setBicycleState(Constants.CODE.get("BIKE_STATE_03"));
    	
    		responseVo.setErrorId(Constants.CODE.get("ERROR_F6"));
    		responseVo = setFaiiMsg(responseVo, vo);
    	
    		QRLog.setResAck("RF:00");
    	
    		bikeService.updateQRLog(QRLog);
        	
        	return responseVo;
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
    
}
