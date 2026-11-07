package fu.se.badmintonshop.modules.auth.repository;

import fu.se.badmintonshop.modules.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByCodeIgnoreCase(String code);
}

