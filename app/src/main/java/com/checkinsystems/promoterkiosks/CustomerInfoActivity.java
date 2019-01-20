package com.checkinsystems.promoterkiosks;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.checkinsystems.promoterkiosks.model.ConfigurationObject;
import com.checkinsystems.promoterkiosks.model.Customer;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import static com.checkinsystems.promoterkiosks.AdminActivity.CONFIG_OBJ;
import static com.checkinsystems.promoterkiosks.AdminActivity.DISCLAIMER;
import static com.checkinsystems.promoterkiosks.AdminActivity.INSTRUCTIONS;
import static com.checkinsystems.promoterkiosks.AdminActivity.MESSAGES;
import static com.checkinsystems.promoterkiosks.AdminActivity.SHARED_PREFS;
import static com.checkinsystems.promoterkiosks.AdminActivity.TICKET_NUM;

public class CustomerInfoActivity extends SerialPortActivity {

    public static final int MAX_WIDTH = 1920;
    private static final String TAG = "CustomerInfoActivity";
    Context mContext;
    ConfigurationObject mObject;


    View mMainContainer;
    EditText mCustomerName, mCustomerEmail;
    Handler mTimer, mTimerForButtons;
    Runnable mRunnable, mButtonTimer;
    Button mInvisibleLeft, mInvisibleRight;
    TextView mInstructions, mDisclaimer;
    ImageView mArrow1, mArrow2;


    boolean leftIsPressed = false;
    boolean rightIsPressed = false;
    int currentApiVersion;
    Button mEnterButton;
    boolean mIsNameSelected = true;

    String mDrawingDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplication = (Application) getApplication();

        try {
            mSerialPort = mApplication.getPrintSerialPort();
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

			/* Create a receiving thread */
            mReadThread = new SerialPortActivity.ReadThread();
            mReadThread.start();
        } catch (SecurityException e) {
            DisplayError(R.string.error_security);
        } catch (IOException e) {
            DisplayError(R.string.error_unknown);
        } catch (InvalidParameterException e) {
            DisplayError(R.string.error_configuration);
        }


        setContentView(R.layout.activity_customer_info);

        mContext = this;


        mMainContainer = findViewById(R.id.container_customer_info);
        mCustomerName = findViewById(R.id.et_customer_name);
        mCustomerEmail = findViewById(R.id.et_customer_email);
        setActivteListener(mCustomerName);
        setActivteListener(mCustomerEmail);


        mInstructions = findViewById(R.id.tv_instructions);
        mDisclaimer = findViewById(R.id.tv_disclaimer);
        mEnterButton = findViewById(R.id.bt_enter_next);
        mArrow1 = findViewById(R.id.arrow_1);
        mArrow2 = findViewById(R.id.arrow_2);

        mInvisibleLeft = findViewById(R.id.bt_left_invisible);
        mInvisibleRight = findViewById(R.id.bt_right_invisible);
        goToAdminListener(mInvisibleLeft);
        goToAdminListener(mInvisibleRight);



        // counter to measure idle time
        mTimer = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                mTimer.removeCallbacks(mRunnable);
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
            }
        };

        File file = new File("data/data/com.checkinsystems.promoterkiosks/shared_prefs/preferences.xml");
        if (file.exists()) {
            SharedPreferences preferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
            mObject = new Gson().fromJson(preferences.getString(CONFIG_OBJ, ""), ConfigurationObject.class);

            SharedPreferences preferences2 = getSharedPreferences(MESSAGES, MODE_PRIVATE);
            mInstructions.setText(preferences2.getString(INSTRUCTIONS, " "));
            mDisclaimer.setText(preferences2.getString(DISCLAIMER, " "));

        } else {

            Toast.makeText(mContext, "Something went wrong \n Startup configuration not found", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(mContext, AdminActivity.class);
            startActivity(intent);
        }

        updateUI();

        mTimer.postDelayed(mRunnable, 30000);  // start the counter

        // handler and runnable for invisible buttons that will bring the user to the AdminActivity
        mTimerForButtons = new Handler();
        mButtonTimer = new Runnable() {
            @Override
            public void run() {

                @SuppressLint("InflateParams")
                final View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_enter_admin, null);
                final EditText password = view.findViewById(R.id.et_dialog_password);


                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext)
                        .setView(view)
                        .setTitle("Edit Configuration?");
                dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (password.getText().toString().equalsIgnoreCase("pk")) {

                            Intent intent = new Intent(mContext, AdminActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(mContext, "wrong password", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }
                });
                dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.create();
                dialog.show();
            }
        };

//        try{
//            PackageInfo info = this.getPackageManager().getPackageInfo(getPackageName(), 0);
//            String version = info.versionName;
//            Resources res = getResources();
//            String text = res.getString(R.string.version, version);
//            mVersion.setText(text);
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }

        //hide the navigatoin bar
        currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {

            getWindow().getDecorView().setSystemUiVisibility(flags);

//         Code below is to handle presses of Volume up or Volume down.
//         Without this, after pressing volume buttons, the navigation bar will
//         show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }

    }

    private void updateUI() {
        if (mIsNameSelected) {
            mArrow1.setVisibility(View.VISIBLE);
            mArrow2.setVisibility(View.INVISIBLE);
            mEnterButton.setText(R.string.next);
            mCustomerName.requestFocus();
        } else {
            mArrow2.setVisibility(View.VISIBLE);
            mArrow1.setVisibility(View.INVISIBLE);
            mEnterButton.setText(R.string.enter);
            mCustomerEmail.requestFocus();
        }


        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyy", Locale.US);
        mDrawingDate = dateFormat.format(mObject.getDrawingDate());
    }

    @Override
    protected void onDataReceived(byte[] buffer, int size) {
        // doing nothing here
    }

    @Override
    public void onUserInteraction() {
        mTimer.removeCallbacks(mRunnable);
        mTimer.postDelayed(mRunnable, 60000);
    }

    @Override
    public void onPause() {
        mTimer.removeCallbacks(mRunnable);

        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            activityManager.moveTaskToFront(getTaskId(), 0);
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer.postDelayed(mRunnable, 60000);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void goToAdminListener(final Button bt) {
        bt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (bt.getId() == mInvisibleLeft.getId()) {
                        leftIsPressed = true;
                        if (rightIsPressed){
                            mTimerForButtons.postDelayed(mButtonTimer, 5000);
                        }
                    }
                    if (bt.getId() == mInvisibleRight.getId()) {
                        rightIsPressed = true;
                        if (leftIsPressed) {
                            mTimerForButtons.postDelayed(mButtonTimer, 5000);
                        }
                    }
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (bt.getId() == mInvisibleLeft.getId()) {
                        leftIsPressed = false;
                        if (mTimerForButtons != null) {
                            mTimerForButtons.removeCallbacks(mButtonTimer);
                        }
                    }
                    if (bt.getId() == mInvisibleRight.getId()) {
                        rightIsPressed = false;
                        if (mTimerForButtons != null) {
                            mTimerForButtons.removeCallbacks(mButtonTimer);
                        }
                    }
                    return true;
                }

                return false;
            }
        });
    }

    public void submitInfo(View view) {

        if (mIsNameSelected) {
            mIsNameSelected = false;
            updateUI();
        } else {

            if (mCustomerName.getText().length() == 0 || mCustomerEmail.getText().length() == 0) {
                Toast.makeText(mContext, "You must enter a name and e-mail address", Toast.LENGTH_SHORT).show();
            } else if (mCustomerName.getText().length() > 0 && mCustomerEmail.getText().length() > 0) {
                Customer customer = new Customer(mCustomerName.getText().toString(), mCustomerEmail.getText().toString());

                // pull the number out of shared preferences, assign it to the customer, then increment it
                SharedPreferences preferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                int ticketNum = preferences.getInt(TICKET_NUM, 0);
                customer.setTicket(ticketNum);

                ticketNum++;
                if (ticketNum >= 99999) {
                    ticketNum = 10000;
                }

                mObject.addCustomer(customer);
                String jsonString = new Gson().toJson(mObject);
                SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
                editor.putString(CONFIG_OBJ, jsonString);
                editor.putInt(TICKET_NUM, ticketNum);
                editor.apply();


                if (mObject.isPrintTickets()) {
                    printTicket(customer);
                    printTicket(customer);
                }


                final AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                        .setView(R.layout.dialog_thank_you);
                dialog.create();
                dialog.setCancelable(false);
                dialog.show();
                Handler handler = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(mContext, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                };
                handler.postDelayed(runnable, 3000);
            }
        }
    }

    private boolean printTicket(Customer customer) {

        String dateString = "Drawing On: " + mDrawingDate;

        int number = customer.getTicket();
        String name = customer.getName();
        String email = customer.getEmail();
        String date = customer.getDate();

        boolean returnValue = true;
        try {

            int iNum = 0;

            byte[] printText = new byte[1024];
            String strTmp = "";

            byte[] oldText = setAlignCenter('2');
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = setBold(false);
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = setWH('2');
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = getGbk(dateString + "\n\n");
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = setWH('4');
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = setBold(true);
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            strTmp = String.valueOf(number);
            oldText = getGbk(strTmp + "\n\n");
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = getGbk(name);
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = getGbk("\n\n");
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = setBold(false);
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = setWH('2');
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = getGbk(email);
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = getGbk("\n\n");
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = getGbk(date);
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = setBold(false);
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = getGbk("\n\n");
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            oldText = CutPaper();
            System.arraycopy(oldText, 0, printText, iNum, oldText.length);
            iNum += oldText.length;

            mOutputStream.write(printText);


        } catch (Exception ex) {
            returnValue = false;
        }
        return returnValue;
    }


    public static byte[] setAlignCenter(char paramChar) {
        byte[] arrayOfByte = new byte[3];
        arrayOfByte[0] = 0x1B;
        arrayOfByte[1] = 0x61;

        switch (paramChar) {
            case '2':
                arrayOfByte[2] = 0x01;
                break;
            case '3':
                arrayOfByte[2] = 0x02;
                break;
            default:
                arrayOfByte[2] = 0x00;
                break;
        }
        return arrayOfByte;
    }

    public static byte[] setWH(char paramChar) {
        byte[] arrayOfByte = new byte[3];
        arrayOfByte[0] = 0x1D;
        arrayOfByte[1] = 0x21;

        switch (paramChar) {
            case '2':
                arrayOfByte[2] = 0x10;
                break;
            case '3':
                arrayOfByte[2] = 0x01;
                break;
            case '4':
                arrayOfByte[2] = 0x11;
                break;
            case '5':
                arrayOfByte[2] = 0x1B;
                break;
            default:
                arrayOfByte[2] = 0x00;
                break;
        }

        return arrayOfByte;
    }

    public static byte[] getGbk(String paramString) {
        byte[] arrayOfByte = null;
        try {
            arrayOfByte = paramString.getBytes("GBK");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return arrayOfByte;

    }

    public static byte[] setBold(boolean paramBoolean) {
        byte[] arrayOfByte = new byte[3];
        arrayOfByte[0] = 0x1B;
        arrayOfByte[1] = 0x45;
        if (paramBoolean) {
            arrayOfByte[2] = 0x01;
        } else {
            arrayOfByte[2] = 0x00;
        }
        return arrayOfByte;
    }

    public static byte[] CutPaper() {
        byte[] arrayOfByte = new byte[]{0x1D, 0x56, 0x42, 0x00};
        return arrayOfByte;
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setActivteListener(final EditText et) {
        et.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int result = event.getAction();
                if (result == MotionEvent.ACTION_DOWN) {
                    if (et.getId() == R.id.et_customer_name) {
                        Log.d(TAG, "here on customer name");
                        mIsNameSelected = true;
                        updateUI();
                    } else if (et.getId() == R.id.et_customer_email) {
                        Log.d(TAG, "here on customer email");
                        mIsNameSelected = false;
                        updateUI();
                    }
                }
                return true;
            }
        });
    }

    public void type(View view) {

        Log.d(TAG, "type method called");

        Button btn = (Button) view;
        String txt = btn.getText().toString();
        char value = txt.charAt(0);

        if (mIsNameSelected) {

            if (mCustomerName.getText().length() > 0) {

                if (txt.equalsIgnoreCase("back")) {

                    char[] text = mCustomerName.getText().toString().toCharArray();
                    char[] temp = Arrays.copyOfRange(text, 0, text.length - 1);
                    mCustomerName.setText(String.valueOf(temp));
                    mCustomerName.setSelection(temp.length);
                }
            }

            if (txt.equalsIgnoreCase("space")) {
                char[] text = mCustomerName.getText().toString().toCharArray();

                Log.d(TAG, "the value of text now : " + String.valueOf(text));

                char[] temp = Arrays.copyOf(text, text.length + 1);

                Log.d(TAG, "the value of temp now: " + String.valueOf(temp));

                temp[temp.length - 1] = ' ';

                Log.d(TAG, "temp to string: " + String.valueOf(temp));

                mCustomerName.setText(String.valueOf(temp));
                mCustomerName.setSelection(temp.length);
            }

            if (!(txt.equalsIgnoreCase("back") || txt.equalsIgnoreCase("space"))) {
                char[] text = mCustomerName.getText().toString().toCharArray();
                char[] temp = Arrays.copyOf(text, text.length + 1);
                temp[temp.length - 1] = value;
                mCustomerName.setText(String.valueOf(temp));
                mCustomerName.setSelection(temp.length);
            }

        } else {
            Log.d(TAG, "the length of the et field is : " + mCustomerName.getText().length());

            if (mCustomerEmail.getText().length() > 0) {

                Log.d(TAG, "the text is longer than 0");

                if (txt.equalsIgnoreCase("back")) {

                    Log.d(TAG, "the text = back ");

                    char[] text = mCustomerEmail.getText().toString().toCharArray();
                    char[] temp = Arrays.copyOfRange(text, 0, text.length - 1);
                    mCustomerEmail.setText(String.valueOf(temp));
                    mCustomerEmail.setSelection(temp.length);
                }
            }

            if (txt.equalsIgnoreCase("space")) {
                char[] text = mCustomerEmail.getText().toString().toCharArray();

                Log.d(TAG, "the value of text now : " + String.valueOf(text));

                char[] temp = Arrays.copyOf(text, text.length + 1);

                Log.d(TAG, "the value of temp now: " + String.valueOf(temp));

                temp[temp.length - 1] = ' ';

                Log.d(TAG, "temp to string: " + String.valueOf(temp));

                mCustomerEmail.setText(String.valueOf(temp));

                mCustomerEmail.setSelection(temp.length);
            }

            if (!(txt.equalsIgnoreCase("back") || txt.equalsIgnoreCase("space"))) {
                char[] text = mCustomerEmail.getText().toString().toCharArray();
                char[] temp = Arrays.copyOf(text, text.length + 1);
                temp[temp.length - 1] = value;
                mCustomerEmail.setText(String.valueOf(temp));
                mCustomerEmail.setSelection(temp.length);
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

}
