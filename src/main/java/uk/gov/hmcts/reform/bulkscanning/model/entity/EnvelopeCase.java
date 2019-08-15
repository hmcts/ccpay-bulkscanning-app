package uk.gov.hmcts.reform.bulkscanning.model.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Builder(builderMethodName = "caseWith")
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "envelope_case")
public class EnvelopeCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ccd_reference")
    private String ccdReference;

    @Column(name = "exception_record_reference")
    private String exceptionRecordReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envelope_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Envelope envelope;

    @CreationTimestamp
    @Column(name = "date_created", nullable = false)
    public LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;
}
