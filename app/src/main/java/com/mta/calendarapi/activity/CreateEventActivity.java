package com.mta.calendarapi.activity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.mta.calendarapi.R;
import com.mta.calendarapi.model.StaticValues;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

public class CreateEventActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    SwitchCompat sASwitch;
    TextView tv_allday, tv_sumary,tv_location, tv_fromdate,tv_fromtime,tv_todate,tv_totime;
    EditText edt_location, edt_sumary;
    LinearLayout ln_settime;
    Button btn_create;
    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = { CalendarScopes.CALENDAR };
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Tạo sự kiện mới");

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Creating event");
        
        String accName = getIntent().getStringExtra(StaticValues.SHAREPREFERENCE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        mCredential.setSelectedAccountName(accName);

        edt_location = (EditText) findViewById(R.id.edt_location);
        edt_sumary = (EditText) findViewById(R.id.edt_sumary);

        tv_allday = (TextView) findViewById(R.id.tv_allday);
        
        ln_settime = (LinearLayout) findViewById(R.id.ln_setTime);
        
        btn_create = (Button) findViewById(R.id.btn_create);
        
        btn_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequest();        
            }
        });

        tv_sumary = (TextView) findViewById(R.id.tv_sumary);

        tv_location = (TextView) findViewById(R.id.tv_location);

        tv_fromdate = (TextView) findViewById(R.id.tv_fromdate);
        tv_fromtime = (TextView) findViewById(R.id.tv_fromtime);

        tv_todate = (TextView) findViewById(R.id.tv_todate);
        tv_totime = (TextView) findViewById(R.id.tv_totime);

        sASwitch = (SwitchCompat) findViewById(R.id.switch_all);
        sASwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    tv_allday.setVisibility(View.VISIBLE);
                    ln_settime.setVisibility(View.GONE);
                }else {
                    tv_allday.setVisibility(View.GONE);
                    ln_settime.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private void sendRequest() {
        new MakeRequestTask(mCredential).execute();

    }


    public void selectTime(View view) {
        final TextView textView = (TextView) view;
        Calendar now = Calendar.getInstance();
        TimePickerDialog dpd = TimePickerDialog.newInstance(this, now.get(Calendar.HOUR),now.get(Calendar.MINUTE),now.get(Calendar.SECOND),true);
        dpd.show(getFragmentManager(), "Datepickerdialog");
        dpd.setOnTimeSetListener(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                String hour = String.valueOf(hourOfDay);
                String minutes = String.valueOf(minute);
                if (hourOfDay<10){
                    hour = "0"+hour;
                }
                if (minute<10){
                    minutes = "0"+minutes;
                }
                textView.setText(hour+":"+minutes);
            }
        });
    }

    public void selectDate(View view) {
        final TextView textView = (TextView) view;
        Calendar now = Calendar.getInstance();
        DatePickerDialog dpd = DatePickerDialog.newInstance(
                this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dpd.show(getFragmentManager(), "Datepickerdialog");
        dpd.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                String month=String.valueOf(monthOfYear+1);
                String day = String.valueOf(dayOfMonth);
                if ((monthOfYear+1)<10){
                    month = "0"+String.valueOf(monthOfYear+1);
                }
                if ((dayOfMonth)<10){
                    day = "0"+String.valueOf(dayOfMonth);
                }
                textView.setText(year+"-"+month+"-"+day);
            }
        });
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {

    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
private class MakeRequestTask extends AsyncTask<String, Void, Void> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential = null;


        MakeRequestTask(GoogleAccountCredential credential) {
            this.credential = credential;
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected Void doInBackground(String... params) {
            try {
                return getEventsFromApi(params);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /*
        * Fetch holiday VN
        * */


        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         * @param params
         */
        private Void getEventsFromApi(String[] params) throws IOException {
            com.google.api.services.calendar.Calendar service = new com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("applicationName").build();
            /*
            *
            * */
            Event event = new Event()
                    .setSummary(edt_sumary.getText().toString())
                    .setLocation(edt_location.getText().toString())
                    .setDescription("no");
            if (! sASwitch.isChecked()){

                String valueDate = tv_fromdate.getText()+"T"+tv_fromtime.getText()+":00.000Z";
                String valueDate2 = tv_todate.getText()+"T"+tv_totime.getText()+":00.000Z";


                Log.d("value332", valueDate);
                DateTime startDateTime = new DateTime(valueDate);
                EventDateTime start = new EventDateTime()
                        .setDateTime(startDateTime).setTimeZone("UTC+07:00");
                event.setStart(start);

                DateTime endDateTime = new DateTime(valueDate2);
                EventDateTime end = new EventDateTime()
                        .setDateTime(endDateTime).setTimeZone("UTC+07:00");
                event.setEnd(end);

                String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
                event.setRecurrence(Arrays.asList(recurrence));

                String calendarId = "primary";
                try {
                    event = service.events().insert(calendarId, event).execute();
                } catch (IOException e) {
                    Log.d("abc",e.getMessage());
                }
            }else {
                String valueAllDay = tv_allday.getText().toString();

                Log.d("value", valueAllDay);

                DateTime startDateTime = new DateTime(valueAllDay+"T00:00:00.000Z");
                EventDateTime start = new EventDateTime()
                        .setDateTime(startDateTime).setTimeZone("UTC+07:00");
                event.setStart(start);

                DateTime endDateTime = new DateTime(valueAllDay+"T23:59:59.000Z");
                EventDateTime end = new EventDateTime()
                        .setDateTime(endDateTime).setTimeZone("UTC+07:00");
                event.setEnd(end);

                String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
                event.setRecurrence(Arrays.asList(recurrence));
                String calendarId = "primary";
                try {
                    event = service.events().insert(calendarId, event).execute();
                } catch (IOException e) {
                    Log.d("abc",e.getMessage());
                }
            }


            return null;
        }


        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(Void a) {
            mProgress.hide();
            Toast.makeText(CreateEventActivity.this, "Done", Toast.LENGTH_SHORT).show();
        }
    }
}
