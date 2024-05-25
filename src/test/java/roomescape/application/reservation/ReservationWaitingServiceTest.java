package roomescape.application.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static roomescape.fixture.MemberFixture.MEMBER_ARU;
import static roomescape.fixture.MemberFixture.MEMBER_PK;
import static roomescape.fixture.ThemeFixture.TEST_THEME;
import static roomescape.fixture.TimeFixture.TWELVE_PM;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import roomescape.application.ServiceTest;
import roomescape.application.reservation.dto.request.ReservationRequest;
import roomescape.domain.member.Member;
import roomescape.domain.member.MemberRepository;
import roomescape.domain.reservation.BookStatus;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationRepository;
import roomescape.domain.reservation.ReservationTime;
import roomescape.domain.reservation.ReservationTimeRepository;
import roomescape.domain.reservation.Theme;
import roomescape.domain.reservation.ThemeRepository;
import roomescape.exception.reservation.DuplicatedReservationException;
import roomescape.exception.reservation.WaitingListExceededException;
import roomescape.fixture.MemberFixture;

@ServiceTest
class ReservationWaitingServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationTimeRepository reservationTimeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("예약을 대기한다.")
    void queueWaitList() {
        Theme theme = themeRepository.save(new Theme("테마 1", "desc", "url"));
        LocalDate date = LocalDate.parse("2023-01-01");
        ReservationTime time = reservationTimeRepository.save(new ReservationTime(LocalTime.of(12, 0)));
        Member member = memberRepository.save(MEMBER_ARU.create());
        ReservationRequest request = new ReservationRequest(member.getId(), date, time.getId(), theme.getId());

        reservationService.enqueueWaitingList(request);

        Optional<Reservation> firstWaiting = reservationRepository.findFirstWaiting(theme, date, time);
        assertThat(firstWaiting).isPresent()
                .get()
                .extracting(Reservation::getMember)
                .isEqualTo(member);
    }

    @Test
    @DisplayName("예약 대기를 취소하면, 다음 대기자가 예약이 확정된다.")
    void cancelWaitList() {
        Theme theme = themeRepository.save(new Theme("테마 1", "desc", "url"));
        LocalDate date = LocalDate.parse("2023-01-01");
        ReservationTime time = reservationTimeRepository.save(new ReservationTime(LocalTime.of(12, 0)));
        Member aru = memberRepository.save(MEMBER_ARU.create());
        Member pk = memberRepository.save(MEMBER_PK.create());
        Reservation firstWaiting = new Reservation(aru, date, time, theme,
                LocalDateTime.parse("1999-01-01T00:00:00"), BookStatus.WAITING);
        Reservation nextWaiting = new Reservation(pk, date, time, theme,
                LocalDateTime.parse("1999-01-03T00:00:00"), BookStatus.WAITING);
        long firstId = reservationRepository.save(firstWaiting)
                .getId();
        long secondId = reservationRepository.save(nextWaiting)
                .getId();

        reservationService.cancelWaitingList(aru.getId(), firstId);

        Reservation reservation = reservationRepository.getById(secondId);
        assertThat(reservation.isBooked()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = BookStatus.class, names = {"BOOKED", "WAITING"})
    @DisplayName("이미 예약된 항목의 경우, 예약 대기를 시도할 경우 예외가 발생한다.")
    void alreadyBookedOnQueueing(BookStatus status) {
        Theme theme = themeRepository.save(new Theme("테마 1", "desc", "url"));
        LocalDate date = LocalDate.parse("2023-01-01");
        ReservationTime time = reservationTimeRepository.save(new ReservationTime(LocalTime.of(12, 0)));
        Member member = memberRepository.save(MEMBER_ARU.create());
        ReservationRequest request = new ReservationRequest(member.getId(), date, time.getId(), theme.getId());
        reservationRepository.save(
                new Reservation(
                        member, date, time, theme, LocalDateTime.parse("1999-01-01T00:00:00"), status
                )
        );

        assertThatCode(() -> reservationService.enqueueWaitingList(request))
                .isInstanceOf(DuplicatedReservationException.class);
    }

    @Test
    @DisplayName("대기 인원이 가득 찼을 때 대기 요청하는 경우 예외를 발생한다.")
    void fullWaitingList() {
        Theme theme = themeRepository.save(TEST_THEME.create());
        ReservationTime time = reservationTimeRepository.save(TWELVE_PM.create());
        Member member = memberRepository.save(MEMBER_ARU.create());
        Member pk = memberRepository.save(MEMBER_PK.create());
        LocalDate date = LocalDate.parse("2023-01-01");
        ReservationRequest request = new ReservationRequest(member.getId(), date, time.getId(), theme.getId());
        reservationRepository.save(
                new Reservation(pk, date, time, theme, LocalDateTime.of(1999, 1, 1, 12, 0), BookStatus.BOOKED)
        );
        for (int count = 1; count <= 5; count++) {
            Member m = memberRepository.save(MemberFixture.createMember("name" + count));
            reservationRepository.save(
                    new Reservation(m, date, time, theme, LocalDateTime.of(1999, 1, 1, 12, 0), BookStatus.WAITING)
            );
        }

        assertThatThrownBy(() -> reservationService.enqueueWaitingList(request))
                .isInstanceOf(WaitingListExceededException.class);
    }
}
