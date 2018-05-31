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

import com.adobe.ags.curly.ApplicationState;
import com.adobe.ags.curly.ConnectionManager;
import com.adobe.ags.curly.CurlyApp;
import static com.adobe.ags.curly.Messages.NO_DATA_LOADED;
import com.adobe.ags.curly.model.ActionUtils;
import com.adobe.ags.curly.xml.Action;
import com.adobe.ags.curly.xml.ErrorBehavior;
import com.sun.javafx.collections.ObservableListWrapper;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

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

    @FXML // fx:id="batchSize"
    private TextField batchSize; // Value injected by FXMLLoader

    @FXML // fx:id="batchNumberChoice"
    private ComboBox<Integer> batchNumberChoice; // Value injected by FXMLLoader

    ObservableList<Integer> highlightedRows = FXCollections.observableArrayList();

    @FXML // fx:id="batchRunStatus"
    private Label batchRunStatus; // Value injected by FXMLLoader

    public ObservableList<Action> getActions() {
        return actionList.getItems();
    }

    @FXML
    void addActionClicked(ActionEvent event) {
        Action action = CurlyApp.editAction(new Action(), null);
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
        ApplicationState.getInstance().errorBehaviorProperty().set(ErrorBehavior.IGNORE);
    }

    @FXML
    void skipIfError(ActionEvent event) {
        ApplicationState.getInstance().errorBehaviorProperty().set(ErrorBehavior.SKIP);
    }

    @FXML
    void haltIfError(ActionEvent event) {
        ApplicationState.getInstance().errorBehaviorProperty().set(ErrorBehavior.HALT);
    }

    @FXML
    void openDataSet(ActionEvent event) {
        CurlyApp.importWizard(this::loadData);
    }

    @FXML
    void singleShotClicked(ActionEvent event) {
        List<Map<String, String>> blankRow = new ArrayList<>();
        blankRow.add(new HashMap<>());
        BatchRunner runner = new BatchRunner(loginHandler, concurencyChoice.getValue(), getActions(), blankRow, defaults, defaults.keySet());
        CurlyApp.openActivityMonitor(runner);
    }

    @FXML
    void batchStartClicked(ActionEvent event) {
        ObservableList<Map<String, String>> selectedItems = FXCollections.observableArrayList();
        if (highlightedRows.isEmpty()) {
            selectedItems.addAll(batchDataTable.getItems());
        } else {
            highlightedRows.stream().map(batchDataTable.getItems()::get).forEach(selectedItems::add);
        }
        BatchRunner runner = new BatchRunner(loginHandler, concurencyChoice.getValue(), getActions(), selectedItems, defaults, defaults.keySet());
        CurlyApp.openActivityMonitor(runner);
    }

    @FXML
    void openActionSequence(ActionEvent event) {
        FileChooser openChooser = new FileChooser();
        openChooser.setTitle(ApplicationState.getMessage("openActionSequence"));
        File sourceFile = openChooser.showOpenDialog(null);
        if (sourceFile != null) {
            List<Action> actions = ActionUtils.readFromFile(sourceFile);
            actionList.getItems().clear();
            actionList.getItems().addAll(actions);
        }
    }

    @FXML
    void saveActionSequence(ActionEvent event) {
        FileChooser openChooser = new FileChooser();
        openChooser.setTitle(ApplicationState.getMessage("saveActionSequence"));
        File targetFile = openChooser.showSaveDialog(null);
        if (targetFile != null) {
            ActionUtils.saveToFile(targetFile, actionList.getItems());
        }
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
        assert batchSize != null : "fx:id=\"batchSize\" was not injected: check your FXML file 'App.fxml'.";
        assert batchNumberChoice != null : "fx:id=\"batchNumberChoice\" was not injected: check your FXML file 'App.fxml'.";

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

        List<Integer> oneThroughNine = IntStream.range(1, 10).boxed().collect(Collectors.toList());
        concurencyChoice.setItems(new ObservableListWrapper<>(oneThroughNine));
        concurencyChoice.selectionModelProperty().addListener((prop, oldValue, newValue)
                -> ConnectionManager.getInstance().resetConnectionManager(newValue.getSelectedItem()));
        Platform.runLater(() -> concurencyChoice.getSelectionModel().selectFirst());

        batchSize.textProperty().addListener((property, oldValue, newValue) -> updateBatchSize(newValue));
        batchNumberChoice.valueProperty().addListener((property, oldValue, newValue) -> showSelectedBatch(newValue));

        batchDataTable.setPlaceholder(new Label(ApplicationState.getMessage(NO_DATA_LOADED)));
        batchDataTable.setRowFactory((TableView<Map<String, String>> param) -> {
            final TableRow<Map<String, String>> row = new TableRow<>();
            row.indexProperty().addListener((property, newValue, oldValue)->updateRowHighlight(row));
            highlightedRows.addListener((ListChangeListener.Change<? extends Integer> c) -> updateRowHighlight(row));
            return row;
        });
    }

    private void updateConnectionTabStyle() {
        connectionTab.setStyle("-fx-background-color:" + (loginHandler.model.loginConfirmedProperty().getValue() ? "#8f8" : "#f88"));
    }

    private void buildVariableGrid(ListChangeListener.Change<? extends Action> change) {
        Map<String, String> variablesWithDefaults = new TreeMap<>();
        change.getList().forEach((Action a) -> variablesWithDefaults.putAll(ActionUtils.getVariablesWithDefaults(a)));
        defaults.keySet().retainAll(variablesWithDefaults.keySet());

        oneShotGrid.getChildren().clear();
        final AtomicInteger row = new AtomicInteger(0);
        variablesWithDefaults.forEach((var, defaultValue) -> {
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

        TableColumn<Map<String, String>, String> numberCol = new TableColumn("");
        numberCol.setCellValueFactory(row -> new ReadOnlyObjectWrapper(
                (row.getTableView().getItems().indexOf(row.getValue()) + 1) + ""));
        batchDataTable.getColumns().add(numberCol);
        numberCol.setMinWidth(50);

        data.get(0).keySet().forEach(varName -> {
            TableColumn<Map<String, String>, String> varCol = new TableColumn(varName);
            varCol.setCellValueFactory(row -> new ReadOnlyObjectWrapper(row.getValue().get(varName)));
            batchDataTable.getColumns().add(varCol);
        });

        batchDataTable.setItems(new ObservableListWrapper<>(data));
    }

    int batchSizeValue = 0;

    private void updateBatchSize(String newValue) {
        try {
            batchSizeValue = Integer.parseInt(newValue);
        } catch (NumberFormatException ex) {
            batchSizeValue = -1;
        }
        if (batchSizeValue <= 0 || batchSizeValue >= batchDataTable.getItems().size()) {
            batchNumberChoice.setValue(null);
            batchNumberChoice.setDisable(true);
            batchSize.clear();
            batchSizeValue = -1;
            if (newValue != null && !newValue.isEmpty()) {
                Platform.runLater(()->batchSize.setText(null));
            }
        } else {
            batchNumberChoice.setDisable(false);
            int numBatches = (batchDataTable.getItems().size() + batchSizeValue - 1) / batchSizeValue;
            numBatches = Math.min(numBatches, 50);
            List<Integer> selections = IntStream.range(1, numBatches+1).boxed().collect(Collectors.toList());
            batchNumberChoice.setItems(new ObservableListWrapper<>(selections));
            batchNumberChoice.setValue(1);
        }
    }

    private void showSelectedBatch(Integer batch) {
        if (batch == null || batch < 0 || batchSizeValue <= 0) {
            highlightedRows.clear();
        } else {
            int batchStart = (batch - 1) * batchSizeValue;
            int batchEnd = Math.min(batchDataTable.getItems().size(), batchStart + batchSizeValue);
            highlightedRows.setAll(IntStream.range(batchStart, batchEnd).boxed().collect(Collectors.toList()));
        }
    }

    private void updateRowHighlight(TableRow<Map<String, String>> row) {
        int r = 160;
        int g = 160;
        int b = 160;
        if (highlightedRows.isEmpty()) {
            r = 255;
            g = 255;
            b = 255;
        } else if (highlightedRows.contains(row.getIndex())) {
            r = 200;
            g = 255;
            b = 200;
        }
        if (row.getIndex() % 2 == 1) {
            r = Math.max(0, r-16);
            g = Math.max(0, g-16);
            b = Math.max(0, b-16);
        }
        row.setBackground(new Background(new BackgroundFill(Color.rgb(r, g, b), null, null)));
    }
}
