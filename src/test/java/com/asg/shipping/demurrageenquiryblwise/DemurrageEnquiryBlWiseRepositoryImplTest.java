package com.asg.shipping.demurrageenquiryblwise;

import com.asg.shipping.demurrageenquiryblwise.dto.DemurrageChargeConfigDto;
import com.asg.shipping.demurrageenquiryblwise.repository.impl.DemurrageEnquiryBlWiseRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemurrageEnquiryBlWiseRepositoryImplTest {

	@Mock
	private EntityManager entityManager;
	@Mock
	private Query query;

	private DemurrageEnquiryBlWiseRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		repository = new DemurrageEnquiryBlWiseRepositoryImpl(entityManager);
		when(entityManager.createNativeQuery(anyString())).thenReturn(query);
		when(query.setParameter(anyString(), any())).thenReturn(query);
	}

	/**
	 * {@code GLOBAL_PARAMETERS.PARAMETER_VALUE} is a VARCHAR2, so the charge POID arrives as a String.
	 * Treating it as a number only would return a config with no charge, and the enquiry would drop
	 * its demurrage charge line while still reporting the demurrage amount.
	 */
	@Test
	void findDemurrageChargeConfig_readsTheChargePoidFromTheVarcharParameter() {
		when(query.getResultList()).thenReturn(List.<Object[]>of(
				new Object[]{"94", BigDecimal.valueOf(3), BigDecimal.ZERO, "Y"}));

		DemurrageChargeConfigDto config = repository.findDemurrageChargeConfig(1L);

		assertEquals(94L, config.getChargePoid());
		assertEquals(3L, config.getTaxPoid());
		assertEquals(0, BigDecimal.ZERO.compareTo(config.getTaxPercentage()));
		assertEquals("Y", config.getTaxApplicable());
	}

	@Test
	void findDemurrageChargeConfig_acceptsANumericParameterValueToo() {
		when(query.getResultList()).thenReturn(List.<Object[]>of(
				new Object[]{BigDecimal.valueOf(94), null, null, "N"}));

		assertEquals(94L, repository.findDemurrageChargeConfig(1L).getChargePoid());
	}

	@Test
	void findDemurrageChargeConfig_leavesTheChargeUnsetWhenTheParameterIsNotAPoid() {
		when(query.getResultList()).thenReturn(List.<Object[]>of(
				new Object[]{"not-a-poid", null, null, "N"}));

		assertNull(repository.findDemurrageChargeConfig(1L).getChargePoid());
	}

	@Test
	void findDemurrageChargeConfig_returnsNullWhenTheParameterIsNotConfigured() {
		when(query.getResultList()).thenReturn(List.of());

		assertNull(repository.findDemurrageChargeConfig(1L));
	}
}
