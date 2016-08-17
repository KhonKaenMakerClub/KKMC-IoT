package com.numberx.kkmctimer.topic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.numberx.kkmctimer.NetpieServiceHelper;
import com.numberx.kkmctimer.R;
import com.numberx.kkmctimer.SettingsActivity;
import com.numberx.kkmctimer.worldclock.Cities;
import com.numberx.kkmctimer.worldclock.CityObj;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class TopicAdapter extends RecyclerSwipeAdapter<TopicAdapter.SimpleViewHolder> {
    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        SwipeLayout swipeLayout;
        TextView textTopic;
        TextView textRefreshRate;
        TextView textLastValue;
        TextView textUnit;
        Button buttonDelete;
        CheckBox topicShow;

        public SimpleViewHolder(View itemView) {
            super(itemView);
            swipeLayout = (SwipeLayout) itemView.findViewById(R.id.swipe);
            textTopic = (TextView) itemView.findViewById(R.id.topic_name);
            textRefreshRate = (TextView) itemView.findViewById(R.id.refresh_time);
            textLastValue = (TextView)itemView.findViewById(R.id.last_value);
            buttonDelete = (Button) itemView.findViewById(R.id.delete);
            textUnit = (TextView)itemView.findViewById(R.id.topic_unit);
            topicShow = (CheckBox)itemView.findViewById(R.id.topic_show);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(getClass().getSimpleName(), "onItemSelected: " + textTopic.getText().toString());
                    Toast.makeText(view.getContext(), "onItemSelected: " + textTopic.getText().toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private Context mContext;
    protected HashMap<Integer, TopicObj> mTopicDb = new HashMap<Integer, TopicObj>();
    SharedPreferences prefs = null;
    public TopicAdapter(Context context) {
        this.mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadData(mContext);
    }
    public void reloadData(Context context) {
        loadData(context);
        notifyDataSetChanged();
    }
    public void addItem(TopicObj item){
        mTopicDb.put(item.mTopicID, item);
        //Topics.SaveTopicsItemToSharePrefs(prefs, item, item.mTopicID);
        Topics.saveTopicsToSharedPrefs(prefs,mTopicDb);
        notifyItemInserted(mTopicDb.size() - 1);
    }
    public void editItem(TopicObj item,int position){
        mTopicDb.put(position,item);
        Topics.SaveTopicsItemToSharePrefs(prefs,item,position);
        notifyItemChanged(position);
    }

    public void loadData(Context context) {
        mTopicDb = Topics.readTopicsFromSharedPrefs(prefs);
    }
    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.topic_item, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SimpleViewHolder viewHolder, final int position) {
        final TopicObj item = mTopicDb.get(position);
        final Handler mHandler = new Handler();
        final Runnable outer = new Object() {
            final Runnable ticker = new Runnable() {
                @Override
                public void run() {
                    if(!mTopicDb.containsKey(position)) return;
                    if(item == null) return;
                    NetpieServiceHelper.getTopic(item.mTopicName, new NetpieServiceHelper.RequestCallback() {
                        @Override
                        public void ResponseText(String data) {
                        }
                        @Override
                        public void ResponseJson(JSONObject data) {
                            try {
                                String val = data.getString("payload");
                                if(viewHolder == null) return;
                                viewHolder.textLastValue.setText("("+val+") ");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void ResponseError(String msg) {
                            //Toast.makeText(mContext,"Error : "+msg,Toast.LENGTH_LONG).show();
                        }
                    });
                    mHandler.postDelayed(ticker, item.mRefreshRate*1000);
                }
            };
        }.ticker;
        mHandler.post(outer);
        viewHolder.swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);
        viewHolder.swipeLayout.addSwipeListener(new SimpleSwipeListener() {
            @Override
            public void onOpen(SwipeLayout layout) {
                YoYo.with(Techniques.Tada).duration(500).delay(100).playOn(layout.findViewById(R.id.trash));
            }
        });
        viewHolder.topicShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TopicObj item = mTopicDb.get(position);
                item.isShow = viewHolder.topicShow.isChecked();
                mTopicDb.put(position, item);
                Topics.SaveTopicsItemToSharePrefs(prefs, item, position);
            }
        });
        viewHolder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //---------- changed here -----------//
                mItemManger.removeShownLayouts(viewHolder.swipeLayout);
                mTopicDb.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, mTopicDb.size());
                mItemManger.closeAllItems();
                Topics.saveTopicsToSharedPrefs(prefs, mTopicDb);
                //-------- re index -------//
                Collection<TopicObj> col = mTopicDb.values();
                Iterator<TopicObj> i = col.iterator();
                HashMap<Integer, TopicObj> tmp = new HashMap<>();
                int count = 0;
                while (i.hasNext()) {
                    TopicObj c = i.next();
                    tmp.put(count, c);
                    count++;
                }
                mTopicDb = tmp;
                Toast.makeText(view.getContext(), "Deleted " + viewHolder.textTopic.getText().toString() + "!", Toast.LENGTH_SHORT).show();
            }
        });
        viewHolder.topicShow.setChecked(item.isShow);
        viewHolder.textTopic.setText(item.mTopicName);
        String refresh_rate = "Refresh Rate : "+item.mRefreshRate;
        viewHolder.textRefreshRate.setText(refresh_rate);
        viewHolder.textUnit.setText(item.mTopicUnit);
        if(item.mWidgetType == 0) {
            viewHolder.textLastValue.setText("-");
        }
        mItemManger.bindView(viewHolder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return mTopicDb.size();
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }
}