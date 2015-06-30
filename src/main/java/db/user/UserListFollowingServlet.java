package db.user;

import main.DBConnectionPool;
import main.Main;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;


import java.util.*;

/**
 * Created by Виталий on 23.03.2015.
 */
public class UserListFollowingServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        JSONObject jsonResponse = new JSONObject();

        String userEmail = request.getParameter("user");
        String limit = request.getParameter("limit");
        String order = request.getParameter("order");

        if (order == null){
            order = "desc";
        }

        if ( !(order.equals("asc") || order.equals("desc")) ) {
            jsonResponse.put("code", 3);
            jsonResponse.put("response", "Incorrect order parameter");
            response.getWriter().println(jsonResponse);
        }

        String since_id = request.getParameter("since_id");
        String max_id = request.getParameter("max_id");

        PreparedStatement pstmt = null;
        Statement sqlQuery = null;
        Connection conn = null;
        // Database
        try {
            conn = Main.dataSource.getConnection();
            Main.connectionPool.printStatus();

            pstmt = conn.prepareStatement("SELECT * FROM users WHERE email=?");
            pstmt.setString(1, userEmail);

            ResultSet rs = null;
            rs = pstmt.executeQuery();

            Map<String, Object> user = new HashMap<>();

            while(rs.next()){
                //Parse values
                if( rs.getString("about").equals("")){
                    user.put("about", JSONObject.NULL);
                }else {
                    user.put("about", rs.getString("about"));
                }

                user.put("email", rs.getString("email"));
                user.put("id", new Integer(rs.getString("id")));
                user.put("isAnonymous", rs.getString("isAnonymous").equals("1")?true:false);
                user.put("name", rs.getString("name"));

                if( rs.getString("name").equals("")){
                    user.put("name", JSONObject.NULL);
                }else {
                    user.put("name", rs.getString("name"));
                }

                if( rs.getString("username").equals("")){
                    user.put("username", JSONObject.NULL);
                }else {
                    user.put("username", rs.getString("username"));
                }
            }

            String sqlSelect = "SELECT * FROM follow WHERE follower_id= " + user.get("id");

            if ( since_id != null){
                sqlSelect = sqlSelect + " AND followee_id >= " +since_id;
            }

            if ( max_id != null){
                sqlSelect = sqlSelect + " AND  followee_id <= " +max_id;
            }

            sqlSelect = sqlSelect + " ORDER BY " + " followee_id " +order;

            if (limit != null){
                sqlSelect = sqlSelect + " LIMIT " +limit +";";
            }else{
                sqlSelect = sqlSelect + ";";
            }

            sqlQuery = conn.createStatement();
            rs = sqlQuery.executeQuery(sqlSelect);

            int size= 0;
            if (rs != null)
            {
                rs.beforeFirst();
                rs.last();
                size = rs.getRow();
            }

            int[] followers = new int[size];

            rs.beforeFirst();
            int i = 0;
            while(rs.next()){
                //Parse values
                followers[i]=rs.getInt("followee_id");
                i++;
            }
            List<Map<String, Object>> arrayResponse = new ArrayList<Map<String, Object>>();

            for (int j = 0; j < size; j++){
                arrayResponse.add(db.user.UserInfo.getFullUserInfoById(conn, followers[j]));
            }

            jsonResponse.put("code", 0);
            jsonResponse.put("response", arrayResponse);
        }
        catch (SQLException ex){
            ex.printStackTrace();
        }catch (Exception ex){
            System.out.println("Other Error in UserListFollowingServlet.");
        }finally {
            if (sqlQuery != null) {
                try {
                    sqlQuery.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        Main.connectionPool.printStatus();
        //Database!!!!
        response.getWriter().println(jsonResponse);
    }

}
