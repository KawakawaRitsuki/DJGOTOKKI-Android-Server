package com.kawakawaplanning.dj_gotokki_android_server;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Preference extends ActionBarActivity {

    SharedPreferences pref;
    EditText et;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        pref = getSharedPreferences("user_data", MODE_PRIVATE);
        et = (EditText)findViewById(R.id.editText);
        et.setText(pref.getInt("port", 10000)+"");
    }

    public void save(View v){

        if (et.getEditableText().length() == 0) {
            et.setError("ポート番号を入力してください。");
        } else {
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt("port",Integer.parseInt(et.getEditableText().toString()));
            editor.commit();
            Toast.makeText(this,"保存しました",Toast.LENGTH_SHORT).show();
        }
    }
}
