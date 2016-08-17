package com.numberx.kkmctimer.topic;

import android.content.SharedPreferences;

public class TopicObj {

    private static final String TOPIC_NAME = "topic_path_";
    private static final String TOPIC_REFRESH_RATE = "topic_refresh_rate_";
    private static final String TOPIC_ID = "topic_id_";
    private static final String TOPIC_UNIT = "topic_unit_";
    private static final String TOPIC_TYPE = "topic_type_";
    private static final String TOPIC_WTEXT = "topic_wname_";
    private static final String TOPIC_SHOW = "topic_show_";
    public String mTopicName;
    public int mTopicID;
    public String mTopicUnit;
    public int mRefreshRate;
    public int mWidgetType;
    public String mButtonName;
    public boolean isShow;

    public TopicObj(String name, int rate,String unit,int type,String wname, int id,boolean show) {
        mTopicName = name;
        mRefreshRate = rate;
        mTopicID = id;
        mTopicUnit = unit;
        mWidgetType = type;
        mButtonName = wname;
        isShow = show;
    }

    @Override
    public String toString() {
        return "TopicObj{" +
                "name=" + mTopicName +
                ", rate=" + mRefreshRate +
                ", unit=" + mTopicUnit +
                ", type=" + mWidgetType +
                ", text=" + mButtonName +
                ", show=" + isShow +
                ", id=" + mTopicID +
                '}';
    }

    public TopicObj(SharedPreferences prefs, int index) {
        mTopicName = prefs.getString(TOPIC_NAME + index, null);
        mRefreshRate = prefs.getInt(TOPIC_REFRESH_RATE + index, 10);
        mTopicUnit = prefs.getString(TOPIC_UNIT + index, null);
        mWidgetType = prefs.getInt(TOPIC_TYPE + index, 0);
        mButtonName = prefs.getString(TOPIC_WTEXT + index, null);
        isShow = prefs.getBoolean(TOPIC_SHOW+index,false);
        mTopicID = prefs.getInt(TOPIC_ID + index, 0);
    }

    public void saveTopicToSharedPrefs(SharedPreferences.Editor editor, int index) {
        editor.putString(TOPIC_NAME + index, mTopicName);
        editor.putInt(TOPIC_REFRESH_RATE + index, mRefreshRate);
        editor.putString(TOPIC_UNIT + index, mTopicUnit);
        editor.putString(TOPIC_WTEXT+index,mButtonName);
        editor.putInt(TOPIC_TYPE + index, mWidgetType);
        editor.putBoolean(TOPIC_SHOW+index,isShow);
        editor.putInt (TOPIC_ID + index, index);
    }
}
