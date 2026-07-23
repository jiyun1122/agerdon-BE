package backend.agerdon.domain.metro.service;

import backend.agerdon.domain.metro.client.SeoulMetroClient;
import backend.agerdon.domain.metro.dto.response.MetroLastTrainResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetroServiceTest {

    @Test
    void readsModernTrainScheduleResponseFields() throws Exception {
        SeoulMetroClient client = mock(SeoulMetroClient.class);
        JsonNode rows = new ObjectMapper().readTree("""
                [
                  {
                    "trainno": "2246",
                    "stnNm": "홍대입구",
                    "lineNm": "2호선",
                    "trainDptreTm": "23:42:30",
                    "arvlStnNm": "성수"
                  },
                  {
                    "trainno": "2250",
                    "stnNm": "홍대입구",
                    "lineNm": "2호선",
                    "trainDptreTm": "00:08:00",
                    "arvlStnNm": "을지로입구"
                  },
                  {
                    "trainno": "2514",
                    "stnNm": "홍대입구",
                    "lineNm": "2호선",
                    "trainDptreTm": null,
                    "arvlStnNm": "홍대입구"
                  }
                ]
                """);
        when(client.getTimetableRows("239", 1, 1)).thenReturn(rows);
        MetroService service = new MetroService(client);

        MetroLastTrainResponse response = service.getLastTrain("239", 1, 1);

        assertEquals("홍대입구", response.getStationName());
        assertEquals("2호선", response.getLine());
        assertEquals(2, response.getLastTrains().size());
        assertEquals("00:08:00", response.getLastTrains().getLast().getDepartTime());
        assertEquals("을지로입구", response.getLastTrains().getLast().getDestination());
    }
}
