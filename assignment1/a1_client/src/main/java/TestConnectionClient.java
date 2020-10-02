import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.ResortsApi;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import io.swagger.client.model.SkierVertical;
import io.swagger.client.model.TopTen;
import java.util.Arrays;
import java.util.List;

public class TestConnectionClient {
  public static void main(String[] args) {
    SkiersApi skiersApi = new SkiersApi();

//    skiersApi.getApiClient().setBasePath("http://localhost:8080/CS6650_A1_Server_war_exploded");
    skiersApi.getApiClient().setBasePath("http://ec2-18-208-192-60.compute-1.amazonaws.com:8080/a1_server_war");

    List<String> resort = Arrays.asList("resort1");
    String dayID = "dayID";

    String resortID = "resortID";
    String skierID = "skierId";
    ApiResponse<SkierVertical> response2 = null;
    try {
      response2 = skiersApi.getSkierDayVerticalWithHttpInfo(resortID, dayID, skierID);
    } catch (ApiException e) {
      e.printStackTrace();
    }
    System.out.println(response2.getStatusCode());

    ApiResponse<SkierVertical> response3 = null;
    try {
      response3 = skiersApi.getSkierResortTotalsWithHttpInfo(skierID, resort);
    } catch (ApiException e) {
      e.printStackTrace();
    }
    System.out.println(response3.getStatusCode());

    LiftRide body = new LiftRide();
    try {
      skiersApi.writeNewLiftRide(body);
    } catch (ApiException e) {
      e.printStackTrace();
    }
  }

}
