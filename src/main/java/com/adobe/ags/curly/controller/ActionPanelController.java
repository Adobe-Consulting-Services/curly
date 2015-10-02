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

import com.adobe.ags.curly.model.Action;
import com.adobe.ags.curly.model.ActionCatalog;
import com.sun.javafx.collections.ObservableListWrapper;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ActionPanelController {
    Runnable persistHandler;
    Runnable closeHandler;
    
    Action source;

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="predefinedCombobox"
    private ComboBox<Action> predefinedCombobox; // Value injected by FXMLLoader

    @FXML // fx:id="actionNameField"
    private TextField actionNameField; // Value injected by FXMLLoader

    @FXML // fx:id="descriptionField"
    private TextArea descriptionField; // Value injected by FXMLLoader

    @FXML // fx:id="curlField"
    private TextArea curlField; // Value injected by FXMLLoader

    @FXML
    void cancelActionPerformed(ActionEvent event) {
        closeHandler.run();        
    }

    @FXML
    void favoriteActionPerformed(ActionEvent event) {

    }

    @FXML
    void okActionPerformed(ActionEvent event) {
        source.setName(actionNameField.getText());
        source.setDescription(descriptionField.getText());
        source.setCommand(curlField.getText());
        if (persistHandler != null) {
            persistHandler.run();
        }
        closeHandler.run();
    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert predefinedCombobox != null : "fx:id=\"predefinedCombobox\" was not injected: check your FXML file 'ActionPanel.fxml'.";
        assert actionNameField != null : "fx:id=\"actionNameField\" was not injected: check your FXML file 'ActionPanel.fxml'.";
        assert descriptionField != null : "fx:id=\"descriptionField\" was not injected: check your FXML file 'ActionPanel.fxml'.";
        assert curlField != null : "fx:id=\"curlField\" was not injected: check your FXML file 'ActionPanel.fxml'.";

        List<Action> actions = new ArrayList<>(ActionCatalog.getCatalog().values());
        predefinedCombobox.setItems(new ObservableListWrapper<>(actions));
        predefinedCombobox.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends Action> observable, Action oldValue, Action newValue) -> {
                    populateValues(observable.getValue(), false);
        });
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
}
