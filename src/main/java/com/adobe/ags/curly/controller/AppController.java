/* 
 * Copyright 2015 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.ags.curly.controller;

import com.adobe.ags.curly.CurlyApp;
import static com.adobe.ags.curly.Messages.NO_DATA_LOADED;
import com.adobe.ags.curly.model.Action;
import com.sun.javafx.collections.ObservableListWrapper;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class AppController {

    Map<String, StringProperty> defaults;
    AuthHandler loginHandler;

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="connectionTab"
    private Tab connectionTab; // Value injected by FXMLLoader

    @FXML // fx:id="passwordField"
    private PasswordField passwordField; // Value injected by FXMLLoader

    @FXML // fx:id="hostField"
    private TextField hostField; // Value injected by FXMLLoader

    @FXML // fx:id="usernameField"
    private TextField usernameField; // Value injected by FXMLLoader

    @FXML // fx:id="sslCheckbox"
    private CheckBox sslCheckbox; // Value injected by FXMLLoader

    @FXML // fx:id="connectionVerificationLabel"
    private Label connectionVerificationLabel; // Value injected by FXMLLoader

    @FXML // fx:id="actionList"
    private ListView<Action> actionList; // Value injected by FXMLLoader

    @FXML // fx:id="oneShotGrid"
    private GridPane oneShotGrid; // Value injected by FXMLLoader

    @FXML // fx:id="singleShotStatus"
    private Label singleShotStatus; // Value injected by FXMLLoader

    @FXML // fx:id="batchDataTable"
    private TableView<Map<String, String>> batchDataTable;
    
    @FXML // fx:id="concurencyChoice"
    private ChoiceBox<Integer> concurencyChoice; // Value injected by FXMLLoader

    @FXML // fx:id="batchRunStatus"
    private Label batchRunStatus; // Value injected by FXMLLoader

    public ObservableList<Action> getActions() {
        return actionList.getItems();
    }    
    
    @FXML
    void addActionClicked(ActionEvent event) {
        Action action = CurlyApp.getInstance().editAction(new Action(), null);
        if (action != null) {
            actionList.getItems().add(action);
        }
    }

    @FXML
    void removeSelectedActionClicked(ActionEvent event) {
        actionList.getItems().removeAll(actionList.getSelectionModel().getSelectedItems());
    }
    
    @FXML
    void ignoreErrors(ActionEvent event) {
        CurlyApp.getInstance().errorBehaviorProperty().set(CurlyApp.ErrorBehavior.IGNORE);
    }
    
    @FXML
    void skipIfError(ActionEvent event) {
        CurlyApp.getInstance().errorBehaviorProperty().set(CurlyApp.ErrorBehavior.SKIP);
    }
    
    @FXML
    void haltIfError(ActionEvent event) {
        CurlyApp.getInstance().errorBehaviorProperty().set(CurlyApp.ErrorBehavior.HALT);
    }
    
    @FXML
    void openDataSet(ActionEvent event) {
        CurlyApp.getInstance().importWizard(this::loadData);
    }

    @FXML
    void singleShotClicked(ActionEvent event) {
        List<Map<String,String>> blankRow = new ArrayList<>();
        blankRow.add(new HashMap<>());
        BatchRunner runner = new BatchRunner(loginHandler, concurencyChoice.getValue(), getActions(), blankRow, defaults, defaults.keySet());
        CurlyApp.getInstance().openActivityMonitor(runner);        
    }

    @FXML
    void batchStartClicked(ActionEvent event) {
        BatchRunner runner = new BatchRunner(loginHandler, concurencyChoice.getValue(), getActions(), batchDataTable.getItems(), defaults, defaults.keySet());
        CurlyApp.getInstance().openActivityMonitor(runner);
    }
    
    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert connectionTab != null : "fx:id=\"connectionTab\" was not injected: check your FXML file 'App.fxml'.";
        assert passwordField != null : "fx:id=\"passwordField\" was not injected: check your FXML file 'App.fxml'.";
        assert hostField != null : "fx:id=\"hostField\" was not injected: check your FXML file 'App.fxml'.";
        assert usernameField != null : "fx:id=\"usernameField\" was not injected: check your FXML file 'App.fxml'.";
        assert sslCheckbox != null : "fx:id=\"sslCheckbox\" was not injected: check your FXML file 'App.fxml'.";
        assert connectionVerificationLabel != null : "fx:id=\"connectionVerificationLabel\" was not injected: check your FXML file 'App.fxml'.";
        assert actionList != null : "fx:id=\"actionList\" was not injected: check your FXML file 'App.fxml'.";
        assert oneShotGrid != null : "fx:id=\"oneShotGrid\" was not injected: check your FXML file 'App.fxml'.";
        assert singleShotStatus != null : "fx:id=\"singleShotStatus\" was not injected: check your FXML file 'App.fxml'.";
        assert batchDataTable != null : "fx:id=\"batchDataTable\" was not injected: check your FXML file 'App.fxml'.";
        assert concurencyChoice != null : "fx:id=\"concurencyChoice\" was not injected: check your FXML file 'App.fxml'.";
        assert batchRunStatus != null : "fx:id=\"batchRunStatus\" was not injected: check your FXML file 'App.fxml'.";

        loginHandler = new AuthHandler(
                hostField.textProperty(), sslCheckbox.selectedProperty(),
                usernameField.textProperty(), passwordField.textProperty());

        connectionVerificationLabel.textProperty().bind(loginHandler.model.statusMessageProperty());
        loginHandler.model.loginConfirmedProperty().addListener((confirmedValue, oldValue, newValue) -> this.updateConnectionTabStyle());
        updateConnectionTabStyle();

        actionList.setCellFactory((listView) -> new ActionListCell());
        actionList.setEditable(true);
        actionList.getItems().addListener(this::buildVariableGrid);

        defaults = new TreeMap<>();
        
        List<Integer> oneThroughNine = IntStream.range(1,10).boxed().collect(Collectors.toList());
        concurencyChoice.setItems(new ObservableListWrapper<>(oneThroughNine));
        Platform.runLater(()->concurencyChoice.getSelectionModel().selectFirst());
        
        batchDataTable.setPlaceholder(new Label(CurlyApp.getMessage(NO_DATA_LOADED)));
    }

    private void updateConnectionTabStyle() {
        connectionTab.setStyle("-fx-background-color:" + (loginHandler.model.loginConfirmedProperty().getValue() ? "#8f8" : "#f88"));
    }

    private void buildVariableGrid(ListChangeListener.Change<? extends Action> change) {
        Map<String, String> variablesWithDefaults = new TreeMap<>();
        change.getList().forEach((Action a) -> variablesWithDefaults.putAll(a.getVariablesWithDefaults()));
        defaults.keySet().retainAll(variablesWithDefaults.keySet());

        oneShotGrid.getChildren().clear();
        final AtomicInteger row = new AtomicInteger(0);
        variablesWithDefaults.forEach((var,defaultValue) -> {
            if (var.equalsIgnoreCase("server")) {
                return;
            }
            if (!defaults.containsKey(var)) {
                defaults.put(var, new SimpleStringProperty(defaultValue));
            }
            TextField text = new TextField();
            text.textProperty().bindBidirectional(defaults.get(var));
            oneShotGrid.add(new Label(var), 0, row.get());
            oneShotGrid.add(text, 1, row.getAndIncrement());
        });
    }
    
    private void loadData(List<Map<String, String>> data) {
        batchDataTable.getColumns().clear();
        
        TableColumn<Map<String,String>, String> numberCol = new TableColumn("");
        numberCol.setCellValueFactory(row->new ReadOnlyObjectWrapper(
                (row.getTableView().getItems().indexOf(row.getValue())+1) + ""));
        batchDataTable.getColumns().add(numberCol);
        
        data.get(0).keySet().forEach(varName->{
            TableColumn<Map<String,String>, String> varCol = new TableColumn(varName);
            varCol.setCellValueFactory(row->new ReadOnlyObjectWrapper(row.getValue().get(varName)));
            batchDataTable.getColumns().add(varCol);
        });
        
        batchDataTable.setItems(new ObservableListWrapper<>(data));
    }
}
