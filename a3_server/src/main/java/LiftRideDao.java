import io.swagger.client.model.LiftRide;
import io.swagger.client.model.SkierVertical;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LiftRideDao {

  private static final Logger logger = LogManager.getLogger(SkierServlet.class);

  private static BasicDataSource dataSource;
  private static final String INSERT_NEW_LIFTRIDE_SQL =
      "INSERT INTO LiftRides (liftRideID, resortID, dayID, vertical, skierID, time) values (?, ?, ?, ?, ?, ?);";
  private static final String GET_SKIER_RESORT_TOTALS_SQL =
      "SELECT IFNULL(SUM(vertical), 0) skier_resort_totals FROM LiftRides where skierID =? AND resortID=?;";
  private static final String GET_SKIER_DAY_VERTICAL_SQL =
      "SELECT IFNULL(sum(vertical), 0) skier_day_vertical FROM LiftRides where skierID =? AND resortID=? AND dayID=?;";

  public LiftRideDao() {
    dataSource = DBCPDataSource.getDataSource();
  }

  public void createLiftRide(LiftRide liftRide) {
    Connection conn = null;
    PreparedStatement ps = null;
    try {
      conn = dataSource.getConnection();
      ps = conn.prepareStatement(INSERT_NEW_LIFTRIDE_SQL);

      String primaryKey = UUID.randomUUID().toString();
      ps.setString(1, primaryKey);
      ps.setString(2, liftRide.getResortID());
      ps.setString(3, liftRide.getDayID());
      ps.setInt(4, Integer.parseInt(liftRide.getLiftID()) * 10);
      ps.setString(5, liftRide.getSkierID());
      ps.setString(6, liftRide.getTime());

      // execute insert SQL statement
      ps.executeUpdate();
    } catch (SQLException e) {
      logger.error(e);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (SQLException se) {
        logger.error(se);
      }
    }
  }

  public SkierVertical getSkierResortTotals(String skierId, String resortId) {
    SkierVertical skierVertical = new SkierVertical();
    Connection conn = null;
    PreparedStatement ps = null;
    try {
      conn = dataSource.getConnection();
      ps = conn.prepareStatement(GET_SKIER_RESORT_TOTALS_SQL);

      ps.setString(1, skierId);
      ps.setString(2, resortId);

      ResultSet rs = ps.executeQuery();

      if (!rs.next()) {
        logger.error("No rs.next().");
      }
      {
        rs.first();
        skierVertical.setTotalVert(rs.getInt("skier_resort_totals"));
      }
    } catch (SQLException e) {
      logger.error(e);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (SQLException se) {
        logger.error(se);
      }
    }

    skierVertical.setResortID(resortId);
    return skierVertical;
  }

  public SkierVertical getSkierDayVertical(String skierId, String resortId, String dayId) {
    SkierVertical skierVertical = new SkierVertical();
    Connection conn = null;
    PreparedStatement ps = null;
    try {
      conn = dataSource.getConnection();
      ps = conn.prepareStatement(GET_SKIER_DAY_VERTICAL_SQL);

      ps.setString(1, skierId);
      ps.setString(2, resortId);
      ps.setString(3, dayId);

      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        logger.error("No rs.next().");
      } else {
        rs.first();
        skierVertical.setTotalVert(rs.getInt("skier_day_vertical"));
      }
    } catch (SQLException e) {
      logger.error(e);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (SQLException se) {
        logger.error(se);
      }
    }

    skierVertical.setResortID(resortId);
    return skierVertical;
  }
}