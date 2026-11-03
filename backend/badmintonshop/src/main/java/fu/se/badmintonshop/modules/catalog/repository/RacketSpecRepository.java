package fu.se.badmintonshop.modules.catalog.repository;

import fu.se.badmintonshop.modules.catalog.entity.RacketSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RacketSpecRepository extends JpaRepository<RacketSpec, Long> {
}
