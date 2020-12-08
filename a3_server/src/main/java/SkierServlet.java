import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import io.swagger.client.model.LiftRide;
import io.swagger.client.model.SkierVertical;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class SkierServlet extends javax.servlet.http.HttpServlet {

  private static final String QUEUE_NAME = "RabbitMQ for assignment 3";

  private final LiftRideDao liftRideDao = new LiftRideDao();

  private ObjectPool<Channel> pool;


  public SkierServlet() {
    pool = new GenericObjectPool<Channel>(new RMQChannelFactory());
    if (System.getProperty("MaxTotalPoolSize") != null) {
      ((GenericObjectPool<Channel>) pool).setMaxTotal(Integer.parseInt(System.getProperty("MaxTotalPoolSize")));
    }
  }

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
    Channel channel = null;
    try {
      channel = pool.borrowObject();
      if (System.getProperty("IsPersistentPublish") != null && System
          .getProperty("IsPersistentPublish").equals("true")) {
        channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
      } else {
        channel.basicPublish("", QUEUE_NAME, MessageProperties.TEXT_PLAIN, message.getBytes());
      }
      pool.returnObject(channel);
    } catch (Exception e) {
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
