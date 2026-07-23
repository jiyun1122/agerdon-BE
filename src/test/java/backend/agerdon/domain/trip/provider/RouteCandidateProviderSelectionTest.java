package backend.agerdon.domain.trip.provider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(properties = "trip.route-provider=real")
class RouteCandidateProviderSelectionTest {

    @Autowired
    private RouteCandidateProvider routeCandidateProvider;

    @Test
    void selectsRealProviderFromProperty() {
        assertInstanceOf(RealRouteCandidateProvider.class, routeCandidateProvider);
    }
}
