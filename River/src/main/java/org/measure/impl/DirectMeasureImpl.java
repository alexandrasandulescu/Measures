package org.measure.impl;

import java.util.ArrayList;
import java.util.List;
import com.modeliosoft.modelio.javadesigner.annotations.objid;
import org.measure.smm.measure.api.IMeasurement;
import org.measure.smm.measure.defaultimpl.measures.DirectMeasure;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.lang.StringBuilder;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.Gson;

import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

class Executable {
  String state;
  String name;
  String platform;
  String execution;
  Object features;
  String sha256;
  String size;
  Object fuzzer;
  Object corpus;
  String id;
}

class Stats {
  int corpus;
  int traced;
  int passed;
  int failed;
  int errored;
}

@objid ("2d633e05-f3cd-4d90-97c3-d4f404a1b44e")
public class DirectMeasureImpl extends DirectMeasure {

  static void trust() throws Exception {
    //Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
    }};

    // Install the all-trusting trust manager
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    // Create all-trusting host name verifier
    HostnameVerifier allHostsValid = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };

    // Install the all-trusting host verifier
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
  }

    @objid ("6adfa71e-eafc-4fa9-9cde-a658277d0fe3")
    @Override
    public List<IMeasurement> getMeasurement() throws Exception {
        trust();
        //String serverURL = getProperty("ServerURL");
        String _jwtToken = getProperty("JWTToken");
        System.out.println(_jwtToken);
        String serverURL = "https://10.18.0.32:8443";
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbklkIjoiNThkMTNkOWRlZWE0NWQxMGE0MjI3NTU2IiwidXNlcklkIjoiNThjYTk4ZGM0MWVlOGE1YjVjMzcwMTU4IiwicGVybWlzc2lvbnMiOlsiZXhlY1JlYWQiLCJleGVjV3JpdGUiLCJ0b2tlbnMiLCJ1c2VycyJdLCJuYW1lIjoidXBsb2FkLWNiIiwiaWF0IjoxNDkwMTA3ODA1fQ.pH6W3HUyV6InQbzCU0wz-PU5PYPi7pxgO1cgIbbfQ-A";

        try {

            // get all executables
            URLConnection getExecsConn = new URL(serverURL + "/api/executables").openConnection();
            getExecsConn.setRequestProperty("Authorization", "Bearer " + jwtToken);
            InputStream execsResponse = getExecsConn.getInputStream();
            InputStreamReader isr = new InputStreamReader(execsResponse, "UTF-8");

            Gson gson = new Gson();
            Executable[] executables = gson.fromJson(isr, Executable[].class);

            List<IMeasurement> result = new ArrayList<IMeasurement>();

            for (int i = 0; i < executables.length; ++i) {
              System.out.println("Reading stats for executable: " + executables[i].id);

              URLConnection getStatsConn = new URL(serverURL + "/api/corpus/" + executables[i].id + "/stats").openConnection();
              getStatsConn.setRequestProperty("Authorization", "Bearer " + jwtToken);
              InputStream statsResponse = getStatsConn.getInputStream();
              InputStreamReader localIsr = new InputStreamReader(statsResponse, "UTF-8");

              Gson localGson = new Gson();
              Stats stats = gson.fromJson(localIsr, Stats.class);
              StatsMeasurement data = new StatsMeasurement();
              System.out.println("Adding stats: corpus: " + stats.corpus +
                  " traced: " + stats.traced + " passed: " + stats.passed +
                  " failed: " + stats.failed + " errored: " + stats.errored);
              data.setValue(stats, executables[i].id);
              result.add(data);
            }

            return result;
        } catch (Exception e) {
            throw new Exception("Error during Measure Execution : " + e.getMessage(), e);
        }
    }

}
