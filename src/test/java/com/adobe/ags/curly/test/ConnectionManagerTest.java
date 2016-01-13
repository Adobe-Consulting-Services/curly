/*
 * Copyright 2015 Adobe Global Services.
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
package com.adobe.ags.curly.test;

import com.adobe.ags.curly.ConnectionManager;
import com.adobe.ags.curly.controller.AuthHandler;
import java.io.IOException;
import java.util.Base64;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brobert
 */
public class ConnectionManagerTest {
    static TestWebServer webserver;
    public static final String TEST_USER = "USER";
    public static final String TEST_PASSWORD = "PASSWORD";
    public ConnectionManagerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException {
        webserver = TestWebServer.getServer();
    }
    
    @AfterClass
    public static void tearDownClass() {
        ConnectionManager.getInstance().shutdown();
        webserver.shutdown();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void getAuthenticatedConnection() throws IOException {
        webserver.requireLogin = true;
        AuthHandler handler = new AuthHandler(
                new ReadOnlyStringWrapper("localhost:"+TestWebServer.IP_PORT), 
                new ReadOnlyBooleanWrapper(false), 
                new ReadOnlyStringWrapper(TEST_USER), 
                new ReadOnlyStringWrapper(TEST_PASSWORD)
        );
        CloseableHttpClient client = handler.getAuthenticatedClient();
        assertNotNull(client);
        HttpUriRequest request = new HttpGet("http://localhost:"+TestWebServer.IP_PORT+"/testUri");
        client.execute(request);
        Header authHeader = webserver.lastRequest.getFirstHeader("Authorization");
        assertNotNull(authHeader);
        String compareToken = "Basic "+Base64.getEncoder().encodeToString((TEST_USER + ":" + TEST_PASSWORD).getBytes());
        assertEquals("Auth token should be expected format", authHeader.getValue(), compareToken);
    }
}
