package com.cgv.mega.screening.service;

import com.cgv.mega.common.enums.ErrorCode;
import com.cgv.mega.common.exception.CustomException;
import com.cgv.mega.movie.entity.Movie;
import com.cgv.mega.movie.repository.MovieRepository;
import com.cgv.mega.reservation.entity.ReservationGroup;
import com.cgv.mega.reservation.repository.ReservationGroupRepository;
import com.cgv.mega.reservation.service.ReservationService;
import com.cgv.mega.screening.dto.*;
import com.cgv.mega.screening.entity.Screening;
import com.cgv.mega.screening.enums.DisplayScreeningSeatStatus;
import com.cgv.mega.screening.enums.ScreeningSeatStatus;
import com.cgv.mega.screening.enums.ScreeningStatus;
import com.cgv.mega.screening.repository.ScreeningQueryRepository;
import com.cgv.mega.screening.repository.ScreeningRepository;
import com.cgv.mega.seat.entity.Seat;
import com.cgv.mega.seat.repository.SeatRepository;
import com.cgv.mega.theater.entity.Theater;
import com.cgv.mega.theater.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ReservationService reservationService;
    private final ScreeningRepository screeningRepository;
    private final ScreeningQueryRepository screeningQueryRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final SeatRepository seatRepository;
    private final ReservationGroupRepository reservationGroupRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final LocalTime THEATER_OPEN_TIME = LocalTime.of(5, 0);
    private static final LocalTime LAST_SCREENING_START_TIME = LocalTime.of(1, 0);
    private static final Duration CLEANING_TIME = Duration.ofMinutes(10);
    private static final int BASE_PRICE = 1000;

    // 상영 등록 가능 시간 조회(관리자용)
    @Transactional(readOnly = true)
    public AvailableScreeningResponse getAvailableScreeningTime(Long movieId, Long theaterId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_SCREENING_START_TIME);
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new CustomException(ErrorCode.MOVIE_NOT_FOUND));

        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new CustomException(ErrorCode.THEATER_NOT_FOUND));

        // 해당 상영관 및 날짜에 상영 예정 시간 목록 조회
        List<ScreeningTimeDto> reservedScreening =
                screeningQueryRepository.getReservedScreening(theaterId, date);

        final Duration totalDuration = Duration.ofMinutes(movie.getDuration()).plus(CLEANING_TIME); // 영화시간 + 청소시간(15분)
        final int intervalSlot = 10; // 10분 간격

        // 상영관 시작(5시) 및 마지막 상영 가능 시간(익일 1시)
        final LocalDateTime open = date.atTime(THEATER_OPEN_TIME);
        final LocalDateTime last = date.plusDays(1).atTime(LAST_SCREENING_START_TIME);

        List<LocalDateTime> candidates = new ArrayList<>();
        LocalDateTime prevEnd = open;

        // 오전 5시 ~ 예약된 마지막 상영 시간까지 10분마다 후보 추가
        for (ScreeningTimeDto st : reservedScreening) {
            LocalDateTime nextStart = st.startTime();

            while (prevEnd.plus(totalDuration).isBefore(nextStart)
                    && prevEnd.isBefore(last)) {
                candidates.add(prevEnd);
                prevEnd = prevEnd.plusMinutes(intervalSlot);
            }

            prevEnd = st.endTime();
        }

        // 예약된 마지막 상영시간 ~ 마지막 상영 가능 시간(익일 1시)까지 10분마다 후보 추가
        while (prevEnd.plus(totalDuration).isBefore(last)) {
            candidates.add(prevEnd);
            prevEnd = prevEnd.plusMinutes(intervalSlot);
        }

        return new AvailableScreeningResponse(candidates);
    }

    // 상영 추가(관리자용)
    @Transactional
    @CacheEvict(
            cacheNames = "screeningMovies",
            key = "'screeningMovies:' + #request.startTime.toLocalDate()"
    )
    public void registerScreening(RegisterScreeningRequest request) {
        // 시작 시간이 현재 시간보다 이전이면 throw
        if (request.startTime().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new CustomException(ErrorCode.INVALID_SCREENING_START_TIME);
        }

        // 영화 없으면 throw
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new CustomException(ErrorCode.MOVIE_NOT_FOUND));

        // 상영관 없으면 throw
        Theater theater = theaterRepository.findById(request.theaterId())
                .orElseThrow(() -> new CustomException(ErrorCode.THEATER_NOT_FOUND));

        // 상영 가능한 시간인지 체크
        LocalTime time = request.startTime().toLocalTime();

        boolean withInOpenDay = !time.isBefore(THEATER_OPEN_TIME);
        boolean withInNextDay = time.isBefore(LAST_SCREENING_START_TIME);

        if (!(withInOpenDay || withInNextDay)) {
            throw new CustomException(ErrorCode.INVALID_SCREENING_START_TIME);
        }

        LocalDateTime endTime = request.startTime()
                .plusMinutes(movie.getDuration())
                .plus(CLEANING_TIME);

        // 겹치는 상영회차가 있는지 조회
        boolean existsOverlap = screeningQueryRepository.existsOverlap(theater.getId(), request.startTime(), endTime);
        if (existsOverlap) {
            throw new CustomException(ErrorCode.DUPLICATE_THEATER_SCREENING_TIME);
        }

        // 해당 영화의 sequence 조회
        int sequence = screeningQueryRepository.getMovieSequence(movie.getId());

        // 상영 Entity 생성
        Screening screening = Screening.createScreening(
                movie, theater, request.startTime(), endTime, sequence, request.movieType()
        );

        // 상영 회차-좌석 생성
        Set<Seat> seats = seatRepository.findByTheaterId(theater.getId());

        if (seats.isEmpty()) {
            throw new CustomException(ErrorCode.SEAT_NOT_FOUND);
        }

        screening.initializeSeats(seats, BASE_PRICE);

        // 저장
        screeningRepository.save(screening);
    }

    // 상영 취소(관리자용)
    @Transactional
    public void cancelScreening(Long screeningId) {
        // 이미 상영 중 이거나 종료된 상영이면 삭제 불가
        Screening screening = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCREENING_NOT_FOUND));

        // 취소할 수 없는 상태 throw
        if (screening.getStatus() == ScreeningStatus.ENDED) {
            throw new CustomException(ErrorCode.SCREENING_CANCEL_NOT_ALLOWED);
        }

        // 해당 상영에 해당하는 예약 존재 하는지 체크
        List<ReservationGroup> reservationGroupList = reservationGroupRepository.findAllByScreeningId(screeningId);

        for (ReservationGroup reservationGroup : reservationGroupList) {
            reservationService.cancelReservationByScreeningCancel(reservationGroup);
        }

        screening.cancelScreening();
    }

    // 해당 날짜의 상영 영화 목록
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "screeningMovies",
            key = "'srceeningMovies:' + #date.toString()"
    )
    public ScreeningDateMovieResponse getScreeningMovies(LocalDate date) {
        List<ScreeningDateMovieResponse.MovieInfo> screeningMovieList = screeningQueryRepository.getScreeningMovieList(date);

        return new ScreeningDateMovieResponse(screeningMovieList);
    }

    // 특정 영화의 상영 목록
    @Transactional(readOnly = true)
    public MovieScreeningResponse getMovieScreeningsForUser(Long movieId, LocalDate date) {
        // 해당 날짜 및 영화의 상영 목록 조회 - 잔여 좌석 포함
        List<MovieScreeningInfoDto> infoDtoList = screeningQueryRepository.getMovieScreeningListForUser(movieId, date);

        return MovieScreeningResponse.from(infoDtoList);
    }

    // 특정 영화의 상영 목록 (관리자용)
    @Transactional(readOnly = true)
    public MovieScreeningForAdminResponse getMovieScreeningsForAdmin(Long movieId) {
        // 해당 영화의 상영 목록 조회 - 잔여 좌석 포함
        List<MovieScreeningForAdminResponse.MovieScreeningInfo> movieScreeningList = screeningQueryRepository.getMovieScreeningListForAdmin(movieId);

        return new MovieScreeningForAdminResponse(movieScreeningList);
    }

    // 해당 상영회차의 좌석 현황 조회
    @Transactional(readOnly = true)
    public ScreeningSeatResponse getScreeningSeatStatus(Long screeningId) {
        List<ScreeningSeatDto> rows = screeningQueryRepository.getScreeningSeat(screeningId);

        if (rows.isEmpty()) {
            throw new CustomException(ErrorCode.SEAT_NOT_FOUND);
        }

        List<String> keys = rows.stream()
                .map(r -> "seat:" + r.screeningSeatId())
                .toList();

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Boolean> holdMap = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            Long screeningSeatId = rows.get(i).screeningSeatId();
            boolean isHold = values.get(i) != null;
            holdMap.put(screeningSeatId, isHold);
        }

        List<ScreeningSeatResponse.ScreeningSeatInfo> screeningSeatInfos = rows.stream()
                .map(r -> {
                    DisplayScreeningSeatStatus status;

                    if (r.status() == ScreeningSeatStatus.RESERVED) {
                        status = DisplayScreeningSeatStatus.RESERVED;

                    } else if (r.status() == ScreeningSeatStatus.FIXING) {
                        status = DisplayScreeningSeatStatus.FIXING;

                    } else if (holdMap.get(r.screeningSeatId())) {
                        status = DisplayScreeningSeatStatus.HOLD;

                    } else {
                        status = DisplayScreeningSeatStatus.AVAILABLE;
                    }

                    return new ScreeningSeatResponse.ScreeningSeatInfo(
                            r.screeningSeatId(),
                            r.rowLabel(),
                            r.colNumber(),
                            r.seatType(),
                            status,
                            r.price()
                    );
                })
                .toList();

        return new ScreeningSeatResponse(screeningId, screeningSeatInfos);
    }

    @Transactional
    public void endPastScreenings() {
        LocalDateTime now = LocalDateTime.now();
        ScreeningStatus ended = ScreeningStatus.ENDED;
        ScreeningStatus scheduled = ScreeningStatus.SCHEDULED;

        screeningRepository.updateStatusToEnded(now, ended, scheduled);
    }
}