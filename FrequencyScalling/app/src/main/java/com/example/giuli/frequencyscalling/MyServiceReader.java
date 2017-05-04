package com.example.giuli.frequencyscalling;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
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

/**
 * Created by giuli on 24/04/2017.
 * al momento questa classe è chiamatra trammite un intent implicito da MainActivity ma non è giusto
 * MyServiceReader e cpuOverhead dovrebbero lavorare insieme!
 * per farlo è necessario chiamare i thread nella stessa classe non i servizzi
 * se non riesco a farli funzionare in simultanea mi accontenterò di avere i valori di
 * corrente prima e dopo il lancio dell'altro servizio
 * bisogna anche settare l'activity che restituisce i valori di ritorno di corrente
 */

public class MyServiceReader extends Service {

    MyThreadReader mtr = null;
    int timeOut = 0;

    public static File outputFile = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent i,int flags,int strId){
        int tempo = i.getExtras().getInt("tempo_battery");
        mtr = new MyServiceReader.MyThreadReader(tempo);
        mtr.start();

        return super.onStartCommand(i,flags,strId);
    }

    public File getOutputFile(){
        return outputFile;
    }

    /**
     * Created by giuli on 06/04/2017.
     */

    public final class MyThreadReader extends Thread {

        String currentPath = "/sys/devices/platform/mt6329-battery/FG_Battery_CurrentConsumption";
        String voltagePath = "/sys/devices/platform/mt6329-battery/power_supply/battery/InstatVolt";
        String current;
        String outputCurrent = "";
        String voltage;
        String outputVoltage = "";



        public MyThreadReader(int time){
            outputFile = new File(getApplicationContext().getFilesDir(),"OverheadBatteryOutput.txt");
            timeOut = time*1000;
        }
        @Override
        public void run(){
            long startTime = System.currentTimeMillis();
            long endTime = startTime;
            //int mSec = 10000; //10 secondi
            int i = 0;
            try {

                while (endTime - startTime < timeOut) {
                    //read the current
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

                    outputCurrent = outputCurrent +current+ "microAmpere"+"\r";
                    outputVoltage = outputVoltage +voltage+ "microVolt"+"\r";

                    writeOutputToFile(outputCurrent,outputVoltage);
                    Log.i("battery", String.valueOf(i));
                    i++;
                    Thread.sleep(3000);
                    endTime = System.currentTimeMillis();

                }

            }
            catch(FileNotFoundException e){
                Log.i("onCreate : ", " File not found exception");
            }
            catch(IOException e){
                Log.i("onCreate : ", " IO Exception");
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(getApplicationContext(),com.example.giuli.frequencyscalling.cpuOverheadActivity.class);
            intent.putExtra(getStringFromOutputFile(),"battery_result");
        }
        public String getOutputCurrent() {
            outputCurrent=outputCurrent;
            return outputCurrent;

        }

        public File writeOutputToFile(String current, String voltage){
            FileWriter fw = null;
            try {
                fw = new FileWriter(outputFile,true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            String output = "current = "+ current + ", voltage = " + voltage +"\r";
            pw.println(output);
            pw.close();

            try {
                fw.close();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return outputFile;
        }

        public String getStringFromOutputFile(){
            FileReader currentFile = null;
            String result = "";
            String totResult = "";
            try {
                currentFile = new FileReader(outputFile);

                BufferedReader fileBuffer = new BufferedReader(currentFile);

                while (!fileBuffer.readLine().isEmpty()) { //finchè trova nuove righe
                    result = fileBuffer.readLine();
                    totResult = totResult + result;
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
            return totResult;
        }

    }

}
