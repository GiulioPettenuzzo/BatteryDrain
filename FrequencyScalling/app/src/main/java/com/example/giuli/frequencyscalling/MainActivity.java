package com.example.giuli.frequencyscalling;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    String curFreqInitialPath = "/sys/devices/system/cpu/cpu";
    String curFrequFinalPath = "/cpufreq/scaling_cur_freq";

    String usagePath = "proc/stat";
    String usage = "";

    TextView frequency;
    TextView totalUsage;
    EditText timeToLive;
    EditText usageToLive;
    Button button;

    String allFrequency = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frequency = (TextView)findViewById(R.id.frequency);
        totalUsage = (TextView)findViewById(R.id.usage);
        timeToLive = (EditText)findViewById(R.id.time);
        usageToLive = (EditText)findViewById(R.id.usageToImpose);
        button = (Button)findViewById(R.id.button);


        for(int cpu = 0;cpu<=7;cpu++){
            allFrequency =  allFrequency+" frequenza cpu" + cpu + " = "  + getFrequency(cpu);
        }
        frequency.setText(allFrequency);


            String currentUsage = String.valueOf(getUsage(0));//cpu0
            //usage = usage + "utilizzazione cpu " + cpu + "=" + currentUsage;
            if(usage.isEmpty()){
                usage = "0";
            }
            else {
                usage = usage.valueOf(Integer.parseInt(usage) + Integer.parseInt(currentUsage));
            }


        totalUsage.setText(usage);


        final int[] usageInteger = new int[1];
        final int[] time = {0};
        final Intent intent = new Intent(this,com.example.giuli.frequencyscalling.cpuOverhead.class);
        final Intent batteryIntent = new Intent(this,com.example.giuli.frequencyscalling.MyServiceReader.class);


        usageToLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = usageToLive.getText().toString();
                if(!value.isEmpty()){
                    usageInteger[0] = Integer.parseInt(value);
                    intent.putExtra("usage", usageInteger[0]);
                }
            }
        });
        timeToLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value1 = timeToLive.getText().toString();
                if(!value1.isEmpty()){
                    time[0] = Integer.parseInt(value1);
                    intent.putExtra("tempo", time[0]);
                    batteryIntent.putExtra("tempo",time[0]);
                }
            }
        });
       // startActivity(intent);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(intent);
                startService(batteryIntent);
            }
        });
//ok


    }

    /**
     * this method is able to give the cpu_freq of the selected cpu given in param
     * @return
     */
    private String getFrequency(int cpu){
        String currentFreqPath = curFreqInitialPath+cpu+curFrequFinalPath;
        String frequency="";
        try {
            FileReader curFreqFile = new FileReader(currentFreqPath);
            BufferedReader buffer = new BufferedReader(curFreqFile);
            frequency = buffer.readLine();

            buffer.close();
            curFreqFile.close();
        }
        catch(FileNotFoundException e){
            Log.e("getFrequency: ","FileNotFoundException");
    }
        catch(IOException e){
            Log.e("getFrequency: ","IOException");
        }
        return frequency;
    }
    private long getUsage(int cpu){
        long tot_ex_time = 0;
        try {
            FileReader myFile = new FileReader(usagePath);
            BufferedReader buffer = new BufferedReader(myFile);
            String thisUsage = "";
            //this cycle will read the correct line about the cpu given in imput

            for(int i = 0;i<=cpu;i++){
                thisUsage = buffer.readLine();
            }
            StringTokenizer st = new StringTokenizer(thisUsage);
            //the first line describes the number of cpu
            st.nextToken();
            //time in userMode
            long userExTime = Long.decode(st.nextToken());
            //time in user mode nice
            long niceExTime = Long.decode(st.nextToken());
            //time in System mode
            long sysExTime = Long.decode(st.nextToken());
            //the two lines that we don't care
            st.nextToken();
            st.nextToken();
            //time in interrupt mode
            long irqExTime = Long.decode(st.nextToken());
            //time in softirqs mode
            long softirqTime = Long.decode(st.nextToken());
            buffer.close();
            myFile.close();
            tot_ex_time = userExTime+niceExTime+sysExTime+irqExTime+softirqTime;


        }
        catch(FileNotFoundException e){
            Log.e("getUsage: ","FileNotFoundException");
        }
        catch(IOException e){
            Log.e("getUsage: ","IOException");
        }
        return tot_ex_time;
    }
}
