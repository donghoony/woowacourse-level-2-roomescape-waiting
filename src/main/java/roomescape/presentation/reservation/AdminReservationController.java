package roomescape.presentation.reservation;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import roomescape.application.reservation.ReservationBookingService;
import roomescape.application.reservation.ReservationService;
import roomescape.application.reservation.dto.request.ReservationFilterRequest;
import roomescape.application.reservation.dto.request.ReservationRequest;
import roomescape.application.reservation.dto.response.ReservationResponse;

@RestController
@RequestMapping("/admin/reservations")
public class AdminReservationController {
    private final ReservationService reservationService;
    private final ReservationBookingService reservationBookingService;

    public AdminReservationController(ReservationService reservationService,
                                      ReservationBookingService reservationBookingService) {
        this.reservationService = reservationService;
        this.reservationBookingService = reservationBookingService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@RequestBody @Valid ReservationRequest request) {
        ReservationResponse response = reservationBookingService.bookReservation(request);
        URI location = URI.create("/reservations/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> findByFilter(@RequestParam(required = false) Long memberId,
                                                                  @RequestParam(required = false) Long themeId,
                                                                  @RequestParam(required = false) LocalDate startDate,
                                                                  @RequestParam(required = false) LocalDate endDate) {
        ReservationFilterRequest request = new ReservationFilterRequest(memberId, themeId, startDate, endDate);
        List<ReservationResponse> responses = reservationService.findByFilter(request);
        return ResponseEntity.ok(responses);
    }
}
