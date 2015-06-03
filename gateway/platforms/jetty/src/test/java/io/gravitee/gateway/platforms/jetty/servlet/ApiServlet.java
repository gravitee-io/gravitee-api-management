package io.gravitee.gateway.platforms.jetty.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet mock d'une api.
 */
@WebServlet(asyncSupported = true, loadOnStartup = 1, urlPatterns = "/*")
public class ApiServlet extends HttpServlet {

  /**
   * Serial id.
   */
  private static final long serialVersionUID = 7084816556439818855L;

  /**
   * Constructeur par défaut.
   */
  public ApiServlet() {
    super();
  }

  public static int errorCode;

  /**
   *
   */
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String query = req.getQueryString();

    if (query != null && !query.isEmpty()) {
      resp.getOutputStream().print(query);
    } else if (errorCode > 0) {
      resp.sendError(this.errorCode);
    } else {
      resp.getOutputStream().print("hello");
    }
  }

}
