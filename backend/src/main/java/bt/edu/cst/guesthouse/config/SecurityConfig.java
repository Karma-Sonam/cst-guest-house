package bt.edu.cst.guesthouse.config;

import bt.edu.cst.guesthouse.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    private SecretKeySpec key(String secret) {
        byte[] bytes=secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(@Value("${app.jwt-secret}") String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
    }
    @Bean JwtDecoder jwtDecoder(@Value("${app.jwt-secret}") String secret, SessionRepository sessions, UserRepository users) {
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> sessionValidator=jwt -> {
            try {
                var session=sessions.findById(UUID.fromString(jwt.getId()));
                var user=users.findById(UUID.fromString(jwt.getSubject()));
                if (session.isPresent() && session.get().userId.equals(UUID.fromString(jwt.getSubject()))
                    && session.get().expiresAt.isAfter(Instant.now()) && user.isPresent() && user.get().active)
                    return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException ignored) { }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Session is no longer valid",null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("guesthouse"),sessionValidator));
        return decoder;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, UserRepository users,
        @Value("${app.frontend-url}") String frontendUrl) throws Exception {
        var cors=new CorsConfiguration(); cors.setAllowedOrigins(Arrays.stream(frontendUrl.split(",")).map(String::strip).filter(value -> !value.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization","Content-Type"));
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**",cors);
        return http.csrf(c -> c.disable()) // Bearer tokens only; no cookie authentication.
            .cors(c -> c.configurationSource(source))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/register","/api/auth/login","/api/public/config","/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwt -> {
                var user=users.findById(UUID.fromString(jwt.getSubject())).orElseThrow();
                return new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("ROLE_"+user.role)));
            }))).build();
    }
}
