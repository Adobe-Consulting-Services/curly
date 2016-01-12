/*
 * Copyright 2016 Adobe Global Services.
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
package com.adobe.ags.curly;

import com.adobe.ags.curly.xml.ErrorBehavior;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 *
 * @author brobert
 */
public class ApplicationState {
    private static ApplicationState singleton;
        
    private ApplicationState() {}
    private final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    private final ObjectProperty<ErrorBehavior> errorBehavior = new SimpleObjectProperty<>(ErrorBehavior.IGNORE);
    private ResourceBundle i18n;

    public static ApplicationState getInstance() {
        if (singleton == null) {
            singleton = new ApplicationState();
        }
        return singleton;
    }


    public BooleanProperty runningProperty() {
        return isRunning;
    }

    public ObjectProperty<ErrorBehavior> errorBehaviorProperty() {
        return errorBehavior;
    }

    public static String getMessage(String key) {
        if (singleton == null || singleton.i18n == null) {
            return key;
        } else {
            return singleton.i18n.getString(key);
        }
    }

    public void setResourceBundle(ResourceBundle bundle) {
        i18n = bundle;
    }

    public ResourceBundle getResourceBundle() {
        return i18n;
    }

    
}
