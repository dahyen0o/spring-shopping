package shopping.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestTemplate;
import shopping.dto.OrderResponse;
import shopping.integration.config.IntegrationTest;
import shopping.integration.util.AuthUtil;
import shopping.integration.util.CartUtil;
import shopping.integration.util.OrderUtil;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static shopping.integration.util.OrderUtil.ORDER_API_URL;

@IntegrationTest
@AutoConfigureMockRestServiceServer
class OrderIntegrationTest {

    private final MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper;
    private final String accessKey;

    @Autowired
    public OrderIntegrationTest(final RestTemplate restTemplate,
                                final ObjectMapper objectMapper,
                                @Value("${currency-layer.access-key}") final String accessKey) {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
        this.objectMapper = objectMapper;
        this.accessKey = accessKey;
    }

    @Test
    @DisplayName("주문 상세 페이지 접속 테스트.")
    void orderPage() {
        RestAssured.given().log().all()
                .contentType(MediaType.TEXT_HTML_VALUE)
                .when().get("/order-detail/1")
                .then().statusCode(HttpStatus.OK.value())
                .log().all();
    }

    @Test
    @DisplayName("사용자 주문 페이지 접속 테스트.")
    void userOrdersPage() {
        RestAssured.given().log().all()
                .contentType(MediaType.TEXT_HTML_VALUE)
                .when().get("/order-history")
                .then().statusCode(HttpStatus.OK.value())
                .log().all();
    }

    @Test
    @DisplayName("주문에 성공한다.")
    void createOrder() throws JsonProcessingException {
        // given
        final String accessToken = AuthUtil.accessToken();

        CartUtil.createCartItem(accessToken, 1L);
        CartUtil.createCartItem(accessToken, 1L);
        CartUtil.createCartItem(accessToken, 2L);

        final String mockResponseJson = objectMapper.writeValueAsString(mockSuccessResponse());
        getMockRequest(1).andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mockResponseJson));

        // when & then
        RestAssured
                .given().log().all()
                .auth().oauth2(accessToken)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().post(ORDER_API_URL)
                .then().log().all()
                .statusCode(HttpStatus.CREATED.value())
                .header("Location", ORDER_API_URL + "/1");
    }

    @Test
    @DisplayName("주문의 상세 정보를 조회한다.")
    void findOrder() throws JsonProcessingException {
        // given
        final String accessToken = AuthUtil.accessToken();

        CartUtil.createCartItem(accessToken, 1L);
        CartUtil.createCartItem(accessToken, 1L);
        CartUtil.createCartItem(accessToken, 2L);

        final String mockResponseJson = objectMapper.writeValueAsString(mockSuccessResponse());
        getMockRequest(1).andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mockResponseJson));

        final String url = OrderUtil.createOrder(accessToken).header("Location");

        // when
        final OrderResponse orderResponse = RestAssured
                .given().log().all()
                .auth().oauth2(accessToken)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().get(url)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract().as(OrderResponse.class);

        // then
        assertThat(orderResponse.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("존재하지 않는 주문의 상세 정보를 조회할 때 실패한다.")
    void findOrderNotExist() {
        // given
        final String accessToken = AuthUtil.accessToken();

        // when & then
        RestAssured
                .given().log().all()
                .auth().oauth2(accessToken)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().get(ORDER_API_URL + "/1")
                .then().log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("사용자의 주문 목록들을 조회한다.")
    void findOrdersByUser() throws JsonProcessingException {
        // given
        final String accessToken = AuthUtil.accessToken();

        final String mockResponseJson = objectMapper.writeValueAsString(mockSuccessResponse());
        getMockRequest(2).andRespond(withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mockResponseJson));

        CartUtil.createCartItem(accessToken, 1L);
        OrderUtil.createOrder(accessToken);

        CartUtil.createCartItem(accessToken, 2L);
        OrderUtil.createOrder(accessToken);

        // when
        final OrderResponse[] orderResponse = RestAssured
                .given().log().all()
                .auth().oauth2(accessToken)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().get(ORDER_API_URL)
                .then().log().all()
                .statusCode(HttpStatus.OK.value())
                .extract().as(OrderResponse[].class);

        // then
        assertThat(orderResponse).hasSize(2);
    }

    private static Map<String, Object> mockSuccessResponse() {
        final Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("quotes", Map.of("USDKRW", 1300.0));
        return response;
    }

    private ResponseActions getMockRequest(final int requestCount) {
        return mockServer
                .expect(ExpectedCount.times(requestCount), requestTo("http://apilayer.net/api/live" +
                        "?access_key=" + accessKey +
                        "&currencies=KRW" +
                        "&source=USD" +
                        "&format=1"));
    }
}
