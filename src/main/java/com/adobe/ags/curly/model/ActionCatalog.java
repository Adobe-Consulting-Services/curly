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

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class ActionCatalog {
    static ActionCatalog singleton;
    public static ActionCatalog getInstance() {
        if (singleton == null) {
            singleton = new ActionCatalog();
        }
        return singleton;
    }
    
    public static Map<String, Action> getCatalog() {
        return getInstance().actionCatalog;
    }
    
    Map<String, Action> actionCatalog;

    private ActionCatalog() {
        readGlobalCatalog();
    }

    private void readGlobalCatalog() {
        actionCatalog = new TreeMap<>();
        Gson gson = new Gson();
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("actions.json");
        
        Action[] actions = gson.fromJson(new InputStreamReader(resourceStream), Action[].class);
        for (Action a : actions) {
            actionCatalog.put(a.getName(), a);
        }
    }   
}