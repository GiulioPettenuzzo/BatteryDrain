package com.example.giuli.frequencyscalling;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FrequencyScaling extends AppCompatActivity {

    String curFreqInitialPath = "/sys/devices/system/cpu/cpu";
    String curFrequFinalPath = "/cpufreq/scaling_cur_freq";

    private int maxFreq_0;
    private int minFreq_0;
    private int maxFreq_4;
    private int minFreq_4;
    /**
     * the device I'm using is an octa-core it is devided into two quad-core each one has the same
     * frequency in every sub-processor.
     * so this must mean, for setting the global frequency I just touch the cpu0 and cpu3
     */
    String maxFreqPathCPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    String minFreqPathCPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
    String maxFreqPathCPU4 = "/sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq";
    String minFreqPathCPU4 = "/sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq";

    TextView currentFreq;
    EditText maxFreqCPU0;
    EditText minFreqCPU0;
    EditText maxFreqCPU4;
    EditText minFreqCPU4;
    Button impose_frequency;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequency_scaling);

        currentFreq = (TextView)findViewById(R.id.current_frequency);
        maxFreqCPU0 = (EditText)findViewById(R.id.max_freq_CPU0);
        minFreqCPU0 = (EditText)findViewById(R.id.min_freq_CPU0);
        maxFreqCPU4 = (EditText)findViewById(R.id.max_freq_CPU3);
        minFreqCPU4 = (EditText)findViewById(R.id.min_freq_CPU3);
        impose_frequency = (Button)findViewById(R.id.impose_frequency_button);

        String allFrequency = "";
        for(int cpu = 0;cpu<=7;cpu++){
            allFrequency =  allFrequency+" frequenza cpu" + cpu + " = "  + getFrequency(cpu);
        }
        currentFreq.setText(allFrequency);

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
        maxFreqCPU4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = maxFreqCPU4.getText().toString();
                if(!value.isEmpty()){
                    int num = Integer.parseInt(value);
                    setMaxFreqCPU4(num);
                }
            }
        });
        minFreqCPU4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = minFreqCPU4.getText().toString();
                if(!value.isEmpty()){
                    int num = Integer.parseInt(value);
                    setMinFreqCPU4(num);
                }
            }
        });
        impose_frequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if there is any EditText's empty fields
                if(getMaxFreqCPU0()!=0&&getMinFreqCPU0()!=0&& getMaxFreqCPU4()!=0&& getMinFreqCPU4()!=0){
                    //the max value must always be greater then min value
                    if(getMaxFreqCPU0()>= getMinFreqCPU4() && getMaxFreqCPU4()>= getMinFreqCPU4()){
                        try {
                            frequencyScaling(getMaxFreqCPU0(),getMinFreqCPU0(), getMaxFreqCPU4(), getMinFreqCPU4());
                            //the last thing to do is to set the new value of textView
                            String allFrequencyFinal = "";
                            for(int cpu = 0;cpu<=7;cpu++){
                                allFrequencyFinal =  allFrequencyFinal+" frequenza cpu" + cpu + " = "  + getFrequency(cpu);
                            }
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
            }
        });
    }

    public void frequencyScaling(int maxFreqCPU0,int minFreqCPU0,int maxFreqCPU3,int minFreqCPU3) throws IOException {
        java.lang.Process su = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(su.getOutputStream());

        os.writeBytes("echo "+maxFreqCPU0+" >> "+maxFreqPathCPU0+"\n");
        os.flush();

        os.writeBytes("echo "+minFreqCPU0+" >> "+minFreqPathCPU0+"\n");
        os.flush();

        os.writeBytes("echo "+maxFreqCPU3+" >> "+ maxFreqPathCPU4 +"\n");
        os.flush();

        os.writeBytes("echo "+minFreqCPU3+" >> "+ minFreqPathCPU4 +"\n");
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
    public void setMaxFreqCPU4(int maxFreq){
        maxFreq = maxFreq*100000;
        this.maxFreq_4 =maxFreq;
    }
    public int getMaxFreqCPU4(){
        return maxFreq_4;
    }
    public void setMinFreqCPU4(int minFreq){
        minFreq = minFreq*100000;
        this.minFreq_4 =minFreq;
    }
    public int getMinFreqCPU4(){
        return minFreq_4;
    }
}
