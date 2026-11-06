package fu.se.badmintonshop.modules.auth;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final JdbcTemplate jdbc;
    private final AuthService auth;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public UserController(JdbcTemplate jdbc, AuthService auth) { this.jdbc = jdbc; this.auth = auth; }
    @GetMapping
    public List<AuthService.User> list() {
        return jdbc.queryForList("SELECT id FROM users ORDER BY id", Long.class).stream().map(auth::user).toList();
    }
    @GetMapping("/{id}")
    public AuthService.User get(@PathVariable long id) {
        var user = auth.user(id);
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản.");
        return user;
    }
    public record Input(String email, String fullName, String password, List<String> roles, Boolean isActive) {}
    @PostMapping
    @Transactional
    public AuthService.User create(@RequestBody Input input) {
        if (input.email() == null || input.email().isBlank() || input.fullName() == null || input.fullName().isBlank() || input.password() == null || input.password().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ thông tin.");
        String email = input.email().trim().toLowerCase(Locale.ROOT);
        uniqueEmail(email, -1);
        var key = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO users(email,full_name,password_hash,status) VALUES(?,?,?,?)", java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, email); statement.setNString(2, input.fullName().trim());
            statement.setString(3, encoder.encode(input.password())); statement.setString(4, Boolean.FALSE.equals(input.isActive()) ? "inactive" : "active");
            return statement;
        }, key);
        long id = Objects.requireNonNull(key.getKey()).longValue();
        roles(id, input.roles() == null ? List.of("CUSTOMER") : input.roles());
        return get(id);
    }
    @PatchMapping("/{id}")
    @Transactional
    public AuthService.User update(@PathVariable long id, @RequestBody Input input) {
        get(id);
        if (input.email() != null) {
            String email = input.email().trim().toLowerCase(Locale.ROOT);
            if (email.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ.");
            uniqueEmail(email, id); jdbc.update("UPDATE users SET email=? WHERE id=?", email, id);
        }
        if (input.fullName() != null) {
            if (input.fullName().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập họ tên.");
            jdbc.update("UPDATE users SET full_name=? WHERE id=?", input.fullName().trim(), id);
        }
        if (input.password() != null && !input.password().isBlank()) jdbc.update("UPDATE users SET password_hash=? WHERE id=?", encoder.encode(input.password()), id);
        if (input.isActive() != null) jdbc.update("UPDATE users SET status=? WHERE id=?", input.isActive() ? "active" : "inactive", id);
        if (input.roles() != null) roles(id, input.roles());
        return get(id);
    }
    private void uniqueEmail(String email, long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE LOWER(email)=? AND id<>?", Integer.class, email, id) > 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã tồn tại.");
    }
    private void roles(long id, List<String> roles) {
        if (roles.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ít nhất một vai trò.");
        jdbc.update("DELETE FROM user_roles WHERE user_id=?", id);
        for (String role : new HashSet<>(roles)) {
            if (role == null || !Set.of("ADMIN", "MANAGER", "STAFF", "CUSTOMER").contains(role.toUpperCase(Locale.ROOT)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ.");
            int count = jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE UPPER(code)=?", id, role.toUpperCase(Locale.ROOT));
            if (count != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không tồn tại.");
        }
    }
}
