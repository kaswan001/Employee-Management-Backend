package net.javaspring.ems.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "audit_approve")
public class AuditApprove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "approved", nullable = true)
    private String approved;

    @Column(name = "approval_time", nullable = true)
    private LocalDateTime approvalTime;

    @Column(name = "audit_level_order")
    private int auditLevelOrder;

    @Column(name = "is_last_level")
    private boolean isLastLevel;

    @Column(name = "approved_status")
    private String approvedStatus;
}
