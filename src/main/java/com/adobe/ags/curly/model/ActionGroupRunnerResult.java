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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;

public class ActionGroupRunnerResult extends RunnerResult<ActionResult> {

    String task;

    public ActionGroupRunnerResult(String taskName, List<Action> actions, Map<String, String> variables, Set<String> reportColumns) {
        task = taskName;
        buildRow(variables, reportColumns);
    }

    private void buildRow(Map<String, String> variables, Set<String> reportColumns) {
        StringBinding successOrNot = Bindings.when(completelySuccessful())
                .then(CurlyApp.getMessage(COMPLETED_SUCCESSFUL))
                .otherwise(CurlyApp.getMessage(COMPLETED_UNSUCCESSFUL));
        reportRow().add(new ReadOnlyStringWrapper(task));
        reportRow().add(Bindings.when(Bindings.greaterThanOrEqual(percentComplete(), 1)).then(successOrNot).otherwise(CurlyApp.getMessage(INCOMPLETE)));
        reportRow().add(Bindings.createStringBinding(()->
                String.format("%.0f%%",100.0*percentComplete().get()),percentComplete()));
        reportColumns.forEach((colName) -> reportRow().add(new SimpleStringProperty(variables.get(colName))));
    }
    
    @Override
    public String toHtml(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        reportRow().forEach(value->sb.append("<td>").append(value.getValue().toString()).append("</td>"));
        sb.append("</tr>");
        if (level > 1) {
            getDetails().forEach(result->sb.append(result.toHtml(level)));
        }
        return sb.toString();
    }
}