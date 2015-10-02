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
import com.adobe.ags.curly.controller.ActionRunner;
import com.sun.javafx.collections.ObservableListWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;

public class ActionResult implements RunnerResult {

    ActionRunner runner;
    BooleanProperty successfulProperty = new SimpleBooleanProperty(false);
    ObservableList<ObservableValue> reportRow = new ObservableListWrapper<>(new ArrayList<>());
    DoubleProperty completionProperty = new SimpleDoubleProperty(0);
    Exception failureCause = null;

    public ActionResult(ActionRunner runner) {
        this.runner = runner;
        reportRow.add(new SimpleStringProperty(runner.getAction().getName()));
        reportRow.add(new SimpleStringProperty(""));
        reportRow.add(new SimpleStringProperty(""));
        setStatus(NOT_STARTED, "");
    }

    private void setStatus(String key, String statusMessage) {
        Platform.runLater(()->{
            ((StringProperty) reportRow.get(1)).set(CurlyApp.getMessage(key));
            ((StringProperty) reportRow.get(2)).set(statusMessage);
        });
    }

    public void setException(Exception ex) {
        successfulProperty.set(false);
        failureCause = ex;
        setStatus(COMPLETED_UNSUCCESSFUL, ex.getMessage());
    }

    public void processHttpResponse(CloseableHttpResponse httpResponse, Action.ResultType resultType) throws IOException {
        StatusLine status = httpResponse.getStatusLine();
        String statusKey = COMPLETED_SUCCESSFUL;
        if (status.getStatusCode() >= 200 && status.getStatusCode() < 400) {
            Platform.runLater(()->{
                successfulProperty.set(true);                    
            });
        } else {
            statusKey = COMPLETED_UNSUCCESSFUL;
        }
        if (resultType == Action.ResultType.html) {
            setStatus(statusKey, status.getReasonPhrase() + " / " + extractHtmlMessage(httpResponse));
        } else {
            setStatus(statusKey, status.getReasonPhrase());
        }
    }

    public String extractHtmlMessage(CloseableHttpResponse httpResponse) throws IOException {
        InputStreamReader reader = new InputStreamReader(httpResponse.getEntity().getContent());
        Optional<String> message = new BufferedReader(reader).lines()
                .filter((String line) -> line.contains("div id=\"message\"")).findFirst();
        if (!message.isPresent()) {
            return CurlyApp.getMessage(COULD_NOT_DETECT_RESPONSE_STATUS);
        }
        String msg = message.get();
        return msg.substring(msg.lastIndexOf(">"));
    }

    @Override
    public BooleanProperty successfulProperty() {
        return successfulProperty;
    }

    @Override
    public DoubleProperty percentComplete() {
        return completionProperty;
    }

    @Override
    public ObservableList<ObservableValue> reportRow() {
        return reportRow;
    }

    @Override
    public Collection<RunnerResult> getDetails() {
        return Collections.EMPTY_LIST;
    }

    public void updateProgress(double d) {
        Platform.runLater(()->{
            completionProperty.set(d);
        });
    }
}
