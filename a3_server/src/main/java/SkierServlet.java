import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.swagger.client.model.LiftRide;
import io.swagger.client.model.SkierVertical;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SkierServlet extends javax.servlet.http.HttpServlet {

  private static final String QUEUE_NAME = "RabbitMQ for assignment 3";

  private static Connection connection;

  public void init() {
    // initialize the connection (this is the socket, so slow)
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(System.getProperty("DB_USERNAME"));
    factory.setPassword(System.getProperty("DB_PASSWORD"));
    factory.setVirtualHost("/"); // I think this is the default "virtual host"
    factory.setHost("ec2-52-91-14-108.compute-1.amazonaws.com"); // For example, something like ec2-x-y-z.compute.amazonaws.com
    factory.setPort(5672); // This is normally the default port that RabbitmQ grabs

    try {
      connection = factory.newConnection();

    } catch (TimeoutException | IOException e) {
      e.printStackTrace();
    }
  }

  private final LiftRideDao liftRideDao = new LiftRideDao();

  protected void doPost(HttpServletRequest request,
      HttpServletResponse response)
      throws javax.servlet.ServletException, IOException {
    response.setContentType("application/json;charset:utf-8");
    String urlPath = request.getPathInfo();

    Gson gson = new Gson();

    // check we have a valid URL!
    if (!isPostUrlValid(urlPath)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      InvalidReturnMessage message = new InvalidReturnMessage();
      message.setMessage("Url wrong, it's an invalid request.");
      response.getWriter().write(gson.toJson(message));
      return;
    }

    LiftRide liftRide = new Gson().fromJson(request.getReader(), LiftRide.class);
    String message = new Gson().toJson(liftRide);

    // Instead of writing the entry into DB, it send it to a RMQ channel
    // create a channel and use that to publish to RabbitMQ. Close it at end of the request.
    Channel channel = connection.createChannel();
    channel.basicQos(1);
    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
    try {
      channel.close();
    } catch (TimeoutException e) {
      e.printStackTrace();
    }

    response.setStatus(HttpServletResponse.SC_CREATED);
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json;charset:utf-8");
    String urlPath = req.getPathInfo();

    String[] urlParts = urlPath.split("/");
    Gson gson = new Gson();
    InvalidReturnMessage invalidReturnMessage;

    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);

      invalidReturnMessage = new InvalidReturnMessage();
      invalidReturnMessage.setMessage("Invalid request.");
      res.getWriter().write(gson.toJson(invalidReturnMessage));
      return;
    }

    if (isGetTotalVerticalForSpecifiedResortsUrlValid(urlPath)) {
      // /skiers/{skierID}/vertical
      String skierID = urlParts[1];
      String resortId = req.getParameterMap().get("resort")[0];

      SkierVertical skierVertical = liftRideDao.getSkierResortTotals(skierID, resortId);

      res.getWriter().write(gson.toJson(skierVertical));
      res.setStatus(HttpServletResponse.SC_OK);
    } else if (isGetTotalVerticalForTheDayUrlValid(urlPath)) {
      // /skiers/{resortID}/days/{dayID}/skiers/{skierID}
      String resortId = urlParts[1];
      String dayId = urlParts[3];
      String skierId = urlParts[5];

      SkierVertical skierVertical = liftRideDao.getSkierDayVertical(skierId, resortId, dayId);

      res.getWriter().write(gson.toJson(skierVertical));
      res.setStatus(HttpServletResponse.SC_OK);
    } else {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

      invalidReturnMessage = new InvalidReturnMessage();
      invalidReturnMessage.setMessage("Invalid request.");
      res.getWriter().write(gson.toJson(invalidReturnMessage));
    }
  }


  private boolean isGetTotalVerticalForTheDayUrlValid(String urlPath) {
    String[] urlParts = urlPath.split("/");

    // Check whether it is /{resortID}/days/{dayID}/skiers/{skierID}
    return (urlParts.length == 6 && urlParts[2].equals("days") && onlyDigits(urlParts[3])
        && urlParts[4]
        .equals("skiers") && onlyDigits(urlParts[5]));
  }

  private boolean isGetTotalVerticalForSpecifiedResortsUrlValid(String urlPath) {
    String[] urlParts = urlPath.split("/");

    // Check whether it is /{skierID}/vertical
    return (urlParts.length == 3 && urlParts[2].equals("vertical") && onlyDigits(urlParts[1]));
  }

  private boolean isPostUrlValid(String urlPath) {
    return urlPath != null && !urlPath.isEmpty() && urlPath.equals("/liftrides");
  }

  // Check if a string contains only digits
  private static boolean onlyDigits(String str) {
    int len = str.length();
    // Traverse the string from start to end
    for (int i = 0; i < len; i++) {
      if (!(str.charAt(i) >= '0' && str.charAt(i) <= '9')) {
        return false;
      }
    }
    return true;
  }
}
