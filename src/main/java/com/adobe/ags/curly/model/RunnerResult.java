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

import com.sun.javafx.collections.ObservableListWrapper;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

public abstract class RunnerResult<T extends RunnerResult> {
    final private DoubleProperty percentSuccess = new SimpleDoubleProperty(0);
    final private DoubleProperty percentComplete = new SimpleDoubleProperty(0);
    final private BooleanProperty started = new SimpleBooleanProperty(false);
    final private ObservableList<ObservableValue> reportRow = new ObservableListWrapper<>(new ArrayList<>());
    final private ObservableList<T> details = new ObservableListWrapper<>(new ArrayList<>());
    
    public DoubleProperty percentSuccess() {
        return percentSuccess;
    };
    public DoubleProperty percentComplete() {
        return percentComplete;
    }
    public BooleanProperty started() {
        return started;
    }
    public BooleanBinding completed() {
        return Bindings.greaterThanOrEqual(percentComplete(), 1.0);
    }
    public BooleanBinding completelySuccessful() {
        return Bindings.greaterThanOrEqual(percentSuccess(), 1.0);
    }
    public ObservableList<ObservableValue> reportRow() {
        return reportRow;
    }
    public ReadOnlyListProperty<T> getDetails() {
        return new ReadOnlyListWrapper<T>(details);
    }
    public void addDetail(T detail) {
        details.add(detail);
        Platform.runLater(()->trackSummaryAgainstAttionalDetail(detail));
    }
    
    private DoubleBinding completionSummary;
    private DoubleBinding successSummary;
    private void trackSummaryAgainstAttionalDetail(T detail) {
        if (details.size() == 1) {
            completionSummary = detail.percentComplete().add(0);
            successSummary = detail.percentSuccess().add(0);
        } else {
            completionSummary = completionSummary.add(detail.percentComplete());
            successSummary = successSummary.add(detail.percentSuccess());
        }
        percentComplete.bind(completionSummary.divide(details.size()));
        percentSuccess.bind(successSummary.divide(details.size()));
    }
    abstract public String toHtml(int level);
}