package roomescape.infrastructure.reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationRepository;

public interface ReservationJpaRepository extends
        ReservationRepository,
        ListCrudRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation> {

    @Override
    List<Reservation> findAllByMemberId(Long id);

    @Override
    boolean existsByTimeId(long timeId);

    @Override
    boolean existsByThemeId(long themeId);

    @Override
    boolean existsByDateAndTimeIdAndThemeId(LocalDate date, long timeId, long themeId);

    @Override
    List<Reservation> findAll(Specification<Reservation> specification);

    @Override
    default Reservation getById(long id) {
        return findById(id).orElseThrow(() -> new NoSuchElementException("존재하지 않는 예약입니다."));
    }
}