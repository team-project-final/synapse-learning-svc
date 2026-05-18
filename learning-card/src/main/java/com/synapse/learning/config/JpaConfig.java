package com.synapse.learning.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // @EnableJpaAuditing — BaseEntity의 createdAt, updatedAt 자동 기록 활성화
}
