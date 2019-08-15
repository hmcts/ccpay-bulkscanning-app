package uk.gov.hmcts.reform.bulkscanning.model.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder(builderMethodName = "paymentMetadataWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment_metadata")
public class PaymentMetadata extends BaseModel{

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
}
