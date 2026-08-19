package com.asg.shipping.demurrageenquiryblwise.repository;

import com.asg.shipping.demurrageenquiryblwise.dto.ContainerDemurrageCalcDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageChargeConfigDto;
import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageEnquiryContainerDto;
import com.asg.shipping.demurrageenquiryblwise.dto.ManifestChargeRowDto;
import com.asg.shipping.demurrageenquiryblwise.dto.PortChargeRowDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
	 * Recalculates the demurrage / detention of one container up to {@code toDate} through
	 * {@code PROC_SH_DEM_DTTN_CONTAINER} - the same procedure the sales invoice bills from, so both
	 * documents agree on the period and the amount.
	 *
	 * @param freeDays free days to apply instead of the ones of the container / line tariff.
	 *                 {@code null} or {@code 0} keeps the free days the tariff resolves.
	 */
	ContainerDemurrageCalcDto calculateContainerDemurrage(Long blPoid, String containerNo, LocalDate toDate,
														  Integer freeDays);

	/**
	 * Slab breakdown of the demurrage of every container of the BL, keyed by container number - what
	 * {@code FUNC_RTN_DEM_DETTN_FULL_TEXT} returns for the amount of that container, so the screen can
	 * show the text the print shows in its Remarks column.
	 *
	 * @param freeDays free days applied instead of the ones of the container / line tariff,
	 *                 {@code null} or {@code 0} keeps the free days the tariff resolves
	 */
	Map<String, String> findContainerDemurrageRemarks(Long blPoid, LocalDate toDate, Integer freeDays);

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
