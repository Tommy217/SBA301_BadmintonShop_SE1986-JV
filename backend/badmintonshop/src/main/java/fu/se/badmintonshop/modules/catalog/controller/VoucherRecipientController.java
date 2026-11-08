package fu.se.badmintonshop.modules.catalog.controller;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.*;
import fu.se.badmintonshop.modules.catalog.entity.*;
import fu.se.badmintonshop.modules.auth.entity.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers/{voucherId}/recipients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class VoucherRecipientController {
    @PersistenceContext private EntityManager em;

    @GetMapping
    public List<Long> list(@PathVariable Integer voucherId) {
        return em.createQuery(
                        "SELECT u.userId FROM UserVoucher u WHERE u.voucherId=:id ORDER BY u.userId",
                        Long.class)
                .setParameter("id", voucherId)
                .getResultList();
    }

    @GetMapping("/accounts")
    public List<java.util.Map<String, Object>> accounts() {
        return em
                .createQuery("SELECT u FROM User u ORDER BY u.fullName,u.id", User.class)
                .getResultList()
                .stream()
                .map(
                        u -> {
                            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                            row.put("id", u.getId());
                            row.put("full_name", u.getFullName());
                            row.put("email", u.getEmail());
                            return row;
                        })
                .toList();
    }

    @PutMapping
    @Transactional
    public List<Long> replace(@PathVariable Integer voucherId, @RequestBody List<Long> userIds) {
        if (userIds == null
                || userIds.size() > 1000
                || userIds.stream().anyMatch(id -> id == null || id <= 0))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Danh sách tài khoản không hợp lệ (tối đa 1000).");
        var voucher = em.find(Voucher.class, voucherId, LockModeType.PESSIMISTIC_WRITE);
        if (voucher == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher.");
        if (!"private".equals(voucher.getAudience()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Chỉ cấp tài khoản cho voucher riêng.");
        for (Long id : userIds.stream().distinct().toList()) {
            if (em.find(User.class, id) == null)
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Không tìm thấy tài khoản #" + id);
        }
        var existing =
                em.createQuery(
                                "SELECT u FROM UserVoucher u WHERE u.voucherId=:id",
                                UserVoucher.class)
                        .setParameter("id", voucherId)
                        .getResultList();
        var requested = userIds.stream().distinct().toList();
        for (var grant : existing) if (!requested.contains(grant.getUserId())) em.remove(grant);
        for (Long id : requested)
            if (existing.stream().noneMatch(g -> g.getUserId().equals(id)))
                em.persist(UserVoucher.builder().userId(id).voucherId(voucherId).build());
        em.flush();
        return list(voucherId);
    }
}
