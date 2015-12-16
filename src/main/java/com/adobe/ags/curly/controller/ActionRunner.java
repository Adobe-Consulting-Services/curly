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
import static com.adobe.ags.curly.Messages.*;
import com.adobe.ags.curly.model.Action;
import com.adobe.ags.curly.model.ActionResult;
import com.google.gson.internal.LinkedTreeMap;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class ActionRunner implements Runnable {

    public static final String UTF8 = "UTF-8";

    public static enum HttpMethod {
        GET, POST, DELETE, HEAD, PUT, TRACE, CONNECT, OPTIONS
    };
    Map<String, List<String>> postVariables = new LinkedTreeMap<>();
    Map<String, List<String>> getVariables = new LinkedTreeMap<>();
    Map<String, String> requestHeaders = new LinkedTreeMap<>();
    Action action;
    String URL;
    long delay = -1;
    HttpMethod httpMethod = HttpMethod.GET;
    boolean httpMethodExplicitlySet = false;
    ActionResult response;
    Supplier<CloseableHttpClient> client;

    public ActionRunner(Supplier<CloseableHttpClient> client, Action action, Map<String, String> variables) throws ParseException {
        this.client = client;
        parseCommand(action);
        applyVariables(variables);
        response = new ActionResult(this);
    }

    public Action getAction() {
        return action;
    }

    @Override
    public void run() {
        if (!CurlyApp.getInstance().runningProperty().get()) {
            response.setException(new Exception(CurlyApp.getMessage(ACTIVITY_TERMINATED)));
            return;
        }
        response.updateProgress(0.5);
        
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                response.setException(ex);
                return;
            }
        }

        HttpUriRequest request;
        try {
            switch (httpMethod) {
                case GET:
                    request = new HttpGet(getURL());
                    break;
                case HEAD:
                    request = new HttpHead(getURL());
                    break;
                case DELETE:
                    request = new HttpDelete(getURL());
                    break;
                case POST:
                    request = new HttpPost(getURL());
                    addPostParams((HttpPost) request);
                    break;
                case PUT:
                    request = new HttpPut(getURL());
                    addPostParams((HttpPut) request);
                    break;
                default:
                    throw new UnsupportedOperationException(CurlyApp.getMessage(UNSUPPORTED_METHOD_ERROR) + ": " + httpMethod.name());
            }

            addHeaders(request);
            CloseableHttpResponse httpResponse = client.get().execute(request);
            response.processHttpResponse(httpResponse, action.getResultType());
            EntityUtils.consume(httpResponse.getEntity());
        } catch (IOException | URISyntaxException ex) {
            Logger.getLogger(ActionRunner.class.getName()).log(Level.SEVERE, null, ex);
            response.setException(ex);
        } finally {
            response.updateProgress(1);
        }
    }

    private String getURL() throws URISyntaxException {
        StringBuilder urlBuilder = new StringBuilder();
        String URI = URL.contains("?") ? URL.substring(0, URL.indexOf('?')) : URL;
        URI = URI.replaceAll("\\s", "%20");
        urlBuilder.append(URI);
        final BooleanProperty hasQueryString = new SimpleBooleanProperty(URL.contains("?"));
        getVariables.forEach((key, values) -> {
            if (values != null) {
                values.forEach(value -> {
                    try {
                        urlBuilder.append(hasQueryString.get() ? "&" : "?")
                                .append(URLEncoder.encode(key, UTF8))
                                .append("=")
                                .append(URLEncoder.encode(value != null ? value : "", UTF8));
                        hasQueryString.set(false);
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(ActionRunner.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
        });
        return urlBuilder.toString();
    }

    private void addHeaders(HttpUriRequest request) {
        requestHeaders.forEach(request::setHeader);
    }

    private void addPostParams(HttpEntityEnclosingRequestBase request) throws UnsupportedEncodingException {
        List<NameValuePair> formParams = new ArrayList<>();
        postVariables.forEach((name, values) -> values.forEach(value -> formParams.add(new BasicNameValuePair(name, value))));
        request.setEntity(new UrlEncodedFormEntity(formParams));
    }

    private void parseCommand(Action action) throws ParseException {
        this.action = action;
        String commandStr = tokenizeParameters(action.getCommand());

        List<String> parts = splitByUnquotedSpaces(commandStr);
        URL = detokenizeParameters(parts.remove(parts.size() - 1));
        int offset = 0;
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (part.startsWith("-")) {
                if (part.length() == 2 && i < parts.size() - 1) {
                    if (parseCmdParam(part.charAt(1), parts.get(i + 1), offset)) {
                        i++;
                    }
                } else {
                    parseCmdParam(part.charAt(1), part.substring(2), offset);
                }
            } else {
                throw new ParseException(CurlyApp.getMessage(UNKNOWN_PARAMETER) + ": " + part, offset);
            }
            offset += part.length() + 1;
        }
    }

    static final Pattern UNQUOTED_SPACES = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

    private List<String> splitByUnquotedSpaces(String str) {
        List<String> list = new ArrayList<>();
        Matcher m = UNQUOTED_SPACES.matcher(str);
        while (m.find()) {
            list.add(m.group(1).replace("\"", ""));
        }
        return list;
    }

    private String tokenizeParameters(String str) {
        Set<String> variableTokens = action.getVariableNames();
        int tokenCounter = 0;
        for (String var : variableTokens) {
            String varPattern = Pattern.quote("${") + var + "(\\|.*?)?" + Pattern.quote("}");
            str = str.replaceAll(varPattern, Matcher.quoteReplacement("${" + (tokenCounter++) + "}"));
        }
        return str;
    }

    private String detokenizeParameters(String str) {
        Set<String> variableTokens = action.getVariableNames();
        int tokenCounter = 0;
        for (String var : variableTokens) {
            str = str.replaceAll(Pattern.quote("${" + (tokenCounter++) + "}"), Matcher.quoteReplacement("${" + var + "}"));
        }
        return str;
    }

    private boolean parseCmdParam(char command, String param, int offset) throws ParseException {
        switch (command) {
            case 'F':
                httpMethod = HttpMethod.POST;
                httpMethodExplicitlySet = true;
            case 'd':
                Map<String, List<String>> vars = postVariables;
                if (!httpMethodExplicitlySet) {
                    httpMethod = HttpMethod.POST;
                } else if (httpMethod != HttpMethod.POST) {
                    vars = getVariables;
                }
                int equals = param.indexOf('=');
                if (equals > -1) {
                    String fieldName = detokenizeParameters(param.substring(0, equals));
                    String value = equals < param.length() - 1 ? detokenizeParameters(param.substring(equals + 1)) : null;
                    if (!vars.containsKey(fieldName)) {
                        vars.put(fieldName, new ArrayList<>());
                    }
                    vars.get(fieldName).add(value);
                } else {
                    throw new ParseException(CurlyApp.getMessage(MISSING_NVP_FORM_ERROR), offset + 1);
                }
                return true;
            case 'X':
                try {
                    httpMethod = HttpMethod.valueOf(param.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new ParseException(CurlyApp.getMessage(UNKNOWN_METHOD_ERROR) + " " + param, offset + 1);
                }
                return true;
            case 'h':
                String[] nvp = param.split(":\\s*");
                if (nvp.length != 2) {
                    throw new ParseException(CurlyApp.getMessage(MISSING_NVP_HEADER_ERROR), offset + 1);
                }
                requestHeaders.put(detokenizeParameters(nvp[0]), detokenizeParameters(nvp[1]));
                return true;
            case 'e':
                requestHeaders.put("referer", detokenizeParameters(param));
                return true;
            case 'u':
                // ignored parameterized options
                return true;
            case 'G':
                httpMethodExplicitlySet = true;
                httpMethod = HttpMethod.GET;
            case 'S':
            case '#':
            case 'v':
                //ignored no-parameter flags
                return false;
            case 'w':
                delay = Long.parseLong(param);
                return true;
            default:
                throw new ParseException(CurlyApp.getMessage(UNKNOWN_PARAMETER) + ": " + command, offset);
        }
    }

    private void applyVariables(Map<String, String> variables) {
        Set<String> variableTokens = action.getVariableNames();
        variableTokens.forEach((String originalName) -> {
            String[] parts = originalName.split("\\|");
            String var = parts[0];
            String replaceVar = Pattern.quote("${" + originalName + "}");
            URL = URL.replaceAll(replaceVar, variables.get(var));
        });
        applyMultiVariablesToMap(variables, postVariables);
        applyMultiVariablesToMap(variables, getVariables);
        applyVariablesToMap(variables, requestHeaders);
    }

    private void applyVariablesToMap(Map<String, String> variables, Map<String, String> target) {
        Set<String> variableTokens = action.getVariableNames();

        Set removeSet = new HashSet<>();
        Map<String, String> newValues = new HashMap<>();

        target.forEach((paramName, paramValue) -> {
            StringProperty paramNameProperty = new SimpleStringProperty(paramName);
            variableTokens.forEach((String originalName) -> {
                String[] variableNameParts = originalName.split("\\|");
                String variableName = variableNameParts[0];
                String variableNameMatchPattern = Pattern.quote("${" + originalName + "}");
                String variableValue = Matcher.quoteReplacement(variables.get(variableName));
                //----
                String newParamValue = newValues.containsKey(paramNameProperty.get()) ? newValues.get(paramNameProperty.get()) : paramValue;
                String newParamName = paramNameProperty.get().replaceAll(variableNameMatchPattern, variableValue);
                paramNameProperty.set(newParamName);
                newParamValue = newParamValue.replaceAll(variableNameMatchPattern, variableValue);
                if (!newParamName.equals(paramName) || !newParamValue.equals(paramValue)) {
                    removeSet.add(paramNameProperty.get());
                    removeSet.add(paramName);
                    newValues.put(newParamName, newParamValue);
                }
            });
        });
        target.keySet().removeAll(removeSet);
        target.putAll(newValues);
    }

    private void applyMultiVariablesToMap(Map<String, String> variables, Map<String, List<String>> target) {
        Set<String> variableTokens = action.getVariableNames();

        Set removeSet = new HashSet<>();
        Map<String, List<String>> newValues = new HashMap<>();

        target.forEach((paramName, paramValues) -> {
            StringProperty paramNameProperty = new SimpleStringProperty(paramName);
            variableTokens.forEach((String originalName) -> {
                String[] variableNameParts = originalName.split("\\|");
                String variableName = variableNameParts[0];
                String variableNameMatchPattern = Pattern.quote("${" + originalName + "}");
                String variableValue = Matcher.quoteReplacement(variables.get(variableName));
                String newParamName = paramNameProperty.get().replaceAll(variableNameMatchPattern, variableValue);
                removeSet.add(paramNameProperty.get());
                removeSet.add(paramName);
                if (newValues.get(paramNameProperty.get()) == null) {
                    newValues.put(paramNameProperty.get(), new ArrayList<>(paramValues.size()));
                }
                if (newValues.get(newParamName) == null) {
                    newValues.put(newParamName, new ArrayList<>(paramValues.size()));
                }
                List<String> newParamValues = newValues.get(paramNameProperty.get());
                for (int i = 0; i < paramValues.size(); i++) {
                    String newParamValue = newParamValues != null && newParamValues.size() > i && newParamValues.get(i) != null ? newParamValues.get(i) : paramValues.get(i);
                    newParamValue = newParamValue.replaceAll(variableNameMatchPattern, variableValue);
                    if (newValues.get(newParamName).size() == i) {
                        newValues.get(newParamName).add(newParamValue);
                    } else {
                        newValues.get(newParamName).set(i, newParamValue);
                    }
                }
                if (!paramNameProperty.get().equals(newParamName)) {
                    newValues.remove(paramNameProperty.get());
                }
                paramNameProperty.set(newParamName);
            });
        });
        target.keySet().removeAll(removeSet);
        target.putAll(newValues);
    }
}
