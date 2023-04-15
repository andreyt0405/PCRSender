package com.pcr.sender;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.Telephony;
import android.util.Log;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_SEND_SMS = 1;
    String currentTime = null;
    String body,defaultSmsApp,minutes,hour;
    String HMO="CLALIT";
    Context context;
    Button button;
    EditText name,id;
    CalendarView calendar;
    Calendar rightNow;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rightNow = Calendar.getInstance();
        setContentView(R.layout.activity_main);
        hour = String.valueOf(rightNow.get(Calendar.HOUR_OF_DAY));// return the hour in 24 hrs format (ranging from 0-23)
        minutes = String.valueOf(rightNow.get(Calendar.MINUTE));
        context = this;
        //Set composant
        button = (Button) findViewById(R.id.button);
        name = findViewById(R.id.name);
        id = findViewById(R.id.id);
        calendar = findViewById(R.id.calendar);
        spinner = findViewById(R.id.spinner);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, R.layout.spinner_text_view,
                getResources().getStringArray(R.array.HMO));
        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                HMO = parent.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ChangeDateType();
        //Button to write to the default sms app
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!(name.getText().toString().equals("")) &&!(id.getText().toString().equals(""))){
                    BuildSMString();
                    //Get the package name and check if my app is not the default sms app
                    final String myPackageName = getPackageName();
                    if (!Telephony.Sms.getDefaultSmsPackage(context).equals(myPackageName)) {

                        //Change the default sms app to my app
                        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.getPackageName());
                        startActivityForResult(intent, 1);
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(), getString(R.string.ERROR), Toast.LENGTH_LONG).show();
                }
            }
        });
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            //show the selected date as a toast
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                month = month+1;
                currentTime = day+"/"+month+"/"+year +" " + hour+":"+minutes;
                Log.i("currentTime",currentTime);
            }
        });
    }

    private void BuildSMString(){
        if(currentTime == null)
        {
            currentTime = new SimpleDateFormat("dd/MM/yyyy HH:mm",
                    Locale.getDefault()).format(new Date());

        }
        defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context);

        String testNumber = String.format("מספר בדיקה: %s",new Random().nextInt(900000000) + 100000000);
        Log.i("currentTime",currentTime);

        String patientName = String.format("שם הנבדק %s",name.getText());
        String patientID = String.format("מספר תעודת זהות/דרכון: %s",id.getText());
        //Set the number and the body for the sms

        String patientResult = String.format("תוצאת בדיקת ה PCR לקורונה של %s שבוצעה",name.getText());

        Log.i("DetailsPat",patientID + " "+patientName+" ");
        String editDate = String.format("במועד %s היא שלילית, הבדיקה תקפה ל72 שעות ממועד הביצוע.", currentTime);
        body = patientName + "\n"+ patientID+'\n' + testNumber+'\n' + "\n" +
                "\n" + patientResult+'\n' + editDate+"\n"+ "\n" +
                "\n" + "אין לפנות בנושא תוצאות בדיקה לחברת הדיגום\n" +
                "בדיקה זו אינה תקפה לצורך יציאה מהארץ\n" +
                "לבירורים יש לפנות לקופ\"ח, במידה ואינך חבר קופה יש לפנות ל 5400*";
    }
    //Write to the default sms app
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void WriteSms(String message) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm"); // I assume d-M, you may refer to M-d for month-day instead.
        Date date = formatter.parse(currentTime); // You will need try/catch around this
        long millis = date.getTime()+ TimeUnit.HOURS.toMillis(16);
        Log.i("millismillis",String.valueOf(millis));

        //Put content values
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, HMO);
        values.put(Telephony.Sms.DATE, millis);
        values.put(Telephony.Sms.CREATOR, HMO);
        values.put(Telephony.Sms.BODY, message);

        //Insert the message
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
        }
        else {
            context.getContentResolver().insert(Uri.parse("content://sms/outbox"), values);
        }
        //Change my sms app to the last default sms
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsApp);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://sdcard/LabResult.docx"));
        context.startActivity(intent);
        readWordFile();
    }

    //Get result from default sms dialog pops up
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                final String myPackageName = getPackageName();
                if (Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {

                    //Write to the default sms app
                    try {
                        WriteSms(body);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private void readWordFile(){
        InputStream file = null;
        String fileOutPut = null;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);
        try {

            AssetManager assetManager = this.getAssets();
            if(spinner.getSelectedItem().equals("CLALIT")) {
                file = assetManager.open("CDFLabTest.docx");
                fileOutPut = "/sdcard/Documents/CLabResult.docx";
            }
            else if(spinner.getSelectedItem().equals("MEUHEDET"))
            {
                file = assetManager.open("MDFLabTest.docx");
                fileOutPut = "/sdcard/Documents/MLabResult.docx";
            }
            else if(spinner.getSelectedItem().equals("MACCABI"))
            {
                file = assetManager.open("MADFLabTest.docx");
                fileOutPut = "/sdcard/Documents/MALabResult.docx";
            }
            XWPFDocument doc = new XWPFDocument(file);
            for (XWPFParagraph p : doc.getParagraphs()) {
                List<XWPFRun> runs = p.getRuns();
                if (runs != null) {
                    for (XWPFRun r : runs) {
                        String text = r.getText(0);
                        if (text != null && text.contains("000000000")) {
                            text = text.replace("000000000", id.getText());
                            r.setText(text, 0);
                        }
                        if (text != null && text.contains("12122021")) {
                            text = text.replace("12122021", currentTime);
                            r.setText(text, 0);
                        }
                        if (text != null && text.contains("NameLast")) {
                            text = text.replace("NameLast", name.getText());
                            r.setText(text, 0);
                            r.setBold(true);
                        }
                    }
                }
            }
            doc.write(new FileOutputStream(fileOutPut));
        }
        catch (IOException E)
        {
            E.printStackTrace();
        }
    }

    private void ChangeDateType()
    {
        if(minutes.length()<2)
        {
            minutes = "0"+minutes;
        }
        if (hour.length() < 2){
            hour = "0"+hour;
        }
    }
    public void openExternalStorage(View view)
    {
        File wordFile = new File("/sdcard/Documents/MLabResult.docx");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                BuildConfig.APPLICATION_ID + ".provider", wordFile);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        startActivity(intent);
    }
}