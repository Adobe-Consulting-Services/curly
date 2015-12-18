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

import com.adobe.ags.curly.CurlyApp.ErrorBehavior;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Action implements Serializable {

    /**
     * @return the resultType
     */
    public ResultType getResultType() {
        return resultType;
    }

    /**
     * @param resultType the resultType to set
     */
    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    public static enum ResultType {html, json, plain};
    private String name;
    private String description;
    private String command;
    private ResultType resultType = ResultType.html;
    private ErrorBehavior errorBehavior = ErrorBehavior.GLOBAL;
    private long delay = 0;

    public Action() {
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }
    
    public ErrorBehavior getErrorBehavior() {
        return errorBehavior;
    }
    
    public void setErrorBehavior(ErrorBehavior behavior) {
        errorBehavior = behavior;
    }
    
    public long getDelay() {
        return delay;
    }
    
    public void setDelay(long d) {
        delay = d;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");
    public Set<String> getVariableNames() {
        Set<String> names = new TreeSet();
        if (command != null && !command.isEmpty()) {
            Matcher matches = VARIABLE_PATTERN.matcher(command);
            while (matches.find()) {
                String rawVariableName = matches.group(1);
                String variableName = rawVariableName.contains("|") ? 
                        rawVariableName.substring(0, rawVariableName.indexOf('|')) : 
                        rawVariableName;
                names.add(variableName);
            }
        }
        return names;
    }
    
    public Map<String, String> getVariablesWithDefaults() {
        Map<String, String> variableDefaults = new TreeMap<>();
        if (command != null && !command.isEmpty()) {
            Matcher matches = VARIABLE_PATTERN.matcher(command);
            while (matches.find()) {
                String rawVariableName = matches.group(1);
                String variableName = rawVariableName.contains("|") ? 
                        rawVariableName.substring(0, rawVariableName.indexOf('|')) : 
                        rawVariableName;
                String value = rawVariableName.contains("|") ? 
                        rawVariableName.substring(rawVariableName.indexOf('|')+1) : 
                        null;
                variableDefaults.put(variableName, value);
            }
        }
        return variableDefaults;
    }    
}
