package com.asg.shipping.importmanifestupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoadEmailFaxResponseDto {
    private List<EmailFaxDetailDto> emailFaxDetails;
}
