package uk.gov.hmcts.reform.bulkscanning.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Builder(builderMethodName = "paymentMetadataWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_metadata")
public class PaymentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "dcn_reference")
    private String dcnReference;

    @Column(name = "bgc_reference")
    private String bgcReference;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "date_banked")
    private LocalDateTime dateBanked;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false)
    public LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;
}
