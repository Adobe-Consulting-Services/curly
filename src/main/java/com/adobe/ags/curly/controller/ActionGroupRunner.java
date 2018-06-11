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

import com.adobe.ags.curly.ApplicationState;
import com.adobe.ags.curly.CurlyApp;
import com.adobe.ags.curly.xml.Action;
import com.adobe.ags.curly.model.ActionGroupRunnerResult;
import com.adobe.ags.curly.model.ActionResult;
import com.adobe.ags.curly.model.RunnerResult;
import com.adobe.ags.curly.model.TaskRunner;
import com.adobe.ags.curly.xml.ErrorBehavior;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
    Function<Boolean, CloseableHttpClient> clientSupplier;
    Action lastAction;

    public ActionGroupRunner(String taskName, Function<Boolean, CloseableHttpClient> clientSupplier, List<Action> actions, Map<String, String> variables, Set<String> reportColumns) throws ParseException {
        this.actions = new LinkedHashMap<>();
        this.clientSupplier = clientSupplier;
        results = new ActionGroupRunnerResult(taskName, actions, variables, reportColumns);
        actions.forEach((Action action) -> {
            lastAction = action;
            ActionRunner runner = null;
            try {
                runner = new ActionRunner(this::withClient, action, variables);
                this.actions.put(action, runner);
                results.addDetail(runner.response);
            } catch (ParseException ex) {
                Logger.getLogger(ActionGroupRunner.class.getName()).log(Level.SEVERE, null, ex);
                ActionResult response = new ActionResult(action.getName(), ex);
                results.addDetail(response);
                response.setException(ex);
                handleError();
            }
        });
        vars = variables;
    }

    CloseableHttpClient client;

    private Optional<Exception> withClient(Function<CloseableHttpClient, Optional<Exception>> process) {
        if (client == null) {
            client = clientSupplier.apply(false);
        }
        Optional<Exception> ex = process.apply(client);
        if (ex.isPresent() && ex.get() instanceof IllegalStateException) {
            System.err.println("Error in HTTP request - RETRYING: " + ex.get().getMessage());
            ex.get().printStackTrace();
            client = clientSupplier.apply(true);
            ex = process.apply(client);
        }
        if (ex.isPresent()) {
            System.err.println("Error in HTTP request, abandoning client: " + ex.get().getMessage());
            ex.get().printStackTrace();
            client = null;
        }
        return ex;
    }

    @Override
    public RunnerResult getResult() {
        return results;
    }

    @Override
    public void run() {
        getResult().started().set(true);
        actions.keySet().forEach((Action action) -> {
            if (!ApplicationState.getInstance().runningProperty().get() || skipTheRest.get()) {
                return;
            }
            ActionRunner runner = actions.get(action);
            ActionResult response;
            int retry = action.getErrorBehavior() == ErrorBehavior.RETRY ? 3 : 1;
            while (retry-- >= 0) {
                try {
                    runner.run();
                    if (!runner.response.completelySuccessful().get()) {
                        if (retry > 0) {
                            System.err.println("Error in HTTP request - RETRYING " + runner.URL);
                            Thread.sleep(250);
                            continue;
                        } else {
                            handleError();
                        }
                    } else if (action.getErrorBehavior() == ErrorBehavior.SKIP_IF_SUCCESSFUL) {
                        skipTheRest.set(true);
                    } else {
                        break;
                    }
                } catch (Exception ex) {
                    Logger.getLogger(ActionGroupRunner.class.getName()).log(Level.SEVERE, null, ex);
                    if (retry <= 0) {
                        response = new ActionResult(runner);
                        response.setException(ex);
                        handleError();
                    }
                }
            }
        });
        results.updateComputations();
    }

    private void handleError() {
        ErrorBehavior behavior;
        if (lastAction == null || lastAction.getErrorBehavior() == ErrorBehavior.GLOBAL) {
            behavior = ApplicationState.getInstance().errorBehaviorProperty().get();
        } else {
            behavior = lastAction.getErrorBehavior();
        }
        switch (behavior) {
            case HALT:
                ApplicationState.getInstance().runningProperty().set(false);
                break;
            case SKIP:
                skipTheRest.set(true);
                break;
            case IGNORE:
            case RETRY:
                break;
        }
    }
}
