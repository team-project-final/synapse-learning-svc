package com.synapse.learning.srs.adapter.in.web;

import com.synapse.learning.srs.adapter.in.web.dto.DailyReviewStatResponse;
import com.synapse.learning.srs.adapter.in.web.dto.ReviewStatsResponse;
import com.synapse.learning.srs.adapter.in.web.dto.RetentionPointResponse;
import com.synapse.learning.srs.adapter.in.web.dto.RetentionStatsResponse;
import com.synapse.learning.srs.adapter.in.web.dto.WeeklyReviewStatResponse;
import com.synapse.learning.srs.adapter.in.web.dto.WeeklyStatsResponse;
import com.synapse.learning.srs.application.port.in.ReviewStatsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ReviewStatsControllerTest {

        @Autowired
        WebApplicationContext context;

        @MockitoBean
        ReviewStatsUseCase reviewStatsUseCase;

        MockMvc mockMvc;

        private static final String TENANT_ID = "00000000-0000-0000-0000-000000000099";
        private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        @DisplayName("GET /stats/overview returns 200")
        void getOverview_returns200() throws Exception {
                List<DailyReviewStatResponse> daily = List.of(
                                new DailyReviewStatResponse(LocalDate.now(), 10L, 80.0));
                given(reviewStatsUseCase.getOverview(any(), any()))
                                .willReturn(new ReviewStatsResponse(daily, 10L, 80.0, 3, 7));

                mockMvc.perform(get("/stats/overview")
                                .with(jwt())
                                .header("X-Tenant-Id", TENANT_ID)
                                .header("X-User-Id", USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.totalReviews").value(10))
                                .andExpect(jsonPath("$.data.overallCorrectRate").value(80.0))
                                .andExpect(jsonPath("$.data.daily[0].reviewCount").value(10));
        }

        @Test
        @DisplayName("GET /stats/heatmap returns 200")
        void getHeatmap_returns200() throws Exception {
                List<WeeklyReviewStatResponse> weekly = List.of(
                                new WeeklyReviewStatResponse(LocalDate.now().with(java.time.DayOfWeek.MONDAY), 20L,
                                                75.0));
                given(reviewStatsUseCase.getHeatmap(any(), any()))
                                .willReturn(new WeeklyStatsResponse(weekly));

                mockMvc.perform(get("/stats/heatmap")
                                .with(jwt())
                                .header("X-Tenant-Id", TENANT_ID)
                                .header("X-User-Id", USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.weekly[0].reviewCount").value(20))
                                .andExpect(jsonPath("$.data.weekly[0].correctRate").value(75.0));
        }

        @Test
        @DisplayName("GET /stats/retention returns 200")
        void getRetention_returns200() throws Exception {
                List<RetentionPointResponse> points = List.of(
                                new RetentionPointResponse(LocalDate.now(), 0, 4L, 75.0));
                given(reviewStatsUseCase.getRetention(any(), any()))
                                .willReturn(new RetentionStatsResponse(points));

                mockMvc.perform(get("/stats/retention")
                                .with(jwt())
                                .header("X-Tenant-Id", TENANT_ID)
                                .header("X-User-Id", USER_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.points[0].daysAgo").value(0))
                                .andExpect(jsonPath("$.data.points[0].reviewCount").value(4))
                                .andExpect(jsonPath("$.data.points[0].retentionRate").value(75.0));
        }

        @Test
        @DisplayName("GET /stats/overview returns 401 without JWT")
        void getOverview_noJwt_returns401() throws Exception {
                mockMvc.perform(get("/stats/overview")
                                .header("X-Tenant-Id", TENANT_ID)
                                .header("X-User-Id", USER_ID))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /stats/overview returns 400 without X-User-Id")
        void getOverview_missingHeader_returns400() throws Exception {
                mockMvc.perform(get("/stats/overview")
                                .with(jwt())
                                .header("X-Tenant-Id", TENANT_ID))
                                .andExpect(status().isBadRequest());
        }
}
