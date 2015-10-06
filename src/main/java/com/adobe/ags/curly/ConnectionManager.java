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
package com.adobe.ags.curly;

import java.util.concurrent.TimeUnit;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class ConnectionManager {
    static private ConnectionManager singleton;
    
    static public ConnectionManager getInstance() {
        if (singleton == null) {
            singleton = new ConnectionManager();
        }
        return singleton;
    }
    
    PoolingHttpClientConnectionManager connectionManager;
    
    private ConnectionManager() {
    }
    
    public void setPoolSize(int size) {
        connectionManager.setDefaultMaxPerRoute(size);
        connectionManager.setMaxTotal(size);
    }
    
    public CloseableHttpClient getAuthenticatedClient(CredentialsProvider creds) {
        CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setDefaultCredentialsProvider(creds)
                .build();
        return httpclient;
    }
    
}
