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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Login {
    private final StringProperty host = new SimpleStringProperty();
    private final StringProperty userName = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final BooleanProperty ssl = new SimpleBooleanProperty();
    private final BooleanBinding requiredFieldsPresent;
    private final StringProperty statusMessage = new SimpleStringProperty();
    private final BooleanProperty loginConfirmed = new SimpleBooleanProperty();

    public Login() {
        requiredFieldsPresent = Bindings.isNotEmpty(host)
                .and(Bindings.isNotEmpty(userName))
                .and(Bindings.isNotEmpty(password));
    }
    
    /**
     * @return the host
     */
    public StringProperty hostProperty() {
        return host;
    }

    /**
     * @return the userName
     */
    public StringProperty userNameProperty() {
        return userName;
    }

    /**
     * @return the password
     */
    public StringProperty passwordProperty() {
        return password;
    }

    /**
     * @return the ssl
     */
    public BooleanProperty sslProperty() {
        return ssl;
    }

    /**
     * @return the requiredFieldsPresent
     */
    public BooleanBinding requiredFieldsPresentProperty() {
        return requiredFieldsPresent;
    }

    /**
     * @return the statusMessage
     */
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    /**
     * @return the loginConfirmed
     */
    public BooleanProperty loginConfirmedProperty() {
        return loginConfirmed;
    }

}
