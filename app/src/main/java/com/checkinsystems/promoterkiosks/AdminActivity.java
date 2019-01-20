package com.checkinsystems.promoterkiosks;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.checkinsystems.promoterkiosks.model.ConfigurationObject;
import com.checkinsystems.promoterkiosks.model.Customer;
import com.checkinsystems.promoterkiosks.model.FileName;
import com.checkinsystems.promoterkiosks.util.FileDownloadClient;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AdminActivity extends AppCompatActivity implements DatePickerFragment.onDateSelectedListener {

    private static final String TAG = "AdminActivity";
    public static final String SHARED_PREFS = "preferences";
    public static final String CONFIG_OBJ = "config_obj";
    public static final String MESSAGES = "messages";
    public static final String INSTRUCTIONS = "instructions";
    public static final String DISCLAIMER = "disclaimer";
    public static final String TICKET_NUM = "ticket_num";
    private static final int REQUEST_PERMISSION = 100;
    private static final String DIALOG_DATE = "dialogDate";



    String mSelectedImage = "";  // to get a hook to the image name from recyclerview adapter
    String mRequestFilesString = " ";  // to get a hook to the request string in AsyncTask

    Context mContext;

    EditText mSystemID, mSystemPassword, mInstructions, mDisclaimer;
    RecyclerView mRecyclerView, mRecyclerView2;
    Button mDateButton, mSaveConfigButton, mRunButton, mUpdateFilesListButton, mSetPictureButton, mUplaodButton, mClearButton;
    TextView mNumOfRecords;
    CheckBox mPrintTickets;

    FileItemAdapter mAdapter;
    CustomerItemAdapter mCustomerAdapter;
    ConfigurationObject mObject;

    int currentApiVersion;
    List<Customer> mCustomers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mContext = this;
        // Here I create an empty list of FileNames to pass to the Adapters for each RecyclerView
        // so I can display the Recyclerview in onCreate without triggering a NullPointerException
        List<FileName> fileNames = new ArrayList<>();
        mCustomers = new ArrayList<>();


        mSystemID = findViewById(R.id.et_system_id);
        mSystemPassword = findViewById(R.id.et_system_password);
        mInstructions = findViewById(R.id.et_edit_instructions);
        mDisclaimer = findViewById(R.id.et_disclaimer);
        mRecyclerView = findViewById(R.id.rv_file_names);
        mRecyclerView2 = findViewById(R.id.rv_records);
        mSaveConfigButton = findViewById(R.id.bt_save_config);
        mDateButton = findViewById(R.id.btn_date);
        mRunButton = findViewById(R.id.bt_run);
        mUpdateFilesListButton = findViewById(R.id.bt_update_files_list);
        mSetPictureButton = findViewById(R.id.bt_set_picture);
        mUplaodButton = findViewById(R.id.bt_upload_data);
        mClearButton = findViewById(R.id.bt_clear_data);
        mNumOfRecords = findViewById(R.id.tv_num_of_records);
        mPrintTickets = findViewById(R.id.cb_print_tickets);

        createInitialConfigObject();
        createInitialMessages();

        Resources res = getResources();
        String records = res.getString(R.string.number_of_records, mObject.getCustomers().size());
        mNumOfRecords.setText(records);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(this);
        mRecyclerView2.setLayoutManager(layoutManager2);

        DividerItemDecoration divider = new DividerItemDecoration(mContext, layoutManager.getOrientation());
        divider.setDrawable(ContextCompat.getDrawable(mContext, R.drawable.divider_dark));
        mRecyclerView.addItemDecoration(divider);
        mRecyclerView2.addItemDecoration(divider);

        mAdapter = new FileItemAdapter(mContext, fileNames);
        mRecyclerView.setAdapter(mAdapter);
        mCustomerAdapter = new CustomerItemAdapter(mContext, mCustomers);
        mRecyclerView2.setAdapter(mCustomerAdapter);

        // in case permission not granted to write data, request it here
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }

        if(mObject.getDrawingDate() == null){
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String formattedDate = dateFormat.format(new Date());
            mDateButton.setText(formattedDate);
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String formattedDate = dateFormat.format(mObject.getDrawingDate());
            mDateButton.setText(formattedDate);
        }

        mPrintTickets.setChecked(mObject.isPrintTickets());

        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date date;
                if(mObject.getDrawingDate() == null) {
                    date = new Date();
                } else {
                    date = mObject.getDrawingDate();
                }

                android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(date);
                dialog.show(manager, DIALOG_DATE);
            }
        });

    }

    private void createInitialConfigObject() {

        // If a ConfigurationObject exists, populate systemID and password
        // Should find a better way to get this directory instead of hard-coding it
        File file = new File("data/data/com.checkinsystems.promoterkiosks/shared_prefs/preferences.xml");

        if (file.exists()) {
            SharedPreferences preferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
            String jsonString = preferences.getString(CONFIG_OBJ, "");
            mObject = new Gson().fromJson(jsonString, ConfigurationObject.class);

            mSystemID.setText(mObject.getSystemID());
            mSystemPassword.setText(mObject.getPassword());

            // load the records for this user
            mCustomers = mObject.getCustomers();

        } else {
            SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
            mObject = new ConfigurationObject("demo", "demo");
            String jsonString = new Gson().toJson(mObject, ConfigurationObject.class);
            editor.putString(CONFIG_OBJ, jsonString);
            editor.putInt(TICKET_NUM, 10000);
            editor.apply();

            mSystemID.setText(mObject.getSystemID());
            mSystemPassword.setText(mObject.getPassword());
        }
    }

    private void createInitialMessages() {
        File file = new File("data/data/com.checkinsystems.promoterkiosks/shared_prefs/messages.xml");

        if (file.exists()) {
            SharedPreferences preferences = getSharedPreferences(MESSAGES, MODE_PRIVATE);
            String instructions = preferences.getString(INSTRUCTIONS, " ");
            String disclaimer = preferences.getString(DISCLAIMER, " ");

            mInstructions.setText(instructions);
            mDisclaimer.setText(disclaimer);
        } else {
            SharedPreferences.Editor editor = getSharedPreferences(MESSAGES, MODE_PRIVATE).edit();
            editor.apply();
        }

    }

    public void updateConfig(View view) {
        if (mSystemID.getText().length() > 0 && mSystemPassword.getText().length() > 0) {
            SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
            SharedPreferences.Editor editor2 = getSharedPreferences(MESSAGES, MODE_PRIVATE).edit();

            if (mSystemID.getText().length() > 0) {
                mObject.setSystemID(mSystemID.getText().toString());
            }
            if (mSystemPassword.getText().length() > 0) {
                mObject.setPassword(mSystemPassword.getText().toString());
            }

            if (mInstructions.getText().length() > 0) {
                editor2.putString(INSTRUCTIONS, mInstructions.getText().toString());
            }
            if (mDisclaimer.getText().length() > 0) {
                editor2.putString(DISCLAIMER, mDisclaimer.getText().toString());
            }

            if(mPrintTickets.isChecked()){
                mObject.setPrintTickets(true);
            } else {
                mObject.setPrintTickets(false);
            }

            String jsonString = new Gson().toJson(mObject);
            editor.putString(CONFIG_OBJ, jsonString);
            editor.apply();
            editor2.apply();

            Toast toast = Toast.makeText(mContext, "Configuration has been saved", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

        } else {
            Toast.makeText(mContext, "You must enter a System ID and Password", Toast.LENGTH_SHORT).show();
        }
    }

    public void runProgram(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.main_fade_in, R.anim.admin_fade_out);
    }

    @SuppressLint("StaticFieldLeak")
    public void getPicturesList(View view) {

        if (mObject != null) {
            mSystemID.setText(mObject.getSystemID());
            mSystemPassword.setText(mObject.getPassword());
            SharedPreferences preferences = getSharedPreferences(MESSAGES, MODE_PRIVATE);
            mInstructions.setText(preferences.getString(INSTRUCTIONS, ""));
            mDisclaimer.setText(preferences.getString(DISCLAIMER, " "));

            mRequestFilesString = "systemid=" + mObject.getSystemID() + "&password=" + mObject.getPassword();
        }
        new AsyncTask<String, Void, List<FileName>>() {

            @Override
            protected List<FileName> doInBackground(String... strings) {
                List<String> fileNames;
                List<FileName> names = new ArrayList<>();
                fileNames = requestFileFromServer(mRequestFilesString);

                for (String str : fileNames) {
                    FileName fileName = new FileName(str);
                    names.add(fileName);
                }
                return names;
            }

            @Override
            protected void onPostExecute(List<FileName> files) {
                updateUI(files, null);
            }
        }.execute(mRequestFilesString);
    }

    public List<String> requestFileFromServer(String params) {

        URL url;
        String response = "";
        List<String> fileNames = new ArrayList<>();
        try {
            url = new URL("http://promoterkiosks.com/login/updateconfig.php");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);


            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(params);

            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            Log.d(TAG, "the response code is : " + conn.getResponseCode());

            if (responseCode == HttpsURLConnection.HTTP_OK) {

                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;

                    Log.e("Res:", response);

                    fileNames = Arrays.asList(response.split(","));
                }
            } else {
                Log.d(TAG, "there was a problem reaching the server");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "There was a problem reaching the server \n check your internet connection");
        }

        return fileNames;
    }

    public void downloadPicture(View view) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("http://www.promoterkiosks.com/login/customers/" + mObject.getSystemID() + "/");

        Retrofit retrofit = builder.build();
        FileDownloadClient fileDownloadClient = retrofit.create(FileDownloadClient.class);

        Call<ResponseBody> call = fileDownloadClient.downloadFileStream(mSelectedImage);

        call.enqueue(new Callback<ResponseBody>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull final Response<ResponseBody> response) {

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... voids) {
                        boolean success = writeImageToDisk(response.body());
                        Log.d(TAG, "You're download was successful");

                        return null;
                    }

                    @Override
                    public void onPostExecute(Void v) {

                    }
                }.execute();

                Toast toast = Toast.makeText(mContext, "You downloaded: " + mSelectedImage, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(AdminActivity.this, "Download Failed :(", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean writeImageToDisk(ResponseBody body) {
        if (body != null) {
            try {
                File imageFile = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PromoterKiosk Main Image.jpg");

                InputStream inputStream = null;
                OutputStream outputStream = null;

                try {
                    byte[] fileReader = new byte[4096];

                    long fileSize = body.contentLength();
                    long fileSizeDownloaded = 0;

                    inputStream = body.byteStream();
                    outputStream = new FileOutputStream(imageFile);

                    while (true) {
                        int read = inputStream.read(fileReader);

                        if (read == -1) {
                            break;
                        }
                        outputStream.write(fileReader, 0, read);

                        fileSizeDownloaded += read;
                    }

                    outputStream.flush();

                    return true;
                } catch (IOException e) {
                    return false;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }
            } catch (IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public void uploadDataToServer(View view) {
        sendConfigToServer(mObject);
    }

    @SuppressLint("StaticFieldLeak")
    private void sendConfigToServer(final ConfigurationObject object) {

        new AsyncTask<ConfigurationObject, Void, String>() {

            @Override
            protected String doInBackground(ConfigurationObject... configurationObjects) {
                URL url;
                String response = "";
                List<String> fileNames = new ArrayList<>();
                try {
                    url = new URL("http://promoterkiosks.com/login/webhook.php");

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);


                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));

                    String jsonString = new Gson().toJson(object);

                    Log.d(TAG, jsonString);

                    writer.write(jsonString);

                    writer.flush();
                    writer.close();
                    os.close();
                    int responseCode = conn.getResponseCode();

                    Log.d(TAG, "the response code is : " + conn.getResponseCode());

                    if (responseCode == HttpsURLConnection.HTTP_OK) {

                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                        while ((line = br.readLine()) != null) {

                            response += line;

                            Log.d(TAG, "Res: " + response);
                        }
                    } else {
                        Log.d(TAG, "there was a problem reaching the server");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "There was a problem reaching the server \n check your internet connection");
                }
                return response;
            }

            @Override
            protected void onPostExecute(String str) {

                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.dialog_upload_data, (ViewGroup) findViewById(R.id.dialog_upload_data));
                TextView textView = layout.findViewById(R.id.tv_upload_report);
                textView.setText(str);

                Toast toast = new Toast(mContext);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();
            }
        }.execute(object);
    }

    public void clearAllData(View view) {

        File file = new File("data/data/com.checkinsystems.promoterkiosks/shared_prefs/preferences.xml");
        if (file.exists()) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            LayoutInflater inflater = LayoutInflater.from(mContext);

            final View v = inflater.inflate(R.layout.dialog_clear_data, null);

            final EditText password = v.findViewById(R.id.et_clear_data_password);

            dialog.setView(v);

            dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    String pw = password.getText().toString();

                    if (pw.equalsIgnoreCase("delete")) {
                        mObject.getCustomers().removeAll(mObject.getCustomers());
                        String jsonString = new Gson().toJson(mObject);
                        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
                        editor.putString(CONFIG_OBJ, jsonString).apply();

                        updateUI(null, mObject.getCustomers());

                        hideKeyboard(mContext, v);

                    } else {
                        Toast toast = Toast.makeText(mContext, "You entered the wrong password ", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                        hideKeyboard(mContext, v);
                    }
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    hideKeyboard(mContext, v);

                    dialog.dismiss();
                }
            });
            dialog.create();
            dialog.show();

        }
    }

    public void exitProgram(View view) {
        finish();
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    private void updateUI(List<FileName> files, List<Customer> customers) {

        customers = mObject.getCustomers();

        if (customers == null) {
            mAdapter = new FileItemAdapter(this, files);
            mRecyclerView.setAdapter(mAdapter);
        } else if (files == null) {
            mCustomerAdapter = new CustomerItemAdapter(this, customers);
            mRecyclerView2.setAdapter(mCustomerAdapter);
            Resources res = getResources();
            String records = res.getString(R.string.number_of_records, mObject.getCustomers().size());
            mNumOfRecords.setText(records);
        } else {
            mAdapter = new FileItemAdapter(this, files);
            mRecyclerView.setAdapter(mAdapter);
            mCustomerAdapter = new CustomerItemAdapter(this, customers);
            mRecyclerView2.setAdapter(mCustomerAdapter);
            Resources res = getResources();
            String records = res.getString(R.string.number_of_records, mObject.getCustomers().size());
            mNumOfRecords.setText(records);
        }
    }


    // todo change this method name

    @Override
    public void someEvent(Date date) {
        mObject.setDrawingDate(date);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String formattedDate = dateFormat.format(mObject.getDrawingDate());
        mDateButton.setText(formattedDate);

    }


    public class FileItemAdapter extends RecyclerView.Adapter<FileItemAdapter.ViewHolder> {

        List<FileName> fileNames;
        Context mContext;
        int selectedPosition = -1;

        public FileItemAdapter(Context context, List<FileName> files) {
            mContext = context;
            fileNames = files;
        }

        @Override
        public FileItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View itemView = inflater.inflate(R.layout.list_item_file_names, parent, false);

            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final FileItemAdapter.ViewHolder holder, int position) {


            FileName file = fileNames.get(position);
            holder.fileName.setText(file.getFileName());

            if (selectedPosition == position) {
                holder.itemView.setBackgroundResource(R.color.highLightBlue);
                mSelectedImage = holder.fileName.getText().toString();
            } else {
                holder.itemView.setBackgroundResource(R.color.lightGrey);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedPosition = holder.getAdapterPosition();
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return fileNames.size();
        }


        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView fileName;
            View itemContainer;


            public ViewHolder(View itemView) {
                super(itemView);

                fileName = itemView.findViewById(R.id.tv_file_names);
                itemContainer = itemView.findViewById(R.id.list_item_container);

            }
        }
    }

    public class CustomerItemAdapter extends RecyclerView.Adapter<CustomerItemAdapter.CustomerHolder> {

        List<Customer> mCustomers;
        Context mContext;

        public CustomerItemAdapter(Context context, List<Customer> customers) {
            mContext = context;
            mCustomers = customers;
        }

        @Override
        public CustomerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = getLayoutInflater().from(mContext);
            View itemView = inflater.inflate(R.layout.list_item_customer, parent, false);
            return new CustomerHolder(itemView);
        }

        @Override
        public void onBindViewHolder(CustomerHolder holder, int position) {
            Customer customer = mCustomers.get(position);
            holder.name.setText(customer.getName());
            holder.email.setText(customer.getEmail());
        }

        @Override
        public int getItemCount() {
            return mCustomers.size();
        }

        public class CustomerHolder extends RecyclerView.ViewHolder {

            TextView name, email;

            public CustomerHolder(View itemView) {
                super(itemView);

                name = itemView.findViewById(R.id.tv_customer_name);
                email = itemView.findViewById(R.id.tv_customer_email);
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}

