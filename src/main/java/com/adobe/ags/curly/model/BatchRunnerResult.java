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

import com.adobe.ags.curly.ApplicationState;
import static com.adobe.ags.curly.Messages.*;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;

public class BatchRunnerResult extends RunnerResult<ActionGroupRunnerResult> {

    LongProperty timeEllapsed = new SimpleLongProperty(0);
    LongProperty timeRemaining = new SimpleLongProperty(0);
    List<Binding> allBindings = new ArrayList<>();

    public BatchRunnerResult() {
        reportRow().add(new ReadOnlyStringWrapper("Batch run"));
        
        StringBinding successOrNot = Bindings.when(completelySuccessful())
                .then(ApplicationState.getMessage(COMPLETED_SUCCESSFUL))
                .otherwise(ApplicationState.getMessage(COMPLETED_UNSUCCESSFUL));
        StringBinding successMessageBinding = Bindings.when(completed())
                .then(successOrNot).otherwise(ApplicationState.getMessage(INCOMPLETE));
        reportRow().add(successMessageBinding);

        reportRow().add(percentCompleteString().concat(" complete"));
        reportRow().add(percentSuccessString().concat(" success"));
        reportRow().add(getDuration());
        allBindings.add(successOrNot);
        allBindings.add(successMessageBinding);
    }

    public void start() {
        started().set(true);
        percentComplete().addListener(this::updateEstimates);
    }

    public void stop() {
        endTime().set(System.currentTimeMillis());
        timeEllapsed.unbind();
        timeRemaining.set(0);
        percentComplete().removeListener(this::updateEstimates);
        invalidateBindings();
        waitUntilFinished();
        updateComputations();
    }
    
    private void waitUntilFinished() {
        while (details.stream().anyMatch(r -> !r.completed().get())) {
            Thread.yield();
        }
    }

    private void updateEstimates(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        Long now = System.currentTimeMillis();
        Long ellapsed = now - startTime().get();
        timeEllapsed.set(ellapsed);
        long totalTime = (long) ((double) ellapsed / percentComplete().get());
        timeRemaining.set(totalTime - ellapsed);
    }

    public LongProperty timeEllapsedProperty() {
        return timeEllapsed;
    }

    public LongProperty timeRemainingProperty() {
        return timeRemaining;
    }

    @Override
    public void invalidateBindings() {
        super.invalidateBindings();
        if (allBindings != null) {
            allBindings.forEach(Binding::invalidate);
        }
    }
    
    @Override
    public String toHtml(int level) {
        invalidateBindings();
        StringBuilder sb = new StringBuilder();
        sb.append("<table><tr>");
        reportRow().forEach(value->sb.append("<td>").append(value.getValue().toString()).append("</td>"));
        sb.append("</tr>");
        if (level > 0) {
            getDetails().forEach(result->sb.append(result.toHtml(level)));
        }
        sb.append("</table>");
        return sb.toString();
    }
}
