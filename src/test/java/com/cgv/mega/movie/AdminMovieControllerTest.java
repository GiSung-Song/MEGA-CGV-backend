package com.cgv.mega.movie;

import com.cgv.mega.common.enums.MovieType;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.dto.RegisterMovieRequest;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieStatus;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.util.TestDataFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class AdminMovieControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private GenreRepository genreRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
    }

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        User user = testDataFactory.createUser("user", "a@b.com", "01012345678");
        User admin = testDataFactory.createUser("admin", "c@d.com", "01098765432");

        testDataFactory.setAdmin(admin);

        userToken = testDataFactory.setLogin(user);
        adminToken = testDataFactory.setLogin(admin);
    }

    @Nested
    class 영화_등록_테스트 {

        @Test
        void 운영자인_경우_등록_성공() throws Exception {
            RegisterMovieRequest request = new RegisterMovieRequest(
                    "인터스텔라", 150, "인터스텔라 설명", "poster.png",
                    Set.of(MovieType.TWO_D, MovieType.THREE_D), Set.of(1L, 2L, 3L));

            mockMvc.perform(post("/api/admin/movies")
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(document("movie-register",
                            requestHeaders(
                                    headerWithName("Authorization").description("JWT Access Token (Bearer)")
                            ),
                            requestFields(
                                    fieldWithPath("title").description("영화 제목"),
                                    fieldWithPath("duration").description("상영 시간 (분 단위)"),
                                    fieldWithPath("description").description("영화 설명"),
                                    fieldWithPath("posterUrl").description("영화 포스터 URL"),
                                    fieldWithPath("types[]").description("상영 타입 목록 (TWO_D, THREE_D)"),
                                    fieldWithPath("genreIds[]").description("영화 장르 ID 목록")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("응답 코드"),
                                    fieldWithPath("message").description("응답 메시지")
                            )
                    ))
                    .andDo(print());

            assertThat(movieRepository.findAll().size()).isEqualTo(1);
        }

        @Test
        void 운영자가_아닌경우_등록_실패_403반환() throws Exception {
            RegisterMovieRequest request = new RegisterMovieRequest(
                    "인터스텔라", 150, "인터스텔라 설명", "poster.png",
                    Set.of(MovieType.TWO_D, MovieType.THREE_D), Set.of(1L, 2L, 3L));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/movies")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }

    @Nested
    class 영화_삭제_테스트 {
        @Test
        void 운영자인_경우_삭제_성공() throws Exception {
            Movie movie = testDataFactory.createMovie("혹성탈출");

            Genre action = genreRepository.findById(1L)
                    .orElseThrow();

            Genre drama = genreRepository.findById(2L)
                    .orElseThrow();

            movie.addGenre(action);
            movie.addGenre(drama);
            movie.addType(MovieType.TWO_D);
            movie.addType(MovieType.THREE_D);

            mockMvc.perform(patch("/api/admin/movies/{movieId}", movie.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andDo(document("movie-delete",
                            pathParameters(
                                    parameterWithName("movieId").description("조회할 영화의 ID")
                            ),
                            requestHeaders(
                                    headerWithName("Authorization").description("JWT Access Token (Bearer)")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("응답 코드"),
                                    fieldWithPath("message").description("응답 메시지")
                            )
                    ))
                    .andDo(print());

            Movie deletedMovie = movieRepository.findById(movie.getId())
                    .orElseThrow();

            assertThat(deletedMovie.getStatus()).isEqualTo(MovieStatus.INACTIVE);
        }

        @Test
        void 운영자가_아닌경우_삭제_실패_403반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/movies/{movieId}", 1L)
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }
}
