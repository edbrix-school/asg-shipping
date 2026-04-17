package com.asg.shipping.bookingFormSH.service;

import java.util.List;

import com.asg.shipping.common.dto.LovItem;

public interface BookingFormLovService {

	List<LovItem> getLineMasterLov(Long linePoid);
	
	
	List<LovItem> getVesselMasterLov(Long linePoid);
	
	List<LovItem> getQuotaionLov(Long poid);
	
	List<LovItem> getSalesmanLov(Long poid);
	
	List<LovItem> getCommodityMasterLov(Long poid);
	
	List<LovItem> getPortMasterLov(Long poid);
	
	List<LovItem> getVoyageMasterLov(Long poid);
	
	List<LovItem> getChargeMasterLov(Long poid);
}
