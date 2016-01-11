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

import com.adobe.ags.curly.xml.Action;
import com.adobe.ags.curly.xml.Actions;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.ObservableList;
import javax.xml.bind.JAXB;

public class ActionUtils {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");

    public static Set<String> getVariableNames(Action action) {
        Set<String> names = new TreeSet();
        if (action.getCommand() != null && !action.getCommand().isEmpty()) {
            Matcher matches = VARIABLE_PATTERN.matcher(action.getCommand());
            while (matches.find()) {
                String rawVariableName = matches.group(1);
                String variableName = rawVariableName.contains("|")
                        ? rawVariableName.substring(0, rawVariableName.indexOf('|'))
                        : rawVariableName;
                names.add(variableName);
            }
        }
        return names;
    }

    public static Map<String, String> getVariablesWithDefaults(Action action) {
        Map<String, String> variableDefaults = new TreeMap<>();
        if (action.getCommand() != null && !action.getCommand().isEmpty()) {
            Matcher matches = VARIABLE_PATTERN.matcher(action.getCommand());
            while (matches.find()) {
                String rawVariableName = matches.group(1);
                String variableName = rawVariableName.contains("|")
                        ? rawVariableName.substring(0, rawVariableName.indexOf('|'))
                        : rawVariableName;
                String variableValue = rawVariableName.contains("|")
                        ? rawVariableName.substring(rawVariableName.indexOf('|') + 1)
                        : null;
                variableDefaults.put(variableName, variableValue);
            }
        }
        return variableDefaults;
    }

    public static List<Action> readFromFile(File sourceFile) {
        Actions parsedActions = JAXB.unmarshal(sourceFile, Actions.class);
        return parsedActions.getAction();
    }

    public static void saveToFile(File targetFile, List<Action> actions) {
        Actions out = new Actions();
        out.getAction().addAll(actions);
        JAXB.marshal(out, targetFile);
    }
}
