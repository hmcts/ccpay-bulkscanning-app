package uk.gov.hmcts.reform.bulkscanning.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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

    @Column(name = "responsible_service_id")
    private String responsibleServiceId;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "envelope_id", referencedColumnName = "id", nullable = false)
    private List<EnvelopePayment> envelopePayments;

    @Column
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "envelope_id", referencedColumnName = "id", nullable = false)
    private List<EnvelopeCase> envelopeCases;

    @Column
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "envelope_id", referencedColumnName = "id", nullable = false)
    private List<StatusHistory> statusHistories;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false)
    public LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;
}
