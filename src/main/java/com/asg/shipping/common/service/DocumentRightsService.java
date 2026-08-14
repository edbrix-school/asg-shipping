package com.asg.shipping.common.service;

import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Server-side equivalent of the legacy {@code ASGCommonClass.IsGrantedRights(docId, rightName)}
 * (ASGCommonClass:1703).
 *
 * <p>In the ADF application the rights of the logged-in user were loaded once at login by
 * {@code PROC_GLOB_USR_RIGHTS_APPSTART} and cached on the session bean
 * (ASGSessionClass.LoadUserRights:534). The JWT issued by the gateway does not carry them, and
 * {@link com.asg.common.lib.annotation.AllowedAction} only checks the action requested for the
 * endpoint's own document — so a check against a <em>different</em> document's rights has to read
 * them back from the database.
 *
 * <p>Rights are stored in {@code GLOBAL_USER_ROLES_RIGHTS_DTL.RIGHTS} as a six character flag
 * string, one {@code '1'}/{@code '0'} per right in the fixed order
 * View / Create / Edit / Delete / Print / Email
 * (ASGSessionClass.AddModifyGrantedRights:783). A user reaches those rows through the roles
 * assigned to them in {@code GLOBAL_USERS_AUTH_ROLES_DTL}; a right is granted if <em>any</em> of
 * the user's roles grants it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentRightsService {

    /**
     * {@code SUBSTR} is 1-based, so the parameter is the right's position in the flag string.
     * A malformed or short RIGHTS value yields NULL, which fails the comparison and denies the
     * right — the same outcome as legacy resetting it to {@code "000000"}
     * (ASGSessionClass.AddModifyGrantedRights:787).
     */
    private static final String HAS_RIGHT_SQL = """
            SELECT COUNT(*)
            FROM GLOBAL_USERS_AUTH_ROLES_DTL uar
            JOIN GLOBAL_USER_ROLES_RIGHTS_DTL urr
              ON urr.USER_ROLE_POID = uar.USER_ROLE_POID
            WHERE uar.USER_POID = ?
              AND urr.DOC_ID = ?
              AND SUBSTR(urr.RIGHTS, ?, 1) = '1'
            """;

    private final JdbcTemplate jdbcTemplate;

    /** Checks the right for the user on the current request. */
    public boolean isGrantedRight(String docId, UserRolesRightsEnum right) {
        return isGrantedRight(UserContext.getUserPoid(), docId, right);
    }

    public boolean isGrantedRight(Long userPoid, String docId, UserRolesRightsEnum right) {
        if (userPoid == null || docId == null || right == null) {
            return false;
        }
        try {
            Integer granted = jdbcTemplate.queryForObject(
                    HAS_RIGHT_SQL, Integer.class, userPoid, docId, rightPosition(right));
            return granted != null && granted > 0;
        } catch (DataAccessException e) {
            // Legacy IsGrantedRights swallowed every failure and returned false, i.e. deny.
            log.error("Unable to read rights for docId {} / userPoid {} — denying", docId, userPoid, e);
            return false;
        }
    }

    /** Position of the right inside the RIGHTS flag string (1-based). */
    private int rightPosition(UserRolesRightsEnum right) {
        return switch (right) {
            case VIEW -> 1;
            case CREATE -> 2;
            case EDIT -> 3;
            case DELETE -> 4;
            case PRINT -> 5;
            case EMAIL -> 6;
        };
    }
}