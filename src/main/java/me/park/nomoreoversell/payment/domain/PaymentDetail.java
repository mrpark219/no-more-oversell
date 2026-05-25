package me.park.nomoreoversell.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.park.nomoreoversell.common.domain.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_detail",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UNI_PAYMENT_DETAIL_PAYMENT_METHOD",
                        columnNames = {"payment_id", "payment_method"}
                ),
                @UniqueConstraint(
                        name = "UNI_PAYMENT_DETAIL_EXTERNAL_TRANSACTION_KEY",
                        columnNames = {"payment_id", "external_transaction_key"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentDetail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private Long paymentAmount;

    @Column(nullable = false)
    private Long canceledAmount;

    @Column(nullable = false)
    private Long remainingAmount;

    @Column(name = "external_transaction_key")
    private String externalTransactionKey;

    @Column(name = "cancel_key")
    private String cancelKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentDetailStatus status;

    @Column
    private String failureReason;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime canceledAt;

    @Builder
    private PaymentDetail(
            Payment payment,
            PaymentMethod paymentMethod,
            Long paymentAmount,
            Long canceledAmount,
            Long remainingAmount,
            String externalTransactionKey,
            String cancelKey,
            PaymentDetailStatus status
    ) {
        this.payment = payment;
        this.paymentMethod = paymentMethod;
        this.paymentAmount = paymentAmount;
        this.canceledAmount = canceledAmount;
        this.remainingAmount = remainingAmount;
        this.externalTransactionKey = externalTransactionKey;
        this.cancelKey = cancelKey;
        this.status = status;
    }

    public static PaymentDetail ready(
            Payment payment,
            PaymentMethod paymentMethod,
            long amount,
            String externalTransactionKey
    ) {
        return PaymentDetail.builder()
                .payment(payment)
                .paymentMethod(paymentMethod)
                .paymentAmount(amount)
                .canceledAmount(0L)
                .remainingAmount(amount)
                .externalTransactionKey(externalTransactionKey)
                .status(PaymentDetailStatus.READY)
                .build();
    }

    public void markApproving() {
        this.status = PaymentDetailStatus.APPROVING;
    }

    public void markApproved() {
        this.status = PaymentDetailStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    public void cancel(long cancelAmount, String cancelKey, String reason) {
        if (cancelAmount <= 0) {
            throw new IllegalArgumentException("취소 금액은 0보다 커야 합니다.");
        }
        validateCancelKey(cancelKey);
        if (this.cancelKey != null) {
            if (this.cancelKey.equals(cancelKey)) {
                if (!this.canceledAmount.equals(cancelAmount)) {
                    throw new IllegalArgumentException("같은 취소 키의 취소 금액은 기존 취소 금액과 같아야 합니다.");
                }
                return;
            }
            throw new IllegalStateException("이미 취소 처리된 결제 상세입니다.");
        }
        if (cancelAmount > remainingAmount) {
            throw new IllegalArgumentException("취소 금액은 남은 결제 금액을 초과할 수 없습니다.");
        }
        this.cancelKey = cancelKey;
        this.canceledAmount += cancelAmount;
        this.remainingAmount -= cancelAmount;
        this.canceledAt = LocalDateTime.now();
        this.failureReason = reason;
        if (remainingAmount == 0) {
            this.status = PaymentDetailStatus.CANCELED;
            return;
        }
        this.status = PaymentDetailStatus.PARTIAL_CANCELED;
    }

    private void validateCancelKey(String cancelKey) {
        if (cancelKey == null || cancelKey.isBlank()) {
            throw new IllegalArgumentException("취소 키는 필수입니다.");
        }
    }

    public void markFailed(String reason) {
        this.status = PaymentDetailStatus.FAILED;
        this.failureReason = reason;
    }

    public void markCancelFailed(String reason) {
        this.status = PaymentDetailStatus.CANCEL_FAILED;
        this.failureReason = reason;
    }

    public boolean isReady() {
        return status == PaymentDetailStatus.READY;
    }

    public boolean isApproving() {
        return status == PaymentDetailStatus.APPROVING;
    }

    public boolean isPgNeeded() {
        return paymentMethod.isPgNeeded();
    }

    public boolean matchesExternalTransactionKey(String externalTransactionKey) {
        return this.externalTransactionKey != null && this.externalTransactionKey.equals(externalTransactionKey);
    }
}
