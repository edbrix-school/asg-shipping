package com.asg.shipping.terminal.containertype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContainerTerminalTypeRequest {

    private Long containerTerminalTypePoid; //for update

    @NotBlank(message = "Container terminal type code is mandatory")
    @Size(max = 10, message = "Container terminal type code must not exceed 10 characters")
    private String containerTerminalTypeCode;

    @NotBlank(message = "Container terminal type name is mandatory")
    @Size(max = 100, message = "Container terminal type name must not exceed 100 characters")
    private String containerTerminalTypeName;

    @NotNull(message = "Container terminal type size is mandatory")
    private Long containerTerminalTypeSize;

    private BigInteger seqNo;

    @Size(max = 1, message = "Active flag must be Y or N")
    private String active;
}

