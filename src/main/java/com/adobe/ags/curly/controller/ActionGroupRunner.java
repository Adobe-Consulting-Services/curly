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
import com.adobe.ags.curly.model.Action;
import com.adobe.ags.curly.model.ActionGroupRunnerResult;
import com.adobe.ags.curly.model.ActionResult;
import com.adobe.ags.curly.model.RunnerResult;
import com.adobe.ags.curly.model.TaskRunner;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.http.impl.client.CloseableHttpClient;

public class ActionGroupRunner implements TaskRunner {

    Map<Action, ActionRunner> actions;
    Map<String, String> vars;
    BooleanProperty skipTheRest = new SimpleBooleanProperty(false);
    ActionGroupRunnerResult results;
    Supplier<CloseableHttpClient> clientSupplier;

    public ActionGroupRunner(String taskName, Supplier<CloseableHttpClient> clientSupplier, List<Action> actions, Map<String, String> variables, Set<String> reportColumns) throws ParseException {
        this.actions = new LinkedHashMap<>();
        this.clientSupplier = clientSupplier;
        actions.forEach((action)->{
            ActionRunner runner = null;
            try {
                runner = new ActionRunner(this::getClient, action, variables);
                this.actions.put(action, runner);
            } catch (ParseException ex) {
                Logger.getLogger(ActionGroupRunner.class.getName()).log(Level.SEVERE, null, ex);
                ActionResult response = new ActionResult(runner);
                response.setException(ex);
                handleError();
            }
        });
        vars = variables;
        results = new ActionGroupRunnerResult(taskName, actions, variables, reportColumns);
    }

    CloseableHttpClient client;
    private CloseableHttpClient getClient() {
        if (client == null) {
            client = clientSupplier.get();
        }
        return client;
    }
    
    @Override
    public RunnerResult getResult() {
        return results;
    }
    
    @Override
    public void run() {
        actions.keySet().forEach((Action action) -> {
            if (!CurlyApp.getInstance().runningProperty().get() || skipTheRest.get()) {
                return;
            }
            ActionRunner runner = actions.get(action);
            ActionResult response;
            try {
                runner.run();
                if (!runner.response.completelySuccessful().get()) {
                    handleError();
                }
                response = runner.response;
            } catch (Exception ex) {
                Logger.getLogger(ActionGroupRunner.class.getName()).log(Level.SEVERE, null, ex);
                response = new ActionResult(runner);
                response.setException(ex);
                handleError();
            }
            results.addDetail(response);
        });
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(ActionGroupRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleError() {
        switch (CurlyApp.getInstance().errorBehaviorProperty().get()) {
            case HALT:
                CurlyApp.getInstance().runningProperty().set(false);
                break;
            case SKIP:
                skipTheRest.set(true);
                break;
            case IGNORE:
                break;
        }
    }
}
