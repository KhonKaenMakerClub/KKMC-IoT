package com.numberx.kkmctimer.topic;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.daimajia.androidanimations.library.fading_entrances.FadeInLeftAnimator;
import com.daimajia.swipe.util.Attributes;
import com.numberx.kkmctimer.DeskClock;
import com.numberx.kkmctimer.R;
import com.numberx.kkmctimer.SettingsActivity;
import com.numberx.kkmctimer.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class TopicActivity extends FragmentActivity {
    private RecyclerView recyclerView;
    private TopicAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.topic_activity);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.dialog_divider_horizontal_light)));

        mAdapter = new TopicAdapter(this);
        mAdapter.setMode(Attributes.Mode.Single);
        recyclerView.setAdapter(mAdapter);

        /* Listeners */
        recyclerView.setOnScrollListener(onScrollListener);
    }
    public void addTopic(String topic,int refresh,String unit,int type,String btName){
        TopicObj item = new TopicObj(topic,refresh,unit,type,btName,mAdapter.getItemCount(),true);
        mAdapter.addItem(item);
    }
    public void editTopic(int index,String topic,int refresh,String unit){

    }
    RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.e("ListView", "onScrollStateChanged");
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, DeskClock.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.menu_item_add:
                FragmentManager fm = getFragmentManager();
                TopicAddDialogFragment dialogFragment = TopicAddDialogFragment.newInstance();
                dialogFragment.show(fm,"AddTopic");//.show(fm, "Sample Fragment");
                return true;
            case R.id.menu_item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i != null) {
                    try {
                        startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        // No activity found to match the intent - ignore
                    }
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topic_menu, menu);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }
}
