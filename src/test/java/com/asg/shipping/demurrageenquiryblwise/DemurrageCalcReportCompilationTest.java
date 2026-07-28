package com.asg.shipping.demurrageenquiryblwise;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	/**
	 * `isBold="true"` on its own is dropped by the PDF exporter - it falls back to plain Helvetica and
	 * the headings, labels and totals come out in normal weight, unlike the legacy print. Only an
	 * explicit bold `pdfFontName` survives the export.
	 */
	@ParameterizedTest
	@ValueSource(strings = {CALC, MASTER, DTL})
	void everyBoldFontNamesABoldPdfFont(String path) throws Exception {
		String xml = new String(getClass().getClassLoader().getResourceAsStream(path).readAllBytes(),
				StandardCharsets.UTF_8);

		Matcher fonts = Pattern.compile("<font[^>]*/>").matcher(xml);
		List<String> unstyled = new ArrayList<>();
		while (fonts.find()) {
			String font = fonts.group();
			if (font.contains("isBold=\"true\"") && !font.contains("pdfFontName")) {
				unstyled.add(font);
			}
		}

		assertTrue(unstyled.isEmpty(), "bold fonts without a bold pdfFontName in " + path + ": " + unstyled);
	}

	/**
	 * A text element shorter than its own line height is silently emptied by Jasper instead of
	 * overflowing - the "Demurrage Calculation" heading disappeared that way at 14pt in a 19px box.
	 */
	@Test
	void headingsAreTallEnoughToRender() throws Exception {
		JasperPrint print = fillDtl();

		List<String> printed = print.getPages().stream()
				.flatMap(page -> page.getElements().stream())
				.filter(JRPrintText.class::isInstance)
				.map(element -> String.valueOf(((JRPrintText) element).getFullText()).trim())
				.toList();

		assertTrue(printed.contains("Demurrage Calculation"),
				"the Demurrage Calculation heading was clipped away, printed texts: " + printed);
		assertTrue(printed.contains("CONTAINER"), "the column headers are missing: " + printed);
	}

	@Test
	void boldTextIsExportedWithABoldPdfFont() throws Exception {
		JasperPrint print = fillDtl();

		String pdf = new String(JasperExportManager.exportReportToPdf(print), StandardCharsets.ISO_8859_1);

		assertTrue(Pattern.compile("/BaseFont\\s*/Helvetica-Bold").matcher(pdf).find(),
				"the column headers of LINE_DEMURRAGE_DTL are not exported in bold");
	}

	/** Fills LINE_DEMURRAGE_DTL off a dummy row so the layout can be checked without a database. */
	private JasperPrint fillDtl() throws Exception {
		return JasperFillManager.fillReport(
				compile(DTL),
				new HashMap<>(Map.of("P_TILL_DATE", "2025-07-28", "P_DISCOUNT", "0", "DOC_KEY_POID", "1")),
				new JREmptyDataSource(1));
	}

	private JasperReport compile(String path) throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
			assertNotNull(in, "report template not found on the classpath: " + path);
			return JasperCompileManager.compileReport(in);
		}
	}
}
