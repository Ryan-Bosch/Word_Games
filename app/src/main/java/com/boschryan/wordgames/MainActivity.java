package com.boschryan.wordgames;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private boolean darkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        darkTheme = sharedPreferences.getBoolean(SettingsFragment.PREFERENCE_THEME, false);
//        if (darkTheme) {
//            setTheme(R.style.DarkTheme);
//        }
    }

    public void onClickPlay(View view) {
        int selection = Integer.parseInt(((TextView) view).getText().toString().substring(0,1));

        // Start QuestionActivity, indicating what level was clicked
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("EXTRA_SELECTION", selection);
        startActivity(intent);
    }
}