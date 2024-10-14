package uk.gov.hmcts.reform.bulkscanning.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
