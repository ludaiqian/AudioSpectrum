package com.flqy.audiospectrum.media;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OutputFormat;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;

import com.flqy.library.R;

public class MediaRecorderEngine extends RecorderEngineBase {
    private MediaRecorder mr;
    private boolean isRecordError; // 是否录制出错
    private Context context;
    private String filePath;
    private boolean recording = false;
    private int outputFormat;
    /**
     * 分贝的计算公式K=20lg(Vo/Vi) Vo当前的振幅值,Vi基准值为600
     */
    private static final int BASE = 600;
    private static final int RATIO = 5;
    private static final int REFRESH_LIMIT = 200;
    private SoundAmplitudeListener soundAmplitudeListener;//声波振幅监听器
    private final Handler handler = new Handler();
    private Runnable updateMicStatusTask = new Runnable() {


        @Override
        public void run() {
            int ratio = getMaxAmplitude() / BASE;
            int db = (int) (20 * Math.log10(Math.abs(ratio)));
            int value = db / RATIO;
            if (value < 0) value = 0;
            if (soundAmplitudeListener != null) {
                soundAmplitudeListener.amplitude(ratio, db, value);
                handler.postDelayed(updateMicStatusTask, REFRESH_LIMIT);
            }
        }
    };

    public MediaRecorderEngine(Context context) {
        this(context, OutputFormat.AMR_NB);

    }

    public MediaRecorderEngine(Context context, int outputFormat) {
        super();
        this.context = context;
        this.outputFormat = outputFormat;
    }


    public void stopRecord() {
        try {
            if (!isRecordError) {
                if (mr != null) {
                    mr.setOnErrorListener(null);
                    try {
                        mr.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mr.release();
                    mr = null;
                    recording = false;
                    recordingTracker.onRecorderStop();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            handler.removeCallbacks(updateMicStatusTask);
        }

    }



    public boolean isRecordError() {
        return isRecordError;
    }

    public int getMaxAmplitude() {
        return mr != null ? mr.getMaxAmplitude() : 0;
    }

    public int getAudioSourceMax() {
        return MediaRecorder.getAudioSourceMax();
    }

    public boolean isRecording() {
        return recording;
    }

    public String getOutputPath() {
        return filePath;
    }

    @SuppressLint("InlinedApi")
    @Override
    public void startRecord(String path) {
        this.filePath = path;

        // 创建录音对象
        mr = new MediaRecorder();

        // 从麦克风源进行录音
        mr.setAudioSource(MediaRecorder.AudioSource.MIC);
//		mr.setAudioSamplingRate(44100);
//		mr.setAudioChannels(2);
        // 设置输出格式
        mr.setOutputFormat(outputFormat);
        if (outputFormat == OutputFormat.AAC_ADTS) {
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        } else {
            // 设置编码格式
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }

        // 设置输出文件
        mr.setOutputFile(path);
        try {
            // 准备录制
            mr.prepare();
            recordingTracker.onRecorderPrepared();
            // 震动
            Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
            vibrator.vibrate(501);
            // 开始录制
            mr.start();
            recordingTracker.onRecorderStart();
            recording = true;
            handler.post(updateMicStatusTask);
        } catch (Exception e) {
            e.printStackTrace();
            isRecordError = true;
            recording = false;
            try {
                mr.stop();
            } catch (Throwable e1) {
                e1.printStackTrace();
            }
            try {
                mr.release();
            } catch (Throwable e1) {
                e1.printStackTrace();
            }
            Toast.makeText(context, R.string.record_fail, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void setSoundAmplitudeListener(SoundAmplitudeListener soundAmplitudeListener) {
        this.soundAmplitudeListener = soundAmplitudeListener;
    }


    @Override
    public void pause() {

    }

    @Override
    public void stop() {
        stopRecord();
    }
}
