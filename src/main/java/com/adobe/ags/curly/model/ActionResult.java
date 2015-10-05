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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;

public class ActionResult implements RunnerResult {

    boolean debugMode = false;
    ActionRunner runner;
    BooleanProperty successfulProperty = new SimpleBooleanProperty(false);
    ObservableList<ObservableValue> reportRow = new ObservableListWrapper<>(new ArrayList<>());
    DoubleProperty completionProperty = new SimpleDoubleProperty(0);
    Exception failureCause = null;

    public ActionResult(ActionRunner runner) {
        this.runner = runner;
        reportRow.add(new SimpleStringProperty(runner.getAction().getName()));
        reportRow.add(new SimpleStringProperty(""));
        reportRow.add(new SimpleIntegerProperty(0));
        reportRow.add(new SimpleStringProperty(""));
        setStatus(NOT_STARTED, 0, "");
    }

    private void setStatus(String key, int responseCode, String statusMessage) {
        Platform.runLater(() -> {
            ((StringProperty) reportRow.get(1)).set(CurlyApp.getMessage(key));
            ((IntegerProperty) reportRow.get(2)).set(responseCode);
            ((StringProperty) reportRow.get(3)).set(statusMessage);
        });
    }

    public void setException(Exception ex) {
        successfulProperty.set(false);
        failureCause = ex;
        setStatus(COMPLETED_UNSUCCESSFUL, -1, ex.getMessage());
    }

    public void processHttpResponse(CloseableHttpResponse httpResponse, Action.ResultType resultType) throws IOException {
        StatusLine status = httpResponse.getStatusLine();
        String statusKey = COMPLETED_SUCCESSFUL;
        if (status.getStatusCode() >= 200 && status.getStatusCode() < 400) {
            Platform.runLater(() -> {
                successfulProperty.set(true);
            });
        } else {
            statusKey = COMPLETED_UNSUCCESSFUL;
        }
        if (resultType == Action.ResultType.html) {
            setStatus(statusKey, status.getStatusCode(), status.getReasonPhrase() + " / " + extractHtmlMessage(httpResponse));
        } else {
            setStatus(statusKey, status.getStatusCode(), status.getReasonPhrase());
        }
    }

    List<String> responseMessage;

    public String extractHtmlMessage(CloseableHttpResponse httpResponse) throws IOException {
        InputStreamReader reader = new InputStreamReader(httpResponse.getEntity().getContent());
        Optional<String> message;
        if (debugMode) {
            responseMessage = new BufferedReader(reader).lines().collect(Collectors.toList());
            message = responseMessage.stream().filter(line -> line.contains("div id=\"Message\"")).findFirst();
        } else {
            message = new BufferedReader(reader).lines()
                    .filter((String line) -> line.contains("div id=\"Message\"")).findFirst();
        }

        if (!message.isPresent()) {
            return CurlyApp.getMessage(COULD_NOT_DETECT_RESPONSE_STATUS);
        }
        String msg = message.get();
        return msg.substring(msg.lastIndexOf("\"Message\">") + 10);
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
        Platform.runLater(() -> {
            completionProperty.set(d);
        });
    }

    @Override
    public String toHtml(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        reportRow.forEach(value -> sb.append("<td>").append(value.getValue().toString()).append("</td>"));
        sb.append("<td><pre>");
        if (responseMessage != null) {
            responseMessage.stream().map(str -> str.replaceAll("<", "&lt;") + "\n").forEach(sb::append);
        }
        sb.append("</pre></td>");
        sb.append("</tr>");
        return sb.toString();
    }
}
