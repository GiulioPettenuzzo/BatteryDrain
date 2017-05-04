package com.example.giuli.frequencyscalling;

        import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

        import java.io.BufferedReader;
        import java.io.File;
        import java.io.FileNotFoundException;
        import java.io.FileReader;
        import java.io.IOException;

public class cpuOverheadActivity extends AppCompatActivity {

    TextView result_usage;
    TextView result_battery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_overhead);

        result_usage = (TextView) findViewById(R.id.result_usage);
        result_battery = (TextView) findViewById(R.id.result_battery);

        Intent i = getIntent();
        String value = i.getStringExtra("result");
        String totValues = i.getStringExtra("result_battery");
        result_usage.setText(value);

       /* Intent j = getIntent();
        String battery_result = j.getStringExtra("battery_result");*/

        //String battery_result = readStringFromBatteryFile();
        result_battery.setText(totValues);
    }
    public String readStringFromBatteryFile(){
        String result = "";
        String current = "-";
        try {
            File outputFile = MyServiceReader.outputFile;
            FileReader fw = new FileReader(outputFile);
            BufferedReader buffer = new BufferedReader(fw);
            while (!current.isEmpty()) {
                current = buffer.readLine();
                result = result + current;
            }
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
        return result;
    }
}
