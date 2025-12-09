package com.cgv.mega.reservation.repository;

import com.cgv.mega.common.config.JpaConfig;
import com.cgv.mega.common.config.QueryDslConfig;
import com.cgv.mega.containers.TestContainerManager;
import com.cgv.mega.genre.entity.Genre;
import com.cgv.mega.genre.repository.GenreRepository;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.enums.MovieType;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.payment.entity.Payment;
import com.cgv.mega.payment.repository.PaymentRepository;
import com.cgv.mega.reservation.dto.ReservationDetailDto;
import com.cgv.mega.reservation.dto.ReservationListDto;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.entity.ScreeningSeat;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.screening.repository.ScreeningSeatRepository;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.seat.repository.SeatRepository;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.repository.TheaterRepository;
import com.cgv.mega.user.entity.User;
import com.cgv.mega.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, ReservationQueryRepository.class, QueryDslConfig.class})
@ActiveProfiles("test")
class ReservationRepositoryTest {

    @Autowired
    private ReservationQueryRepository reservationQueryRepository;

    @Autowired
    private ReservationGroupRepository reservationGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScreeningSeatRepository screeningSeatRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        TestContainerManager.registerMySQL(registry);
    }

    private User user;
    private Movie movie;
    private Screening screening;
    private ReservationGroup reservationGroup1;
    private ReservationGroup reservationGroup2;
    private Payment payment1;
    private Payment payment2;

    @BeforeEach
    void setUp() {
        user = User.createUser("테스터", "a@b.com", "encodedPassword", "01012345678");
        userRepository.save(user);

        movie = Movie.createMovie("인터스텔라", 150, "인터스텔라 설명", "인터스텔라.png");

        Genre action = genreRepository.findById(1L)
                .orElseThrow();

        Genre drama = genreRepository.findById(2L)
                .orElseThrow();

        movie.addGenre(action);
        movie.addGenre(drama);
        movie.addType(MovieType.TWO_D);
        movie.addType(MovieType.THREE_D);

        movieRepository.save(movie);

        Theater theater = theaterRepository.findById(1L)
                .orElseThrow();

        Set<Seat> seats = seatRepository.findByTheaterId(theater.getId());

        LocalDateTime startTime1 = LocalDateTime.of(2026, 11, 11, 6, 00);
        LocalDateTime endTime1 = startTime1.plusMinutes(movie.getDuration()).plusMinutes(15);

        screening = Screening.createScreening(movie, theater, startTime1, endTime1, 1, MovieType.TWO_D);
        screening.initializeSeats(seats, 1000);

        screeningRepository.save(screening);

        Iterator<Seat> iterator = seats.iterator();

        ScreeningSeat screeningSeat1 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), iterator.next().getId()).get();
        ScreeningSeat screeningSeat2 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), iterator.next().getId()).get();

        reservationGroup1 = ReservationGroup.createReservationGroup(user.getId());
        reservationGroup1.addReservation(screeningSeat1);
        reservationGroup1.addReservation(screeningSeat2);

        ScreeningSeat screeningSeat3 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), iterator.next().getId()).get();
        ScreeningSeat screeningSeat4 = screeningSeatRepository.findByScreeningIdAndSeatId(screening.getId(), iterator.next().getId()).get();

        reservationGroup2 = ReservationGroup.createReservationGroup(user.getId());
        reservationGroup2.addReservation(screeningSeat3);
        reservationGroup2.addReservation(screeningSeat4);

        reservationGroupRepository.saveAll(List.of(reservationGroup1, reservationGroup2));

        payment1 = Payment.createPayment(
                reservationGroup1, user.getName(), user.getPhoneNumber(), user.getEmail(),
                "merchant-uid-1", BigDecimal.valueOf(500.00));

        payment2 = Payment.createPayment(
                reservationGroup2, user.getName(), user.getPhoneNumber(), user.getEmail(),
                "merchant-uid-2", BigDecimal.valueOf(1000.00));

        paymentRepository.saveAll(List.of(payment1, payment2));
    }

    @Test
    void 예약_상세_조회() {
        ReservationDetailDto reservationDetail = reservationQueryRepository.getReservationDetail(
                user.getId(), reservationGroup1.getId()
        );

        assertThat(reservationDetail.buyerEmail()).isEqualTo(user.getEmail());
        assertThat(reservationDetail.title()).isEqualTo(movie.getTitle());
        assertThat(reservationDetail.merchantUid()).isEqualTo(payment1.getMerchantUid());
    }

    @Test
    void 예약_목록_조회() {
        Page<ReservationListDto> reservationList = reservationQueryRepository.getReservationList(
                user.getId(), PageRequest.of(0, 5)
        );

        assertThat(reservationList.getTotalPages()).isEqualTo(1);
        assertThat(reservationList.getTotalElements()).isEqualTo(2);

        assertThat(reservationList.getContent().get(0).title()).isEqualTo(movie.getTitle());
        assertThat(reservationList.getContent().get(1).title()).isEqualTo(movie.getTitle());
    }
}