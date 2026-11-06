package fu.se.badmintonshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, fu.se.badmintonshop.modules.auth.AuthService authService) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new fu.se.badmintonshop.modules.auth.TokenFilter(authService), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> res.sendError(401)).accessDeniedHandler((req,res,ex) -> res.sendError(403)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/login", "/api/v1/payments/payos/webhook").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/staff/**", "/api/inventory/**", "/api/invoices/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/vouchers/usages", "/api/vouchers/usages/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/brands/**", "/api/vouchers/**", "/api/carriers/**").permitAll()
                .requestMatchers("/api/products/**", "/api/categories/**", "/api/brands/**", "/api/vouchers/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                .requestMatchers("/api/v1/payments/payos/confirm-webhook").hasRole("ADMIN")
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
