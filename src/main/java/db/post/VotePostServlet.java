package db.post;

import main.DBConnectionPool;
import main.Main;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;

/**
 * Created by vitaly on 22.06.15.
 */
public class VotePostServlet extends HttpServlet {

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

        long postId = 0;
        int vote = 0;
        postId = JSONRequest.getLong("post");
        vote = JSONRequest.getInt("vote");

        if (vote != 1 && vote != -1 || postId == 0) {
            status = 3;
            message = "Wrong JSON";
        }
        Connection conn = null;
        Statement sqlQuery = null;
        try {
            conn = Main.dataSource.getConnection();
            Main.connectionPool.printStatus();
            if (status == 0) {
                String likes = vote > 0 ? "likes" : "dislikes";
                String query = "update post set " + likes + " = " + likes + " + 1" + " where id = " + postId + ";";
                sqlQuery = conn.createStatement();
                int result = sqlQuery.executeUpdate(query);
                if (result == 0) {
                    status = 1;
                    message = "There is no such POST";
                }
            }
            createResponse(conn, response, status, message, postId);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        Main.connectionPool.printStatus();
    }

    private void createResponse(Connection conn, HttpServletResponse response, short status, String message, long postId) throws IOException, SQLException {
        response.setContentType("json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setStatus(HttpServletResponse.SC_OK);

        JSONObject obj = new JSONObject();
        JSONObject data = null;
        if (status == 0) {
            data = getPostDetails(conn, (int) postId);
            if (data == null) {
                status = 1;
                message = "There is no such POST";
            }
        }
        if (status == 0) {
            obj.put("response", data);
        } else {
            obj.put("error", message);
        }
        obj.put("code", status);
        response.getWriter().write(obj.toString());
    }

    //TODO - опять копирую((( -  в функцию надо!!!
    public JSONObject getPostDetails(Connection conn, int id) throws IOException, SQLException {

        JSONObject data = new JSONObject();
        ResultSet resultSet;
        PreparedStatement pstmt = null;
        try {

            pstmt = conn.prepareStatement("select * from post where id = ?");
            pstmt.setInt(1, id);
            resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                data.put("date", resultSet.getString("date_of_creating").substring(0, 19));
                data.put("forum", resultSet.getString("forum"));
                data.put("id", resultSet.getInt("id"));
                data.put("isApproved", resultSet.getBoolean("isApproved"));
                data.put("isHighlighted", resultSet.getInt("isHighlighted") == 1 ? true : false);
                data.put("isEdited", resultSet.getBoolean("isEdited"));
                data.put("isSpam", resultSet.getBoolean("isSpam"));
                data.put("isDeleted", resultSet.getBoolean("isDeleted"));
                data.put("message", resultSet.getString("message"));
                data.put("likes", resultSet.getInt("likes"));
                data.put("dislikes", resultSet.getInt("dislikes"));
                data.put("points", resultSet.getInt("likes") - resultSet.getInt("dislikes"));
                String temp = resultSet.getString("parent");

                if (temp.equals("")) {
                    data.put("parent", JSONObject.NULL);
                }else {
                    int indexLast = temp.lastIndexOf("_");
                    data.put("parent", Integer.parseInt(temp.substring(indexLast + 1)));
                }
                data.put("thread", resultSet.getInt("thread"));
                data.put("user", resultSet.getString("user_email"));
            } else {
                data = null;
            }

        }catch(SQLException ex) {
            ex.printStackTrace();
        }
        return data;
    }
}
