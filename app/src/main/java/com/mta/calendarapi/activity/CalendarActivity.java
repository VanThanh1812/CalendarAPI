package com.mta.calendarapi.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.mta.calendarapi.R;
import com.mta.calendarapi.adapter.EventAdapter;
import com.mta.calendarapi.model.StaticValues;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.CalendarMode;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import static com.mta.calendarapi.activity.MainActivity.REQUEST_GOOGLE_PLAY_SERVICES;

public class CalendarActivity extends AppCompatActivity {
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY, CalendarScopes.CALENDAR };
    private ProgressDialog mProgress;
    ListView lst;
    EventAdapter adapter;
    ArrayList<Event> items;
    String accName;

    FloatingActionMenu fab_menu;
    GoogleAccountCredential mCredential;
    String[] MODE_REQUEST ={"HOLIDAYS", "EVENTS", "BIRTHDAY"};
    String[] ID_CALENDAR = {"en.vietnamese#holiday@group.v.calendar.google.com", // VN - UK - US - JP
                                    "en.uk#holiday@group.v.calendar.google.com",
                                    "en.usa#holiday@group.v.calendar.google.com",
                                    "en.japanese#holiday@group.v.calendar.google.com"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        accName = getIntent().getStringExtra("ACCOUNTNAME");
        getSupportActionBar().setTitle(" "+accName);
        getSupportActionBar().setIcon(R.drawable.ic_calendar);

        fab_menu = (FloatingActionMenu) findViewById(R.id.menu);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.menu_viewall);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab_menu.close(true);
                Intent i = new Intent(CalendarActivity.this, AllEventActivity.class);
                i.putExtra(StaticValues.SHAREPREFERENCE, accName);
                startActivity(i);
            }
        });

        FloatingActionButton fab2 = (FloatingActionButton) findViewById(R.id.menu_add);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab_menu.close(true);
                Intent i = new Intent(CalendarActivity.this, CreateEventActivity.class);
                i.putExtra(StaticValues.SHAREPREFERENCE, accName);
                startActivity(i);

            }
        });

        FloatingActionButton fab3 = (FloatingActionButton) findViewById(R.id.menu_del);
        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab_menu.close(true);
                Snackbar.make(lst, "Long click item event to get delete option", Snackbar.LENGTH_LONG).show();
            }
        });



        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Request API");
        lst = (ListView) findViewById(R.id.lst_event);
        lst.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                Log.d("longclcik", adapterView.getItemAtPosition(i).toString());
                final Event event= (Event) adapterView.getItemAtPosition(i);
                final AlertDialog.Builder aBuilder = new AlertDialog.Builder(lst.getContext());
                aBuilder.setTitle("Delete Event"+" '"+event.getSummary()+"'");
                aBuilder.setMessage("Are you sure ?");
                aBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                aBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        new MakeRequestDelete(mCredential, event, lst.getContext()).execute();
                    }
                });
                aBuilder.show();
                return true;
            }
        });

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        mCredential.setSelectedAccountName(accName);

        MaterialCalendarView materialCalendarView = (MaterialCalendarView) findViewById(R.id.cld_main);
        //getTime() returns the current date in default time zone
        materialCalendarView.state().edit()
                .setCalendarDisplayMode(CalendarMode.MONTHS)
                .commit();

        materialCalendarView.setDynamicHeightEnabled(true);
        materialCalendarView.setCurrentDate(java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC+07:00")));
        materialCalendarView.setSelectedDate(java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC+07:00")));
        materialCalendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                mainHandleApi(accName, date);
                fab_menu.close(true);
            }
        });

    }

    private void mainHandleApi(String accName, CalendarDay date) {
        new MakeRequestTask(mCredential, date, MODE_REQUEST[1]).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_viewholiday:
                Intent i = new Intent(CalendarActivity.this, AllHoliday.class);
                i.putExtra(StaticValues.SHAREPREFERENCE, accName);
                startActivity(i);
                fab_menu.close(true);
                return true;
            case R.id.action_changeacc:
                logOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logOut() {
        SharedPreferences settings =
                getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear().apply();
        finish();
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                CalendarActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private class MakeRequestDelete extends AsyncTask<String, Void, Void>{
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential = null;
        private Event event;
        Context c;
        public MakeRequestDelete(GoogleAccountCredential credential, Event event, Context c) {
            this.credential = credential;
            this.event = event;
            this.c = c;
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgress.dismiss();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress.show();
        }

        @Override
        protected Void doInBackground(String... strings) {
            deleteEvent();
            return null;
        }

        private void deleteEvent() {
            try {
                mService.events().delete("primary", event.getId()).execute();
            } catch (IOException e) {
                Log.d("error", e.getMessage());
            }
        }
    }

    private class MakeRequestTask extends AsyncTask<String, Void, List<Event>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential = null;
        CalendarDay date = null;
        String mode_request = null;
        MakeRequestTask(GoogleAccountCredential credential, CalendarDay date, String mode_request) {
            this.credential = credential;
            this.date = date;
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
            this.mode_request = mode_request;
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
        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         * @param params
         */
        private List<Event> getEventsFromApi(String[] params) throws IOException {
            // List the next 10 events from the primary calendar.
//            DateTime now = new DateTime(System.currentTimeMillis());
           Calendar service = new Calendar.Builder(transport, jsonFactory, credential)
                    .setApplicationName("applicationName").build();

            // Iterate through entries in calendar list
            String pageToken = null;
            do {
                CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
                List<CalendarListEntry> items = calendarList.getItems();

                for (CalendarListEntry calendarListEntry : items) {
                    Log.d("calendar", calendarListEntry.getId()+" "+calendarListEntry.getSummary());
                }
                pageToken = calendarList.getNextPageToken();
            } while (pageToken != null);

            String pageTokenEvent = null;
            Events events;
            do {
                java.util.Calendar cal = date.getCalendar();
                cal.add(java.util.Calendar.DATE,+1);
                DateTime timeMax = new DateTime(cal.getTimeInMillis());
                DateTime timeMin = new DateTime(date.getDate());
                events = service.events().list("primary").setTimeMin(timeMin).setTimeMax(timeMax).setShowDeleted(false).setPageToken(pageTokenEvent).execute();
                List<Event> items = events.getItems();

                /*for (Event event : items) {
                    Log.d("eventSumm",items.size()+event.getSummary()+"\n"+event.getStart().getDate().toStringRfc3339()+"\n"+event.getEnd().getDate().toStringRfc3339());

                }*/
                for (int i = 0; i< items.size(); i++){
                    Log.d("numberevnt", String.valueOf(items.size()));
                    Event event = items.get(i);
                    Log.d("eventSumm",event.toString());
                    Log.d("eventSumm",event.getSummary());

                }
                pageTokenEvent = events.getNextPageToken();
            } while (pageTokenEvent != null);

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
            switch (this.mode_request){
                case "HOLIDAYS":{
                    if (output == null || output.size() == 0) {
                        Toast.makeText(CalendarActivity.this, "Some Error!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("output", String.valueOf(output.size()));
                        Toast.makeText(CalendarActivity.this, "You have "+output.size()+" event", Toast.LENGTH_SHORT).show();
                        /*
                        * create list view
                        * */
                        ListView lst = new ListView(credential.getContext());
                        adapter = new EventAdapter(CalendarActivity.this, (ArrayList<Event>) output);
                        lst.setAdapter(adapter);
                        /*
                        *  create alert
                        * */

                    }
                    break;
                }
                case "EVENTS":{
                    if (output == null || output.size() == 0) {
                        Toast.makeText(CalendarActivity.this, "Don't have any event", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("output", String.valueOf(output.size()));
                        Toast.makeText(CalendarActivity.this, "You have "+output.size()+" event", Toast.LENGTH_SHORT).show();
                        adapter = new EventAdapter(CalendarActivity.this, (ArrayList<Event>) output);
                        lst.setAdapter(adapter);

                    }
                    break;
                }
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();

            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                  //  mOutputText.setText("The following error occurred:\n"
                            //+ mLastError.getMessage());
                }
            } else {
                Toast.makeText(CalendarActivity.this, "Request cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
