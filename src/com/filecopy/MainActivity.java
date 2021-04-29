package com.filecopy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.Environment;
import android.os.AsyncTask;

import android.os.ServiceManager; 
import android.os.IPowerManager;
import android.os.RemoteException;

import android.os.SystemClock;

public class MainActivity extends Activity {

    MyAsyncTask task;

    private long mSize;
    private long mCsize;    

    TextView current, total;

    Button copyButton;
    ProgressBar progressbar;
    boolean isAsyncTask = false;

    String TARGETFOLDER = "/storage/ext_sdcard/inavi3d/mapdata";
    String SECONDFOLDER = "/storage/ext_sdcard/test";
    String COPYFOLDER = "/inavi/mapdata";
    String REMOVEFOLDER = "/inavi/temp";
    String SECONDREMOVEFOLDER = "/inavi/test_B";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressbar = (ProgressBar) findViewById(R.id.progress);
        copyButton = (Button) findViewById(R.id.copy);        

        current = (TextView) findViewById(R.id.current);
        total = (TextView)findViewById(R.id.total);

        task = new MyAsyncTask();

        copyButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(task.getStatus() == AsyncTask.Status.FINISHED || task.getStatus() == AsyncTask.Status.RUNNING){                                        
                    return;                
                }else{       

                    task = new MyAsyncTask();
                    File targetF = new File(TARGETFOLDER);
                    File secondF = new File(SECONDFOLDER); 
                    if(targetF.exists() && secondF.exists()){                         
                        removeFile(REMOVEFOLDER);                                                     
                        //removeFile(SECONDREMOVEFOLDER);
                        checkSize(targetF);
                        //checkSize(secondF);

                        task.execute(); 
                    } else {
                        total.setText("해당 파일/폴더가 존재하지 않습니다.");
                    }                    
                }
            }
        }) ;

    }

    @Override
    protected void onResume(){
        super.onResume();
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    @Override
    public void onBackPressed(){
        if(isAsyncTask==true){
            return;
        }
        super.onBackPressed();
    }

    private void removeFile(String targetFile) {
        try{
            File file = new File(targetFile); 

            if(file.exists()){
                File[] childFileList = file.listFiles();

                for(File childFile : childFileList){
                    if(childFile.isDirectory()) {
                        removeFile(childFile.getAbsolutePath());    
                    } else {                        
                        childFile.delete();                                                
                    }
                }                
                file.delete();                    
            }
        }catch(Exception e){
            e.printStackTrace();
            total.setText("복사 진행중 에러가 발생하였습니다.");
            current.setText("");
            progressbar.setProgress(0);
        }
    }

    private void checkSize(File targetFolder){    
        try{
            File[] listFile = targetFolder.listFiles();   
            for(int i=0;i<listFile.length;i++){                
                    if(listFile[i].isFile()){
                        mSize += listFile[i].length();
                    }else{                        
                        checkSize(listFile[i]);
                    }
            }
        }catch(Exception e){
            e.printStackTrace();
            total.setText("복사 진행중 에러가 발생하였습니다.");
            current.setText("");
            progressbar.setProgress(0);
        }
     }

    public void reboot() {
       IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
       try {
            pm.reboot(false, null, true); 
       } catch(RemoteException e) { 
            e.printStackTrace();
       } 
    }


    class MyAsyncTask extends AsyncTask<Void, Integer, Boolean>
    {                

        private void mapdataCopy(File targetF, File copyF){                                    
            if(!copyF.exists()){
                copyF.mkdir();            
            }

            File[] ff = targetF.listFiles();
            int currentsize = 0;
            for (File file : ff) {
                File temp = new File(copyF.getAbsolutePath() + File.separator + file.getName());
                if(file.isDirectory()){
                    temp.mkdir();
                    mapdataCopy(file, temp);
                } else {
                    FileInputStream fis = null;
                    FileOutputStream fos = null;
                    try {
                        fis = new FileInputStream(file);
                        fos = new FileOutputStream(temp) ;
                        byte[] b = new byte[20480];  //20kb
                        int cnt = 0;
                        
                        while((cnt=fis.read(b)) != -1){
                            mCsize += cnt ;
                            fos.write(b, 0, cnt);
                            publishProgress();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        total.setText("복사 진행중 에러가 발생하였습니다.");
                        current.setText("");
                        progressbar.setProgress(0);
                    } finally {
                        try {
                            fis.close();
                            fos.close();                      
                        } catch (IOException e) {                            
                            e.printStackTrace();
                            total.setText("복사 진행중 에러가 발생하였습니다.");
                            current.setText("");
                            progressbar.setProgress(0);
                        }
                    }
                }
            }
        }   

        @Override
        protected Boolean doInBackground(Void... params) {            
            File targerFolder = new File(TARGETFOLDER);
            File copyFolder = new File(COPYFOLDER);            
            File secondTargerFolder = new File(SECONDFOLDER);
            File secondCopyFolder = new File(COPYFOLDER);
            isAsyncTask = true;
            //Log.d("@@@@@@","copy start");
            mapdataCopy(targerFolder,copyFolder);
            //mapdataCopy(secondTargerFolder,secondCopyFolder);
            //Log.d("@@@@@@","copy end");
            return true;
        }
 
        @Override
        protected void onPreExecute() {            
            super.onPreExecute();       
            total.setText("  /  "+mSize/1048576 + " MB");      
            progressbar.setProgress(0);   
            progressbar.setMax((int)(mSize/1048576));                     
        }

        @Override
        protected void onPostExecute(Boolean result) {            
            super.onPostExecute(result);            
            progressbar.getProgressDrawable().setColorFilter(
                    getResources().getColor(R.color.colorSucess),
                    android.graphics.PorterDuff.Mode.SRC_IN); 
            isAsyncTask = false;
            mSize = 0;
            mCsize =0;    
            //reboot();     
        }

        @Override
        protected void onCancelled(){     
            super.onCancelled();            
            total.setText("복사 진행중 에러가 발생하였습니다.");
            current.setText("");
            progressbar.setProgress(0);            
        }
 
 
        @Override
        protected void onProgressUpdate(Integer... values) {            
            super.onProgressUpdate(values);             
            progressbar.setProgress((int)(mCsize/1048576));
            if(mCsize>1048576){
                current.setText(""+mCsize/1048576);
            }
        }
        
    }



}
