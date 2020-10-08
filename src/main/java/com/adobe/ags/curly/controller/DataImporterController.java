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
import com.adobe.ags.curly.model.ActionUtils;
import com.adobe.ags.curly.xml.Action;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.javafx.collections.ObservableListWrapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static com.adobe.ags.curly.Messages.*;

public class DataImporterController {

    public static final String DONT_USE = "";

    @FXML // fx:id="currentFileName"
    private TextField currentFileName; // Value injected by FXMLLoader

    @FXML // fx:id="worksheetSelector"
    private ComboBox<String> worksheetSelector; // Value injected by FXMLLoader

    @FXML // fx:id="contentTable"
    private TableView<List<String>> contentTable; // Value injected by FXMLLoader

    @FXML // fx:id="skipFirstSelection"
    private ChoiceBox<Integer> skipFirstSelection; // Value injected by FXMLLoader

    @FXML
    void chooseFile(ActionEvent event) {
        FileChooser openFileDialog = new FileChooser();
        openFileDialog.setTitle(ApplicationState.getMessage(CHOOSE_TEXT_OR_EXCEL));
        File selected = openFileDialog.showOpenDialog(null);
        if (selected != null && selected.exists() && selected.isFile()) {
            openFile(selected);
        }
    }

    Consumer<List<Map<String, String>>> okHandler = null;
    Runnable callWhenDone = null;
    List<String> colMapping;
    ObservableList<List<String>> tableData;

    @FXML
    void okPressed(ActionEvent event) {
        okHandler.accept(tableData.stream().map(row -> {
            Map<String, String> params = new HashMap<>();
            for (int i = 0; i < row.size(); i++) {
                if (!colMapping.get(i).equals(DONT_USE)) {
                    params.put(colMapping.get(i), row.get(i));
                }
            }
            return params;
        }).skip(skipFirstSelection.getValue()).collect(Collectors.toList()));
        if (callWhenDone != null) {
            callWhenDone.run();
        }
    }

    ObservableList<Action> actions;

    public void setActions(ObservableList<Action> actions) {
        this.actions = actions;
    }

    public void setFinishImportHandler(Consumer<List<Map<String, String>>> handler) {
        okHandler = handler;
    }

    public void whenFinished(Runnable finishAction) {
        callWhenDone = finishAction;
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert currentFileName != null : "fx:id=\"currentFileName\" was not injected: check your FXML file 'DataImporter.fxml'.";
        assert worksheetSelector != null : "fx:id=\"worksheetSelector\" was not injected: check your FXML file 'DataImporter.fxml'.";
        assert contentTable != null : "fx:id=\"contentTable\" was not injected: check your FXML file 'DataImporter.fxml'.";
        assert skipFirstSelection != null : "fx:id=\"skipFirstSelection\" was not injected: check your FXML file 'DataImporter.fxml'.";

        worksheetSelector.getSelectionModel().selectedItemProperty().addListener(this::changeSheet);
        List<Integer> zeroThroughTen = IntStream.range(0, 10).boxed().collect(Collectors.toList());
        skipFirstSelection.setItems(new ObservableListWrapper<>(zeroThroughTen));
        Platform.runLater(() -> skipFirstSelection.getSelectionModel().selectFirst());

        contentTable.setPlaceholder(new Label(ApplicationState.getMessage(NO_DATA_LOADED)));
    }

    private void openFile(File file) {
        sheetReader = null;
        worksheetSelector.getItems().clear();
        try {
            currentFileName.setText(null);
            if (file.getName().toLowerCase().endsWith("txt")) {
                openTextFile(file);
            } else if (file.getName().toLowerCase().endsWith("json")) {
                openJson(file);
            } else if (file.getName().toLowerCase().endsWith("xls")) {
                openLegacyExcel(file);
            } else if (file.getName().toLowerCase().endsWith("xlsx")) {
                openExcel(file);
            }
            currentFileName.setText(file.getName());
        } catch (IOException | InvalidFormatException ex) {
            Logger.getLogger(DataImporterController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void openTextFile(File file) throws FileNotFoundException, IOException {
        try ( BufferedReader reader = new BufferedReader(new FileReader(file))) {
            setTableData(reader.lines().map(line -> Arrays.asList(line.split("\\t"))).collect(Collectors.toList()));
        }
    }

    private void openLegacyExcel(File file) throws IOException {
        openWorkbook(new HSSFWorkbook(new FileInputStream(file)));
    }

    private void openExcel(File file) throws IOException, InvalidFormatException {
        openWorkbook(new XSSFWorkbook(file));
    }

    private void openJson(File file) throws FileNotFoundException, IOException {
        FileReader fileReader = new FileReader(file);
        try ( BufferedReader reader = new BufferedReader(fileReader)) {
            JsonParser parser = new JsonParser();
            JsonObject data = parser.parse(reader).getAsJsonObject();

            data.entrySet().stream().
                    filter((entry) -> (entry.getValue().isJsonArray())).
                    forEach((entry) -> {
                        worksheetSelector.getItems().add(entry.getKey());
                    });

            sheetReader = (String node) -> readNodes(data.get(node).getAsJsonArray());
            skipFirstSelection.setValue(1);
            Platform.runLater(() -> worksheetSelector.getSelectionModel().selectFirst());
        }
    }

    private List<List<String>> readNodes(JsonArray data) {
        TreeSet<String> attributes = new TreeSet<>();
        List<Map<String, String>> rows = new ArrayList<>();
        data.forEach((elem) -> {
            if (elem.isJsonObject()) {
                JsonObject row = elem.getAsJsonObject();
                Map<String, String> rowMap = row.entrySet().stream().
                        filter((entry) -> entry.getValue().isJsonPrimitive()).
                        collect(Collectors.toMap((entry) -> entry.getKey(),
                                (entry) -> entry.getValue().getAsString()));
                rows.add(rowMap);
                attributes.addAll(rowMap.keySet());
            }
        });
        List<List<String>> results = rows.stream().map((row)
                -> attributes.stream().map((attr)
                        -> row.get(attr)).collect(Collectors.toList())
        ).collect(Collectors.toList());
        results.add(0, new ArrayList<String>(attributes));
        return results;
    }

    private void openWorkbook(Workbook workbook) throws IOException {
        try {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                worksheetSelector.getItems().add(workbook.getSheetName(i));
            }

            sheetReader = (String sheetName) -> readSheet(workbook.getSheet(sheetName));

            Platform.runLater(() -> worksheetSelector.getSelectionModel().selectFirst());
        } finally {
            workbook.close();
        }
    }

    Callback<String, List<List<String>>> sheetReader = null;

    private void changeSheet(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (sheetReader != null) {
            setTableData(sheetReader.call(newValue));
        }
    }

    private List<List<String>> readSheet(Sheet sheet) {
        List<List<String>> data = new ArrayList<>();
        IntegerProperty numColumns = new SimpleIntegerProperty(sheet.getRow(0).getLastCellNum());
        sheet.forEach(row -> {
            List<String> rowData = new ArrayList<>();
            numColumns.set(Math.max(numColumns.get(), row.getLastCellNum()));
            for (int i = 0; i < numColumns.get(); i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String col = getStringValueFromCell(cell);
                rowData.add(col);
            }
            data.add(rowData);
        });
        return data;
    }

    private void setTableData(List<List<String>> data) {
        tableData = new ObservableListWrapper<>(data);
        int numberOfColumns = data.stream().map(List::size).reduce(Math::max).orElse(0);
        contentTable.getColumns().clear();
        colMapping = new ArrayList<>();
        for (int i = 0; i < numberOfColumns; i++) {
            colMapping.add(DONT_USE);
            TableColumn<List<String>, String> column = new TableColumn<>();
            final int index = i;
            ComboBox<String> selector = generateVariableSelector();
            selector.getSelectionModel().selectedItemProperty().
                    addListener((prop, oldValue, newValue) -> colMapping.set(index, newValue));
            column.setGraphic(selector);
            column.setCellValueFactory((TableColumn.CellDataFeatures<List<String>, String> param) -> {
                List<String> row = param.getValue();
                String value = row.size() > index ? row.get(index) : null;
                return new SimpleStringProperty(value);
            });
            contentTable.getColumns().add(column);
        }
        contentTable.setItems(tableData);
    }

    private String getStringValueFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        switch (cellType) {
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case BLANK:
                return null;
            case NUMERIC:
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    return Integer.toString((int) num);
                } else {
                    return Double.toString(cell.getNumericCellValue());
                }
            case STRING:
                return cell.getStringCellValue();
            default:
                return "???";
        }
    }

    private ComboBox<String> generateVariableSelector() {
        Set vars = new TreeSet<>();
        actions.stream().map(action -> ActionUtils.getVariableNames(action)).forEach(vars::addAll);
        ComboBox<String> box = new ComboBox<>();
        vars.add(DONT_USE);
        box.getItems().addAll(vars);
        return box;
    }
}
