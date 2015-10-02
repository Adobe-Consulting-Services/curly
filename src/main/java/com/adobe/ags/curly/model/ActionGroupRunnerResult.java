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
package com.adobe.ags.curly.model;

import com.adobe.ags.curly.CurlyApp;
import static com.adobe.ags.curly.Messages.*;
import com.sun.javafx.collections.ObservableListWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

public class ActionGroupRunnerResult implements RunnerResult<ActionResult> {

    Map<Action, ActionResult> results;
    DoubleProperty completionPercentage = new SimpleDoubleProperty(0);
    BooleanProperty successfulProperty = new SimpleBooleanProperty(false);
    ObservableList<ObservableValue> row;

    public ActionGroupRunnerResult(List<Action> actions, Map<String, String> variables, Set<String> reportColumns) {
        results = new LinkedHashMap<>();
        results.keySet().addAll(actions);
        row = new ObservableListWrapper<>(new ArrayList<>());
        buildRow(variables, reportColumns);
    }

    @Override
    public BooleanProperty successfulProperty() {
        return successfulProperty;
    }

    @Override
    public DoubleProperty percentComplete() {
        return completionPercentage;
    }

    @Override
    public Collection<ActionResult> getDetails() {
        return results.values();
    }

    public void recordResult(Action action, ActionResult response) {
        // In case this affects the UI we should run this on the main JavaFX thread
        Platform.runLater(() -> {
            results.put(action, response);
            DoubleProperty zero = new SimpleDoubleProperty(0);
            DoubleBinding sum = Bindings.add(zero, 0d);
            BooleanBinding allSuccessful = Bindings.and((new SimpleBooleanProperty(true)), (new SimpleBooleanProperty(true)));
            for (ActionResult theResponse : results.values()) {
                if (theResponse != null) {
                    sum = sum.add(theResponse.percentComplete());
                    allSuccessful = allSuccessful.and(theResponse.successfulProperty());
                }
            }
            completionPercentage.bind(Bindings.divide(sum, results.size()));
            successfulProperty.bind(allSuccessful);
        });
    }

    @Override
    public ObservableList<ObservableValue> reportRow() {
        return row;
    }

    private void buildRow(Map<String, String> variables, Set<String> reportColumns) {
        StringBinding successOrNot = Bindings.when(successfulProperty)
                .then(CurlyApp.getMessage(COMPLETED_SUCCESSFUL))
                .otherwise(CurlyApp.getMessage(COMPLETED_UNSUCCESSFUL));
        row.add(Bindings.when(Bindings.greaterThanOrEqual(completionPercentage, 1)).then(successOrNot).otherwise(CurlyApp.getMessage(INCOMPLETE)));
        row.add(Bindings.concat(Bindings.multiply(completionPercentage, 100), "%"));
        reportColumns.forEach((colName) -> row.add(new SimpleStringProperty(variables.get(colName))));
    }
    
    @Override
    public String toHtml(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        row.forEach(value->sb.append("<td>").append(value.toString()).append("</td>"));
        sb.append("</tr>");
        if (level > 1) {
            results.values().forEach(result->sb.append(result.toHtml(level)));
        }
        return sb.toString();
    }
}