/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package org.wso2.carbon.iot.android.sense.util;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.wso2.carbon.iot.android.sense.constants.SenseConstants;
import org.wso2.carbon.iot.android.sense.transport.TransportHandlerException;
import org.wso2.carbon.iot.android.sense.transport.mqtt.AndroidSenseMQttTransportHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SenseClientAsyncExecutor extends AsyncTask<String, Void, Map<String, String>> {

    private final static String TAG = "SenseService Client";
    private static List<String> cookies;
    public HostnameVerifier SERVER_HOST = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
//            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
            HttpsURLConnection.getDefaultHostnameVerifier();
            return true;
            //return hv.verify(allowHost, session);
        }
    };
    String access_token;
    String refresh_token;
    TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
    };
    private Context context;


    public SenseClientAsyncExecutor(Context context) {
        this.context = context;

    }

    private HttpsURLConnection getTrustedConnection(HttpsURLConnection conn) {
        try {

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            conn.setDefaultSSLSocketFactory(sc.getSocketFactory());

//            urlConnection.setSSLSocketFactory(sslCtx.getSocketFactory());
            return conn;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {

            Log.e(SenseClientAsyncExecutor.class.getName(), "Invalid Certificate");
            return null;
        }

    }

    @Override
    protected Map<String, String> doInBackground(String... parameters) {
        if (android.os.Debug.isDebuggerConnected())
            android.os.Debug.waitForDebugger();
        String response;
        Map<String, String> response_params = new HashMap<>();

        String endpoint = parameters[0];
        String body = parameters[1];
        String option = parameters[2];
        String jsonBody = parameters[3];

        if (jsonBody != null && !jsonBody.isEmpty()) {
            body = jsonBody;
        }

        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }

        Log.v(TAG, "post'" + body + "'to" + url);


        HttpURLConnection conn = null;
        HttpsURLConnection sConn;
        try {

            if (url.getProtocol().toLowerCase().equals("https")) {

                sConn = (HttpsURLConnection) url.openConnection();

                sConn = getTrustedConnection(sConn);
                sConn.setHostnameVerifier(SERVER_HOST);

                conn = sConn;

            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            if (cookies != null) {
                for (String cookie : cookies) {
                    conn.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
                }

            }
            if (conn == null) {
                return null;

            }

            byte[] bytes = body.getBytes();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod(option);
            if (jsonBody != null && !jsonBody.isEmpty()) {
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + LocalRegister.getAccessToken());
            } else {
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            }
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "close");

            // post the request
            int status;

            if (!option.equals("DELETE")) {
                OutputStream out = conn.getOutputStream();
                out.write(bytes);
                out.close();
                // handle the response
                conn.connect();
                status = conn.getResponseCode();
                access_token = conn.getHeaderField("access");
                refresh_token = conn.getHeaderField("refresh");

                LocalRegister.setAccessToken(access_token);
                LocalRegister.setRefreshToken(refresh_token);

                response_params.put("status", String.valueOf(status));
                Log.v("Response Status", status + "" + " access : " + LocalRegister.getAccessToken() + " refresh : " + LocalRegister.getRefreshToken());

                List<String> receivedCookie = conn.getHeaderFields().get("Set-Cookie");
                if(receivedCookie!=null){
                    cookies=receivedCookie;

                }

                try {
                    InputStream inStream = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                            builder.append("\n"); // append a new line
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            inStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // System.out.println(builder.toString());
                    response = builder.toString();
                    response_params.put("response", response);
                    Log.v("Response Message", response);
                } catch (IOException ex) {

                }

            } else {
                status = Integer.valueOf(SenseConstants.Request.REQUEST_SUCCESSFUL);
            }


        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response_params;
    }
}
