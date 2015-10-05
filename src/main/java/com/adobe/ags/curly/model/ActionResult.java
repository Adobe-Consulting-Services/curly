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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        boolean successfulResponseCode = false;
        if (status.getStatusCode() >= 200 && status.getStatusCode() < 400) {
            successfulResponseCode = true;
        } else {
            statusKey = COMPLETED_UNSUCCESSFUL;
        }
        if (resultType == Action.ResultType.html) {
            ParsedResponseMessage message = extractHtmlMessage(httpResponse).orElse(UNKNOWN_RESPONSE);
            if (message.type == RESULT_TYPE.FAIL) {
                successfulResponseCode = false;
                statusKey = COMPLETED_UNSUCCESSFUL;
            }
            setStatus(statusKey, status.getStatusCode(), status.getReasonPhrase() + " / " + message.message);
        } else {
            setStatus(statusKey, status.getStatusCode(), status.getReasonPhrase());
        }
        successfulProperty.set(successfulResponseCode);
    }

    List<String> responseMessage;
    private final static ParsedResponseMessage UNKNOWN_RESPONSE = new ParsedResponseMessage(RESULT_TYPE.WARN, CurlyApp.getMessage(COULD_NOT_DETECT_RESPONSE_STATUS));

    public static enum RESULT_TYPE {
        NEUTRAL(
            // These are successful or unsuccessful depending on the status code
                Pattern.compile(".*?"+Pattern.quote("<div id=\"Message\">") + "(.*)")
        ),
        WARN(),
        FAIL(
            // Regardless of status code, if these are detected the operation should log as failure
                Pattern.compile(".*?"+Pattern.quote("<div class=\"error\">") + "(.*?)" + Pattern.quote("</div>"))
        );

        Pattern[] patterns;

        RESULT_TYPE(Pattern... p) {
            patterns = p;
        }
    };

    private static class ParsedResponseMessage {

        RESULT_TYPE type;
        String message;

        public ParsedResponseMessage(RESULT_TYPE type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    private Optional<ParsedResponseMessage> extractHtmlMessage(CloseableHttpResponse httpResponse) throws IOException {
        InputStreamReader reader = new InputStreamReader(httpResponse.getEntity().getContent());
        Stream<String> lines = new BufferedReader(reader).lines();
        if (debugMode) {
            responseMessage = lines.collect(Collectors.toList());
            lines = responseMessage.stream();
        }
        return lines.map(line -> {
            for (RESULT_TYPE resultType : RESULT_TYPE.values()) {
                for (Pattern p : resultType.patterns) {
                    Matcher m = p.matcher(line);
                    if (m.matches()) {
                        return new ParsedResponseMessage(resultType, line);
                    }
                }
            }
            return null;
        }).filter(e -> e != null).findFirst();
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
