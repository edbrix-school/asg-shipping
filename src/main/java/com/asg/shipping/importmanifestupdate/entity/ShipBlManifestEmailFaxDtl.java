package com.asg.shipping.importmanifestupdate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(
        name = "SHIP_BL_MANIFEST_EMAIL_FAX_DTL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipBlManifestEmailFaxDtl {

    @EmbeddedId
    private ShipBlManifestEmailFaxId id;

    @Column(name = "ADDRESS_POID", nullable = false)
    private Long addressPoid;

    @Column(name = "FAX", length = 25)
    private String fax;

    @Column(name = "EMAIL1", length = 200)
    private String email1;

    @Column(name = "EMAIL2", length = 200)
    private String email2;

    @Column(name = "SEND_EMAIL_FAX", length = 25)
    private String sendEmailFax;

    @Column(name = "SEND_YES_NO", length = 25)
    private String sendYesNo;

    @Column(name = "FAX_LOG", length = 4000)
    private String faxLog;

    @Column(name = "EMAIL_LOG", length = 4000)
    private String emailLog;

}
