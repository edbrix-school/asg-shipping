package com.asg.shipping.demurrageenquiryblwise.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Writes an amount with the 3 decimals the Demurrage Enquiry screen shows, whatever scale it was
 * built with - Oracle hands back {@code 56} for a NUMBER column and the enquiry has to render
 * {@code 56.000}, as the legacy grid did through its amount columns.
 *
 * <p>Applies to amounts only. Percentages keep their own scale, matching the legacy
 * {@code AmtColumns} which listed the amount columns and left the percentages out.
 */
public class AmountSerializer extends JsonSerializer<BigDecimal> {

	public static final int SCALE = 3;

	@Override
	public void serialize(BigDecimal value, JsonGenerator generator, SerializerProvider serializers)
			throws IOException {
		// Jackson never routes nulls here, they are written by the null serializer.
		generator.writeNumber(value.setScale(SCALE, RoundingMode.HALF_UP));
	}
}
