module cn {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires dropbox.core.sdk;
    requires jpbc.api;
    requires jama;
    requires bcprov.jdk15on;
    requires jpbc.plaf;
    requires org.kohsuke.github.api;
    requires google.http.client.jackson2;
    requires com.google.api.client.auth;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;
    requires google.api.client;
    requires com.google.api.client;
    requires google.api.services.drive.v3.rev197;
    requires com.fasterxml.jackson.databind;
    requires jdk.httpserver;


    opens cn to javafx.fxml;
    exports cn;
}