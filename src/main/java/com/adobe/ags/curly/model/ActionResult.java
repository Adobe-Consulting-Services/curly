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
import com.adobe.ags.curly.controller.ActionRunner;
import com.adobe.ags.curly.xml.ResultType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;

public class ActionResult extends RunnerResult<RunnerResult> {

    public static enum RESULT_TYPE {
        NEUTRAL(
                // These are successful or unsuccessful depending on the status code
                Pattern.compile(".*?" + Pattern.quote("<div id=\"Message\">") + "(.*)")
        ),
        WARN(),
        FAIL(
                // Regardless of status code, if these are detected the operation should log as failure
                Pattern.compile(".*?" + Pattern.quote("<div class=\"error\">") + "(.*?)" + Pattern.quote("</div>"))
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

    boolean debugMode = false;
    ActionRunner runner;
    Exception failureCause = null;
    List<String> responseMessage;
    private final static ParsedResponseMessage UNKNOWN_RESPONSE = 
            new ParsedResponseMessage(RESULT_TYPE.WARN, ApplicationState.getMessage(COULD_NOT_DETECT_RESPONSE_STATUS));

    public ActionResult(ActionRunner runner) {
        this.runner = runner;
        reportRow().add(new SimpleStringProperty(runner.getAction().getName()));
        reportRow().add(new SimpleStringProperty(""));
        reportRow().add(new SimpleIntegerProperty(0));
        reportRow().add(new SimpleStringProperty(""));
        reportRow().add(getDuration());
        setStatus(NOT_STARTED, 0, "");
    }

    private void setStatus(String key, int responseCode, String statusMessage) {
        Platform.runLater(() -> {
            ((StringProperty) reportRow().get(1)).set(ApplicationState.getMessage(key));
            ((IntegerProperty) reportRow().get(2)).set(responseCode);
            ((StringProperty) reportRow().get(3)).set(statusMessage);
        });
    }

    public void setException(Exception ex) {
        percentSuccess().set(0);
        failureCause = ex;
        setStatus(COMPLETED_UNSUCCESSFUL, -1, ex.getMessage());
    }

    public void processHttpResponse(CloseableHttpResponse httpResponse, ResultType resultType) throws IOException {
        StatusLine status = httpResponse.getStatusLine();
        String statusKey = COMPLETED_SUCCESSFUL;
        boolean successfulResponseCode = false;
        if (status.getStatusCode() >= 200 && status.getStatusCode() < 400) {
            successfulResponseCode = true;
        } else {
            statusKey = COMPLETED_UNSUCCESSFUL;
        }
        String resultMessage = "";
        if (resultType == ResultType.HTML) {
            ParsedResponseMessage message = extractHtmlMessage(httpResponse).orElse(UNKNOWN_RESPONSE);
            if (message.type == RESULT_TYPE.FAIL) {
                successfulResponseCode = false;
                statusKey = COMPLETED_UNSUCCESSFUL;
            }
            resultMessage = status.getReasonPhrase() + " / " + message.message;
        } else {
            resultMessage = status.getReasonPhrase();
        }
        percentSuccess().set(successfulResponseCode ? 1 : 0);
        invalidateBindings();
        setStatus(statusKey, status.getStatusCode(), resultMessage);
    }

    private Optional<ParsedResponseMessage> extractHtmlMessage(CloseableHttpResponse httpResponse) throws IOException {
        if (httpResponse == null || httpResponse.getEntity() == null) {
            return Optional.empty();
        }
        InputStreamReader reader = new InputStreamReader(httpResponse.getEntity().getContent());
        Stream<String> lines = new BufferedReader(reader).lines();
        if (debugMode) {
            responseMessage = lines.collect(Collectors.toList());
            lines = responseMessage.stream();
        }
        return lines.map(this::parseMessagePatterns).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    private Optional<ParsedResponseMessage> parseMessagePatterns(String line) {
        for (RESULT_TYPE resultType : RESULT_TYPE.values()) {
            for (Pattern p : resultType.patterns) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    return Optional.of(new ParsedResponseMessage(resultType, m.group(1)));
                }
            }
        }
        return Optional.empty();
    }

    public void updateProgress(double d) {
        Platform.runLater(() -> {
            percentComplete().set(d);
        });
    }

    @Override
    public String toHtml(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        reportRow().forEach(value -> sb.append("<td>").append(value.getValue().toString()).append("</td>"));

        sb.append("<td><pre>");
        if (responseMessage != null) {
            responseMessage.stream().map(str -> str.replaceAll("<", "&lt;") + "\n").forEach(sb::append);
        }
        sb.append("</pre></td>");
        sb.append("</tr>");
        return sb.toString();
    }
}
