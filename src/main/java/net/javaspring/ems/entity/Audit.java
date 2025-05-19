package net.javaspring.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import net.javaspring.ems.utils.AuditType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit")
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "audit_type", nullable = false)
    private AuditType auditType;

    @Column(name = "require_all_approval_passing", nullable = false)
    private boolean requireAllApprovalPassing;

    @Column(name = "require_peer_review", nullable = false)
    private boolean requirePeerReview;

    @Column(name = "allowed_to_leapfrog", nullable = false)
    private boolean allowedToLeapfrog;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "amountMoney")
    private Long amountMoney;

    @ToString.Exclude
    @OneToMany(mappedBy = "audit", cascade = {CascadeType.PERSIST,CascadeType.MERGE,CascadeType.REFRESH}, fetch = FetchType.EAGER)
    private List<AuditApprove> approvals;

    @Column(name = "status", columnDefinition = "TINYINT(4)")
    private Integer status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

}
