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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;

public class ConnectionManager {

    static private ConnectionManager singleton;

    static public ConnectionManager getInstance() {
        if (singleton == null) {
            singleton = new ConnectionManager();
        }
        return singleton;
    }

    CookieStore cookieStore = new BasicCookieStore();
    PoolingHttpClientConnectionManager connectionManager;
    ThreadLocal<HttpClientContext> sharedContext = new ThreadLocal<>();
    private int httpPoolSize = 4;

    private ConnectionManager() {
    }

    private void createNewConnectionManager() {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(new TrustSelfSignedStrategy());

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    builder.build(), NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", new PlainConnectionSocketFactory())
                    .register("https", sslsf)
                    .build();
            connectionManager = new PoolingHttpClientConnectionManager(r);
            connectionManager.setValidateAfterInactivity(500);
            sharedContext = ThreadLocal.withInitial(HttpClientContext::new);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void resetConnectionManager(int maxPoolSize) {
        if (connectionManager == null || connectionManager.getTotalStats().getAvailable() == 0) {
            createNewConnectionManager();
        }
        if (connectionManager.getMaxTotal() != maxPoolSize || connectionManager.getDefaultMaxPerRoute() != maxPoolSize) {
            httpPoolSize = maxPoolSize;
            connectionManager.setDefaultMaxPerRoute(maxPoolSize);
            connectionManager.setMaxTotal(maxPoolSize);
        }
    }

    public void shutdown() {
        if (connectionManager != null) {
            connectionManager.close();
            connectionManager.shutdown();
        }
        connectionManager = null;
    }

    public CloseableHttpClient getAuthenticatedClient(CredentialsProvider creds) {
        resetConnectionManager(httpPoolSize);
        return HttpClients.custom()
                .setDefaultCredentialsProvider(creds)
                .setDefaultCookieStore(cookieStore)
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true)
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
    }

    public static HttpContext getContext() {
        return getInstance().sharedContext.get();
    }
}
