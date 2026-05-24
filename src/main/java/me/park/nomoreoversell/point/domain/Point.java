package me.park.nomoreoversell.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.park.nomoreoversell.common.domain.BaseTimeEntity;
import me.park.nomoreoversell.exception.InsufficientPointBalanceException;

@Entity
@Table(
        name = "point",
        uniqueConstraints = {
                @UniqueConstraint(name = "UNI_POINT_USER_ID", columnNames = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long balance;

    @Builder
    public Point(Long userId, Long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public boolean canAfford(long amount) {
        return balance >= amount;
    }

    public void deduct(long amount) {
        if (balance < amount) {
            throw new InsufficientPointBalanceException();
        }
        this.balance -= amount;
    }

    public void restore(long amount) {
        this.balance += amount;
    }
}
