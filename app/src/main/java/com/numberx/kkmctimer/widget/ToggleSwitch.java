package com.numberx.kkmctimer.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.numberx.kkmctimer.NetpieServiceHelper;
import com.numberx.kkmctimer.SettingsActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Comdet Phaudphut on 24-May-16.
 */
public class ToggleSwitch extends ToggleButton implements View.OnClickListener {
    private boolean mAttached;
    private SharedPreferences prefs;
    private int value;
    private String topic;
    private int refresh_tick = 5000;
    private String[] of = null;
    private boolean isRunning = false;
    private final ContentObserver mValueChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateValue();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateValue();
        }
    };

    private final Runnable mTicker = new Runnable() {
        public void run() {
            //updateValue();

            //long now = SystemClock.uptimeMillis();
            //long next = now + (refresh_tick - now % refresh_tick);
            if(topic == null || refresh_tick <= 0 || topic.isEmpty()) return;
            NetpieServiceHelper.getTopic(topic, new NetpieServiceHelper.RequestCallback() {
                @Override
                public void ResponseText(String data) {
                }

                @Override
                public void ResponseJson(JSONObject data) {
                    try {
                        if(isRunning) {
                            String val = data.getString("payload");
                            setValue(Integer.parseInt(val));
                            updateValue();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void ResponseError(String msg) {
                    //Toast.makeText(getContext(), "Error : " + msg, Toast.LENGTH_LONG).show();
                }
            });
            if(isRunning) {
                getHandler().postDelayed(mTicker, refresh_tick);
            }
        }
    };

    public ToggleSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ToggleSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToggleSwitch(Context context) {
        super(context);
        init();
    }

    private void init(){
        setOnClickListener(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        of = prefs.getString(SettingsActivity.PAYLOAD_ONOFF, "1:0").split(":");
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            registerObserver();
            updateValue();
            mTicker.run();

        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            unregisterObserver();
            mAttached = false;
            getHandler().removeCallbacks(mTicker);
        }
    }

    private void registerObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.registerContentObserver(Settings.System.CONTENT_URI, true, mValueChangeObserver);
    }

    private void unregisterObserver() {
        final ContentResolver resolver = getContext().getContentResolver();
        resolver.unregisterContentObserver(mValueChangeObserver);
    }

    public void setValue(int value) {
        this.value = value;
        updateValue();
    }
    public void setRefreshTime(int sec){
        refresh_tick = sec * 1000;
    }
    public void setTopic(String topic){
        this.topic = topic;
    }
    private void updateValue() {
        try {
            if (of.length == 2) {
                if (Integer.parseInt(of[0]) == value) {
                    setChecked(true);
                } else {
                    setChecked(false);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void onClick(View view) {
        try {
            isRunning = false;
            String payload = isChecked()?of[0]:of[1];
            if(topic == null || topic.isEmpty()) return;
            NetpieServiceHelper.putTopic(topic,payload,true, new NetpieServiceHelper.RequestCallback() {
                @Override
                public void ResponseText(String data) {
                }

                @Override
                public void ResponseJson(JSONObject data) {
                    Toast.makeText(getContext(), "Publish to topic success", Toast.LENGTH_LONG).show();
                    isRunning = true;
                    mTicker.run();
                }

                @Override
                public void ResponseError(String msg) {
                    Toast.makeText(getContext(),"Error : "+msg,Toast.LENGTH_LONG).show();
                    isRunning = true;
                    mTicker.run();
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
