package fu.se.badmintonshop.modules.delivery.repository;

import fu.se.badmintonshop.modules.delivery.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarrierRepository extends JpaRepository<Carrier, Integer> {
    Optional<Carrier> findByCode(String code);

    List<Carrier> findByIsActiveTrue();
}