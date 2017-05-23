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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by giuliopettenuzzo on 23/05/17.
 */

public class FrequencyGeneratorService extends Service {

    String currFreq = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";

    PowerManager pm = null;
    PowerManager.WakeLock w1 = null;//
    FrequencyGeneratorService.ThreadCounterOverhead tco = null;

    String result = "";


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
        int currentUtilization = 0;
        while(currentUtilization<=100){
            tco = new ThreadCounterOverhead(currentUtilization,10);
            tco.start();
            while(tco.isAlive()){
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            currentUtilization=currentUtilization+10;
        }
        return super.onStartCommand(i,flags,strId);
    }
    @Override
    public void onDestroy(){
        Intent intent = new Intent(this,FrequencyGeneratorActivity.class);
        intent.putExtra("response",result);
        startActivity(intent);
        super.onDestroy();

    }

    private final class ThreadCounterOverhead extends Thread {

        int usage,time,notUsage;
        String usagePath = "proc/stat";
        File outputFile = null;
        String resultInString="";

        public void setResult(String currentResult){
            result = result + currentResult;
        }

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

        public String printResult(String esecuzione,String sleep,String frequency) throws IOException{
            FileWriter fw = new FileWriter(outputFile,true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            String myOutput = "Esecuzione: " + esecuzione + ", sleep: " + sleep + ", frequency: " + frequency;
            pw.println(myOutput);
            pw.close();
            bw.close();
            fw.close();
            return myOutput;
        }

        /**
         * this class is alowed to read the frequency in time O(1)
         *
         * @return
         */
        public String getFrequency() {
            String frequency = "";
            try {
                FileReader curFreqFile = new FileReader(currFreq);
                BufferedReader buffer = new BufferedReader(curFreqFile);
                frequency = buffer.readLine();

                buffer.close();
                curFreqFile.close();
            } catch (FileNotFoundException e) {
                Log.e("getFrequency: ", "FileNotFoundException");
            } catch (IOException e) {
                Log.e("getFrequency: ", "IOException");
            }
            return frequency;
        }


        public void run(){
            long timeUnit = 100;
            long start = System.currentTimeMillis();
            long current = start;
            long execTime = timeUnit*usage;
            long sleepTime = timeUnit*notUsage;
            int dummy = 0;
            String frequency = "";


            try{
                ArrayList<String> listLine = readCpusString();
                long initialExTime = getTotalUsage(listLine);
                long initialSleepTime = getTotalSleep(listLine);

                int i=0,j=0;
                while(current-start<time){
                    long intStart = System.currentTimeMillis();
                    long intCurr = intStart;

                    if(execTime != 0){
                        while(intCurr-intStart<execTime){
                            dummy++;
                            intCurr=System.currentTimeMillis();

                        }
                    }
                    if(sleepTime!=0){
                        try{
                            Thread.sleep(sleepTime);
                        }
                        catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    //totBatteryConsumption = totBatteryConsumption + getCurrentAndVoltage();
                    frequency = frequency + getFrequency();
                    current = System.currentTimeMillis();
                    Log.i("utilizzazione grande", String.valueOf(j));
                    j++;
                }
                ArrayList<String> listLine2 = readCpusString();
                long finalExTime = getTotalUsage(listLine2);
                long finalSleep = getTotalSleep(listLine2);
                long totUse = finalExTime - initialExTime;
                long totSlp = finalSleep - initialSleepTime;
                resultInString = printResult(String.valueOf(totUse),String.valueOf(totSlp),frequency);
                setResult(resultInString);
            }
            catch(IOException e){
                e.printStackTrace();
            }

            w1.release();

           // Intent result = new Intent(getBaseContext(),cpuOverheadActivity.class);
           // result.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           // result.putExtra("result",resultInString);
            //result.putExtra("result_battery",totBatteryConsumption);
           // startActivity(result);
        }

    }
}
