package com.example.giuli.frequencyscalling;

        import android.content.Intent;
        import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class cpuOverheadActivity extends AppCompatActivity {

    TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_overhead);

        result = (TextView) findViewById(R.id.result_usage);

        Intent i = getIntent();
        String value = i.getStringExtra("result");
        result.setText(value);
    }
}
