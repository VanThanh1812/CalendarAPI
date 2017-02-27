package com.mta.calendarapi.activity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.mta.calendarapi.R;
import com.mta.calendarapi.adapter.EventAdapter;
import com.mta.calendarapi.model.StaticValues;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllEventActivity extends AppCompatActivity {
    ProgressDialog mProgress;
    EventAdapter adapter;
    ListView lst,lst2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_event);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String accName = getIntent().getStringExtra(StaticValues.SHAREPREFERENCE);
        getSupportActionBar().setTitle("All Event");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        lst = (ListView) findViewById(R.id.lst_viewall);

        mProgress = new ProgressDialog(this);
        GoogleAccountCredential mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(CalendarScopes.CALENDAR_READONLY))
                .setBackOff(new ExponentialBackOff());
        mCredential.setSelectedAccountName(accName);
        new MakeRequestTask(mCredential,null).execute();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MakeRequestTask extends AsyncTask<String, Void, List<Event>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential = null;
        CalendarDay date = null;

        MakeRequestTask(GoogleAccountCredential credential, CalendarDay date) {
            this.credential = credential;
            this.date = date;
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
        protected List<Event> doInBackground(String... params) {
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
        private List<Event> getEventsFromApi(String[] params) throws IOException {
            String pageTokenEvent = null;
            Events events;
            do {
                events = mService.events().list("primary")
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .setPageToken(pageTokenEvent)
                        .execute();
                pageTokenEvent = events.getNextPageToken();
            }while (pageTokenEvent!=null);

            for (Event event : events.getItems()) {
//                DateTime start = event.getStart().getDateTime();
//                if (start == null) {
//                    // All-day events don't have start times, so just use
//                    // the start date.
//                    start = event.getStart().getDate();
//                }
//                eventStrings.add(
//                        String.format("%s (%s)", event.getSummary(), start));
                Log.d("event", event.toString()+"\n");
            }
            return events.getItems();
        }


        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<Event> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                Toast.makeText(AllEventActivity.this, "Don't have any event", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("output", String.valueOf(output.size()));
                Toast.makeText(AllEventActivity.this, "You have total "+output.size()+" event", Toast.LENGTH_SHORT).show();
                adapter = new EventAdapter(AllEventActivity.this, (ArrayList<Event>) output);
                lst.setAdapter(adapter);
            }
        }
    }

}
