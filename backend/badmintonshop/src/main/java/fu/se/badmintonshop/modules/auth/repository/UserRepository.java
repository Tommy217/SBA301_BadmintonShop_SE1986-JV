package fu.se.badmintonshop.modules.auth.repository;

import fu.se.badmintonshop.modules.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findWithRolesById(@Param("id") Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query(value = "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r " +
                   "WHERE (:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                   "AND (:roleCode IS NULL OR LOWER(r.code) = LOWER(:roleCode))",
           countQuery = "SELECT COUNT(DISTINCT u) FROM User u LEFT JOIN u.roles r " +
                        "WHERE (:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:roleCode IS NULL OR LOWER(r.code) = LOWER(:roleCode))")
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("roleCode") String roleCode, Pageable pageable);
}

