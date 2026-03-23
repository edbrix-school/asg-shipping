package com.asg.shipping.mafitrailerdateupdateform.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.asg.shipping.mafitrailerdateupdateform.dto.BlDetailsProjection;
import com.asg.shipping.mafitrailerdateupdateform.dto.BlDetailsProjectionImpl;
import com.asg.shipping.mafitrailerdateupdateform.dto.VoyageProjection;
import com.asg.shipping.mafitrailerdateupdateform.dto.VoyageProjectionImpl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ShipReadOnlyRepository {
	private final EntityManager entityManager;

	@Transactional(readOnly = true)
	public Optional<VoyageProjection> findVoyageDetailsById(Long transactionPoid) {

		String sql = """
				    SELECT
				        svh.VOYAGE_NO     AS voyageNo,
				        svh.JOB_NO        AS jobNo,
				        slm.LINE_POID     AS linePoid,
				        slm.LINE_CODE     AS lineCode,
				        slm.LINE_NAME     AS lineName,
				        svm.VESSEL_POID as vesselPoid,
				   		svm.VESSEL_CODE as vesselCode,
				   		svm.VESSEL_NAME as vesselName
				    FROM SHIP_VOYAGE_HDR svh
				    LEFT JOIN SHIP_LINE_MASTER slm
				           ON slm.LINE_POID = svh.LINE_POID
				    LEFT JOIN SHIP_VESSEL_MASTER svm
				           ON svm.VESSEL_POID = svh.VESSEL_POID
				    WHERE svh.TRANSACTION_POID = :transactionPoid
				      AND (svh.DELETED IS NULL OR svh.DELETED = 'N')
				""";

		@SuppressWarnings("unchecked")
		List<Object[]> result = entityManager.createNativeQuery(sql).setParameter("transactionPoid", transactionPoid)
				.getResultList();

		if (result.isEmpty()) {
			return Optional.empty();
		}

		Object[] row = result.get(0);

		return Optional.of(new VoyageProjectionImpl((String) row[0], (String) row[1],
				row[2] != null ? ((Number) row[2]).longValue() : null, (String) row[3], (String) row[4],
				row[5] != null ? ((Number) row[5]).longValue() : null, (String) row[6], (String) row[7]));
	}

	public Optional<BlDetailsProjection> findBlDetailsForMafi(Long blPoid) {

		String sql = """
				    SELECT
				        b.TRANSACTION_POID   AS blPoid,
				        b.BL_NUMBER         AS blNumber
				    FROM SHIP_BL_MANIFEST_HDR b
				    WHERE b.TRANSACTION_POID = :blPoid
				      AND (b.DELETED IS NULL OR b.DELETED = 'N')
				""";

		@SuppressWarnings("unchecked")
		List<Object[]> result = entityManager.createNativeQuery(sql).setParameter("blPoid", blPoid).getResultList();

		if (result.isEmpty()) {
			return Optional.empty();
		}

		Object[] row = result.get(0);

		return Optional.of(new BlDetailsProjectionImpl(((Number) row[0]).longValue(), (String) row[1], (String) row[2],
				(BigDecimal) row[3], (BigDecimal) row[4], ((Number) row[5]).longValue()));
	}

}
