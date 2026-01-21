package com.asg.shipping.receipts.service.impl;

import com.asg.shipping.dayCloseShiping.repository.ArShDayEndCloseHdrRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionDateService {

	private final ArShDayEndCloseHdrRepository dayEndCloseRepository;


	public LocalDate calculateTransactionDate() {
		log.info("Calculating transaction date");

		// Get last day close date (max transaction date where deleted = 'N')
		LocalDate lastDayCloseDate = dayEndCloseRepository.findAll().stream()
				.filter(record -> record.getDeleted() == null || record.getDeleted().equals("N"))
				.map(record -> record.getTransactionDate())
				.max(LocalDate::compareTo)
				.orElse(LocalDate.now().minusDays(1));

		LocalDate candidateDate = lastDayCloseDate.plusDays(1);
		LocalDate today = LocalDate.now();

		// Ensure date is not less than today
		if (candidateDate.isBefore(today)) {
			candidateDate = today;
		}

		// Skip Friday (5) and Saturday (6)
		while (candidateDate.getDayOfWeek() == DayOfWeek.FRIDAY || 
		       candidateDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
			candidateDate = candidateDate.plusDays(1);
		}

		log.info("Calculated transaction date: {}", candidateDate);
		return candidateDate;
	}
}
