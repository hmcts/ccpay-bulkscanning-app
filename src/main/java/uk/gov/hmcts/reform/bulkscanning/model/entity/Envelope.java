package uk.gov.hmcts.reform.bulkscanning.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleService;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder(builderMethodName = "envelopeWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "envelope")
public class Envelope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "responsible_service")
    private ResponsibleService responsibleService;

    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;

    @Column
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "envelope_id", referencedColumnName = "id", nullable = false)
    private List<Payment> payments;

    @Column
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "envelope_id", referencedColumnName = "id", nullable = false)
    private List<Case> cases;
}
