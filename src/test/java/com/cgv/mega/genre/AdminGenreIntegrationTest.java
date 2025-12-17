package com.cgv.mega.genre;

import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AdminGenreIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    private String adminToken;

    @BeforeEach
    void setUp() {
        User admin = testDataFactory.createUser("admin", "c@d.com", "01098765432");
        testDataFactory.setAdmin(admin);
        adminToken = testDataFactory.setLogin(admin);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();
        TestContainerManager.startElasticSearch();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
        TestContainerManager.registerElasticsearch(registry);
    }

    @Test
    void 모든_장르_조회() throws Exception {
        mockMvc.perform(get("/api/admin/genres")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andDo(document("genre-list",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT Access Token (Bearer)")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.genreInfoList[].id").description("장르 식별자 ID"),
                                fieldWithPath("data.genreInfoList[].name").description("장르명")
                        )
                ))
                .andDo(print());
    }
}