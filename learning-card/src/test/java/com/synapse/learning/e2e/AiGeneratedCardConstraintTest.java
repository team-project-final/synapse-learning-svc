package com.synapse.learning.e2e;

import com.synapse.learning.card.adapter.out.persistence.CardDeckJpaRepository;
import com.synapse.learning.card.adapter.out.persistence.FlashCardJpaRepository;
import com.synapse.learning.card.domain.model.CardDeck;
import com.synapse.learning.card.domain.model.FlashCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flyway V18 검증 — 실제 PostgreSQL 제약에서 AI_GENERATED card_type 저장 가능 여부 확인
 *
 * H2 테스트는 Flyway가 아닌 Hibernate DDL로 스키마를 생성하므로 CHECK 제약이 없어
 * AI_GENERATED가 통과되지만, 실제 PostgreSQL은 V9의 chk_cards_card_type 제약이 적용된다.
 * V18 마이그레이션이 해당 제약을 AI_GENERATED 포함으로 확장함을 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("[E2E][Constraint] V18 AI_GENERATED card_type — PostgreSQL 제약 검증")
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
                        .name("V18 제약 검증 덱")
                        .build());
    }

    @Test
    @DisplayName("V18 마이그레이션 후 AI_GENERATED card_type 카드가 PostgreSQL 제약 없이 저장된다")
    void aiGeneratedCardType_실제PostgreSQL제약에서_저장된다() {
        UUID tenantId = UUID.randomUUID();
        UUID userId   = UUID.randomUUID();
        CardDeck deck = createDeck(tenantId, userId);

        FlashCard card = FlashCard.builder()
                .deckId(deck.getId())
                .tenantId(tenantId)
                .cardType("AI_GENERATED")
                .frontContent("스택(Stack)이란?")
                .backContent("LIFO(Last In First Out) 구조의 자료구조")
                .build();

        FlashCard saved = flashCardJpaRepository.saveAndFlush(card);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCardType()).isEqualTo("AI_GENERATED");
    }

    @Test
    @DisplayName("기존 card_type(qa, cloze, definition)도 V18 마이그레이션 후 정상 저장된다 (회귀)")
    void 기존_cardType_회귀_저장된다() {
        UUID tenantId = UUID.randomUUID();
        UUID userId   = UUID.randomUUID();
        CardDeck deck = createDeck(tenantId, userId);

        for (String cardType : new String[]{"qa", "cloze", "definition"}) {
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
    }
}
