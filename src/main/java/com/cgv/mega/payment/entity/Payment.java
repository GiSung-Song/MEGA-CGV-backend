package com.cgv.mega.payment.entity;

import com.cgv.mega.common.entity.BaseTimeEntity;
import com.cgv.mega.payment.enums.PaymentStatus;
import com.cgv.mega.reservation.entity.ReservationGroup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_payments_merchant_uid",
                        columnNames = "merchant_uid"
                ),
                @UniqueConstraint(
                        name = "uq_payments_payment_id",
                        columnNames = "payment_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_payments_reservation_group",
                        columnList = "reservation_group_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reservation_group_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_reservation_group")
    )
    private ReservationGroup reservationGroup;

    @Column(name = "buyer_name", nullable = false, length = 50)
    private String buyerName;

    @Column(name = "buyer_phone_number", nullable = false, length = 20)
    private String buyerPhoneNumber;

    @Column(name = "buyer_email", nullable = false, length = 50)
    private String buyerEmail;

    @Column(name = "merchant_uid", nullable = false, length = 100)
    private String merchantUid;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "refund_amount", precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "pg_provider", length = 50)
    private String pgProvider;

    @Column(name = "pay_method", length = 30)
    private String payMethod;

    @Column(name = "card_name", length = 50)
    private String cardName;

    @Column(name = "card_quota")
    private Integer cardQuota;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "webhook_verified", nullable = false)
    private boolean webhookVerified;

    @Builder(access = AccessLevel.PRIVATE)
    private Payment(
            ReservationGroup reservationGroup,
            String buyerName,
            String buyerPhoneNumber,
            String buyerEmail,
            String merchantUid,
            BigDecimal expectedAmount
    ) {
       this.reservationGroup = reservationGroup;
       this.buyerName = buyerName;
       this.buyerPhoneNumber = buyerPhoneNumber;
       this.buyerEmail = buyerEmail;
       this.merchantUid = merchantUid;
       this.expectedAmount = expectedAmount;
       this.status = PaymentStatus.READY;
       this.webhookVerified = false;
    }

    public static Payment createPayment(
            ReservationGroup reservationGroup,
            String buyerName,
            String buyerPhoneNumber,
            String buyerEmail,
            String merchantUid,
            BigDecimal expectedAmount
    ) {
        return Payment.builder()
                .reservationGroup(reservationGroup)
                .buyerName(buyerName)
                .buyerPhoneNumber(buyerPhoneNumber)
                .buyerEmail(buyerEmail)
                .merchantUid(merchantUid)
                .expectedAmount(expectedAmount)
                .build();
    }

    public boolean isFinalized() {
        return status != PaymentStatus.READY;
    }

    public void failedPayment(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
    }

    public void successPayment(
            String paymentId,
            BigDecimal paidAmount,
            String pgProvider,
            String payMethod,
            String cardName,
            Integer cardQuota,
            LocalDateTime paidAt
    ) {
        this.paymentId = paymentId;
        this.paidAmount = paidAmount;
        this.pgProvider = pgProvider;
        this.payMethod = payMethod;
        this.cardName = cardName;
        this.cardQuota = cardQuota;
        this.paidAt = paidAt;
        this.status = PaymentStatus.COMPLETED;
    }

    public void cancelPayment(BigDecimal refundAmount, String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
        this.refundAmount = refundAmount;
    }
}