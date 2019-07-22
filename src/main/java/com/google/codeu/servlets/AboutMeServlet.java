package com.google.codeu.servlets;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.*;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.codeu.data.Datastore;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.google.codeu.data.User;


/**
 * Handles fetching and saving user data.
 */
@WebServlet("/about")
public class AboutMeServlet extends HttpServlet {

  private Datastore datastore;

 @Override
 public void init() {
  datastore = new Datastore();
 }

 /**
  * Responds with the "about me" section for a particular user.
  */
 @Override
 public void doGet(HttpServletRequest request, HttpServletResponse response)
   throws IOException {

  response.setContentType("text/html");

  String user = request.getParameter("user");

  if(user == null || user.equals("")) {
   // Request is invalid, return empty response
   return;
  }

  User userData = datastore.getUser(user);

  if(userData == null || userData.getAboutMe() == null) {
      return;
    }

  String userJsonString = new Gson().toJson(userData);
  System.out.println(userJsonString);


  response.getOutputStream().println(userJsonString);
 }

 @Override
 public void doPost(HttpServletRequest request, HttpServletResponse response)
   throws IOException {

  UserService userService = UserServiceFactory.getUserService();
  if (!userService.isUserLoggedIn()) {
   response.sendRedirect("/index.html");
   return;
  }


  String userEmail = userService.getCurrentUser().getEmail();
  String aboutMe = Jsoup.clean(request.getParameter("about-me"), Whitelist.none());
  String clubName = Jsoup.clean(request.getParameter("club-name"), Whitelist.none());
  String clubType = Jsoup.clean(request.getParameter("club-type"), Whitelist.none());
  //response.getOutputStream().println(clubType);
  String meetingLocation = Jsoup.clean(request.getParameter("meeting-location"), Whitelist.none());
  //String Logo = Jsoup.clean(request.getParameter("logo"), Whitelist.none());


  User user = new User(userEmail, aboutMe, clubName,clubType, meetingLocation);
  datastore.storeUser(user);


  //response.getOutputStream().println("Saving about me for " + userEmail);

  response.sendRedirect("/user-page.html?user=" + userEmail);

 }
}
