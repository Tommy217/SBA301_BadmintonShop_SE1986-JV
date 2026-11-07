package fu.se.badmintonshop.modules.auth;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AuthService {
 private final JdbcTemplate jdbc;
 private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
 private record Session(long userId, Instant expiresAt) {}
 private final Map<String,Session> sessions = new ConcurrentHashMap<>();
 public record User(long id,String email,String fullName,List<String> roles,boolean isActive) {}
 public AuthService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 public User user(long id) {
  return jdbc.query("SELECT u.id,u.email,u.full_name,u.status,r.code AS role_code FROM users u "
   + "LEFT JOIN user_roles ur ON ur.user_id=u.id LEFT JOIN roles r ON r.id=ur.role_id WHERE u.id=? ORDER BY r.code", rs -> {
    if (!rs.next()) return null;
    String email=rs.getString("email"), fullName=rs.getString("full_name");
    boolean active="active".equals(rs.getString("status"));
    var roles=new LinkedHashSet<String>();
    do { String role=rs.getString("role_code"); if(role!=null) roles.add(role.toUpperCase(Locale.ROOT)); } while(rs.next());
    return new User(id,email,fullName,List.copyOf(roles),active);
   },id);
 }
 public Map<String,Object> login(String email,String password) {
  var rows=jdbc.queryForList("SELECT id,password_hash FROM users WHERE LOWER(email)=?",email.trim().toLowerCase(Locale.ROOT));
  if(rows.isEmpty() || password==null || !encoder.matches(password,(String)rows.getFirst().get("password_hash"))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email hoặc mật khẩu không đúng.");
  User user=user(((Number)rows.getFirst().get("id")).longValue());
  if(!user.isActive()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Tài khoản đã bị khóa.");
  Instant now=Instant.now();sessions.entrySet().removeIf(e->!e.getValue().expiresAt().isAfter(now));
  String token=UUID.randomUUID().toString()+UUID.randomUUID();sessions.put(token,new Session(user.id(),now.plusSeconds(28800)));
  jdbc.update("UPDATE users SET last_login_at=CURRENT_TIMESTAMP WHERE id=?",user.id());
  return Map.of("id",user.id(),"email",user.email(),"fullName",user.fullName(),"roles",user.roles(),"authToken",token);
 }
 public User authenticate(String header) {
  if(header==null || !header.startsWith("Bearer ")) return null;
  String token=header.substring(7);Session session=sessions.get(token);if(session==null)return null;
  if(!session.expiresAt().isAfter(Instant.now())){sessions.remove(token);return null;}
  User user=user(session.userId());if(user==null || !user.isActive()){sessions.remove(token);return null;}return user;
 }
 public void logout(String header){if(header!=null && header.startsWith("Bearer "))sessions.remove(header.substring(7));}
}
