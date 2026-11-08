package fu.se.badmintonshop.modules.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;

    @GetMapping
    public List<Long> list(@PathVariable Integer voucherId) {
        return jdbc.queryForList("SELECT user_id FROM user_vouchers WHERE voucher_id=? ORDER BY user_id", Long.class, voucherId);
    }

    @GetMapping("/accounts")
    public List<java.util.Map<String, Object>> accounts() {
        return jdbc.queryForList("SELECT id,full_name,email FROM users ORDER BY full_name,id");
    }

    @PutMapping
    @Transactional
    public List<Long> replace(@PathVariable Integer voucherId, @RequestBody List<Long> userIds) {
        if (userIds == null || userIds.size() > 1000 || userIds.stream().anyMatch(id -> id == null || id <= 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách tài khoản không hợp lệ (tối đa 1000).");
        var vouchers = jdbc.queryForList("SELECT audience FROM vouchers WITH (UPDLOCK) WHERE id=?", String.class, voucherId);
        if (vouchers.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher.");
        if (!"private".equals(vouchers.getFirst())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ cấp tài khoản cho voucher riêng.");
        for (Long id : userIds.stream().distinct().toList()) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id=?", Long.class, id) == 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy tài khoản #" + id);
        }
        jdbc.update("DELETE FROM user_vouchers WHERE voucher_id=?", voucherId);
        for (Long id : userIds.stream().distinct().toList())
            jdbc.update("INSERT INTO user_vouchers(user_id,voucher_id) VALUES(?,?)", id, voucherId);
        return list(voucherId);
    }
}
