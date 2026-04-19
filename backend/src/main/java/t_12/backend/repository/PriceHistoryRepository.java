package t_12.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import t_12.backend.entity.PriceHistory;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Integer> {

    List<PriceHistory> findAllByOrderByRecordedAtAsc();

    List<PriceHistory> findAllByOrderByRecordedAtDesc();

    List<PriceHistory> findByRecordedAtAfterOrderByRecordedAtAsc(LocalDateTime since);
}