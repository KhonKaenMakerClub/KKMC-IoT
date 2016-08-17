package com.numberx.kkmctimer.widget;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.numberx.kkmctimer.NetpieServiceHelper;
import com.numberx.kkmctimer.R;
import com.numberx.kkmctimer.SettingsActivity;
import com.numberx.kkmctimer.topic.TopicObj;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Comdet Phaudphut on 24-May-16.
 */
public class PushButton extends Button implements View.OnClickListener {
    SharedPreferences prefs;
    public PushButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public PushButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PushButton(Context context) {
        super(context);
        init();
    }

    private void init(){
        setOnClickListener(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    private String topic;

    public void setTopic(String topic){
        this.topic = topic;
    }

    @Override
    public void onClick(View view) {
        if(topic == null || topic.isEmpty()) return;
        NetpieServiceHelper.putTopic(topic,prefs.getString(SettingsActivity.PAYLOAD_PUSH,"1"),true, new NetpieServiceHelper.RequestCallback() {
            @Override
            public void ResponseText(String data) {
            }

            @Override
            public void ResponseJson(JSONObject data) {
                Toast.makeText(getContext(),"Publish to topic success",Toast.LENGTH_LONG).show();
            }

            @Override
            public void ResponseError(String msg) {
                Toast.makeText(getContext(),"Error : "+msg,Toast.LENGTH_LONG).show();
            }
        });
    }
}
