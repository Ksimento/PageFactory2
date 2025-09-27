package ru.sbt.edu_power.e2e_core;

public class CoreConstants {

    //    Survey
    public static final String SURVEY_MODAL = "//div[@class = 'modal' and contains(@style, 'block')]";
    public static final String CONTENT_WRAPPER = "//div[@id = 'login-screen' " +
                                                 "or @id = 'content-wrapper' " +
                                                 "or @id = 'contentGrid' " +
                                                 "or @id = 'content-wrapper-layout' " +
                                                 "or @class = 'wrap'" +
                                                 "or contains(@class,'_ServerMessage') " +
                                                 "or @id = 'upload-page' " +
                                                 "or contains(@class,'_ContentWrapper')]";
}
