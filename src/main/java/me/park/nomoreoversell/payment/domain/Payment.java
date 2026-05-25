package me.park.nomoreoversell.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.park.nomoreoversell.common.domain.BaseTimeEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "UNI_PAYMENT_ORDER_SHEET_ID", columnNames = "order_sheet_id"),
                @UniqueConstraint(name = "UNI_PAYMENT_TOKEN", columnNames = "payment_token")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderSheetId;

    @Column(nullable = false)
    private String paymentToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentDetail> details = new ArrayList<>();

    @Builder
    private Payment(Long orderSheetId, String paymentToken, PaymentStatus status) {
        this.orderSheetId = orderSheetId;
        this.paymentToken = paymentToken;
        this.status = status;
    }

    public static Payment ready(Long orderSheetId, String paymentToken) {
        return Payment.builder()
                .orderSheetId(orderSheetId)
                .paymentToken(paymentToken)
                .status(PaymentStatus.READY)
                .build();
    }

    public void addDetail(PaymentDetail detail) {
        if (detail.getPayment() != this) {
            throw new IllegalArgumentException("결제 상세는 생성 시 연결된 결제를 변경할 수 없습니다.");
        }
        this.details.add(detail);
    }

    public Optional<PaymentDetail> findDetail(PaymentMethod paymentMethod) {
        return details.stream()
                .filter(detail -> detail.getPaymentMethod() == paymentMethod)
                .findFirst();
    }

    public List<PaymentDetail> orderedDetails() {
        return details.stream()
                .sorted(Comparator.comparingInt(detail -> detail.getPaymentMethod().getPayOrder()))
                .toList();
    }

    public List<PaymentDetail> pgDetails() {
        return details.stream()
                .filter(PaymentDetail::isPgNeeded)
                .sorted(Comparator.comparingInt(detail -> detail.getPaymentMethod().getPayOrder()))
                .toList();
    }

    public List<PaymentDetail> nonPgDetails() {
        return details.stream()
                .filter(detail -> !detail.isPgNeeded())
                .sorted(Comparator.comparingInt(detail -> detail.getPaymentMethod().getPayOrder()))
                .toList();
    }

    public boolean hasPgDetail() {
        return details.stream().anyMatch(PaymentDetail::isPgNeeded);
    }

    public long totalPaymentAmount() {
        return details.stream()
                .mapToLong(PaymentDetail::getPaymentAmount)
                .sum();
    }

    public long totalCanceledAmount() {
        return details.stream()
                .mapToLong(PaymentDetail::getCanceledAmount)
                .sum();
    }

    public long totalRemainingAmount() {
        return details.stream()
                .mapToLong(PaymentDetail::getRemainingAmount)
                .sum();
    }

    public void markReady() {
        this.status = PaymentStatus.READY;
    }

    public void markApproving() {
        this.status = PaymentStatus.APPROVING;
    }

    public void markApproved() {
        this.status = PaymentStatus.APPROVED;
    }

    public void markCanceled() {
        this.status = PaymentStatus.CANCELED;
    }

    public void markCancelFailed() {
        this.status = PaymentStatus.CANCEL_FAILED;
    }

    public void cancelDetail(PaymentMethod paymentMethod, long cancelAmount, String cancelKey, String reason) {
        findDetail(paymentMethod)
                .orElseThrow(() -> new IllegalArgumentException("취소할 결제 상세를 찾을 수 없습니다."))
                .cancel(cancelAmount, cancelKey, reason);
        if (totalRemainingAmount() == 0) {
            this.status = PaymentStatus.CANCELED;
        }
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
    }

    public boolean isReady() {
        return status == PaymentStatus.READY;
    }

    public boolean isApproving() {
        return status == PaymentStatus.APPROVING;
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }
}
