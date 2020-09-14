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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;

public class ConnectionManager {

    static public boolean USE_LOGIN_COOKIE = true;
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
        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true)
                .setRedirectStrategy(new LaxRedirectStrategy());
        if (!USE_LOGIN_COOKIE) {
            builder.setDefaultCredentialsProvider(creds);
        }
        return builder.build();
    }

    private static final String LOGIN_URL = "/libs/granite/core/content/login.html/j_security_check";

    public static int performLogin(CloseableHttpClient client, CredentialsProvider creds, String urlBase) throws IOException {
        if (!USE_LOGIN_COOKIE) {
            return -1;
        }
        UsernamePasswordCredentials loginCreds = (UsernamePasswordCredentials) creds.getCredentials(AuthScope.ANY);
        try {
            HttpPost post = new HttpPost(urlBase + LOGIN_URL);
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("j_validate", "true"));
            params.add(new BasicNameValuePair("j_username", loginCreds.getUserName()));
            params.add(new BasicNameValuePair("j_password", loginCreds.getPassword()));
            post.setEntity(new UrlEncodedFormEntity(params));
            int responseCode = client.execute(post).getStatusLine().getStatusCode();
            return responseCode;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    public static HttpClientContext getContext() {
        return getInstance().sharedContext.get();
    }
}
