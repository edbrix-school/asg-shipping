package com.asg.shipping.shippingofoqv2.repository;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.shippingofoqv2.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Repository
public class ShippingOFOQProcRepositoryImpl implements ShippingOFOQProcRepository {

    /** Legacy passes the arrival date to the procedure as a dd-MMM-yyyy string (common.getDateSql). */
    private static final DateTimeFormatter ORACLE_DATE =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OFOQLoadItemDetailsResponse loadOFOQDetails(LoadOFOQDetailsRequest request) {

        if (request.getVoyageNo() == null || request.getVoyageNo().isBlank()) {
            throw new ValidationException("Voyage number is mandatory. Please insert the same.");
        }
        if (request.getVesselPoid() == null) {
            throw new ValidationException("Vessel details is mandatory. Please insert the same.");
        }
        if (request.getArrivalDate() == null) {
            throw new ValidationException("Arrival date is mandatory. Please insert the same.");
        }

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("PROC_LOAD_OFOQ_API_DATA");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_VESSEL_POID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_ARRIVAL_DATE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_ARRIVAL_DATE_TO", String.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("P_MANIFEST_OUTDATA", void.class, ParameterMode.REF_CURSOR);
        query.registerStoredProcedureParameter("P_BL_OUTDATA", void.class, ParameterMode.REF_CURSOR);
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);

        query.setParameter("P_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", UserContext.getDocumentId());
        query.setParameter("P_DOC_KEY_POID", request.getTransactionPoid());
        query.setParameter("P_VESSEL_POID", String.valueOf(request.getVesselPoid()));
        query.setParameter("P_ARRIVAL_DATE", request.getArrivalDate().format(ORACLE_DATE));
        // Legacy always passes NULL here - data is loaded on a single arrival date.
        query.setParameter("P_ARRIVAL_DATE_TO", null);

        query.execute();

        String status = (String) query.getOutputParameterValue("P_STATUS");
        if (isProcedureError(status)) {
            log.error("Error from PROC_LOAD_OFOQ_API_DATA: {}", status);
            throw new ValidationException("Some error occured while loading the vessel details. " + status);
        }

        List<OFOQItemDtlDto> lineDetails = new ArrayList<>();
        ResultSet manifestRs = (ResultSet) query.getOutputParameterValue("P_MANIFEST_OUTDATA");
        if (manifestRs != null) {
            try {
                Set<String> columns = columnLabels(manifestRs);
                while (manifestRs.next()) {
                    lineDetails.add(OFOQItemDtlDto.builder()
                            .drillDownLinkInfo(getString(manifestRs, columns, "DRILLDOWN_LINK_INFO"))
                            .vesselVoyagePoid(getLong(manifestRs, columns, "VESSEL_VOYAGE_POID"))
                            .lineName(getString(manifestRs, columns, "LINE_NAME"))
                            .linePoid(getLong(manifestRs, columns, "LINE_POID"))
                            .vesselName(getString(manifestRs, columns, "VESSEL_NAME"))
                            .voyageNo(getString(manifestRs, columns, "VOYAGE_NO"))
                            .jobNo(getString(manifestRs, columns, "JOB_NO"))
                            .arrivalDate(getLocalDate(manifestRs, columns, "ARRIVAL_DATE"))
                            .sailDate(getLocalDate(manifestRs, columns, "SAIL_DATE"))
                            .build());
                }
            } catch (SQLException e) {
                log.error("Error reading manifest cursor", e);
                throw new ValidationException("Error loading manifest details: " + e.getMessage());
            }
        }

        List<OFOQBlDtlDto> blDetails = new ArrayList<>();
        ResultSet blRs = (ResultSet) query.getOutputParameterValue("P_BL_OUTDATA");
        if (blRs != null) {
            try {
                Set<String> columns = columnLabels(blRs);
                while (blRs.next()) {
                    blDetails.add(OFOQBlDtlDto.builder()
                            .drillDownLinkInfo(getString(blRs, columns, "DRILLDOWN_LINK_INFO"))
                            .companyPoid(getLong(blRs, columns, "COMPANY_POID"))
                            .manifestPoid(getLong(blRs, columns, "MANIFEST_POID"))
                            .manifestDocRef(getString(blRs, columns, "MANIFEST_DOC_REF"))
                            .blNumber(getString(blRs, columns, "BL_NUMBER"))
                            .build());
                }
            } catch (SQLException e) {
                log.error("Error reading BL cursor", e);
                throw new ValidationException("Error loading BL details: " + e.getMessage());
            }
        }

        if (lineDetails.isEmpty() && blDetails.isEmpty()) {
            log.warn("No OFOQ details found for vesselPoid: {} arrivalDate: {}",
                    request.getVesselPoid(), request.getArrivalDate());
        }

        return new OFOQLoadItemDetailsResponse(lineDetails, blDetails);
    }


    @Override
    public List<OFOQManifestXmlDto> loadOFOQManifestXml(Long transactionPoid, String blNumber, String manifestType, String docRef) {
        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("PROC_LOAD_OFOQ_API_MANIFEST_XML");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_VESSEL_VOYAGE_POID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_MANIFEST_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BL_NUMBER", String.class, ParameterMode.IN);


        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_XMLDATA_OUTDATA", void.class, ParameterMode.REF_CURSOR);


        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", UserContext.getDocumentId());
        query.setParameter("P_DOC_KEY_POID", transactionPoid);
        query.setParameter("P_DOC_REF", docRef);
        // Legacy passes an empty vessel voyage poid so the procedure picks up every selected line
        // of the document. Passing the vessel master poid here filters the manifest down to nothing.
        query.setParameter("P_VESSEL_VOYAGE_POID", "");
        query.setParameter("P_MANIFEST_TYPE", manifestType);
        query.setParameter("P_BL_NUMBER", blNumber);

        query.execute();


        String status = (String) query.getOutputParameterValue("P_STATUS");

        if (isProcedureError(status)) {
            log.error("Error from PROC_LOAD_OFOQ_API_MANIFEST_XML: {}", status);
            throw new ValidationException("Some error happened when fetching the XML data: " + status);
        }


        ResultSet rs = (ResultSet) query.getOutputParameterValue("P_XMLDATA_OUTDATA");


        List<OFOQManifestXmlDto> result = new ArrayList<>();

        if (rs == null) {
            return result;
        }

        try {
            while (rs.next()) {
                result.add(
                        OFOQManifestXmlDto.builder()
                                .xmlData(rs.getString("XML_DATA"))
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Error reading XML cursor", e);
            throw new ValidationException("Error loading OFOQ manifest XML: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String saveOFOQApiResponse(
            Long transactionPoid,
            String docRef,
            String manifestType,
            int responseCode,
            String httpStatusText,
            String extractedMessage
    ) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("PROC_SAVE_OFOQ_API_RESPONSE");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_MANIFEST_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESPONSE_CODE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESPONSE_MSG", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESPONSE", String.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_XMLDATA_OUTDATA", void.class, ParameterMode.REF_CURSOR);

        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", UserContext.getDocumentId());
        query.setParameter("P_DOC_KEY_POID", transactionPoid);
        query.setParameter("P_DOC_REF", docRef);
        query.setParameter("P_MANIFEST_TYPE", manifestType);
        query.setParameter("P_RESPONSE_CODE", String.valueOf(responseCode));
        query.setParameter("P_RESPONSE_MSG", httpStatusText);
        query.setParameter("P_RESPONSE", extractedMessage);

        query.execute();

        String status = (String) query.getOutputParameterValue("P_STATUS");
        if (isProcedureError(status)) {
            log.error("Error from PROC_SAVE_OFOQ_API_RESPONSE: {}", status);
        }

        String functionalRefId = null;
        ResultSet rs = (ResultSet) query.getOutputParameterValue("P_XMLDATA_OUTDATA");

        try {
            if (rs != null && rs.next()) {
                functionalRefId = rs.getString("FUNCTIONAL_REF_ID");
                log.info("OFOQ API response saved. Functional Ref ID: {}", functionalRefId);
            }
        } catch (Exception e) {
            log.error("Error reading PROC_SAVE_OFOQ_API_RESPONSE cursor", e);
            throw new ValidationException(
                    "Error reading OFOQ API response cursor: " + e.getMessage()
            );
        }

        if (functionalRefId == null) {
            log.warn("No functional reference is available for transactionPoid: {}", transactionPoid);
        }

        return functionalRefId;
    }


    @Override
    public String saveOFOQManifestResponse(
            Long transactionPoid,
            String docRef,
            String functionalRefId,
            String responseCode,
            String httpStatusText,
            String xmlResponse,
            String manifestType,
            String blNumber
    ) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "PROC_SAVE_OFOQ_API_MANIFEST_RESPONSE"
                );

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_FUNCTIONAL_REF_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESPONSE_CODE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESPONSE_MSG", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_XML_RESPONSE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_MANIFEST_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BL_NUMBER", String.class, ParameterMode.IN);


        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("P_XMLDATA_OUTDATA", void.class, ParameterMode.REF_CURSOR);


        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", UserContext.getDocumentId());
        query.setParameter("P_DOC_KEY_POID", transactionPoid);
        query.setParameter("P_DOC_REF", docRef);
        query.setParameter("P_FUNCTIONAL_REF_ID", functionalRefId);
        query.setParameter("P_RESPONSE_CODE", responseCode);
        query.setParameter("P_RESPONSE_MSG", httpStatusText);
        query.setParameter("P_XML_RESPONSE", xmlResponse);
        query.setParameter("P_MANIFEST_TYPE", manifestType);
        query.setParameter("P_BL_NUMBER", blNumber);


        query.execute();


        String status = (String) query.getOutputParameterValue("P_STATUS");

        // The manifest has already been accepted by OFOQ at this point - a failure to persist the
        // response must not roll back the submission, so mirror the legacy bean and only report it.
        if (isProcedureError(status)) {
            log.error("Error from PROC_SAVE_OFOQ_API_MANIFEST_RESPONSE: {}", status);
        }


        ResultSet rs = (ResultSet) query.getOutputParameterValue("P_XMLDATA_OUTDATA");

        if (rs != null) {
            try {
                while (rs.next()) {
                    log.info(
                            "OFOQ Manifest response saved successfully. Message: {}",
                            rs.getString("PROVISIONAL_RESPONSE")
                    );
                }
            } catch (Exception e) {
                log.error("Error reading PROC_SAVE_OFOQ_API_MANIFEST_RESPONSE cursor", e);
            }
        }
        return status;
    }

    /** Legacy treats a status as failed only when it contains "ERROR". */
    private boolean isProcedureError(String status) {
        return status != null && status.toUpperCase(Locale.ENGLISH).contains("ERROR");
    }

    private Set<String> columnLabels(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        Set<String> labels = new HashSet<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            labels.add(metaData.getColumnLabel(i).toUpperCase(Locale.ENGLISH));
        }
        return labels;
    }

    private String getString(ResultSet rs, Set<String> columns, String column) throws SQLException {
        return columns.contains(column) ? rs.getString(column) : null;
    }

    private Long getLong(ResultSet rs, Set<String> columns, String column) throws SQLException {
        if (!columns.contains(column)) {
            return null;
        }
        Object value = rs.getObject(column);
        return value instanceof Number number ? number.longValue() : null;
    }

    private LocalDate getLocalDate(ResultSet rs, Set<String> columns, String column) throws SQLException {
        if (!columns.contains(column)) {
            return null;
        }
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime().toLocalDate();
    }

}
