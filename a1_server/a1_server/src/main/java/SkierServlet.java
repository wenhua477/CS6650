import com.google.gson.Gson;
import io.swagger.client.model.LiftRide;
import io.swagger.client.model.SkierVertical;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SkierServlet extends javax.servlet.http.HttpServlet {


  private static InitialContext ctx = null;
  //  private static final DataSource ds = null;
  private static Connection conn = null;
  private static PreparedStatement ps = null;
  private static ResultSet rs = null;

  private static final String driver = "com.mysql.cj.jdbc.Driver";
  private static final String url =
      "jdbc:mysql://database-2.cehjlxxknfu2.us-east-1.rds.amazonaws.com:3306/";
  private static final String dbName = "db_for_a2";
  private static final String userName = "wenhua";
  private static final String password = "12345678";


  private static final String INSERT_NEW_LIFTRIDE_SQL =
      "INSERT INTO LiftRides (liftRideID, resortID, dayID, vertical, skierID, time) values (?, ?, ?, ?, ?, ?);";
  private static final String GET_SKIER_RESORT_TOTALS_SQL =
      "SELECT IFNULL(SUM(vertical), 0) skier_resort_totals FROM LiftRides where skierID =? AND resortID=?;";
  private static final String GET_SKIER_DAY_VERTICAL_SQL =
      "SELECT IFNULL(sum(vertical), 0) skier_day_vertical FROM LiftRides where skierID =? AND resortID=? AND dayID=?;";

  public void init() throws ServletException {
    try {
      Class.forName(driver);
    } catch (ClassNotFoundException e) {
      //TODO logger logout the exception
      e.printStackTrace();
    }

    try {
      conn = DriverManager.getConnection(url + dbName, userName, password);
    } catch (SQLException e) {
      // TODo log exception out
      e.printStackTrace();
    }
  }

  public void destroy() {
    try {
      if (rs != null) {
        rs.close();
      }
      if (ps != null) {
        ps.close();
      }
      if (conn != null) {
        conn.close();
      }
      if (ctx != null) {
        ctx.close();
      }
    } catch (SQLException se) {
      // TODO logger
      System.out.println("SQLException: " + se.getMessage());
    } catch (NamingException ne) {
      // TODO logger
      System.out.println("NamingException: " + ne.getMessage());
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
    try {
      ps = conn.prepareStatement(INSERT_NEW_LIFTRIDE_SQL);

      ps.setString(1, UUID.randomUUID().toString().replace("-", ""));
      ps.setString(2, liftRide.getResortID());
      ps.setString(3, liftRide.getDayID());
      ps.setInt(4, Integer.parseInt(liftRide.getLiftID()) * 10);
      ps.setString(5, liftRide.getSkierID());
      ps.setString(6, liftRide.getTime());

      ps.executeUpdate();
    } catch (SQLException e) {
      //todo log
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
    System.out.println(urlPath);

    if (urlPath == null || urlPath.isEmpty()) {
      System.out.println(" here?");
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);

      invalidReturnMessage = new InvalidReturnMessage();
      invalidReturnMessage.setMessage("Invalid request.");
      res.getWriter().write(gson.toJson(invalidReturnMessage));
      return;
    }

    if (isGetTotalVerticalForSpecifiedResortsUrlValid(urlPath)) {
      // /skiers/{skierID}/vertical
      res.setStatus(HttpServletResponse.SC_OK);

      SkierVertical skierVertical = new SkierVertical();
      String resortId = req.getParameterMap().get("resort")[0];
      skierVertical.setResortID(resortId);
      skierVertical.setTotalVert(1000000);
      res.getWriter().write(gson.toJson(skierVertical));
    } else if (isGetTotalVerticalForTheDayUrlValid(urlPath)) {
      // /skiers/{resortID}/days/{dayID}/skiers/{skierID}
      res.setStatus(HttpServletResponse.SC_OK);

      SkierVertical skierVertical = new SkierVertical();
      String resortId = urlParts[1];
      String dayId = urlParts[3];
      String skierId = urlParts[5];
      try {
        ps = conn.prepareStatement(GET_SKIER_DAY_VERTICAL_SQL);

        ps.setString(1, skierId);
        ps.setString(2, resortId);
        ps.setString(3, dayId);

        rs = ps.executeQuery();
        if (!rs.next()) {
          // TODo logout somethings wrong in db
        } else {
          rs.first();
          skierVertical.setTotalVert(rs.getInt("skier_day_vertical"));
        }
      } catch (SQLException e) {
        // TODO log
        e.printStackTrace();
      }

      skierVertical.setResortID(resortId);

      res.getWriter().write(gson.toJson(skierVertical));
    } else {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);

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
