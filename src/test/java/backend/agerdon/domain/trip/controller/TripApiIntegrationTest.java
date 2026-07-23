package backend.agerdon.domain.trip.controller;

import backend.agerdon.domain.member.entity.Member;
import backend.agerdon.domain.member.repository.MemberRepository;
import backend.agerdon.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TripApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String authorization;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(Member.builder()
                .email("api-trip@example.com")
                .password("encoded-password")
                .nickname("api-tester")
                .build());
        authorization = "Bearer " + jwtTokenProvider.createToken(member.getEmail());
    }

    @Test
    void requiresJwtAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/trips/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-401"));
    }

    @Test
    void createsTripAndReturnsTimerAndRoutes() throws Exception {
        mockMvc.perform(post("/api/v1/trips")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripId").isNumber())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.timer.state").value("RUNNING"))
                .andExpect(jsonPath("$.data.routes.length()").value(4))
                .andExpect(jsonPath("$.data.routes[0].recommended").value(true))
                .andExpect(jsonPath("$.data.routes[1].recommended").value(false))
                .andExpect(jsonPath("$.data.routes[2].recommended").value(false))
                .andExpect(jsonPath("$.data.routes[3].recommended").value(false));
    }

    @Test
    void acceptsCoordinatesOutsideConventionalLatitudeRange() throws Exception {
        String unrestrictedRequest = validCreateRequest().replace("37.5500", "127.0000");

        mockMvc.perform(post("/api/v1/trips")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unrestrictedRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submitsResultOnceAndRemovesTripFromCurrent() throws Exception {
        String responseBody = mockMvc.perform(post("/api/v1/trips")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);
        long tripId = response.path("data").path("tripId").asLong();

        mockMvc.perform(patch("/api/v1/trips/{tripId}/result", tripId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUCCESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/trips/{tripId}/result", tripId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"MISSED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP-003"));

        mockMvc.perform(get("/api/v1/trips/current")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private String validCreateRequest() {
        return """
                {
                  "origin": {
                    "name": "홍익대학교 T동",
                    "address": "서울특별시 마포구",
                    "latitude": 37.5500,
                    "longitude": 126.9200
                  },
                  "destination": {
                    "name": "집",
                    "address": "서울특별시",
                    "latitude": 37.5000,
                    "longitude": 127.0000
                  }
                }
                """;
    }
}
