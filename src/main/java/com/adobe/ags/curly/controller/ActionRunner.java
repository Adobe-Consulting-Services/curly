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

public class ActionRunner implements Runnable {

    public static enum HttpMethod {
        GET, POST, DELETE, HEAD, PUT, TRACE, CONNECT, OPTIONS
    };
    Map<String, String> postVariables = new LinkedTreeMap<>();
    Map<String, String> requestHeaders = new LinkedTreeMap<>();
    Action action;
    String URL;
    HttpMethod httpMethod;
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

        HttpUriRequest request;
        try {
            switch (httpMethod) {
                case GET:
                    request = new HttpGet(URL);
                    break;
                case HEAD:
                    request = new HttpHead(URL);
                    break;
                case DELETE:
                    request = new HttpDelete(URL);
                    break;
                case POST:
                    request = new HttpPost(URL);
                    addPostParams((HttpPost) request);
                    break;
                case PUT:
                    request = new HttpPut(URL);
                    addPostParams((HttpPut) request);
                    break;
                default:
                    throw new UnsupportedOperationException(CurlyApp.getMessage(UNSUPPORTED_METHOD_ERROR) + ": " + httpMethod.name());
            }

            addHeaders(request);
            CloseableHttpResponse httpResponse = client.get().execute(request);
            response.processHttpResponse(httpResponse, action.getResultType());
        } catch (IOException ex) {
            Logger.getLogger(ActionRunner.class.getName()).log(Level.SEVERE, null, ex);
            response.setException(ex);
        } finally {
            response.updateProgress(1);
        }
    }

    private void addHeaders(HttpUriRequest request) {
        requestHeaders.forEach(request::setHeader);
    }

    private void addPostParams(HttpEntityEnclosingRequestBase request) throws UnsupportedEncodingException {
        List<NameValuePair> formParams = new ArrayList<>();
        postVariables.forEach((name, value) -> formParams.add(new BasicNameValuePair(name, value)));
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
        List<String> variableTokens = action.getVariableNames();
        int tokenCounter = 0;
        for (String var : variableTokens) {
            str = str.replaceAll(Pattern.quote("${" + var + "}"), Matcher.quoteReplacement("${" + (tokenCounter++) + "}"));
        }
        return str;
    }

    private String detokenizeParameters(String str) {
        List<String> variableTokens = action.getVariableNames();
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
                String[] nvp = param.split("=");
                if (nvp.length != 2) {
                    throw new ParseException(CurlyApp.getMessage(MISSING_NVP_FORM_ERROR), offset + 1);
                }
                postVariables.put(detokenizeParameters(nvp[0]), detokenizeParameters(nvp[1]));
                return true;
            case 'X':
                try {
                    httpMethod = HttpMethod.valueOf(param.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    throw new ParseException(CurlyApp.getMessage(UNKNOWN_METHOD_ERROR) + " " + param, offset + 1);
                }
                return true;
            case 'h':
                nvp = param.split(":\\s*");
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
            case 'S':
            case '#':
            case 'v':
                //ignored no-parameter flags
                return false;
            default:
                throw new ParseException(CurlyApp.getMessage(UNKNOWN_PARAMETER) + ": " + command, offset);
        }
    }

    private void applyVariables(Map<String, String> variables) {
        List<String> variableTokens = action.getVariableNames();
        variableTokens.forEach((String originalName) -> {
            String[] parts = originalName.split("\\|");
            String var = parts[0];
            String replaceVar = Pattern.quote("${" + originalName + "}");
            URL = URL.replaceAll(replaceVar, variables.get(var));
        });
        applyVariablesToMap(variables, postVariables);
        applyVariablesToMap(variables, requestHeaders);
    }

    private void applyVariablesToMap(Map<String, String> variables, Map<String, String> target) {
        List<String> variableTokens = action.getVariableNames();

        Set removeSet = new HashSet<>();
        Map<String, String> newValues = new HashMap<>();
        variableTokens.forEach((String originalName) -> {
            String[] parts = originalName.split("\\|");
            String var = parts[0];
            String replaceVar = Pattern.quote("${" + originalName + "}");
            postVariables.forEach((param, value) -> {
                String newParam = param.replaceAll(replaceVar, Matcher.quoteReplacement(variables.get(var)));
                value = newValues.containsKey(newParam) ? newValues.get(newParam) : variables.get(var);
                String newValue = param.replaceAll(replaceVar, Matcher.quoteReplacement(value));
                if (!newParam.equals(param) || !newValue.equals(value)) {
                    removeSet.add(param);
                    newValues.put(newParam, newValue);
                }
            });
        });
        postVariables.keySet().removeAll(removeSet);
        postVariables.putAll(newValues);
    }
}