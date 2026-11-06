package fu.se.badmintonshop.modules.auth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
 private final AuthService auth;
 public AuthController(AuthService auth){this.auth=auth;}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest r){return auth.login(r.email(),r.password());}
 @GetMapping("/me") public AuthService.User me(Authentication a){return (AuthService.User)a.getPrincipal();}
 @PostMapping("/logout") public void logout(@RequestHeader("Authorization")String header){auth.logout(header);}
}
