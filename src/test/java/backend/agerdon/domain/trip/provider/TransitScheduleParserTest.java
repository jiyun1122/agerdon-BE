package backend.agerdon.domain.trip.provider;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitScheduleParserTest {

    @Test
    void resolvesAfterMidnightTimeToNextDateWhenRequestedAtNight() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 23, 23, 0);

        LocalDateTime parsed = TransitScheduleParser.parse("00:36:00", requestedAt).orElseThrow();

        assertEquals(LocalDateTime.of(2026, 7, 24, 0, 36), parsed);
    }

    @Test
    void keepsAfterMidnightTimeOnCurrentDateWhenRequestedAfterMidnight() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 7, 24, 0, 40);

        LocalDateTime parsed = TransitScheduleParser.parse("003600", requestedAt).orElseThrow();

        assertEquals(LocalDateTime.of(2026, 7, 24, 0, 36), parsed);
    }

    @Test
    void parsesBusFullDateTimeFormat() {
        LocalDateTime parsed = TransitScheduleParser.parse(
                "20260724032000",
                LocalDateTime.of(2026, 7, 23, 23, 0)
        ).orElseThrow();

        assertEquals(LocalDateTime.of(2026, 7, 24, 3, 20), parsed);
    }

    @Test
    void rejectsUnknownTimeFormat() {
        assertTrue(TransitScheduleParser.parse(
                "곧 도착",
                LocalDateTime.of(2026, 7, 23, 23, 0)
        ).isEmpty());
    }
}
