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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;

public class BatchRunnerResult extends RunnerResult<ActionGroupRunnerResult> {

    LongProperty timeEllapsed = new SimpleLongProperty(0);
    LongProperty timeRemaining = new SimpleLongProperty(0);
    Long startTime = 0L;

    public BatchRunnerResult() {
        reportRow().add(new ReadOnlyStringWrapper("Batch run"));
        
        StringBinding successOrNot = Bindings.when(completelySuccessful())
                .then(CurlyApp.getMessage(COMPLETED_SUCCESSFUL))
                .otherwise(CurlyApp.getMessage(COMPLETED_UNSUCCESSFUL));
        reportRow().add(Bindings.when(completed())
                .then(successOrNot).otherwise(CurlyApp.getMessage(INCOMPLETE)));

        reportRow().add(Bindings.createStringBinding(()->
                String.format("%.0f%%",100.0*percentComplete().get()),percentComplete()));
    }

    public void start() {
        startTime = System.currentTimeMillis();
        percentComplete().addListener(this::updateEstimates);
    }

    public void stop() {
        timeEllapsed.unbind();
        timeRemaining.set(0);
        percentComplete().removeListener(this::updateEstimates);
    }

    private void updateEstimates(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        Long now = System.currentTimeMillis();
        Long ellapsed = now - startTime;
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
    public String toHtml(int level) {
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
