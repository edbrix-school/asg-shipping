package com.asg.shipping.demurrageenquiryblwise.repository;

import com.asg.shipping.demurrageenquiryblwise.dto.ContainerDemurrageCalcDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageChargeConfigDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryContainerDto;
import com.asg.shipping.demurrageenquiryblwise.dto.ManifestChargeRowDto;
import com.asg.shipping.demurrageenquiryblwise.dto.PortChargeRowDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Read only queries backing the Demurrage Enquiry - BL wise screen (100-144). Nothing is persisted
 * by this document, every figure is derived from the BL, the line tariff and the charge masters.
 */
public interface DemurrageEnquiryBlWiseRepository {

	boolean blExists(Long blPoid);

	/**
	 * Containers of the BL together with the demurrage period defaults, the empty-in move date and
	 * the delivery order printing status.
	 */
	List<DemurrageEnquiryContainerDto> loadContainers(Long blPoid);

	/**
	 * Delivery order status of the BL - DOISSUED when the DO has already been printed.
	 */
	String findDoStatus(Long blPoid);

	/**
	 * Recalculates the demurrage of one container up to {@code toDate}.
	 */
	ContainerDemurrageCalcDto calculateContainerDemurrage(Long blPoid, String containerNo, LocalDate toDate);

	/**
	 * Charges of the BL manifest that have not been received or invoiced yet.
	 */
	List<ManifestChargeRowDto> findManifestCharges(Long blPoid);

	/**
	 * Demurrage charge POID (SHDEMURRAGE global parameter) and its applicable tax.
	 */
	DemurrageChargeConfigDto findDemurrageChargeConfig(Long companyPoid);

	/**
	 * Late collection and revalidation charges applicable on the BL.
	 */
	List<PortChargeRowDto> findPortCharges(Long blPoid, Long companyPoid);

	/**
	 * Container size (20 / 40 / ...) of an ISO type.
	 */
	String getContainerSize(String equipmentIsoType);
}
