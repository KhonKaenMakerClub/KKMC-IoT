/*
 * Copyright (C) 2015 carlosperate http://carlosperate.github.io
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
package com.numberx.kkmctimer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.numberx.kkmctimer.provider.Alarm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Synchronises the Alarms with the KKMCTimer server alarms by providing methods to retrieve, edit,
 * add and delete alarms on the server.
 */
public class KKMCTimerSync {
    private static final String LOG_TAG = "KKMCTimerSync: ";

    private Context mActivityContext;
    private AlarmClockFragment mAlarmFragment;

    // Used to schedule a permanently running background KKMCTimer server check
    private ScheduledExecutorService scheduleServerCheck;

    /**
     * Public constructor. Saves the class context to be able to check the network connectivity
     * and display a progress dialog.
     *
     * @param activityContext Context of the activity (no application context) requesting the sync.
     */
    public KKMCTimerSync(Context activityContext, String alarmFragmentTag) {
        this.mActivityContext = activityContext;
        Activity activity = (Activity) this.mActivityContext;
        this.mAlarmFragment = (AlarmClockFragment)
                activity.getFragmentManager().findFragmentByTag(alarmFragmentTag);
    }

    /**
     * Synchronisation procedure to push all alarms from the KKMCTimer server onto the phone.
     */

    public void syncPushToPhone() {
        final String topic = "/get_all_alarm";
        final String get_topic = "/resp_all_alarm";
        final String id = (new Random()).nextInt()+"";
        NetpieServiceHelper.putTopic(topic, id, new NetpieServiceHelper.RequestCallback() {
            @Override
            public void ResponseText(String data) { }
            @Override
            public void ResponseJson(final JSONObject data) {
                final Handler mHandler = new Handler();
                final Runnable outer = new Object() {
                    final Runnable ticker = new Runnable() {
                        @Override
                        public void run() {
                            if(data == null) return;
                            NetpieServiceHelper.getTopic(get_topic, new NetpieServiceHelper.RequestCallback() {
                                @Override
                                public void ResponseText(String data) {
                                }
                                @Override
                                public void ResponseJson(JSONObject data) {
                                    try {
                                        String val = data.getString("payload");
                                        JSONObject obj = new JSONObject(val);
                                        int last_id = obj.getInt("last_id");
                                        if(last_id!=Integer.parseInt(id)){
                                            mHandler.postDelayed(ticker, 5000);
                                        }else{
                                            JSONArray jAlarms = obj.getJSONArray("alarms");
                                            List<Alarm> serverAlarms = new LinkedList<Alarm>();
                                            for (int i=0; i<jAlarms.length(); i++) {
                                                serverAlarms.add(alarmFromJson(jAlarms.getJSONObject(i)));
                                                if (Log.LOGV) Log.v("Alarm from server: " + serverAlarms.get(i).toString());
                                            }
                                            // Get local alarms, pass
                                            ContentResolver cr = mActivityContext.getContentResolver();
                                            List<Alarm> localAlarms = Alarm.getAlarms(cr, null);

                                            // Now we have lists of all the alarms, because we are pushing to the phone check the
                                            // current local alarms and remove them if they are not in the list
                                            for (Alarm localAlarm: localAlarms) {
                                                toNextLocalAlarmIteration: {
                                                    for (Alarm serverAlarm : serverAlarms) {
                                                        if (localAlarm.kkmctimerId == serverAlarm.kkmctimerId) {
                                                            // Because we are pushing to the phone update the alarm to whatever is in
                                                            // the server, including the server timestamp
                                                            copyAndroidProperties(localAlarm, serverAlarm);
                                                            mAlarmFragment.asyncUpdateAlarm(serverAlarm, false, true);
                                                            serverAlarms.remove(serverAlarm);
                                                            break toNextLocalAlarmIteration;
                                                        }
                                                    }
                                                    // This is only executed if a match between server and local alarm was not found
                                                    mAlarmFragment.asyncDeleteAlarm(localAlarm, null, true);
                                                }
                                            }
                                            // The rest of the serverAlarms are new to the phone and present in the server
                                            for (Alarm serverAlarm : serverAlarms) {
                                                mAlarmFragment.asyncAddAlarm(serverAlarm, true);
                                            }
                                        }
                                    } catch (Exception e) {
                                        if ((e instanceof JSONException) || (e instanceof NullPointerException)) {
                                            launchToast(R.string.kkmctimer_sync_fail);
                                            Log.w(LOG_TAG + "Exception reading callback from push to phone operation: " + e);
                                            return;
                                        } else {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }

                                @Override
                                public void ResponseError(String msg) {
                                    Toast.makeText(mActivityContext,"Error : "+msg,Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    };
                }.ticker;
                mHandler.post(outer);
            }

            @Override
            public void ResponseError(String msg) {
                Toast.makeText(mActivityContext,"Error : "+msg,Toast.LENGTH_LONG).show();
            }
        });
    }
    /**
     * Adds an alarm to the KKMCTimer server.
     *
     * @param alarm New Alarm to add to KKMCTimer server.
     */
    public void addServerAlarm(final Alarm alarm) {
        // First check if alarm has no associated KKMCTimer server ID
        if (alarm.kkmctimerId == Alarm.INVALID_ID) {
            String topic = "/add_alarm";
            final int kkmctimerId = (new Random()).nextInt(99999);
            alarm.kkmctimerId = kkmctimerId;
            try {
                String buf = alarm.kkmctimerId+":";
                buf += alarm.hour+":";
                buf += alarm.minutes+":";
                buf += (alarm.daysOfWeek.isMondayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isTuesdayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isWednesdayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isThursdayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isFridayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isSaturdayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isSundayEnabled()?"1":"0")+":";
                buf += (alarm.enabled?"1":"0")+":";
                buf += alarm.label+":";
                buf += alarm.timestamp;

                NetpieServiceHelper.putTopic(topic, buf, new NetpieServiceHelper.RequestCallback() {
                    @Override
                    public void ResponseText(String data) { }
                    @Override
                    public void ResponseJson(JSONObject data) {
                        try {
                            int code = data.getInt("code");
                            if (code == 200) {
                                // We need the alarm back before we can edit the KKMCTimer ID
                                ContentResolver cr = mActivityContext.getContentResolver();
                                Alarm addedAlarm = Alarm.getAlarm(cr, alarm.id);
                                addedAlarm.kkmctimerId = kkmctimerId;
                                // Last argument causes the bypass of the Alarm.updateAlarm() automatic timestamp
                                // and the edit of the alarm in the KKMCTimer server
                                mAlarmFragment.asyncUpdateAlarm(addedAlarm, false, true);
                                launchToast(R.string.kkmctimer_add_successful);
                            } else {
                                launchToast(R.string.kkmctimer_add_unsuccessful);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void ResponseError(String msg) {
                        Toast.makeText(mActivityContext,"Error : "+msg,Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            launchToast(R.string.kkmctimer_add_existing);
        }
    }

    /**
     * Edits an alarm from the KKMCTimer server.
     *
     * @param alarm KKMCTimer Alarm to edit.
     */
    public void editServerAlarm(final Alarm alarm) {
        // First check if alarm has an associated KKMCTimer server ID
        if (alarm.kkmctimerId != Alarm.INVALID_ID) {
            String topic = "/edit_alarm";
            JSONObject json = new JSONObject();
            try {
                /*json.put("id", alarm.kkmctimerId);
                json.put("hour", alarm.hour);
                json.put("minute", alarm.minutes);
                json.put("monday",alarm.daysOfWeek.isMondayEnabled());
                json.put("tuesday",alarm.daysOfWeek.isTuesdayEnabled());
                json.put("wednesday", alarm.daysOfWeek.isWednesdayEnabled());
                json.put("thursday", alarm.daysOfWeek.isThursdayEnabled());
                json.put("friday",alarm.daysOfWeek.isFridayEnabled());
                json.put("saturday",alarm.daysOfWeek.isSaturdayEnabled());
                json.put("sunday",alarm.daysOfWeek.isSundayEnabled());
                json.put("enabled", alarm.enabled);
                json.put("label", alarm.label);*/
                String buf = alarm.kkmctimerId+":";
                buf += alarm.hour+":";
                buf += alarm.minutes+":";
                buf += (alarm.daysOfWeek.isMondayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isTuesdayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isWednesdayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isThursdayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isFridayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isSaturdayEnabled()?"1":"0")+":";
                buf += (alarm.daysOfWeek.isSundayEnabled()?"1":"0")+":";
                buf += (alarm.enabled?"1":"0")+":";
                buf += alarm.label;
                NetpieServiceHelper.putTopic(topic, buf , new NetpieServiceHelper.RequestCallback() {
                    @Override
                    public void ResponseText(String data) { }
                    @Override
                    public void ResponseJson(JSONObject data) {
                        try {
                            int code = data.getInt("code");
                            if (code == 200) {
                                ContentResolver cr = mActivityContext.getContentResolver();
                                Alarm editedAlarm = Alarm.getAlarmLightuppiId(cr, alarm.kkmctimerId);
                                //editedAlarm.timestamp = newTimestamp;
                                // Last argument causes the bypass of the Alarm.updateAlarm() automatic timestamp
                                // and the edit of the alarm in the KKMCTimer server
                                mAlarmFragment.asyncUpdateAlarm(editedAlarm, false, true);
                                launchToast(R.string.kkmctimer_edit_successful);
                            } else {
                                launchToast(R.string.kkmctimer_edit_unsuccessful);
                            }
                        }catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void ResponseError(String msg) {
                        Toast.makeText(mActivityContext,"Error : "+msg,Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            launchToast(R.string.kkmctimer_no_server_ID);
        }
    }

    /**
     * Deletes an alarm from the KKMCTimer server.
     *
     * @param alarm KKMCTimer Alarm to delete.
     */
    public void deleteServerAlarm(Alarm alarm) {
        // First check if alarm has an associated KKMCTimer server ID
        if (alarm.kkmctimerId != Alarm.INVALID_ID) {
            String topic = "/delete_alarm";
            NetpieServiceHelper.putTopic(topic, Long.toString(alarm.kkmctimerId), new NetpieServiceHelper.RequestCallback() {
                @Override
                public void ResponseText(String data) {
                }

                @Override
                public void ResponseJson(JSONObject data) {
                    try {
                        int code = data.getInt("code");
                        if (code == 200) {
                            launchToast(R.string.kkmctimer_delete_successful);
                        } else {
                            launchToast(R.string.kkmctimer_delete_unsuccessful);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void ResponseError(String msg) {
                    Toast.makeText(mActivityContext,"Error : "+msg,Toast.LENGTH_LONG).show();
                }
            });
        } else {
            launchToast(R.string.kkmctimer_no_server_ID);
        }
    }

    /**
     * Uses AsyncTask to create a task away from the main UI thread, where the wrapper class is
     * called from. This task takes a URL string and uses it to create an HttpUrlConnection.
     * Once the connection has been established, the AsyncTask downloads the contents of the
     * web page as an InputStream. Finally, the InputStream is converted into a string, which is
     * displayed in the UI by the AsyncTask's onPostExecute method.
     */

    private Alarm alarmFromJson(JSONObject aJson) {
        // If mSelectedAlarm is null then we're creating a new alarm.
        Alarm alarm = new Alarm();
        alarm.alert = RingtoneManager.getActualDefaultRingtoneUri(
                mActivityContext, RingtoneManager.TYPE_ALARM);
        if (alarm.alert == null) {
            alarm.alert = Uri.parse("content://settings/system/alarm_alert");
        }
        // Setting the vibrate option to always true, as there is no attribute in KKMCTimer
        alarm.vibrate = true;
        // Setting the 'delete after use' option to always false, as there is no such feature in
        // the KKMCTimer alarm system and all alarms are repeatable
        alarm.deleteAfterUse = false;

        // Parsing the JSON data
        try {
            alarm.hour = aJson.getInt("hour");
            alarm.minutes = aJson.getInt("minute");
            alarm.enabled = aJson.getBoolean("enabled");
            alarm.daysOfWeek.setDaysOfWeek(aJson.getBoolean("monday"), Calendar.MONDAY);
            alarm.daysOfWeek.setDaysOfWeek(aJson.getBoolean("tuesday"), Calendar.TUESDAY);
            alarm.daysOfWeek.setDaysOfWeek(aJson.getBoolean("wednesday"), Calendar.WEDNESDAY);
            alarm.daysOfWeek.setDaysOfWeek(aJson.getBoolean("thursday"), Calendar.THURSDAY);
            alarm.daysOfWeek.setDaysOfWeek(aJson.getBoolean("friday"), Calendar.FRIDAY);
            alarm.daysOfWeek.setDaysOfWeek(aJson.getBoolean("saturday"), Calendar.SATURDAY);
            alarm.daysOfWeek.setDaysOfWeek(aJson.getBoolean("sunday"), Calendar.SUNDAY);
            alarm.label = aJson.getString("label");
            alarm.kkmctimerId = aJson.getLong("id");
            alarm.timestamp = aJson.getLong("timestamp");
        } catch (JSONException e) {
            Log.w(LOG_TAG + " JSONException: " + e.toString());
            alarm = null;
        }
        return alarm;
    }

    /**
     * Because the LightUpDrop Alarms have more data than the KKMCTimer Alarms this method is used
     * to copy the properties over from one alarm to the other.
     *
     * @param droid Alarm local to the phone.
     * @param pi Alarm coming from the KKMCTimer server.
     */
    private void copyAndroidProperties(Alarm droid, Alarm pi) {
        pi.id = droid.id;
        pi.alert = droid.alert;
        pi.vibrate = droid.vibrate;
        pi.deleteAfterUse = droid.deleteAfterUse;
    }

    /**
     * Initiates a background thread to check if the KKMCTimer server is reachable.
     *
     * @param guiHandler Handler for the activity GUI, for which to send one of the two runnables.
     * @param online Runnable to execute in the Handler if the server is online.
     * @param offline Runnable to execute in the Handler if the server is offline.
     */
    public void startBackgroundServerCheck(
            final Handler guiHandler, final Runnable online, final Runnable offline) {
        // Check for network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                mActivityContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if ((networkInfo != null) && networkInfo.isConnected() &&
                ((scheduleServerCheck == null) || scheduleServerCheck.isShutdown())) {
            // Get the ping address
            /*final Uri.Builder pingUri = getServerUriBuilder();
            pingUri.appendPath("ping");
            // Schedule the background server check
            scheduleServerCheck = Executors.newScheduledThreadPool(1);
            scheduleServerCheck.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    int response = 0;
                    try {
                        URL url = new URL(pingUri.build().toString());
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(3000);    // milliseconds
                        conn.setConnectTimeout(5000); // milliseconds
                        conn.setRequestMethod("GET");
                        conn.setDoInput(true);
                        conn.connect();
                        response = conn.getResponseCode();
                    } catch (Exception e) {
                        // Safely ignored as a response!=200 will trigger the offline title
                    }
                    if (response == 200) {
                        if (Log.LOGV) Log.i(LOG_TAG + "Server response 200");
                        guiHandler.post(online);
                    } else {
                        if (Log.LOGV) Log.i(LOG_TAG + "Server response NOT 200");
                        guiHandler.post(offline);
                    }
                }
            }, 0, 30, TimeUnit.SECONDS);
            */
            guiHandler.post(online);
            if (Log.LOGV) Log.v(LOG_TAG + "BackgroundServerCheck started");
        } else {
            if (Log.LOGV) Log.d(LOG_TAG + "Server response NOT 200");
            guiHandler.post(offline);
        }
    }

    /** Stops the background server check */
    public void stopBackgroundServerCheck() {
        try{
            scheduleServerCheck.shutdown();
            if (Log.LOGV) Log.v(LOG_TAG + "BackgroundServerCheck stopped");
        } catch (NullPointerException e) {
            // This will be triggered due to the network being unavailable, safe to ignore
        }
    }

    /** Launches a Toast in the main gui thread */
    private void launchToast(final int resourceId) {
        ((Activity)mActivityContext).runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mActivityContext, resourceId, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void launchToast(final String toastText) {
        ((Activity)mActivityContext).runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mActivityContext, toastText, Toast.LENGTH_LONG).show();
            }
        });
    }
}
