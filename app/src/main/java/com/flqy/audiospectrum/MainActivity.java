package com.flqy.audiospectrum;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.flqy.audiospectrum.media.MediaRecorderEngine;
import com.flqy.audiospectrum.media.RecorderEngine;
import com.flqy.library.widget.SpectrumView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private MediaRecorderEngine recorderEngine;
    private Handler handler = new Handler();
    private SpectrumView vl;
    private SpectrumView vr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recorderEngine = new MediaRecorderEngine(this);
        final TextView time = (TextView) findViewById(R.id.time);
        final View recordState = findViewById(R.id.recordState);
        vl = (SpectrumView) findViewById(R.id.spectrumViewLeft);
        vr = (SpectrumView) findViewById(R.id.spectrumViewRight);
        recorderEngine.setSoundAmplitudeListener(new RecorderEngine.SoundAmplitudeListener() {
            @Override
            public void amplitude(int amplitude, int db, int value) {
                vl.updateForward(value);
                vr.updateBackward(value);
            }
        });

        View startRecord = findViewById(R.id.startRecord);
        final File path = new File(getCacheDir(), "test.amr");
        recorderEngine.setRecorderListener(new RecorderEngine.RecorderEngineListener() {
            @Override
            public void onRecorderStart() {
                time.setText("00:00");
                recordState.setVisibility(View.VISIBLE);
            }

            @Override
            public void onRecorderStop() {
                recordState.setVisibility(View.INVISIBLE);
                time.setText("00:00");
                //topAnimSmoothly();
                vl.reset();
                vr.reset();
            }

            @Override
            public void onRecorderPrepared() {

            }

            @Override
            public void onRecorderProgress(int seconds) {
                time.setText(generateText(seconds));
            }

            @Override
            public void onRecorderStreamError() {
                recordState.setVisibility(View.INVISIBLE);
                time.setText("00:00");
                //stopAnimSmoothly();
                vl.reset();
                vr.reset();
            }
        });
        startRecord.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, final MotionEvent event) {
                onRecordTouched(event);
                return false;
            }

            private void onRecordTouched(MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        recorderEngine.startRecord(path.getPath());
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        break;
                    case MotionEvent.ACTION_UP:
                        recorderEngine.stopRecord();
                        break;
                    case MotionEvent.ACTION_MOVE:

                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE:
                        recorderEngine.stopRecord();
                        break;
                }
            }
        });
    }

    private int times;

    private void stopAnimSmoothly() {
        times = Math.max(vl.getItemCount(), vr.getItemCount());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                vl.updateForward(0);
                vr.updateBackward(0);
                times--;
                if (times > 0) {
                    handler.postDelayed(this, 200);
                }
            }
        }, 200);
    }

    private String generateText(long seconds) {
        int percent = (int) (seconds / 60);
        int second = (int) (seconds % 60);
        return (percent < 10 ? ("0" + percent) : percent) + ":" + (second < 10 ? ("0" + second) : second);
    }

}
