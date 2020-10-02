import io.swagger.client.model.LiftRide;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SkierServlet extends javax.servlet.http.HttpServlet {

  protected void doPost(HttpServletRequest req,
      HttpServletResponse res)
      throws javax.servlet.ServletException, IOException {

    res.setStatus(HttpServletResponse.SC_OK);
    String urlPath = req.getPathInfo();
    System.out.println(urlPath);

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
//      res.getWriter().write("missing paramterers");
      return;
    }
    if (urlPath.equals("/skiers/liftrides")) {
      res.setStatus(HttpServletResponse.SC_OK);
    }

//    response.getWriter().write("Dummy valid data for Post");
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json;charset:utf-8");
    String urlPath = req.getPathInfo();
    System.out.println(urlPath);
    LiftRide liftRide = new LiftRide();


    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
//      res.getWriter().write("missing paramterers ");
      return;
    }

    if (urlPath.equals("/resort/day/top10vert")) {
      res.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    String[] urlParts = urlPath.split("/");
    // and now validate url path and return the response status code
    // (and maybe also some value if input is valid)

    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
      res.setStatus(HttpServletResponse.SC_OK);
      // do any sophisticated processing with urlParts which contains all the url params
      // TODO: process url params in `urlParts`
//      res.getWriter().write("Dummy valid data for Get");
    }
  }

  private boolean isUrlValid(String[] urlPath) {
    // TODO: validate the request url path according to the API spec
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    return true;
  }
}
