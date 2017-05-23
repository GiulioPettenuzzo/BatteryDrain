package com.example.giuli.frequencyscalling;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by giuli on 13/04/2017.
 */

public class cpuOverhead extends Service {
    //PowerManager class gives you control of the power state of the device.
    PowerManager pm = null;
    PowerManager.WakeLock w1 = null;//
    ThreadCounterOverhead tco = null;
    //MyServiceReader.MyThreadReader msr = null;
    String currentPath = "/sys/devices/platform/ab8500-i2c.0/ab8500-fg.0/power_supply/ab8500_fg/current_now";
    String voltagePath = "/sys/devices/platform/ab8500-i2c.0/ab8500-fg.0/power_supply/ab8500_fg/voltage_now";
    String current;
    String outputCurrent = "";
    String voltage;
    String outputVoltage = "";
    String totBatteryConsumption;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate(){
        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        w1 = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"tag");

    }
    public int onStartCommand(Intent i,int flags,int strId){
        w1.acquire();
        int usage = i.getExtras().getInt("usage");
        int tempo = i.getExtras().getInt("tempo");
        int minFrequency = i.getExtras().getInt("min_frequency");
        // prova a farlo in un thread separato e farlo partire qui
        CPUFrequencyScaling frequencyScaling = new CPUFrequencyScaling(minFrequency,10,tempo);
        tco = new ThreadCounterOverhead(usage,tempo);

        frequencyScaling.start();
        tco.start();

        return super.onStartCommand(i,flags,strId);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }




    private final class ThreadCounterOverhead extends Thread{
        int usage,time,notUsage;
        String usagePath = "proc/stat";
        File outputFile = null;
        String resultInString="";

        public ThreadCounterOverhead(int use,int t){
            super();
            usage = use;
            notUsage = 100-usage;
            time = t*1000;
            //File sd = Environment.getExternalStorageDirectory();
            outputFile = new File(getApplicationContext().getFilesDir(),"OverheadCPUoutput.txt");
        }

        public ArrayList<String> readCpusString() throws IOException{
            ArrayList<String> cpuValues = new ArrayList<String>();
            FileReader useFile = new FileReader(usagePath);
            BufferedReader b1 = new BufferedReader(useFile);
            String s = b1.readLine();//salto la prima riga
            s = b1.readLine();
            while(s!=null){
                StringTokenizer st = new StringTokenizer(s);
                String firstWord = st.nextToken();
                if(firstWord.startsWith("cpu")){
                    cpuValues.add(s);
                }
                else{
                    break;
                }
                s = b1.readLine();
            }
            b1.close();
            useFile.close();
            return cpuValues;
        }
        //ritorna l'utilizzo totale: somma dell'utilizzo di tutte le cpu
        //perchè non farlo solo leggendo la prima riga??
        public long getTotalUsage(ArrayList<String> als){
            long totalExTime = 0;
            for(int x = 0;x<als.size();x++){
                String s = als.get(x);
                StringTokenizer st = new StringTokenizer(s);
                st.nextToken();
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
                totalExTime = totalExTime+userExTime+niceExTime+sysExTime+irqExTime+softirqTime;
            }
            return totalExTime;
        }
        public long getTotalSleep(ArrayList<String> als){
            long totalSleepTime = 0;
            for(int x = 0;x<als.size();x++){
                String s = als.get(x);
                StringTokenizer st = new StringTokenizer(s);
                st.nextToken();
                st.nextToken();
                st.nextToken();
                st.nextToken();
                long initSleep = Long.valueOf(st.nextToken());
                long initIOWait = Long.valueOf(st.nextToken());
                totalSleepTime = totalSleepTime + initSleep + initIOWait;
            }
            return totalSleepTime;
        }

        public String printResult(String esecuzione,String sleep) throws IOException{
            FileWriter fw = new FileWriter(outputFile,true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            String myOutput = "Esecuzione: " + esecuzione + ", sleep: " + sleep;
            pw.println(myOutput);
            pw.close();
            bw.close();
            fw.close();
            return myOutput;
        }

        public String printResultOnString(){
            String result = "";
            try {
                FileReader fr = new FileReader(outputFile);
                BufferedReader buffered = new BufferedReader(fr);
                result= buffered.readLine();
            }
            catch(FileNotFoundException e){
                e.printStackTrace();
            }
            catch(IOException e){
                e.printStackTrace();
            }

            return result;
        }



        public void run(){
            long timeUnit = 100;
            long start = System.currentTimeMillis();
            long current = start;
            long execTime = timeUnit*usage;
            long sleepTime = timeUnit*notUsage;
            int dummy = 0;
            String currentBatteryValues = "";


            try{
                ArrayList<String> listLine = readCpusString();
                long initialExTime = getTotalUsage(listLine);
                long initialSleepTime = getTotalSleep(listLine);

                int i=0,j=0;
                while(current-start<time){
                    long intStart = System.currentTimeMillis();
                    long intCurr = intStart;

                    //qui cerco i valori di corrente e li scrivo su file
                   // totBatteryConsumption = totBatteryConsumption + " 1 = " + getCurrentAndVoltage();
                    if(execTime != 0){
                        while(intCurr-intStart<execTime){
                            dummy++;
                            intCurr=System.currentTimeMillis();
                            /*if(dummy%1000000000==0){
                                totBatteryConsumption = totBatteryConsumption + getCurrentAndVoltage();
                            }*/
                        }
                    }
                   // totBatteryConsumption = totBatteryConsumption + " 2 = "  + getCurrentAndVoltage();
                    if(sleepTime!=0){
                        try{
                            Thread.sleep(sleepTime);
                        }
                        catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    totBatteryConsumption = totBatteryConsumption + getCurrentAndVoltage();
                    current = System.currentTimeMillis();
                    Log.i("utilizzazione grande", String.valueOf(j));
                    j++;
                }
                ArrayList<String> listLine2 = readCpusString();
                long finalExTime = getTotalUsage(listLine2);
                long finalSleep = getTotalSleep(listLine2);
                long totUse = finalExTime - initialExTime;
                long totSlp = finalSleep - initialSleepTime;
                resultInString = printResult(String.valueOf(totUse),String.valueOf(totSlp));
            }
            catch(IOException e){
                e.printStackTrace();
            }

            w1.release();

            stopSelf();
            Intent result = new Intent(getBaseContext(),cpuOverheadActivity.class);
            result.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            result.putExtra("result",resultInString);
            result.putExtra("result_battery",totBatteryConsumption);
            startActivity(result);

        }

        public String getCurrentAndVoltage(){
            String output = "";
            try {
                FileReader currentFile = new FileReader(currentPath);
                BufferedReader currentBuffer = new BufferedReader(currentFile);
                current = currentBuffer.readLine();


                currentBuffer.close();
                currentFile.close();
                //read the voltage
                FileReader voltageFile = new FileReader(voltagePath);
                BufferedReader voltageBuffer = new BufferedReader(voltageFile);
                voltage = voltageBuffer.readLine();

                voltageBuffer.close();
                voltageFile.close();

                outputCurrent = outputCurrent + current + "microAmpere" + "\r";
                outputVoltage = outputVoltage + voltage + "microVolt" + "\r";
                output = outputCurrent;//+outputVoltage;
            }catch(IOException e){
                e.printStackTrace();
            }
            return output;
        }
    }
    private final class CPUFrequencyScaling extends Thread{

        int minFreq;
        int maxFreq;
        int time;


        String maxFreqPathCPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
        String minFreqPathCPU0 = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
        String currFreq = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";


        public CPUFrequencyScaling(int minFreq,int maxFreq,int t){
            minFreq = minFreq*100000;
            maxFreq = maxFreq*100000;
            this.minFreq = minFreq;
            this.maxFreq = maxFreq;
            time = t*1000;
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
        public void run(){
            long start = System.currentTimeMillis();
            long current = start;
            while(current-start<time) {
                try {
                    java.lang.Process su = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(su.getOutputStream());

                    os.writeBytes("echo " + minFreq + " >> " + minFreqPathCPU0 + "\n");
                    os.flush();


                    os.writeBytes("exit\n");
                    os.flush();


                    os.close();


                    su.waitFor();

                    Log.i("getFrequency", getFrequency());

                    current = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    Log.i("InterruptedException", "frequencyScaling: ");
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.i("IOException", "frequencyScaling: ");
                    e.printStackTrace();
                }
            }
        }
    }
}
