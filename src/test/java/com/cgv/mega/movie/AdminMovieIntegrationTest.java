package com.cgv.mega.movie;

import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.dto.RegisterMovieRequest;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.entity.MovieDocument;
import com.cgv.mega.movie.enums.MovieStatus;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.movie.repository.MovieSearchRepository;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import com.cgv.mega.util.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.megacgv.com", uriPort = 443)
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
public class AdminMovieIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MovieSearchRepository movieSearchRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.startRedis();
        TestContainerManager.startElasticSearch();

        TestContainerManager.registerMySQL(registry);
        TestContainerManager.registerRedis(registry);
        TestContainerManager.registerElasticsearch(registry);
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

    @AfterEach
    void clear() {
        movieSearchRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class 영화_등록 {
        @Test
        @Commit
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

            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        List<MovieDocument> docs = StreamSupport.stream(movieSearchRepository.findAll().spliterator(), false)
                                .collect(Collectors.toList());
                        assertThat(docs).hasSize(1);
                        assertThat(docs.get(0).getTitle()).isEqualTo("인터스텔라");
                    });
        }

        @Test
        @Transactional
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
    class 영화_삭제 {
        @Test
        @Commit
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

            movieSearchRepository.save(MovieDocument.from(movie));

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

            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        List<MovieDocument> docs = StreamSupport.stream(
                                movieSearchRepository.findAll().spliterator(), false
                        ).collect(Collectors.toList());
                        assertThat(docs).isEmpty();
                    });
        }

        @Test
        @Transactional
        void 운영자가_아닌경우_삭제_실패_403반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/api/admin/movies/{movieId}", 1L)
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }

    @Nested
    @Transactional
    class 영화_목록_조회 {

        @BeforeEach
        void setUp() {
            Movie movie1 = testDataFactory.createMovie("혹성탈출");
            Movie movie2 = testDataFactory.createMovie("인터스텔라");

            Genre action = genreRepository.findById(1L)
                    .orElseThrow();

            Genre drama = genreRepository.findById(2L)
                    .orElseThrow();

            movie1.addGenre(action);
            movie1.addGenre(drama);
            movie1.addType(MovieType.TWO_D);
            movie1.addType(MovieType.THREE_D);

            movie2.addGenre(drama);
            movie2.addType(MovieType.TWO_D);
            movie2.addType(MovieType.THREE_D);

            movieSearchRepository.save(MovieDocument.from(movie1));
            movieSearchRepository.save(MovieDocument.from(movie2));
        }

        @Test
        void 영화_목록_조회() throws Exception {
            mockMvc.perform(get("/api/admin/movies")
                            .header("Authorization", adminToken)
                            .param("keyword", "혹성")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].title").value("혹성탈출"))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
                    .andDo(document("movie-list-search",
                            queryParameters(
                                    parameterWithName("keyword").description("조회할 영화 제목").optional(),
                                    parameterWithName("page").description("페이지 번호(기본 0)").optional(),
                                    parameterWithName("size").description("한 페이지 크기 (기본 10)").optional()
                            ),
                            requestHeaders(
                                    headerWithName("Authorization").description("JWT Access Token (Bearer)")
                            ),
                            responseFields(
                                    fieldWithPath("status").description("HTTP 응답 코드"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("data.content[].id").description("영화 ID"),
                                    fieldWithPath("data.content[].title").description("영화 제목"),
                                    fieldWithPath("data.content[].genres[]").description("장르 목록"),
                                    fieldWithPath("data.content[].types[]").description("상영 타입 목록"),
                                    fieldWithPath("data.content[].posterUrl").description("포스터 이미지 경로"),
                                    fieldWithPath("data.pageInfo.page").description("현재 페이지 번호"),
                                    fieldWithPath("data.pageInfo.size").description("페이지 크기"),
                                    fieldWithPath("data.pageInfo.totalElements").description("총 검색 결과 수"),
                                    fieldWithPath("data.pageInfo.totalPages").description("총 페이지 수"),
                                    fieldWithPath("data.pageInfo.last").description("마지막 페이지 여부")
                            )
                    ))
                    .andDo(print());
        }

        @Test
        void 영화_목록_조회_없음() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/movies")
                            .header("Authorization", adminToken)
                            .param("keyword", "히히히")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0))
                    .andDo(print());
        }

        @Test
        void 운영자가_아닌경우_조회_실패_403반환() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/movies")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden())
                    .andDo(print());
        }
    }

    @Test
    @Transactional
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

        mockMvc.perform(get("/api/admin/movies/{movieId}", movie.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("혹성탈출"))
                .andExpect(jsonPath("$.data.duration").value(150))
                .andExpect(jsonPath("$.data.genres", containsInAnyOrder("ACTION", "DRAMA")))
                .andExpect(jsonPath("$.data.types", containsInAnyOrder("2D", "3D")))
                .andDo(document("movie-info-admin",
                        requestHeaders(
                                headerWithName("Authorization").description("JWT Access Token (Bearer)")
                        ),
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