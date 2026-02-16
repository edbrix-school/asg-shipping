package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

import java.util.List;

/**
 * Generic bulk save request for detail tables
 */
@Data
public class BulkSaveRequest<T> {
    private List<T> details;
    private List<Long> deleteIds;
}

