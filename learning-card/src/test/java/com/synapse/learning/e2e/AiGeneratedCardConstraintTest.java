package com.synapse.learning.e2e;

import com.synapse.learning.card.adapter.out.persistence.CardDeckJpaRepository;
import com.synapse.learning.card.adapter.out.persistence.FlashCardJpaRepository;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.card.domain.model.FlashCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V20 card_type 정규화 검증 — 실제 PostgreSQL 제약에서 canonical 값만 허용되는지 확인
 *
 * V20 마이그레이션 이후 DB 제약: card_type IN ('basic', 'cloze', 'definition')
 * - 'qa', 'AI_GENERATED' 등 레거시 값은 서비스 레이어에서 정규화된 후 저장됨
 * - DB에 직접 레거시 값을 넣으면 제약 위반
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("[E2E][Constraint] V20 card_type 정규화 — PostgreSQL 제약 검증")
class AiGeneratedCardConstraintTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    CardDeckJpaRepository cardDeckJpaRepository;

    @Autowired
    FlashCardJpaRepository flashCardJpaRepository;

    private CardDeck createDeck(UUID tenantId, UUID userId) {
        return cardDeckJpaRepository.saveAndFlush(
                CardDeck.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .name("V20 제약 검증 덱")
                        .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"basic", "cloze", "definition"})
    @DisplayName("V20 이후 canonical 소문자 값(basic/cloze/definition)은 DB에 직접 저장된다")
    void canonicalCardType_저장된다(String cardType) {
        UUID tenantId = UUID.randomUUID();
        CardDeck deck = createDeck(tenantId, UUID.randomUUID());

        FlashCard card = FlashCard.builder()
                .deckId(deck.getId())
                .tenantId(tenantId)
                .cardType(cardType)
                .frontContent("앞면")
                .backContent("뒷면")
                .build();

        FlashCard saved = flashCardJpaRepository.saveAndFlush(card);
        assertThat(saved.getCardType()).isEqualTo(cardType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"qa", "AI_GENERATED", "BASIC", "CLOZE"})
    @DisplayName("V20 이후 레거시/대문자 값은 DB에 직접 저장하면 제약 위반 — 서비스 레이어 정규화 필수")
    void legacyCardType_직접저장_제약위반(String cardType) {
        UUID tenantId = UUID.randomUUID();
        CardDeck deck = createDeck(tenantId, UUID.randomUUID());

        FlashCard card = FlashCard.builder()
                .deckId(deck.getId())
                .tenantId(tenantId)
                .cardType(cardType)
                .frontContent("앞면")
                .backContent("뒷면")
                .build();

        assertThatThrownBy(() -> flashCardJpaRepository.saveAndFlush(card))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
