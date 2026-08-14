package com.g9latam.team62.fintech_api.repository;
 
import com.g9latam.team62.fintech_api.model.LinkStatus;
import com.g9latam.team62.fintech_api.model.Transaction;
import com.g9latam.team62.fintech_api.model.TransactionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
 
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserId(Long userId);
    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);
    void deleteByUserId(Long userId);

    List<Transaction> findByUserIdAndOperationNumberAndSource(Long userId, String operationNumber, TransactionSource source);

    List<Transaction> findByUserIdAndAmountAndDateAndDescriptionAndSource(
            Long userId, BigDecimal amount, LocalDate date, String description, TransactionSource source
    );

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.source = :source AND t.linkStatus = :linkStatus AND t.amount = :amount AND t.date BETWEEN :startDate AND :endDate")
    List<Transaction> findCandidates(
            @Param("userId") Long userId,
            @Param("source") TransactionSource source,
            @Param("linkStatus") LinkStatus linkStatus,
            @Param("amount") BigDecimal amount,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
