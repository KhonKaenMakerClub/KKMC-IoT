package com.numberx.kkmctimer.topic;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.numberx.kkmctimer.R;

import org.w3c.dom.Text;

public class TopicAddDialogFragment extends DialogFragment {
    EditText textTopic = null;
    EditText textRefresh = null;
    EditText textUnit = null;
    TextView lbUnit = null;
    Spinner spinType = null;
    public static TopicAddDialogFragment newInstance() {
        TopicAddDialogFragment frag = new TopicAddDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", R.string.add_new_topic);
        frag.setArguments(args);
        return frag;
    }
    public static TopicAddDialogFragment newInstance(int index) {
        TopicAddDialogFragment frag = new TopicAddDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", R.string.edit_topic);
        args.putInt("edit_index",index);
        frag.setArguments(args);
        return frag;
    }
    private void updateAddGuiByType(int type){
        switch (type){
            case 0:
                textRefresh.setEnabled(true);
                textUnit.setEnabled(true);
                textUnit.setText("");
                lbUnit.setText(R.string.topic_unit);
                break;
            case 1:
                textRefresh.setEnabled(false);
                textUnit.setEnabled(true);
                textUnit.setText("");
                lbUnit.setText(R.string.widget_text);
                break;
            case 2:
                textRefresh.setEnabled(true);
                textUnit.setEnabled(true);
                textUnit.setText(R.string.default_onoff_string);
                lbUnit.setText(R.string.widget_onoff);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int title = getArguments().getInt("title");
        final boolean isEdit = getArguments().containsKey("edit_index");
        final int index = getArguments().getInt("edit_index",-1);
        View rootView = inflater.inflate(R.layout.topic_add_dialog, container, false);
        getDialog().setTitle(title);
        textTopic = (EditText)rootView.findViewById(R.id.text_topic);
        textTopic.setSelection(textTopic.getText().length());
        textRefresh = (EditText)rootView.findViewById(R.id.text_refresh_rate);
        textUnit = (EditText)rootView.findViewById(R.id.text_topic_unit);
        spinType = (Spinner)rootView.findViewById(R.id.topic_type);
        lbUnit = (TextView)rootView.findViewById(R.id.lb_unit);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),R.array.topic_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinType.setAdapter(adapter);
        spinType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateAddGuiByType(i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                updateAddGuiByType(spinType.getSelectedItemPosition());
            }
        });
        Button btCancel = (Button)rootView.findViewById(R.id.add_topic_cancel);
        Button btAdd = (Button)rootView.findViewById(R.id.add_topic_add);
        btAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isEdit) {
                    ((TopicActivity) getActivity()).addTopic(
                            textTopic.getText().toString(),
                            Integer.parseInt(textRefresh.getText().toString()),
                            textUnit.getText().toString(),
                            spinType.getSelectedItemPosition(),
                            textUnit.getText().toString()
                            );
                } else {
                    ((TopicActivity) getActivity()).editTopic(
                            index,
                            textTopic.getText().toString(),
                            Integer.parseInt(textRefresh.getText().toString()),
                            textUnit.getText().toString());
                }
                dismiss();
            }
        });
        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        return rootView;
    }
}