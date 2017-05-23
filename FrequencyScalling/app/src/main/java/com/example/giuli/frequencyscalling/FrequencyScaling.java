package com.example.giuli.frequencyscalling;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class FrequencyScaling extends AppCompatActivity {

    String currFreq = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";

    private int maxFreq_0;
    private int minFreq_0;
    /**
     * the device I'm using is an octa-core it is devided into two quad-core each one has the same
     * frequency in every sub-processor.
     * so this must mean, for setting the global frequency I just touch the cpu0 and cpu3
     */
   // String maxFreqPathCPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    String minFreqPathCPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
    String maxFreqPathCPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_setspeed";
    String governorPath = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    static DataOutputStream os;

    TextView currentFreq;
    EditText maxFreqCPU0;
    EditText minFreqCPU0;

    Button impose_frequency;
    Button refresh_button;

    TextView readMaxFreq;
    TextView readMinFreq;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequency_scaling);

        currentFreq = (TextView)findViewById(R.id.current_frequency);
        maxFreqCPU0 = (EditText)findViewById(R.id.max_freq_CPU0);
        minFreqCPU0 = (EditText)findViewById(R.id.min_freq_CPU0);
        impose_frequency = (Button)findViewById(R.id.impose_frequency_button);
        readMaxFreq = (TextView)findViewById(R.id.read_max_freq);
        readMinFreq = (TextView)findViewById(R.id.read_min_freq);
        refresh_button = (Button)findViewById(R.id.refresh_button);

        String allFrequency = "";

        allFrequency =  " frequenza cpu" + " = "  + getFrequency();

        currentFreq.setText(allFrequency);
        readMinFreq.setText(getMinFreq());
        readMaxFreq.setText(getMaxFreq());

        maxFreqCPU0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = maxFreqCPU0.getText().toString();
                if(!value.isEmpty()){
                    int num = Integer.parseInt(value);
                    setMaxFreqCPU0(num);
                }
            }
        });
        minFreqCPU0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = minFreqCPU0.getText().toString();
                if(!value.isEmpty()){
                    int num = Integer.parseInt(value);
                    setMinFreqCPU0(num);
                }
            }
        });

        impose_frequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if there is any EditText's empty fields
                if(getMaxFreqCPU0()!=0&&getMinFreqCPU0()!=0){
                    //the max value must always be greater then min value
                    if(getMaxFreqCPU0()>= getMinFreqCPU0()){
                        try {

                            frequencyScaling(getMaxFreqCPU0(), getMinFreqCPU0());

                            //the last thing to do is to set the new value of textView
                            String allFrequencyFinal = "";

                            allFrequencyFinal =  " frequenza cpu" +  " = "  + getFrequency();

                            currentFreq.setText(allFrequencyFinal);
                        } catch (IOException e) {
                            Toast toast = Toast.makeText(getApplicationContext(),"permission denied",Toast.LENGTH_SHORT);
                            toast.show();
                            e.printStackTrace();
                        }
                    }
                    else{
                        Toast toast = Toast.makeText(getApplicationContext(),"hai inserito un massimo piu grande del minimo",Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                else{
                    Toast toast = Toast.makeText(getApplicationContext(),"non hai inserito tutti  valori",Toast.LENGTH_SHORT);
                    toast.show();
                }
                readMinFreq.setText(getMinFreq());
                readMaxFreq.setText(getMaxFreq());
            }
        });

        refresh_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFreq.setText(getFrequency());

                readMinFreq.setText(getMinFreq());
                readMaxFreq.setText(getMaxFreq());
            }
        });


    }

    public void frequencyScaling(int maxFreqCPU0,int minFreqCPU0) throws IOException {

        Process su = Runtime.getRuntime().exec("su");
        os = new DataOutputStream(su.getOutputStream());


        //os.writeBytes("echo "+maxFreqCPU0+" >> "+maxFreqPathCPU0+"\n");
        os.writeBytes("echo conservative >> "+governorPath+"\n");
        os.flush();


        os.writeBytes("echo "+maxFreqCPU0+" >> "+maxFreqPathCPU0+"\n");
        os.flush();

        os.writeBytes("echo "+minFreqCPU0+" >> "+minFreqPathCPU0+"\n");
        os.flush();


        os.writeBytes("exit\n");
        os.flush();


        os.close();

        try {
            su.waitFor();
        } catch (InterruptedException e) {
            Log.i("InterruptedException", "frequencyScaling: ");
            e.printStackTrace();
        }
    }
    public void closeFrequencyFile() throws IOException {
        os.close();
    }
    public String getFrequency(){
        String frequency="";
        try {
            FileReader curFreqFile = new FileReader(currFreq);
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
    public String getMaxFreq(){
        String maxFreq = "";

        try{
            FileReader maxReaderFile = new FileReader(maxFreqPathCPU0);
            BufferedReader buffer = new BufferedReader(maxReaderFile);
            maxFreq = (buffer.readLine());

            buffer.close();
            maxReaderFile.close();

        }
        catch(FileNotFoundException e){
            Log.e("getMaxFrequency: ","FileNotFoundException");
        }
        catch(IOException e){
            Log.e("getMaxFrequency: ","IOException");
        }
        return maxFreq;
    }
    public String getMinFreq(){
        String minFreq = "";
        try{
            FileReader maxReaderFile = new FileReader(minFreqPathCPU0);
            BufferedReader buffer = new BufferedReader(maxReaderFile);
            minFreq = (buffer.readLine());

            buffer.close();
            maxReaderFile.close();
        }
        catch(FileNotFoundException e){
            Log.e("getMinFrequency: ","FileNotFoundException");
        }
        catch(IOException e){
            Log.e("getMinFrequency: ","IOException");
        }
        return minFreq;
    }
    public void setMaxFreqCPU0(int maxFreq){
        maxFreq = maxFreq*100000;
        this.maxFreq_0=maxFreq;
    }
    public int getMaxFreqCPU0(){
        return maxFreq_0;
    }
    public void setMinFreqCPU0(int minFreq){
        minFreq = minFreq*100000;
        this.minFreq_0=minFreq;
    }
    public int getMinFreqCPU0(){
        return minFreq_0;
    }
}

