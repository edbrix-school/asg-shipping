package com.asg.shipping.terminal.containertype.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContainerTerminalTypeResponse {

    private Long containerTerminalTypePoid;

    private String containerTerminalTypeCode;

    private String containerTerminalTypeName;

    private Long containerTerminalTypeSize;

    private String active;

    private BigInteger seqNo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime modifiedDate;

    private String modifiedBy;
}

