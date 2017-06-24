package com.flqy.audiospectrum.media;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

/**
 * RecorderEngineBase提供统一的录音计时，计时器和录音过程分离
 */
public abstract class RecorderEngineBase implements RecorderEngine {

    protected RecordingTracker recordingTracker;
    private int maxSeconds = 60;

    public RecorderEngineBase() {
        recordingTracker = new RecordingTracker(null);
    }

    public class RecordingTracker implements RecorderEngineListener {

        private RecorderEngineListener recorderEngineListener;
        private TrackerThread thread;
        private boolean timeOver = false;

        public RecordingTracker(RecorderEngineListener recorderEngineListener) {
            super();
            this.recorderEngineListener = recorderEngineListener;
        }

        @SuppressLint("HandlerLeak")
        private Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        int seconds = (Integer) msg.obj;
                        onRecorderProgress(seconds);
                        break;
                    case 2:
                        stop();
                        // stopTrack();
                    default:
                        break;
                }
            }
        };

        /**
         * 计时器和录音过程分离
         *
         * @author ludq@hyxt.com
         */
        public class TrackerThread extends Thread {

            public void stopTrack() {
                interrupt();
            }

            ;

            public void run() {
                long millis = 0;
                int lastSentTrackSecond = 0;
                timeOver = false;
                while (!isInterrupted() && (millis = System.currentTimeMillis() - startTime) < getDuring() * 1000) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                        break;
                    }
                    tracedMillis = millis;
                    int trackedSeconds = (int) millis / 1000;
                    if (trackedSeconds - lastSentTrackSecond >= 1) {
                        lastSentTrackSecond = trackedSeconds;
                        handler.obtainMessage(1, trackedSeconds).sendToTarget();
                    }
                }
                if (millis > getDuring() * 1000) {
                    timeOver = true;
                }
                tracedMillis = System.currentTimeMillis() - startTime;
                handler.obtainMessage(2).sendToTarget();
            }
        }

        @Override
        public void onRecorderStart() {
            startTracking();
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderStart();
        }

        private long tracedMillis;
        private long startTime;

        private void startTracking() {
            if (thread != null) {
                stopTrack();
            }
            startTime = System.currentTimeMillis();
            thread = new TrackerThread();
            thread.start();
        }

        @Override
        public void onRecorderStop() {
            stopTrack();
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderStop();
        }

        private void stopTrack() {
            if (thread != null) {
                thread.stopTrack();
                thread = null;
            }
        }

        public int getTrackedSeconds() {
            return (int) (tracedMillis / 1000);
        }

        public long getTracedMillis() {
            return tracedMillis;
        }

        @Override
        public void onRecorderPrepared() {
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderPrepared();
        }

        @Override
        public void onRecorderProgress(int seconds) {
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderProgress(seconds);
        }

        @Override
        public void onRecorderStreamError() {
            if (recorderEngineListener != null)
                recorderEngineListener.onRecorderStreamError();
            stopTrack();
        }

        public boolean isTimeOver() {
            return timeOver;
        }

        ;

    }

    public int getTrackedSeconds() {
        return recordingTracker.getTrackedSeconds();
    }

    public long getTracedMillis() {
        return recordingTracker.getTracedMillis();
    }

    public boolean isTimeOver() {
        return recordingTracker.isTimeOver();
    }

    @Override
    public final void setDuring(int seconds) {
        maxSeconds = seconds;
    }

    @Override
    public final int getDuring() {
        return maxSeconds;
    }

    public final void setRecorderListener(RecorderEngineListener recorderEngineListener) {
        this.recordingTracker = new RecordingTracker(recorderEngineListener);
    }

}
