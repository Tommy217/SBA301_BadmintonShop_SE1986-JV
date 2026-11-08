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

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"role"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findWithRoleById(@Param("id") Long id);

    boolean existsByEmailIgnoreCase(String email);

    @Query(value = "SELECT u FROM User u JOIN FETCH u.role r " +
                   "WHERE LOWER(r.code) != 'customer' " +
                   "AND (:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                   "AND (:roleCode IS NULL OR LOWER(r.code) = LOWER(:roleCode)) " +
                   "AND (:status IS NULL OR LOWER(u.status) = LOWER(:status))",
           countQuery = "SELECT COUNT(u) FROM User u JOIN u.role r " +
                        "WHERE LOWER(r.code) != 'customer' " +
                        "AND (:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:roleCode IS NULL OR LOWER(r.code) = LOWER(:roleCode)) " +
                        "AND (:status IS NULL OR LOWER(u.status) = LOWER(:status))")
    Page<User> searchEmployees(
        @Param("keyword") String keyword,
        @Param("roleCode") String roleCode,
        @Param("status") String status,
        Pageable pageable
    );

    @Query(value = "SELECT u FROM User u JOIN FETCH u.role r " +
                   "WHERE LOWER(r.code) = 'customer' " +
                   "AND (:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                   "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                   "AND (:status IS NULL OR LOWER(u.status) = LOWER(:status))",
           countQuery = "SELECT COUNT(u) FROM User u JOIN u.role r " +
                        "WHERE LOWER(r.code) = 'customer' " +
                        "AND (:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:status IS NULL OR LOWER(u.status) = LOWER(:status))")
    Page<User> searchCustomers(
        @Param("keyword") String keyword,
        @Param("status") String status,
        Pageable pageable
    );
}
