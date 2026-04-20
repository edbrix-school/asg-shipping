package com.asg.shipping.bookingFormSH.service;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormCreateDTO;
import com.asg.shipping.bookingFormSH.dto.BookingFormDto;
import com.asg.shipping.bookingFormSH.dto.BookingFormUpdateDTO;

/**
 * Service interface for Booking Form operations
 */
public interface BookingFormService {

	/**
	 * Search Booking Form records
	 */
	Map<String, Object> searchBookingForm(String docId, FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate);

	/**
	 * Get Booking Form by ID
	 */
	BookingFormDto getBookingForm(Long id);

	/**
	 * Create new Booking Form
	 */
	BookingFormDto createBookingForm(BookingFormCreateDTO dto);

	/**
	 * Update existing Booking Form
	 */
	void updateBookingForm(Long id, BookingFormUpdateDTO dto);

	/**
	 * Delete Booking Form (soft delete)
	 */
	void deleteBookingForm(Long id);

	/**
	 * Generate COPRAR booking file
	 */
	String generateCoprarBooking(Long transactionPoid);

	/**
	 * Get empty shipper POID
	 */
	String getEmptyShipper(Long companyPoid);



	/**
	 * Process empty container load
	 */
	String processEmptyContainerLoad(Long transactionPoid);
	
	byte[] mateBookingPrintForm(Long transactionPoid) throws Exception;
	
	byte[] cntEmptyBookingPrintForm(Long transactionPoid) throws Exception;
	
	byte[] cntReturnBookingPrintFormAll(Long transactionPoid, String printStamp) throws Exception;
}
