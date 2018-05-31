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

import com.adobe.ags.curly.ApplicationState;
import com.adobe.ags.curly.ConnectionManager;
import static com.adobe.ags.curly.Messages.*;
import com.adobe.ags.curly.model.Login;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;

public class AuthHandler {

    private static final String TEST_PAGE = "/content.json";

    public final Login model;

    public AuthHandler(StringProperty host, BooleanProperty ssl, StringProperty userName, StringProperty password) {
        model = new Login();

        model.hostProperty().bindBidirectional(host);
        model.sslProperty().bindBidirectional(ssl);
        model.userNameProperty().bindBidirectional(userName);
        model.passwordProperty().bindBidirectional(password);

        model.hostProperty().addListener(this::triggerLoginTest);
        model.sslProperty().addListener(this::triggerLoginTest);
        model.userNameProperty().addListener(this::triggerLoginTest);
        model.passwordProperty().addListener(this::triggerLoginTest);

        model.statusMessageProperty().set(ApplicationState.getMessage(INCOMPLETE_FIELDS));
        model.loginConfirmedProperty().set(false);

    }

    public String getUrlBase() {
        StringBuilder builder = new StringBuilder();
        if (model.sslProperty().get()) {
            builder.append("https://");
        } else {
            builder.append("http://");
        }
        builder.append(model.hostProperty().get());
        return builder.toString();
    }

    public CloseableHttpClient getAuthenticatedClient() {
        return ConnectionManager.getInstance().getAuthenticatedClient(getCredentialsProvider());
    }

    private CredentialsProvider getCredentialsProvider() {
        CredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                model.userNameProperty().get(),
                model.passwordProperty().get()
        ));
        return creds;
    }

    /**
     * This allows connection testing to occur atomically without gumming-up the
     * user experience
     */
    AtomicInteger activityCounter = new AtomicInteger();
    ScheduledThreadPoolExecutor testExecutor = new ScheduledThreadPoolExecutor(10);

    private void triggerLoginTest(ObservableValue v, Object oldVal, Object newVal) {
        final int testValue = activityCounter.incrementAndGet();
        testExecutor.schedule(() -> {
            if (activityCounter.get() == testValue) {
                loginTest();
            }
        }, 500, TimeUnit.MILLISECONDS);
    }

    private void loginTest() {
        CloseableHttpClient client = null;
        try {
            if (!model.requiredFieldsPresentProperty().get()) {
                Platform.runLater(() -> {
                    model.loginConfirmedProperty().set(false);
                    model.statusMessageProperty().set(ApplicationState.getMessage(INCOMPLETE_FIELDS));
                });
                return;
            }

            String url = getUrlBase() + TEST_PAGE;
            URL testUrl = new URL(url);
            InetAddress address = InetAddress.getByName(testUrl.getHost());
            if (address == null || isDnsRedirect(address)) {
                throw new UnknownHostException("Unknown host " + testUrl.getHost());
            }

            Platform.runLater(() -> {
                model.loginConfirmedProperty().set(false);
                model.statusMessageProperty().set(ApplicationState.getMessage(ATTEMPTING_CONNECTION));
            });
            client = getAuthenticatedClient();
            HttpGet loginTest = new HttpGet(url);
            HttpResponse response = client.execute(loginTest);
            StatusLine responseStatus = response.getStatusLine();

            if (responseStatus.getStatusCode() >= 200 && responseStatus.getStatusCode() < 300) {
                Platform.runLater(() -> {
                    model.loginConfirmedProperty().set(true);
                    model.statusMessageProperty().set(ApplicationState.getMessage(CONNECTION_SUCCESSFUL));
                });
            } else {
                Platform.runLater(() -> {
                    model.loginConfirmedProperty().set(false);
                    model.statusMessageProperty().set(ApplicationState.getMessage(CONNECTION_ERROR) + responseStatus.getReasonPhrase() + " (" + responseStatus.getStatusCode() + ")");
                });
            }
        } catch (MalformedURLException | IllegalArgumentException | UnknownHostException ex) {
            Logger.getLogger(AuthHandler.class.getName()).log(Level.SEVERE, null, ex);
            Platform.runLater(() -> {
                model.statusMessageProperty().set(ApplicationState.getMessage(CONNECTION_ERROR) + ex.getMessage());
                model.loginConfirmedProperty().set(false);
            });
        } catch (Throwable ex) {
            Logger.getLogger(AuthHandler.class.getName()).log(Level.SEVERE, null, ex);
            Platform.runLater(() -> {
                model.statusMessageProperty().set(ApplicationState.getMessage(CONNECTION_ERROR) + ex.getMessage());
                model.loginConfirmedProperty().set(false);
            });
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) {
                    Logger.getLogger(AuthHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * ISP DNS providers commonly redirect to their own branded search pages in
     * order to drive revenue This greedy business practice can result in a hung
     * connection so we have to detect it and avoid those redirects at all
     * costs.
     *
     * @param address
     * @return
     */
    private boolean isDnsRedirect(InetAddress address) {
        byte[] ip = address.getAddress();
        // Detect TWC rr.com redirects -- note that bytes are signed values 
        // so we have alias them back to positive integers first
        return (ip[0] & 0x0ff) == 198 && (ip[1] & 0x0ff) == 105;
    }
}
