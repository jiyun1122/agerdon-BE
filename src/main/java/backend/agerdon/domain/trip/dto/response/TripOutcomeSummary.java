package backend.agerdon.domain.trip.dto.response;

public record TripOutcomeSummary(long successCount, long missedCount) {

    public long totalCount() {
        return successCount + missedCount;
    }
}
