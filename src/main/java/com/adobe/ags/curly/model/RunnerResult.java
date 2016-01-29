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
import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

public abstract class RunnerResult<T extends RunnerResult> {

    final private DoubleProperty percentSuccess = new SimpleDoubleProperty(0);
    final private DoubleProperty percentComplete = new SimpleDoubleProperty(0);
    final private BooleanProperty started = new SimpleBooleanProperty(false);
    final private ObservableList<ObservableValue> reportRow = new ObservableListWrapper<>(new ArrayList<>());
    final private ObservableList<T> details = new ObservableListWrapper<>(new ArrayList<>());
    final private LongProperty startTime = new SimpleLongProperty(-1);
    final private LongProperty endTime = new SimpleLongProperty(-1);

    public RunnerResult() {
        started.addListener((prop, oldVal, newVal) -> {
            if (newVal && startTime.get() < 0) {
                startTime.set(System.currentTimeMillis());
            }
        });
        completed().addListener(((prop, oldVal, newVal) -> {
            if (newVal && endTime.get() < 0) {
                endTime.set(System.currentTimeMillis());
            }
        }));
    }

    public LongProperty startTime() {
        return startTime;
    }

    public LongProperty endTime() {
        return endTime;
    }

    public NumberBinding getDuration() {
        return Bindings.createLongBinding(() -> {
            if (startTime.get() == -1) {
                return 0L;
            } else if (endTime.get() == -1) {
                return System.currentTimeMillis() - startTime.get();
            } else {
                return endTime.get() - startTime.get();
            }
        }, startTime, endTime);
    }

    public DoubleProperty percentSuccess() {
        return percentSuccess;
    }

    ;
    public DoubleProperty percentComplete() {
        return percentComplete;
    }

    public BooleanProperty started() {
        return started;
    }

    final public BooleanBinding completed() {
        return Bindings.greaterThanOrEqual(percentComplete(), 1.0);
    }

    public BooleanBinding completelySuccessful() {
        return Bindings.greaterThanOrEqual(percentSuccess(), 1.0);
    }

    public ObservableList<ObservableValue> reportRow() {
        return reportRow;
    }

    public ReadOnlyListProperty<T> getDetails() {
        return new ReadOnlyListWrapper<>(details);
    }

    public void addDetail(T detail) {
        details.add(detail);
        Platform.runLater(() -> trackSummaryAgainstAttionalDetail(detail));
    }

    private DoubleBinding completionSummary;
    private DoubleBinding successSummary;

    private void trackSummaryAgainstAttionalDetail(T detail) {
        if (completionSummary == null) {
            completionSummary = detail.percentComplete().add(0);
        } else {
            completionSummary = completionSummary.add(detail.percentComplete());
        }
        if (successSummary == null) {
            successSummary = detail.percentSuccess().add(0);
        } else {
            successSummary = successSummary.add(detail.percentSuccess());
        }
        percentComplete.bind(completionSummary.divide(details.size()));
        percentSuccess.bind(successSummary.divide(details.size()));
    }

    abstract public String toHtml(int level);
}
