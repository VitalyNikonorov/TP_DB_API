package db.thread;

import main.Main;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by vitaly on 23.06.15.
 */
public class CloseThreadServlet extends HttpServlet {

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) throws ServletException, IOException {

        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (Exception e) { /*report an error*/ }

        JSONObject JSONRequest = new JSONObject(jb.toString());

        short status = 0;
        String message = "";

        long threadId= 0;
        if (JSONRequest.has("thread")) {
            threadId = JSONRequest.getLong("thread");
        } else {
            status = 2;
            message = "Incorrect Request JSON";
        }

        Connection connection = null;
        Statement sqlQuery = null;
        try {
            connection = Main.dataSource.getConnection();
            Main.connectionPool.printStatus();
            sqlQuery = connection.createStatement();
            int result = 0;
            String query;

            if (status == 0) {
                query = "update thread set isClosed = 1 where id = " + threadId + ";";
                result = sqlQuery.executeUpdate(query);
                if (result == 0) {
                    status = 1;
                    message = "There is no such Thread";
                }
            }
            createResponse(response, status, message, threadId);
        } catch (SQLException e) {
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void createResponse(HttpServletResponse response, short status, String message, long threadId) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        JSONObject data = new JSONObject();
        if (status != 0) {
            data.put("error", message);
        } else {
            data.put("thread", threadId);
        }
        obj.put("response", data);
        obj.put("code", status);
        response.getWriter().write(obj.toString());
    }
}