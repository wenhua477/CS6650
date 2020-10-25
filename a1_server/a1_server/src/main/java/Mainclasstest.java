import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Mainclasstest {
  public static void main(String[] args) {
    String sql = "insert into LiftRides values ('l2', 'r2', 'd', 20, 's', 't');";

    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    System.out.println("after find name");
//    String url = "jdbc:mysql://localhost:3306/Test?serverTimezone=UTC";
    String url =
        "jdbc:mysql://database-2.cehjlxxknfu2.us-east-1.rds.amazonaws.com:3306/";
//    database-2.cehjlxxknfu2.us-east-1.rds.amazonaws.com
    String userName = "wenhua";
    String password = "12345678";
    String dbName = "db_for_a2";
    String driver = "com.mysql.jdbc.Driver";
    try {
      Connection conn = DriverManager.getConnection(url + dbName, "wenhua", password);
//      conn = DriverManager.getConnection(url + dbName, userName, password);
      System.out.println("looked db url");
      PreparedStatement ps = conn.prepareStatement(sql);
      System.out.println("finished prepare INSERT_NEW_LIFTRIDE_SQL statement");
      ps.executeUpdate();

//      Statement st = conn.createStatement();
//      st.executeUpdate(INSERT_NEW_LIFTRIDE_SQL);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
