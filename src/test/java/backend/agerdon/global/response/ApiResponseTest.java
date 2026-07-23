package backend.agerdon.global.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    @Test
    void successCreatesDefaultSuccessResponse() {
        String data = "response data";

        ApiResponse<String> response = ApiResponse.success(data);

        assertTrue(response.isSuccess());
        assertEquals("SUCCESS", response.getCode());
        assertEquals("요청에 성공했습니다.", response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
    }
}
