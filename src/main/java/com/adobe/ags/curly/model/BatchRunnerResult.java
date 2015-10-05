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
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class BatchRunnerResult implements RunnerResult<ActionGroupRunnerResult> {

    BooleanProperty successful = new SimpleBooleanProperty(false);
    DoubleProperty completedPercent = new SimpleDoubleProperty(0);
    ObservableList<ActionGroupRunnerResult> details = new ObservableListWrapper<>(new ArrayList<>());
    ObservableList<ObservableValue> row = new ObservableListWrapper<>(new ArrayList<>());
    LongProperty timeEllapsed = new SimpleLongProperty(0);
    LongProperty timeRemaining = new SimpleLongProperty(0);
    Long startTime = 0L;

    public BatchRunnerResult() {
        details.addListener((ListChangeListener.Change<? extends RunnerResult> c) -> {
            if (c.next() && c.wasAdded()) {
                updateAccumulatedProperties();
            }
        });

        row.add(new ReadOnlyStringWrapper("Batch run"));
        
        StringBinding successOrNot = Bindings.when(successful)
                .then(CurlyApp.getMessage(COMPLETED_SUCCESSFUL))
                .otherwise(CurlyApp.getMessage(COMPLETED_UNSUCCESSFUL));
        row.add(Bindings.when(Bindings.greaterThanOrEqual(completedPercent, 1))
                .then(successOrNot).otherwise(CurlyApp.getMessage(INCOMPLETE)));

        row.add(Bindings.concat(Bindings.multiply(completedPercent, 100), "%"));
    }

    public void start() {
        startTime = System.currentTimeMillis();
        completedPercent.addListener(this::updateEstimates);
    }

    public void stop() {
        timeEllapsed.unbind();
        timeRemaining.set(0);
        completedPercent.removeListener(this::updateEstimates);
    }

    private void updateEstimates(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        Long now = System.currentTimeMillis();
        Long ellapsed = now - startTime;
        timeEllapsed.set(ellapsed);
        long totalTime = (long) ((double) ellapsed / completedPercent.get());
        timeRemaining.set(totalTime - ellapsed);
    }

    public LongProperty timeEllapsedProperty() {
        return timeEllapsed;
    }

    public LongProperty timeRemainingProperty() {
        return timeRemaining;
    }
    
    @Override
    public BooleanProperty successfulProperty() {
        return successful;
    }

    @Override
    public DoubleProperty percentComplete() {
        return completedPercent;
    }

    @Override
    public ObservableList<ObservableValue> reportRow() {
        return row;
    }

    @Override
    public Collection<ActionGroupRunnerResult> getDetails() {
        return details;
    }

    private void updateAccumulatedProperties() {
        List<BooleanProperty> successfulList = getDetails().stream().map(RunnerResult::successfulProperty).collect(Collectors.toList());
        BooleanBinding allSuccessful = Bindings.createBooleanBinding(() -> {
            return successfulList.stream().allMatch(BooleanProperty::get);
        }, successfulList.toArray(new BooleanProperty[0]));
        successful.bind(allSuccessful);

        List<DoubleProperty> completeList = getDetails().stream().map(RunnerResult::percentComplete).collect(Collectors.toList());
        DoubleBinding totalComplete = Bindings.createDoubleBinding(()
                -> completeList.stream().map(DoubleProperty::get).reduce(0.0, (total, val) -> total + val), completeList.toArray(new DoubleProperty[0]));
        completedPercent.bind(Bindings.divide(totalComplete, (double) completeList.size()));
    }

    @Override
    public String toHtml(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><tr>");
        row.forEach(value->sb.append("<td>").append(value.getValue().toString()).append("</td>"));
        sb.append("</tr>");
        if (level > 0) {
            details.forEach(result->sb.append(result.toHtml(level)));
        }
        sb.append("</table>");
        return sb.toString();
    }
}
