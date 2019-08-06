package uk.gov.hmcts.reform.bulkscanning.model.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder(builderMethodName = "paymentWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment")
public class Payment {

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
    private Currency currency;

    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "date_banked")
    private LocalDateTime dateBanked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Envelope envelope;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;
}
