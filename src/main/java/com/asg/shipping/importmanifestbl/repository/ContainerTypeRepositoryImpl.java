package com.asg.shipping.importmanifestbl.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ContainerTypeRepositoryImpl implements ContainerDropdownRepository {


    private final EntityManager entityManager;

    @Override
    public List<Object[]> findContainerTypes(Long voyageTranPoid) {

        String sql = """
            SELECT
                ct.GROUP_POID,
                ct.CONTAINER_TYPE_POID,
                ct.CONTAINER_TYPE_CODE,
                ct.CONTAINER_TYPE_NAME,
                ct.CONTAINER_GRP_POID,
                ct.CONTAINER_TYPE_SIZE,
                ct.CONTAINER_TYPE_ISO_NAME,
                ct.CONTAINER_CARGO_WEIGHT,
                ct.CONTAINER_TARE_WEIGHT,
                ct.CONTAINER_TEU_FACTOR,
                ct.CONTAINER_TYPE_CATEGORY,
                ct.ACTIVE,
                ct.SEQNO,
                ct.CONTAINER_APMT_TYPE_CODE
            FROM SHIP_CONTAINER_TYPE_MASTER ct
            WHERE (
                ct.CONTAINER_TYPE_CODE IN (
                    SELECT GET_CONTAINER_POID_CODE(dtl.CONTAINER_TYPE_POID)
                    FROM SHIP_LINE_MASTER_TYPE_DTL dtl
                    WHERE dtl.line_poid IN (
                        SELECT vh.line_poid
                        FROM SHIP_VOYAGE_HDR vh
                        WHERE vh.transaction_poid = :voyageTranPoid
                    )
                )
                OR :voyageTranPoid IS NULL
            )
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("voyageTranPoid", voyageTranPoid);

        return query.getResultList();
    }

    @Override
    public List<Object[]> findAllCommodities() {

        String sql = """
        
                SELECT
            scm.GROUP_POID,
            scm.COMODITY_POID,
            scm.COMODITY_CODE,
            scm.COMODITY_NAME,
            scm.COMODITY_NAME2,
            scm.ACTIVE,
            scm.SEQNO,
            scm.CREATED_BY,
            scm.CREATED_DATE,
            scm.LASTMODIFIED_BY,
            scm.LASTMODIFIED_DATE,
            scm.DELETED
        FROM SHIP_COMODITY_MASTER scm
        """;

        return entityManager
                .createNativeQuery(sql)
                .getResultList();
    }
}