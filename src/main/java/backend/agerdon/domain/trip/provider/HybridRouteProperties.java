package backend.agerdon.domain.trip.provider;

import backend.agerdon.domain.trip.entity.RouteType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "trip.hybrid")
public class HybridRouteProperties {

    private List<Station> stations = new ArrayList<>();

    @Getter
    @Setter
    public static class Station {
        private String code;
        private String name;
        private String line;
        private double latitude;
        private double longitude;
        private int subwayFare;
        private DirectionSchedule eastbound;
        private DirectionSchedule westbound;
        private List<BusRoute> busRoutes = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class DirectionSchedule {
        private String label;
        private String terminal;
        private String lastTime;
    }

    @Getter
    @Setter
    public static class BusRoute {
        private RouteType type;
        private String routeNo;
        private String stopName;
        private double stopLatitude;
        private double stopLongitude;
        private String lastTime;
        private int rideMinutes;
        private int fare;
    }
}
