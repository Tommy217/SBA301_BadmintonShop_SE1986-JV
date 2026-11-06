package fu.se.badmintonshop.modules.auth;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
public class TokenFilter extends OncePerRequestFilter {
 private final AuthService auth;
 public TokenFilter(AuthService auth){this.auth=auth;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  var user=auth.authenticate(request.getHeader("Authorization"));
  if(user!=null){
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,user.roles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+r)).toList()));
   // Existing controllers receive identity from the verified token only.
   request=new HttpServletRequestWrapper(request){
    @Override public String getHeader(String name){return "X-User-Id".equalsIgnoreCase(name)?Long.toString(user.id()):super.getHeader(name);}
    @Override public Enumeration<String> getHeaders(String name){return "X-User-Id".equalsIgnoreCase(name)?Collections.enumeration(List.of(Long.toString(user.id()))):super.getHeaders(name);}
   };
  }
  chain.doFilter(request,response);
 }
}
