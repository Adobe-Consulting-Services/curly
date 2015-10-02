/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.adobe.ags.curly.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.web.WebView;

public class RunnerActivityController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="progressBar"
    private ProgressBar progressBar; // Value injected by FXMLLoader

    @FXML // fx:id="statusLabel"
    private Label statusLabel; // Value injected by FXMLLoader

    @FXML // fx:id="ellapsedLabel"
    private Label ellapsedLabel; // Value injected by FXMLLoader

    @FXML // fx:id="remainingLabel"
    private Label remainingLabel; // Value injected by FXMLLoader

    @FXML // fx:id="reportStyle"
    private ChoiceBox<?> reportStyle; // Value injected by FXMLLoader

    @FXML // fx:id="goStopButton"
    private Button goStopButton; // Value injected by FXMLLoader

    @FXML // fx:id="reportWebview"
    private WebView reportWebview; // Value injected by FXMLLoader


    @FXML
    void copyClicked(ActionEvent event) {

    }

    @FXML
    void goStopClicked(ActionEvent event) {

    }

    @FXML
    void saveClicked(ActionEvent event) {

    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'RunnerReport.fxml'.";
        assert statusLabel != null : "fx:id=\"statusLabel\" was not injected: check your FXML file 'RunnerReport.fxml'.";
        assert ellapsedLabel != null : "fx:id=\"ellapsedLabel\" was not injected: check your FXML file 'RunnerReport.fxml'.";
        assert remainingLabel != null : "fx:id=\"remainingLabel\" was not injected: check your FXML file 'RunnerReport.fxml'.";
        assert reportStyle != null : "fx:id=\"reportStyle\" was not injected: check your FXML file 'RunnerReport.fxml'.";
        assert goStopButton != null : "fx:id=\"goStopButton\" was not injected: check your FXML file 'RunnerReport.fxml'.";
        assert reportWebview != null : "fx:id=\"reportWebview\" was not injected: check your FXML file 'RunnerReport.fxml'.";

    }
}
