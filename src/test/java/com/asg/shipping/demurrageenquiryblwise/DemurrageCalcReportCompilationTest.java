package com.asg.shipping.demurrageenquiryblwise;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "View Demurrage Calculation" print of the Demurrage Enquiry - BL wise document is served from
 * these templates, so a missing or broken one has to break the build instead of the request.
 */
class DemurrageCalcReportCompilationTest {

	private static final String CALC = "jasper/Shipping/SH/LINE_DEMURRAGE_CALC.jrxml";
	private static final String MASTER = "jasper/Shipping/SH/LINE_DEMURRAGE_MASTER.jrxml";
	private static final String DTL = "jasper/Shipping/SH/LINE_DEMURRAGE_DTL.jrxml";

	@ParameterizedTest
	@ValueSource(strings = {CALC, MASTER, DTL})
	void templateCompiles(String path) throws Exception {
		assertNotNull(compile(path));
	}

	@Test
	void calcDeclaresTheParametersThePrintServiceSupplies() throws Exception {
		Set<String> parameters = Arrays.stream(compile(CALC).getParameters())
				.map(JRParameter::getName)
				.collect(Collectors.toSet());

		// Supplied by DemurrageEnquiryBlWiseServiceImpl.printDemurrageCalculation
		assertTrue(parameters.containsAll(Set.of(
				"DOC_ID", "DOC_KEY_POID", "P_TILL_DATE", "P_DISCOUNT",
				"SUB_HEADER", "SUBREPORT_DEMURRAGE_MASTER", "SUBREPORT_DEMURRAGE_DTL")),
				"missing parameters, found: " + parameters);
	}

	private JasperReport compile(String path) throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
			assertNotNull(in, "report template not found on the classpath: " + path);
			return JasperCompileManager.compileReport(in);
		}
	}
}
