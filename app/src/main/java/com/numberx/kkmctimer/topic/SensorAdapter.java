package com.numberx.kkmctimer.topic;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.numberx.kkmctimer.AnalogClock;
import com.numberx.kkmctimer.R;
import com.numberx.kkmctimer.SettingsActivity;
import com.numberx.kkmctimer.Utils;
import com.numberx.kkmctimer.widget.PushButton;
import com.numberx.kkmctimer.widget.TextClock;
import com.numberx.kkmctimer.widget.TextSensor;
import com.numberx.kkmctimer.widget.ToggleSwitch;
import com.numberx.kkmctimer.worldclock.Cities;
import com.numberx.kkmctimer.worldclock.CityObj;

import java.text.Collator;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class SensorAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final Context mContext;
    private final Collator mCollator = Collator.getInstance();
    protected HashMap<Integer, TopicObj> mTopicDb = new HashMap<>();
    protected int mClocksPerRow;

    //SharedPreferences prefs = null;
    public SensorAdapter(Context context) {
        super();
        mContext = context;
        //loadTopicsDb(context);
        mInflater = LayoutInflater.from(context);
        mClocksPerRow = context.getResources().getInteger(R.integer.world_clocks_per_row);
        //PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public void reloadData(Context context) {
        loadTopicsDb(context);
        notifyDataSetChanged();
    }

    public void loadTopicsDb(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mTopicDb = Topics.readTopicsOnlyShowFromSharedPrefs(prefs);
    }
/*
    public void updateHomeLabel(Context context) {
        // Update the "home" label if the home time zone clock is shown
        if (needHomeCity() && mCitiesList.length > 0) {
            ((CityObj) mCitiesList[0]).mCityName =
                    context.getResources().getString(R.string.home_label);
        }
    }*/

    public boolean needHomeCity() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (sharedPref.getBoolean(SettingsActivity.KEY_AUTO_HOME_CLOCK, false)) {
            String homeTZ = sharedPref.getString(
                    SettingsActivity.KEY_HOME_TZ, TimeZone.getDefault().getID());
            final Date now = new Date();
            return TimeZone.getTimeZone(homeTZ).getOffset(now.getTime())
                    != TimeZone.getDefault().getOffset(now.getTime());
        } else {
            return false;
        }
    }
/*
    public boolean hasHomeCity() {
        return (mCitiesList != null) && mCitiesList.length > 0
                && ((CityObj) mCitiesList[0]).mCityId == null;
    }*/

    @Override
    public int getCount() {
        if (mClocksPerRow == 1) {
            // In the special case where we have only 1 clock per view.
            return mTopicDb.size();
        }

        // Otherwise, each item in the list holds 1 or 2 clocks
        return (mTopicDb.size()  + 1)/2;
    }

    @Override
    public Object getItem(int p) {
        return null;
    }

    @Override
    public long getItemId(int p) {
        return p;
    }

    @Override
    public boolean isEnabled(int p) {
        return false;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        // Index in cities list
        int index = position * mClocksPerRow;
        if (index < 0 || index >= mTopicDb.size()) {
            return null;
        }

        if (view == null) {
            view = mInflater.inflate(R.layout.world_clock_list_item, parent, false);
        }
        // The world clock list item can hold two world clocks
        View rightClock = view.findViewById(R.id.city_right);
        updateView(view.findViewById(R.id.city_left), mTopicDb.get(index));
        if (rightClock != null) {
            // rightClock may be null (landscape phone layout has only one clock per row) so only
            // process it if it exists.
            if (index + 1 < mTopicDb.size()) {
                rightClock.setVisibility(View.VISIBLE);
                updateView(rightClock, mTopicDb.get(index + 1));
            } else {
                // To make sure the spacing is right , make sure that the right clock style is
                // selected even if the clock is invisible.

                // If there's only the one item, center it. If there are other items in the list,
                // keep it side-aligned.
                if (getCount() == 1) {
                    rightClock.setVisibility(View.GONE);
                } else {
                    rightClock.setVisibility(View.INVISIBLE);
                }
            }
        }

        return view;
    }

    private void updateView(View clock, TopicObj topicObj) {
        View nameLayout= clock.findViewById(R.id.city_name_layout);
        TextView name = (TextView)(nameLayout.findViewById(R.id.city_name));
        TextView dayOfWeek = (TextView)(nameLayout.findViewById(R.id.city_day));
        name.setText(topicObj.mTopicName);
        View separator = clock.findViewById(R.id.separator);
        separator.setVisibility(View.VISIBLE);
        TextSensor sensor = (TextSensor)(clock.findViewById(R.id.sensor_item));
        PushButton pusher = (PushButton)(clock.findViewById(R.id.button));
        ToggleSwitch toggler = (ToggleSwitch)(clock.findViewById(R.id.toggle_button));
        if(topicObj.mWidgetType == 0){
            sensor.setVisibility(View.VISIBLE);
            pusher.setVisibility(View.GONE);
            toggler.setVisibility(View.GONE);
            sensor.setValue(0.0f);
            sensor.setRefreshTime(topicObj.mRefreshRate);
            sensor.setTopic(topicObj.mTopicName);
            if(topicObj.mTopicUnit!="") {
                sensor.setUnit(Utils.formatUnit(topicObj.mTopicUnit, (int) mContext.getResources().getDimension(R.dimen.header_font_size)));
            }
        }else if(topicObj.mWidgetType == 1){
            sensor.setVisibility(View.GONE);
            pusher.setVisibility(View.VISIBLE);
            toggler.setVisibility(View.GONE);
            pusher.setText(topicObj.mButtonName);
            pusher.setTopic(topicObj.mTopicName);
        }else if(topicObj.mWidgetType == 2){
            sensor.setVisibility(View.GONE);
            pusher.setVisibility(View.GONE);
            toggler.setVisibility(View.VISIBLE);
            if(!topicObj.mTopicUnit.isEmpty() && topicObj.mTopicUnit.contains(":")) {
                toggler.setTextOn(topicObj.mTopicUnit.split(":")[0]);
                toggler.setTextOff(topicObj.mTopicUnit.split(":")[1]);
            }
            toggler.setTopic(topicObj.mTopicName);
            toggler.setRefreshTime(topicObj.mRefreshRate);
        }
    }
}
