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
import com.adobe.ags.curly.Messages;
import com.adobe.ags.curly.xml.Action;
import com.adobe.ags.curly.model.ActionCatalog;
import com.adobe.ags.curly.model.ActionUtils;
import com.adobe.ags.curly.xml.ErrorBehavior;
import com.sun.javafx.collections.ObservableListWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class ActionPanelController {

    Runnable persistHandler;
    Runnable closeHandler;

    Action source;

    @FXML // fx:id="predefinedCombobox"
    private ComboBox<Action> predefinedCombobox; // Value injected by FXMLLoader

    @FXML // fx:id="actionNameField"
    private TextField actionNameField; // Value injected by FXMLLoader

    @FXML // fx:id="descriptionField"
    private TextArea descriptionField; // Value injected by FXMLLoader

    @FXML // fx:id="delayField"
    private TextField delayField; // Value injected by FXMLLoader

    @FXML // fx:id="curlField"
    private TextArea curlField; // Value injected by FXMLLoader

    @FXML // fx:id="favoritesButton"
    private Button favoritesButton;

    @FXML // fx:id="predefinedCombobox"
    private ComboBox<ErrorBehavior> errorBehaviorCombobox; // Value injected by FXMLLoader

    @FXML
    void cancelActionPerformed(ActionEvent event) {
        closeHandler.run();
    }

    @FXML
    void favoriteActionPerformed(ActionEvent event) {
        if (ActionUtils.isFavorite(source)) {
            ActionUtils.removeFavorite(actionNameField.getText());
        } else {
            updateSourceObject();
            ActionUtils.addFavorite(source);
        }
        updateFavoriteButton(null, null, actionNameField.getText());
    }

    @FXML
    void okActionPerformed(ActionEvent event) {
        updateSourceObject();
        // Update favorite settings file automatically
        if (ActionUtils.isFavorite(source)) {
            ActionUtils.addFavorite(source);
        }
        if (persistHandler != null) {
            persistHandler.run();
        }
        closeHandler.run();
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert predefinedCombobox != null : "fx:id=\"predefinedCombobox\" was not injected: check your FXML file 'ActionPanel.fxml'.";
        assert favoritesButton != null : "fx:id=\"favoritesButton\" was not injected: check your FXML file 'ActionPanel.fxml'.";
        assert actionNameField != null : "fx:id=\"actionNameField\" was not injected: check your FXML file 'ActionPanel.fxml'.";
        assert descriptionField != null : "fx:id=\"descriptionField\" was not injected: check your FXML file 'ActionPanel.fxml'.";
        assert curlField != null : "fx:id=\"curlField\" was not injected: check your FXML file 'ActionPanel.fxml'.";

        List<Action> actions = new ArrayList<>();
        actions.addAll(ActionUtils.getFavoriteList());
        actions.addAll(
                ActionCatalog.getCatalog().values().stream()
                .filter(action -> !ActionUtils.isFavorite(action)).collect(Collectors.toList()));
        predefinedCombobox.setItems(new ObservableListWrapper<>(actions));
        predefinedCombobox.setButtonCell(new ActionListCell());
        predefinedCombobox.setCellFactory((listView) -> new ActionListCell());
        predefinedCombobox.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends Action> observable, Action oldValue, Action newValue) -> {
                    populateValues(observable.getValue(), false);
                });
        errorBehaviorCombobox.getItems().addAll(ErrorBehavior.values());
        errorBehaviorCombobox.setConverter(new StringConverter<ErrorBehavior>() {
            @Override
            public String toString(ErrorBehavior object) {
                return CurlyApp.getMessage("errorBehavior_" + object.name());
            }

            @Override
            public ErrorBehavior fromString(String string) {
                return null;
            }
        });
        errorBehaviorCombobox.setValue(ErrorBehavior.GLOBAL);
        actionNameField.textProperty().addListener(this::updateFavoriteButton);
    }

    public void updateFavoriteButton(ObservableValue nameProperty, String oldName, String newName) {
        boolean isFavorite = ActionUtils.isFavorite(newName);
        if (isFavorite) {
            favoritesButton.setText(CurlyApp.getMessage(Messages.REMOVE_FAVORITE));
        } else {
            favoritesButton.setText(CurlyApp.getMessage(Messages.ADD_FAVORITE));
        }
        predefinedCombobox.setButtonCell(new ActionListCell());
    }

    public void populateValues(Action source, boolean retainAsSource) {
        if (source != null) {
            if (retainAsSource) {
                this.source = source;
            }
            actionNameField.setText(source.getName());
            descriptionField.setText(source.getDescription());
            curlField.setText(source.getCommand());
        } else {
            this.source = new Action();
        }
    }

    public void onPersist(Runnable persistHandler) {
        this.persistHandler = persistHandler;
    }

    public void whenFinished(Runnable handler) {
        closeHandler = handler;
    }

    private void updateSourceObject() {
        source.setName(actionNameField.getText());
        source.setDescription(descriptionField.getText());
        source.setCommand(curlField.getText());
        if (delayField.getText() != null && !delayField.getText().trim().isEmpty()) {
            source.setDelay(Long.parseLong(delayField.getText()));
        }
        source.setErrorBehavior(errorBehaviorCombobox.getValue());
    }
}
