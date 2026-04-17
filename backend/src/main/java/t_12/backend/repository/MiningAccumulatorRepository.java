package t_12.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import t_12.backend.entity.MiningAccumulator;
import java.util.List;

public interface MiningAccumulatorRepository extends JpaRepository<MiningAccumulator, String> {

    List<MiningAccumulator> findAll();
}
