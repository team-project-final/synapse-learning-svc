package com.synapse.learning.config;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;

/**
 * platform-svc가 RS256으로 발급한 JWT를 검증하는 Resource Server 설정.
 * 공개키는 synapse.jwt.public-key(raw base64 X509 또는 PEM)로 주입한다.
 * 컨트롤러는 여전히 X-User-Id / X-Tenant-Id 헤더로 사용자를 식별한다(JWT 검증과 별개).
 */
@Configuration
public class SecurityConfig {

    @Bean
    @Profile("!dev")
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
            .build();
    }

    @Bean
    @Profile("dev")
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(List.of("*"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(false);
                return config;
            }))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${synapse.jwt.public-key:}") String publicKey) {
        RSAPublicKey key = StringUtils.hasText(publicKey) ? parsePublicKey(publicKey) : generateEphemeralPublicKey();
        return NimbusJwtDecoder.withPublicKey(key).build();
    }

    private RSAPublicKey parsePublicKey(String publicKey) {
        try {
            String normalized = publicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(normalized);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException("synapse.jwt.public-key 파싱에 실패했습니다", ex);
        }
    }

    private RSAPublicKey generateEphemeralPublicKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return (RSAPublicKey) keyPair.getPublic();
        } catch (Exception ex) {
            throw new IllegalStateException("임시 JWT 공개키 생성에 실패했습니다", ex);
        }
    }
}
