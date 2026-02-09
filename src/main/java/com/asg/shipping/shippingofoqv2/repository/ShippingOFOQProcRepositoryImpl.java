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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class ShippingOFOQProcRepositoryImpl implements ShippingOFOQProcRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OFOQLoadItemDetailsResponse loadOFOQDetails(LoadOFOQDetailsRequest request) {

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
        query.setParameter("P_VESSEL_POID", request.getVesselPoid().toString());
        query.setParameter("P_ARRIVAL_DATE", request.getArrivalDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
        query.setParameter("P_ARRIVAL_DATE_TO", null);

        query.execute();

        String status = (String) query.getOutputParameterValue("P_STATUS");
        if (!"SUCCESS".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Failed to load OFOQ details : " + status);
        }

        ResultSet manifestRs = (ResultSet) query.getOutputParameterValue("P_MANIFEST_OUTDATA");
        List<OFOQItemDtlDto> lineDetails = new ArrayList<>();
        try {
            while (manifestRs.next()) {
                lineDetails.add(OFOQItemDtlDto.builder()
                        .drillDownLinkInfo(manifestRs.getString(1))
                        .vesselVoyagePoid(manifestRs.getLong(3))
                        .lineName(manifestRs.getString(4))
                        .linePoid(manifestRs.getLong(5))
                        .vesselName(manifestRs.getString(6))
                        .voyageNo(manifestRs.getString(7))
                        .jobNo(manifestRs.getString(8))
                        .arrivalDate(
                                manifestRs.getTimestamp(9) != null
                                        ? manifestRs.getTimestamp(9).toLocalDateTime().toLocalDate()
                                        : null
                        )
                        .sailDate(
                                manifestRs.getTimestamp(10) != null
                                        ? manifestRs.getTimestamp(10).toLocalDateTime().toLocalDate()
                                        : null
                        )

                        .build());
            }
        } catch (Exception e) {
            log.error("Error reading manifest cursor", e);
            throw new ValidationException("Error loading manifest details: " + e.getMessage());
        }

        ResultSet blRs = (ResultSet) query.getOutputParameterValue("P_BL_OUTDATA");
        List<OFOQBlDtlDto> blDetails = new ArrayList<>();
        try {
            while (blRs.next()) {
                blDetails.add(OFOQBlDtlDto.builder()
                        .drillDownLinkInfo(blRs.getString(1))
                        .companyPoid(blRs.getLong(2))
                        .manifestPoid(blRs.getLong(3))
                        .manifestDocRef(blRs.getString(4))
                        .blNumber(blRs.getString(5))
                        .build());
            }
        } catch (Exception e) {
            log.error("Error reading BL cursor", e);
            throw new ValidationException("Error loading BL details: " + e.getMessage());
        }

        return new OFOQLoadItemDetailsResponse(lineDetails, blDetails);
    }


    @Override
    public List<OFOQManifestXmlDto> loadOFOQManifestXml(Long transactionPoid,String blNumber,String manifestType,String docRef,Long vesselPoid) {
        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("PROC_LOAD_OFOQ_API_MANIFEST_XML");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_REF", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_VESSEL_VOYAGE_POID", Long.class, ParameterMode.IN);
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
        query.setParameter("P_VESSEL_VOYAGE_POID", vesselPoid);
        query.setParameter("P_MANIFEST_TYPE", manifestType);
        query.setParameter("P_BL_NUMBER", blNumber);

        query.execute();


        String status = (String) query.getOutputParameterValue("P_STATUS");

        if (status != null && status.startsWith("ERROR")) {
            log.error("Error from PROC_LOAD_OFOQ_API_MANIFEST_XML: {}", status);
            throw new ValidationException(status);
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
            String responseMessage,
            String xmlResponse
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
        query.setParameter("P_RESPONSE_MSG", responseMessage);
        query.setParameter("P_RESPONSE", xmlResponse);

        query.execute();

        String status = (String) query.getOutputParameterValue("P_STATUS");
        if (status != null && status.startsWith("ERROR")) {
            log.warn("Warning from PROC_SAVE_OFOQ_API_RESPONSE: {}", status);
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

        return functionalRefId;
    }


    @Override
    public String saveOFOQManifestResponse(
            Long transactionPoid,
            String docRef,
            String functionalRefId,
            String responseCode,
            String responseMessage,
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
        query.setParameter("P_RESPONSE_CODE", String.valueOf(responseCode));
        query.setParameter("P_RESPONSE_MSG", responseMessage);
        query.setParameter("P_XML_RESPONSE", xmlResponse);
        query.setParameter("P_MANIFEST_TYPE", manifestType);
        query.setParameter("P_BL_NUMBER", blNumber);


        query.execute();


        String status = (String) query.getOutputParameterValue("P_STATUS");

        if (status != null && status.startsWith("ERROR")) {
            log.error("Error from PROC_SAVE_OFOQ_API_MANIFEST_RESPONSE: {}", status);
            throw new ValidationException(status);
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
                throw new ValidationException(
                        "Error reading OFOQ Manifest response cursor: " + e.getMessage()
                );
            }
        }
        return status;
    }

    @Override
    public String getParameterValue(String parameterName) {
        try {
            String query = "SELECT PARAMETER_VALUE FROM GLOBAL_PARAMETERS WHERE PARAMETER_NAME = ? AND DELETED = 'N'";
            return (String) entityManager.createNativeQuery(query)
                    .setParameter(1, parameterName)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to load parameter {}: {}", parameterName, e.getMessage());
            return null;
        }
    }

}
