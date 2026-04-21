package com.asg.shipping.bookingFormSH.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asg.shipping.common.dto.LovItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingFormLovServiceImpl implements BookingFormLovService {

	private final JdbcTemplate jdbcTemplate;

	private static final RowMapper<LovItem> LOV_ITEM_ROW_MAPPER = new RowMapper<LovItem>() {
		@Override
		public LovItem mapRow(ResultSet rs, int rowNum) throws SQLException {
			LovItem item = new LovItem();
			item.setPoid(rs.getLong("POID"));
			item.setCode(rs.getString("CODE"));
			item.setDescription(rs.getString("DESCRIPTION"));
			item.setValue(rs.getLong("POID"));
			item.setLabel(rs.getString("CODE"));
			return item;
		}
	};

	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getLineMasterLov(Long poid) {
		log.info("Fetching LINE_MASTER LOV with linePoid filter: {}", poid);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT LINE_POID AS POID, ");
		sql.append("       LINE_CODE AS CODE, ");
		sql.append("       LINE_NAME AS DESCRIPTION ");
		sql.append("FROM SHIP_LINE_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");

		List<Object> params = new ArrayList<>();

		if (poid != null) {
			sql.append("AND LINE_POID = ? ");
			params.add(poid);
		}

		List<LovItem> result;
		if (params.isEmpty()) {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
		} else {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		}
		log.info("Fetched {} LINE_MASTER LOV items", result.size());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getVesselMasterLov(Long poid) {
		log.info("Fetching VESSEL_MASTER LOV with linePoid filter: {}", poid);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT VESSEL_POID AS POID, ");
		sql.append("       VESSEL_CODE AS CODE, ");
		sql.append("       VESSEL_NAME AS DESCRIPTION ");
		sql.append("FROM SHIP_VESSEL_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");

		List<Object> params = new ArrayList<>();

		if (poid != null) {
			sql.append("AND VESSEL_POID = ? ");
			params.add(poid);
		}

		List<LovItem> result;
		if (params.isEmpty()) {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
		} else {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		}
		log.info("Fetched {} VESSEL_MASTER LOV items", result.size());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getQuotaionLov(Long poid) {
		log.info("Fetching QUOTATION LOV with linePoid filter: {}", poid);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT TRANSACTION_POID AS POID, ");
		sql.append("       TO_CHAR (TRANSACTION_POID) AS CODE, ");
		sql.append("       'ADD CUSTOMER LATTER' AS DESCRIPTION ");
		sql.append("FROM SALES_QUOTATION_HDR ");
		sql.append("WHERE (DELETED = 'N' OR DELETED IS NULL)");

		List<Object> params = new ArrayList<>();

		if (poid != null) {
			sql.append("AND TRANSACTION_POID = ? ");
			params.add(poid);
		}

		List<LovItem> result;
		if (params.isEmpty()) {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
		} else {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		}
		log.info("Fetched {} QUOTATION LOV items", result.size());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getSalesmanLov(Long poid) {
		log.info("Fetching SALESMAN_MASTER LOV with linePoid filter: {}", poid);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT SALESMAN_POID AS POID, ");
		sql.append("       SALESMAN_CODE AS CODE, ");
		sql.append("       SALESMAN_NAME AS DESCRIPTION ");
		sql.append("FROM SALES_SALESMAN_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");

		List<Object> params = new ArrayList<>();

		if (poid != null) {
			sql.append("AND SALESMAN_POID = ? ");
			params.add(poid);
		}

		List<LovItem> result;
		if (params.isEmpty()) {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
		} else {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		}
		log.info("Fetched {} SALESMAN_MASTER LOV items", result.size());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getCommodityMasterLov(Long poid) {
		log.info("Fetching COMMODITY_MASTER LOV with linePoid filter: {}", poid);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT COMODITY_POID AS POID, ");
		sql.append("       COMODITY_CODE AS CODE, ");
		sql.append("       COMODITY_NAME AS DESCRIPTION ");
		sql.append("FROM SHIP_COMODITY_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");

		List<Object> params = new ArrayList<>();

		if (poid != null) {
			sql.append("AND COMODITY_POID = ? ");
			params.add(poid);
		}

		List<LovItem> result;
		if (params.isEmpty()) {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
		} else {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		}
		log.info("Fetched {} COMMODITY_MASTER LOV items", result.size());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getPortMasterLov(Long poid) {
		log.info("Fetching PORT_MASTER LOV with linePoid filter: {}", poid);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT PORT_POID AS POID, ");
		sql.append("       PORT_CODE AS CODE, ");
		sql.append("       PORT_NAME AS DESCRIPTION ");
		sql.append("FROM SHIP_PORT_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");

		List<Object> params = new ArrayList<>();

		if (poid != null) {
			sql.append("AND PORT_POID = ? ");
			params.add(poid);
		}

		List<LovItem> result;
		if (params.isEmpty()) {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
		} else {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		}
		log.info("Fetched {} PORT_MASTER LOV items", result.size());
		return result;
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getVoyageMasterLov(Long poid) {
		log.info("Fetching VOYAGE_MASTER LOV with linePoid filter: {}", poid);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT TRANSACTION_POID AS POID, ");
		sql.append("       VOYAGE_NO AS CODE, ");
		sql.append("       DOC_REF AS DESCRIPTION ");
		sql.append("FROM SHIP_VOYAGE_HDR ");
		sql.append("WHERE (DELETED = 'N' OR DELETED IS NULL)");

		List<Object> params = new ArrayList<>();

		if (poid != null) {
			sql.append("AND TRANSACTION_POID = ? ");
			params.add(poid);
		}

		List<LovItem> result;
		if (params.isEmpty()) {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
		} else {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		}
		log.info("Fetched {} VOYAGE_MASTER LOV items", result.size());
		return result;
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getChargeMasterLov(Long poid) {
		log.info("Fetching CHARGE_MASTER LOV with linePoid filter: {}", poid);

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT CHARGE_POID AS POID, ");
		sql.append("       CHARGE_CODE AS CODE, ");
		sql.append("       CHARGE_NAME AS DESCRIPTION ");
		sql.append("FROM SHIP_CHARGE_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");

		List<Object> params = new ArrayList<>();

		if (poid != null) {
			sql.append("AND CHARGE_POID = ? ");
			params.add(poid);
		}

		List<LovItem> result;
		if (params.isEmpty()) {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
		} else {
			result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		}
		log.info("Fetched {} CHARGE_MASTER LOV items", result.size());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getEquipmentIsoTypeLov(String code) {
		log.info("Fetching EQUIPMENT_ISO_TYPE LOV for code: {}", code);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT CONTAINER_TYPE_POID AS POID, ");
		sql.append("       CONTAINER_TYPE_CODE AS CODE, ");
		sql.append("       CONTAINER_TYPE_NAME AS DESCRIPTION ");
		sql.append("FROM SHIP_CONTAINER_TYPE_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");
		sql.append("AND CONTAINER_TYPE_CODE = ? ");
		List<Object> params = new ArrayList<>();
		params.add(code);
		List<LovItem> result = params.isEmpty()
				? jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER)
				: jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		log.info("Fetched {} EQUIPMENT_ISO_TYPE LOV items", result.size());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LovItem> getImcoClassTypeLov(String code) {
		log.info("Fetching IMCO_CLASS_TYPE LOV for code: {}", code);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT IMCO_CLASS_TYPE_POID AS POID, ");
		sql.append("       IMCO_CLASS_TYPE_CODE AS CODE, ");
		sql.append("       IMCO_CLASS_TYPE_NAME AS DESCRIPTION ");
		sql.append("FROM SHIP_IMCO_CLASS_TYPE_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");
		sql.append("AND IMCO_CLASS_TYPE_CODE = ? ");
		List<Object> params = new ArrayList<>();
		params.add(code);
		List<LovItem> result = params.isEmpty()
				? jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER)
				: jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		log.info("Fetched {} IMCO_CLASS_TYPE LOV items", result.size());
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LovItem>getOogTypeLov(String code) {
		log.info("Fetching OOG_TYPE LOV for code: {}", code);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT OOG_TYPE_POID AS POID, ");
		sql.append("       OOG_TYPE_CODE AS CODE, ");
		sql.append("       OOG_TYPE_NAME AS DESCRIPTION ");
		sql.append("FROM SHIP_OOG_TYPE_MASTER ");
		sql.append("WHERE ACTIVE = 'Y' ");
		sql.append("AND OOG_TYPE_POID = ? ");
		List<Object> params = new ArrayList<>();
		params.add(code);

		List<LovItem> result = params.isEmpty()
				? jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER)
				: jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
		log.info("Fetched {} OOG_TYPE LOV items", result.size());
		return result;
	}

}
