package fu.se.badmintonshop.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    JdbcTemplate jdbc;
    AuthService auth;
    @BeforeEach void setup() {
        // Keep the test database alive across JDBC connections.
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:auth" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE users(id BIGINT PRIMARY KEY,email VARCHAR(190),password_hash VARCHAR(255),full_name VARCHAR(150),status VARCHAR(20),last_login_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE roles(id INT PRIMARY KEY,code VARCHAR(30))");
        jdbc.execute("CREATE TABLE user_roles(user_id BIGINT,role_id INT)");
        String hash = new BCryptPasswordEncoder().encode("123");
        String[] roles = {"admin", "manager", "staff", "customer"};
        for (int i = 0; i < roles.length; i++) {
            jdbc.update("INSERT INTO users VALUES(?,?,?,?,?,NULL)",i+1,roles[i]+"@gmail.com",hash,roles[i],"active");
            jdbc.update("INSERT INTO roles VALUES(?,?)",i+1,roles[i]);
            jdbc.update("INSERT INTO user_roles VALUES(?,?)",i+1,i+1);
        }
        auth = new AuthService(jdbc);
    }
    @Test void authenticatesAllFourDatabaseRolesWithoutExposingPasswords() {
        for (String role : new String[]{"admin","manager","staff","customer"}) {
            var result = auth.login("  " + role.toUpperCase() + "@GMAIL.COM ", "123");
            var user = auth.authenticate("Bearer " + result.get("authToken"));
            assertEquals(java.util.List.of(role.toUpperCase()), user.roles());
            assertFalse(result.containsKey("password_hash"));
            assertFalse(result.containsKey("password"));
        }
    }
    @Test void rejectsBadCredentialsAndMockTokens() {
        assertThrows(ResponseStatusException.class, () -> auth.login("customer@gmail.com", "wrong"));
        assertThrows(ResponseStatusException.class, () -> auth.login("missing@gmail.com", "123"));
        assertNull(auth.authenticate("Bearer mock-old-token"));
    }
    @Test void reloadsRolesAndStatusFromDatabaseAndRevokesLogout() {
        String header = "Bearer " + auth.login("customer@gmail.com", "123").get("authToken");
        jdbc.update("UPDATE user_roles SET role_id=3 WHERE user_id=4");
        assertEquals(java.util.List.of("STAFF"), auth.authenticate(header).roles());
        auth.logout(header);
        assertNull(auth.authenticate(header));
        header = "Bearer " + auth.login("customer@gmail.com", "123").get("authToken");
        jdbc.update("UPDATE users SET status='locked' WHERE id=4");
        assertNull(auth.authenticate(header));
        assertThrows(ResponseStatusException.class, () -> auth.login("customer@gmail.com", "123"));
    }

    @Test void joinedIdentityReadKeepsUsersWithoutRolesAndCombinesMultipleRoles() {
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(4,3)");
        assertEquals(java.util.Set.of("CUSTOMER", "STAFF"), new java.util.HashSet<>(auth.user(4).roles()));
        jdbc.update("DELETE FROM user_roles WHERE user_id=4");
        assertNotNull(auth.user(4));
        assertTrue(auth.user(4).roles().isEmpty());
        assertNull(auth.user(999));
    }

    @Test void simultaneousAuthenticationDoesNotNeedANestedPoolConnection() throws Exception {
        try (var pool = new com.zaxxer.hikari.HikariDataSource()) {
            pool.setDataSource(jdbc.getDataSource());
            pool.setMaximumPoolSize(2);
            pool.setMinimumIdle(0);
            pool.setConnectionTimeout(1000);
            var measuredAuth = new AuthService(new JdbcTemplate(pool));
            String header = "Bearer " + measuredAuth.login("customer@gmail.com", "123").get("authToken");
            var gate = new java.util.concurrent.CountDownLatch(1);
            try (var executor = java.util.concurrent.Executors.newFixedThreadPool(8)) {
                var checks = new java.util.ArrayList<java.util.concurrent.Future<AuthService.User>>();
                for (int i = 0; i < 16; i++) checks.add(executor.submit(() -> {
                    gate.await();
                    return measuredAuth.authenticate(header);
                }));
                gate.countDown();
                for (var check : checks) assertEquals(java.util.List.of("CUSTOMER"), check.get(5, java.util.concurrent.TimeUnit.SECONDS).roles());
            }
        }
    }
}
