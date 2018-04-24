package com.example.feco.servocontrol;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    private Button btnJobb;
    private Button btnBal;
    private Button btnFel;
    private Button btnLe;
    private View mDecorView;
    private WebView webview;
    private int servoHorizontPozicio;
    private int servoVerticalPozicio;
    private String rpiUrl = "http://muustar.asuscomm.com";
    private Vibrator vibe;
    private ProgressBar progressBarHorizontal;
    private ProgressBar progressBarVertical;
    private String summary;
    private String summary2;
    private ImageView imageHiba;
    private RelativeLayout rlCountDown;
    private Boolean vezerelheto = true;
    private Boolean vezerlesFolyamatban = false;
    private ProgressBar prgCountDown;
    private TextView prgText;
    private WebView webviewUsbCamera;
    private SensorManager mSensorManager;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity
    private CountDownTimer countDownTimer;
    private long startTime;
    private long remainingTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewElemekInicializalasa();
        hideSystemUI();
        sensorBeallitas();
        elsoCameraInicializalasa();
        masodikCameraInicializalas();
        progressBarInicializalas();


    }

    private void viewElemekInicializalasa() {
        //gombok ----------------------
        btnJobb = (Button) findViewById(R.id.jobbGomb);
        btnBal = (Button) findViewById(R.id.balGomb);
        btnFel = (Button) findViewById(R.id.felGomb);
        btnLe = (Button) findViewById(R.id.leGomb);
        btnBal.setOnTouchListener(this);
        btnJobb.setOnTouchListener(this);
        btnFel.setOnTouchListener(this);
        btnLe.setOnTouchListener(this);

        // ------------ Grumpy Cat kép --------------------
        imageHiba = (ImageView) findViewById(R.id.imageHiba);

        //---------------- visszaszámláló
        rlCountDown = (RelativeLayout) findViewById(R.id.relativeLayoutCountDown);
        prgCountDown = (ProgressBar) findViewById(R.id.progressBarCountDown);
        prgText = (TextView) findViewById(R.id.textProgress);

        //--- vibrátor-------
        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    }

    private void sensorBeallitas() {
        /* do this in onCreate */
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent se) {
            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta; // perform low-cut filter

            if (mAccel > 12) {
                //Toast toast = Toast.makeText(getApplicationContext(), "Device has shaken.", Toast.LENGTH_LONG);
                //toast.show();
                Log.i("FECO", "shake");
                webviewUsbCamera.setVisibility(View.VISIBLE);
                webviewUsbCamera.loadData(summary2, "text/html", null);
                webviewUsbCamera.setAlpha(0f);
                webviewUsbCamera.animate().alpha(1f).setDuration(3000);

                mAccel = 0;
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    private void hideSystemUI() {
        mDecorView = getWindow().getDecorView();
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void showSystemUI() {
        // This snippet shows the system bars. It does this by removing all the flags
        // except for the ones that make the content appear under the system bars.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        if (webview != null) {
            webview.loadData(summary, "text/html", null);
        }
        if (foglaltE() != 0) {
            vezerelheto = false;
        } else {
            vezerelheto = true;
        }

        //Toast.makeText(getApplicationContext(), "visszaállás", Toast.LENGTH_LONG).show();

        hideSystemUI();
    }


    private void elsoCameraInicializalasa() {

        //printSecreenInfo();
        // screen size
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        int htmlImgWidth = screenWidth;
        int htmlImgHeight = screenHeight;

        webview = (WebView) findViewById(R.id.webview);
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) webview.getLayoutParams();
        // 1280x720 => 1,77
        float ratio = 1280f / 720f;

        if (screenWidth < 1280) {
            int kepSzelesseg = screenWidth - 30 - 30;
            int kepMagassag = Math.round(kepSzelesseg / ratio);
            //Log.i("FECO", "kepSzelesseg: " + kepSzelesseg);
            //Log.i("FECO", "kepMagassag: " + kepMagassag);
            p.leftMargin = 30;
            p.rightMargin = 30;
            p.topMargin = Math.round((screenHeight - kepMagassag) / 2);
            p.bottomMargin = Math.round((screenHeight - kepMagassag) / 2);
        } else {
            //htmlImgWidth = 1000;
            //htmlImgHeight = 600;
            //ratio = 1280f / 670f; // tablet
            //ratio = 1280f / 790f; // LG G5

            int kepSzelesseg = Math.round(screenWidth - 120f - 60f);
            int kepMagassag = Math.round(kepSzelesseg / ratio);
            p.leftMargin = (screenWidth - kepSzelesseg) / 2;
            p.rightMargin = (screenWidth - kepSzelesseg) / 2;
            p.topMargin = Math.round((screenHeight - kepMagassag) / 2);
            p.bottomMargin = Math.round((screenHeight - kepMagassag) / 2);
        }

        webview.setLayoutParams(p);

        summary = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"screenWidth=device-screenHeight, initial-scale=1\"><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\"><script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script><script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script></head><body><div class=\"\"><img src=\"http://muustar.asuscomm.com:48461\" class=\"img-responsive\" alt=\"Cinque Terre\" screenWidth=\"" + htmlImgWidth + "\" screenHeight=\"" + htmlImgHeight + "\"></div></body></html>";
        webview.setWebChromeClient(new WebChromeClient());
        webview.loadData(summary, "text/html", null);
        //webview.loadUrl("http://muustar.asuscomm.com/vid.html");

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setAllowContentAccess(true);

        webview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                servoHorizontPozicio = 6;
                new Background_get().execute("posh=" + servoHorizontPozicio);
                servoVerticalPozicio = 5;
                new Background_get().execute("posv=" + servoVerticalPozicio);
                progressBarHorizontal.setProgress(100 - ((servoHorizontPozicio - 2) * 10));
                progressBarVertical.setProgress((servoVerticalPozicio - 2) * 10);
                return true;
            }
        });
    }

    private void masodikCameraInicializalas() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int htmlImgWidth = size.x;
        int htmlImgHeight = size.y;
        //Log.i("FECO", "w" + screenWidth + " x h" + screenHeight + " :  " + ((float) screenWidth / (float) screenHeight));
        webviewUsbCamera = (WebView) findViewById(R.id.webviewUsbCamera);
        //webviewUsbCamera.setVisibility(View.VISIBLE);
        summary2 = "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"screenWidth=device-screenHeight, initial-scale=1\"><link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\"><script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script><script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script></head><body><div class=\"\"><img src=\"http://muustar.asuscomm.com:48462\" class=\"img-responsive\" alt=\"Cinque Terre\" screenWidth=\"" + htmlImgWidth + "\" screenHeight=\"" + htmlImgHeight + "\"></div></body></html>";
        webviewUsbCamera.setWebChromeClient(new WebChromeClient());
        WebSettings webSettings2 = webviewUsbCamera.getSettings();
        webSettings2.setJavaScriptEnabled(true);
        webSettings2.setAppCacheEnabled(true);
        webSettings2.setLoadsImagesAutomatically(true);
        webSettings2.setAllowContentAccess(true);
        webviewUsbCamera.loadData(summary2, "text/html", null);

        webviewUsbCamera.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                webviewUsbCamera.animate().alpha(0f).setDuration(3000);
                new CountDownTimer(3100, 1000) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        webviewUsbCamera.stopLoading();
                    }
                }.start();
                //webviewUsbCamera.setVisibility(View.INVISIBLE);

                return true;
            }
        });

    }

    private void progressBarInicializalas() {
        progressBarHorizontal = (ProgressBar) findViewById(R.id.progressBar);
        progressBarVertical = (ProgressBar) findViewById(R.id.progressBarV);
        servoHorizontPozicio = pozicioLekerdezes("h");
        servoVerticalPozicio = pozicioLekerdezes("v");
        progressBarHorizontal.setProgress(100 - ((servoHorizontPozicio - 2) * 10));
        progressBarVertical.setProgress((servoVerticalPozicio - 2) * 10);
    }

    private long foglaltE() {
        DownloadTask foglalte = new DownloadTask();
        String foglaltErtek = "";
        long ertek;
        try {
            foglaltErtek = foglalte.execute(rpiUrl + "/foglaltsag").get();
            foglaltErtek = foglaltErtek.trim();
            ertek = Long.parseLong(foglaltErtek);
            //Log.i("FECO",""+foglaltErtek);
            return ertek;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // false = ha nem foglalt akkor vezérelhető
        return -1;
    }

    private int pozicioLekerdezes(String irany) {
        int resultPos = -1;
        if (irany == "h" || irany == "v") {
            DownloadTask position = new DownloadTask();
            String positionResult = "";
            String poziciok = null;
            try {
                if (irany == "h") {
                    positionResult = position.execute(rpiUrl + "/positionh").get();
                } else if (irany == "v") {
                    positionResult = position.execute(rpiUrl + "/positionv").get();
                }
                poziciok = positionResult.trim();
                //Toast.makeText(getApplicationContext(), poziciok, Toast.LENGTH_LONG).show();
                resultPos = Integer.parseInt(poziciok.toString());
                imageHiba.setVisibility(View.INVISIBLE);
            } catch (InterruptedException e) {
                Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (ExecutionException e) {
                Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (RuntimeException e) {
                imageHiba.setImageResource(R.drawable.grumpyhiba);
                imageHiba.setVisibility(View.VISIBLE);
                //Toast.makeText(getApplicationContext(), "Probléma a kapcsolattal", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }


        }
        return resultPos;

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        long beolvasottIdo = 0;
        boolean pressed = true;
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (!vezerlesFolyamatban) {
                beolvasottIdo = foglaltE();
                Log.i("FECO", "beolvasottIdo "+beolvasottIdo);
                if (beolvasottIdo == 0)
                    vezerelheto = true;
            } else {
                remainingTime = (long) System.currentTimeMillis() - (long) beolvasottIdo;
                Log.i("FECO","remainingTime: "+ System.currentTimeMillis()+" "+remainingTime);
                vezerelheto = false;
            }
            if (vezerelheto) {
                startTimer(20, prgCountDown, prgText);
            } else {
                //startTimer(10, prgCountDown, prgText);
            }
            if (vezerlesFolyamatban) {

                //long[] pattern = { 0, 100,50,100};
                //vibe.vibrate(pattern, -1);
                vibe.vibrate(100);

                if (pressed) {
                    if (view.getTag().equals("bal")) {
                        //Log.i("FECO", "bal gomb");
                        if (servoHorizontPozicio < 12) {
                            view.setAlpha(1f);
                            servoHorizontPozicio = pozicioLekerdezes("h");
                            int ujPozicio = servoHorizontPozicio;
                            // <---- bal
                            ujPozicio++;
                            new Background_get().execute("posh=" + ujPozicio);
                            servoHorizontPozicio = ujPozicio;

                        }

                    } else if (view.getTag().equals("jobb")) {

                        if (servoHorizontPozicio > 2) {
                            view.setAlpha(1f);
                            servoHorizontPozicio = pozicioLekerdezes("h");
                            int ujPozicio = servoHorizontPozicio;

                            // ----> jobb
                            ujPozicio--;
                            //Log.i("FECO", "jobb gomb");
                            new Background_get().execute("posh=" + ujPozicio);
                            servoHorizontPozicio = ujPozicio;

                        }
                    } else if (view.getTag().equals("fel")) {
                        //Log.i("FECO", "servo: " + servoVerticalPozicio);

                        if (servoVerticalPozicio < 12) {
                            view.setAlpha(1f);
                            servoVerticalPozicio = pozicioLekerdezes("v");
                            int ujPozicio = servoVerticalPozicio;

                            // FEL
                            ujPozicio++;
                            //Log.i("FECO", "fel gomb");
                            new Background_get().execute("posv=" + ujPozicio);
                            servoVerticalPozicio = ujPozicio;
                        }
                    } else if (view.getTag().equals("le")) {
                        //Log.i("FECO", "(le)");

                        if (servoVerticalPozicio > 2) {
                            view.setAlpha(1f);
                            servoVerticalPozicio = pozicioLekerdezes("v");
                            int ujPozicio = servoVerticalPozicio;

                            // LE
                            ujPozicio--;
                            //Log.i("FECO", "le gomb");
                            new Background_get().execute("posv=" + ujPozicio);
                            servoVerticalPozicio = ujPozicio;

                        }
                    }
                    progressBarHorizontal.setProgress(100 - ((servoHorizontPozicio - 2) * 10));
                    progressBarVertical.setProgress((servoVerticalPozicio - 2) * 10);
                    pressed = false;
                    //Toast.makeText(getApplicationContext(),""+view.getTag(),Toast.LENGTH_LONG).show();
                    view.setEnabled(false);

                }
            }
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            //view.setAlpha(0.1f);
            pressed = true;
        }

        return true;
    }


    private class Background_get extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                /* Change the IP to the IP you set in the arduino sketch */
                URL url = new URL(rpiUrl + "?" + params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    result.append(inputLine).append("\n");

                in.close();
                connection.disconnect();
                return result.toString();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            View view;
            view = (Button) findViewById(R.id.balGomb);
            view.setEnabled(true);
            view.setAlpha(0f);
            view = (Button) findViewById(R.id.jobbGomb);
            view.setEnabled(true);
            view.setAlpha(0f);
            view = (Button) findViewById(R.id.felGomb);
            view.setEnabled(true);
            view.setAlpha(0f);
            view = (Button) findViewById(R.id.leGomb);
            view.setEnabled(true);
            view.setAlpha(0f);

        }


    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(strings[0]); //kézbe vettük az url címet
                urlConnection = (HttpURLConnection) url.openConnection(); //megnyitottunk egy kapcsolatot a címhez
                //letöltjük a címen lévő HTML adatokat
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return "Failed";
            }

        }
    }


    private void printSecreenInfo() {

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        Log.i("FECO", "density :" + metrics.density);
        Log.i("FECO", "scaledDensity :" + metrics.scaledDensity);

        // density interms of dpi
        Log.i("FECO", "D density :" + metrics.densityDpi);

        // horizontal pixel resolution
        Log.i("FECO", "width pix :" + metrics.widthPixels);

        // vertical pixel resolution
        Log.i("FECO", "height pix :" + metrics.heightPixels);

        // actual horizontal dpi
        Log.i("FECO", "xdpi :" + metrics.xdpi);

        // actual vertical dpi
        Log.i("FECO", "ydpi :" + metrics.ydpi + "\n---------------------------");

    }

    private void startTimer(final int sec, final ProgressBar progressBar, final TextView progressTv) {
        //Log.i("FECO", "timer fut");
        vezerelheto = false;
        vezerlesFolyamatban = true;
        startTime = System.currentTimeMillis();
        //Log.i("FECO", "idő: " + startTime);
        new Background_get().execute("foglaltsag=" + startTime);
        rlCountDown.setAlpha(1);
        countDownTimer = new CountDownTimer(sec * 1000 + 100, 500) {
            // 500 means, onTick function will be called at every 500 milliseconds
            final float egyseg = 100f / sec;

            @Override
            public void onTick(long l) {
                if (vezerlesFolyamatban) {
                    long seconds = l / 1000;
                    progressBar.setProgress((int) (seconds * egyseg));
                    //Log.i("FECO", "prg: " + (int) (seconds * egyseg));
                    progressTv.setText(String.format("%02d", seconds % 60));
                    // format the textview to show the easily readable format
                } else {

                    Log.i("FECO", "cancel");
                }
            }

            @Override
            public void onFinish() {
                //Toast.makeText(getApplicationContext(), "visszaszámlálás kész", Toast.LENGTH_LONG).show();
                vezerelheto = true;
                vezerlesFolyamatban = false;
                rlCountDown.setAlpha(0);
                new Background_get().execute("foglaltsag=0");
            }
        }.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (vezerlesFolyamatban) {

            vezerlesFolyamatban = false;
            new Background_get().execute("foglaltsag=0");
            countDownTimer.cancel();
            rlCountDown.setAlpha(0);
        }

    }
}
