package com.numberx.kkmctimer.widget;
// Copyright 2013 Google Inc. All Rights Reserved.

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import android.widget.Toast;

import com.numberx.kkmctimer.R;
import com.numberx.kkmctimer.NetpieServiceHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Based on {@link android.widget.TextClock}, This widget displays a constant time of day using
 * format specifiers. {@link android.widget.TextClock} Doesn't support a non ticking clock.
 */
public class TextSensor extends TextView {
    private boolean mAttached;

    private float value;
    private CharSequence unit;
    private String topic;
    private int refresh_tick = 5000;
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
                        String val = data.getString("payload");
                        setValue(Float.parseFloat(val));
                        updateValue();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void ResponseError(String msg) {
                    //Toast.makeText(getContext(), "Error : " + msg, Toast.LENGTH_LONG).show();
                }
            });
            getHandler().postDelayed(mTicker, refresh_tick);
        }
    };

    @SuppressWarnings("UnusedDeclaration")
    public TextSensor(Context context) {
        this(context, null);
    }

    @SuppressWarnings("UnusedDeclaration")
    public TextSensor(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextSensor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray styledAttributes = context.obtainStyledAttributes(
                attrs, R.styleable.TextTime, defStyle, 0);

        styledAttributes.recycle();
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

    public void setValue(float value) {
        this.value = value;
        updateValue();
    }
    public void setRefreshTime(int sec){
        refresh_tick = sec * 1000;
    }
    public void setTopic(String topic){
        this.topic = topic;
    }
    public void setUnit(CharSequence unit){
        this.unit = unit;
    }
    private void updateValue() {
        //CharSequence seet = CharSequence. (String.format("%.2f", value))+unit;
        //CharSequence r = TextUtils.concat(String.format("%.2f", value), unit);
        if(unit!=null) {
            setText(TextUtils.concat(String.format("%.2f", value), unit));
        }else{
            setText(String.format("%.2f", value));
        }
        setContentDescription(String.format("%.2f", value));
    }
}
