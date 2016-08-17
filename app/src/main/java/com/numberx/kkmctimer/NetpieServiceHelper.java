package com.numberx.kkmctimer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Comdet Phaudphut on 03-Apr-16.
 */
public class NetpieServiceHelper {
    private static String AppId="";
    private static String ApiKey="";
    private static String host = "";
    private static Context mContext;
    public static void Init(Context c,String nHost,String appId, String apiKey) {
        AppId = appId;
        ApiKey = apiKey;
        host = nHost;
        mContext = c;
    }
    public interface RequestCallback{
        public void ResponseText(String data);
        public void ResponseJson(JSONObject data);
        public void ResponseError(String msg);
    }
    public static void getTopic(String topic,RequestCallback callback) {
        ConnectivityManager connMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String url = "https://"+host+"/topic/"+AppId+topic+"?auth="+ApiKey;
            new RequestTask(callback).execute(url);
        } else {
            Toast.makeText(mContext,R.string.kkmctimer_no_connection,Toast.LENGTH_LONG).show();
        }
    }
    public static void putTopic(String topic,String data,RequestCallback callback){
        putTopic(topic,data,false,callback);
    }
    public static void putTopic(String topic,String data,boolean retain,RequestCallback callback){
        ConnectivityManager connMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String url = "https://"+host+"/topic/"+AppId+topic+"?auth="+ApiKey+((retain)?"&retain":"");
            new PushTask(callback).execute(url,data);
        } else {
            Toast.makeText(mContext,R.string.kkmctimer_no_connection,Toast.LENGTH_LONG).show();
        }
    }
    static class PushTask extends AsyncTask<String,String,String>{
        RequestCallback res;
        PushTask(RequestCallback callback){
            res = callback;
        }
        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                HttpPut puter = new HttpPut(uri[0]);
                puter.setEntity(new StringEntity(uri[1]));
                response = httpclient.execute(puter);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    responseString = out.toString();
                    out.close();
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject json = new JSONObject(result);
                res.ResponseJson(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            res.ResponseText(result);

        }
    }
    static class RequestTask extends AsyncTask<String, String, String> {
        RequestCallback res;
        RequestTask(RequestCallback callback){
            res = callback;
        }
        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    responseString = out.toString();
                    out.close();
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONArray jArray = new JSONArray(result);
                JSONObject json = jArray.getJSONObject(0);
                res.ResponseJson(json);
            } catch (JSONException e) {
                e.printStackTrace();
                res.ResponseError("JSON parse error!");
            } catch (Exception e){
                e.printStackTrace();
                res.ResponseError("Error!");
            } finally {
                res.ResponseText(result);
            }
        }
    }
}
