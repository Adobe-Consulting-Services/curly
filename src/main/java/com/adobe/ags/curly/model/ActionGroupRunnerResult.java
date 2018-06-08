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
import com.adobe.ags.curly.xml.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;

public class ActionGroupRunnerResult extends RunnerResult<ActionResult> {
    StringBinding successOrNot;
    StringBinding completeStatus;
    List<Binding> allBindings = new ArrayList<>();
    
    String task;

    public ActionGroupRunnerResult(String taskName, List<Action> actions, Map<String, String> variables, Set<String> reportColumns) {
        task = taskName;
        buildRow(variables, reportColumns);
    }

    private void buildRow(Map<String, String> variables, Set<String> reportColumns) {
        successOrNot = Bindings.when(completelySuccessful())
                .then(ApplicationState.getMessage(COMPLETED_SUCCESSFUL))
                .otherwise(ApplicationState.getMessage(COMPLETED_UNSUCCESSFUL));
        completeStatus = Bindings.when(Bindings.greaterThanOrEqual(percentComplete(), 1))
                .then(successOrNot)
                .otherwise(ApplicationState.getMessage(INCOMPLETE));
        
        reportRow().add(new ReadOnlyStringWrapper(task));
        reportRow().add(completeStatus);
        reportRow().add(percentCompleteString().concat(" complete"));
        reportRow().add(percentSuccessString().concat(" success"));
        reportRow().add(getDuration());
        reportColumns.forEach((colName) -> reportRow().add(new SimpleStringProperty(variables.get(colName))));
        allBindings.add(successOrNot);
        allBindings.add(completeStatus);
    }
    
    @Override
    public void invalidateBindings() {
        allBindings.forEach(Binding::invalidate);
        super.invalidateBindings();
    }
    
    @Override
    public String toHtml(int level) {
        invalidateBindings();
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        reportRow().forEach(value->sb.append("<td>").append(String.valueOf(value.getValue())).append("</td>"));
        sb.append("</tr>");
        if (level > 1) {
            getDetails().forEach(result-> sb.append(result.toHtml(level)));
        }
        return sb.toString();
    }
}