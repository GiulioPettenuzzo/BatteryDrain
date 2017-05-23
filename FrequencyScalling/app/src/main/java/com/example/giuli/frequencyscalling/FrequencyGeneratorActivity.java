package com.example.giuli.frequencyscalling;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FrequencyGeneratorActivity extends AppCompatActivity {

    TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequency_generator);
        result = (TextView) findViewById(R.id.result);

        Intent intent = getIntent();
        String response = intent.getStringExtra("response");
        result.setText(response);

    }

}
