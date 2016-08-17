package com.numberx.kkmctimer.topic;


import android.content.SharedPreferences;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class Topics {
    private static final String NUMBER_OF_TOPICS = "number_of_topics";

    public static void saveTopicsToSharedPrefs(SharedPreferences prefs, HashMap<Integer, TopicObj> topics) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(NUMBER_OF_TOPICS, topics.size());
        Collection<TopicObj> col = topics.values();
        Iterator<TopicObj> i = col.iterator();
        int count = 0;
        while (i.hasNext()) {
            TopicObj c = i.next();
            c.saveTopicToSharedPrefs(editor, count);
            count++;
        }
        editor.apply();
    }
    public static void SaveTopicsItemToSharePrefs(SharedPreferences prefs,TopicObj item,int id){
        SharedPreferences.Editor editor = prefs.edit();
        item.saveTopicToSharedPrefs(editor, id);
        editor.apply();
    }
    public static  HashMap<Integer, TopicObj> readTopicsFromSharedPrefs(SharedPreferences prefs) {
        int size = prefs.getInt(NUMBER_OF_TOPICS, -1);
        HashMap<Integer, TopicObj> c = new HashMap<>();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                TopicObj o = new TopicObj(prefs, i);
                if (o.mTopicID != -1) {
                    c.put(o.mTopicID, o);
                }
            }
        }
        return c;
    }
    public static  HashMap<Integer, TopicObj> readTopicsOnlyShowFromSharedPrefs(SharedPreferences prefs) {
        int size = prefs.getInt(NUMBER_OF_TOPICS, -1);
        HashMap<Integer, TopicObj> c = new HashMap<>();
        int idc = 0;
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                TopicObj o = new TopicObj(prefs, i);
                if(!o.isShow) continue;
                if (o.mTopicID != -1) {
                    c.put(idc, o);
                    idc++;
                }
            }
        }
        return c;
    }
}
