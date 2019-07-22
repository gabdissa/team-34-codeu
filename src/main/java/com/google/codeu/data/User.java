package com.google.codeu.data;

public class User { //User is actually a club

  private String email; //Login email
  private String clubName; //Name of the Club
  private String aboutMe; // Club Description
  private String clubType; //Type of Club
  private String meetingLocation; //Meeting Location
  //private String Logo;//Club Logo or Image

  public User(String email, String aboutMe, String clubName, String clubType, String meetingLocation) {
    this.email = email;
    this.aboutMe = aboutMe;
    this.clubName = clubName;
    this.clubType = clubType;
    this.meetingLocation = meetingLocation;
    //this.Logo = Logo;

  }

  public String getEmail(){
    return email;
  }

  public String getAboutMe() {
    return aboutMe;
  }

  public String getClubName() {
    return clubName;
  }

  public String getClubType() {
    return clubType;
  }

  public String getMeetingLocation() {
    return meetingLocation;
  }

}
