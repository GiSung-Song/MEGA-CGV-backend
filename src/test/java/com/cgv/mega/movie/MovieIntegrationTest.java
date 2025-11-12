package com.cgv.mega.movie;

import com.cgv.mega.common.enums.MovieType;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.util.TestDataFactory;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MovieIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startElasticSearch();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerElasticsearch(registry);
    }

    @Test
    void 영화_상세_조회() throws Exception {
        Movie movie = testDataFactory.createMovie("혹성탈출");

        Genre action = genreRepository.findById(1L)
                .orElseThrow();

        Genre drama = genreRepository.findById(2L)
                .orElseThrow();

        movie.addGenre(action);
        movie.addGenre(drama);
        movie.addType(MovieType.TWO_D);
        movie.addType(MovieType.THREE_D);

        mockMvc.perform(get("/api/movies/{movieId}", movie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("혹성탈출"))
                .andExpect(jsonPath("$.data.duration").value(150))
                .andExpect(jsonPath("$.data.genres", containsInAnyOrder("ACTION", "DRAMA")))
                .andExpect(jsonPath("$.data.types", containsInAnyOrder("2D", "3D")))
                .andDo(document("movie-info",
                        pathParameters(
                                parameterWithName("movieId").description("조회할 영화의 ID")
                        ),
                        responseFields(
                                fieldWithPath("status").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.title").description("영화 제목"),
                                fieldWithPath("data.duration").description("상영 시간 (분 단위)"),
                                fieldWithPath("data.description").description("영화 설명"),
                                fieldWithPath("data.posterUrl").description("영화 포스터 URL"),
                                fieldWithPath("data.genres[]").description("영화 장르 목록(액션, 드라마 등)"),
                                fieldWithPath("data.types[]").description("상영 타입 목록 (2D, 3D 등)")
                        )
                ))
                .andDo(print());
    }
}
