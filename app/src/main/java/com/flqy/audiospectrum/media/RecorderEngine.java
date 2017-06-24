package com.flqy.audiospectrum.media;

public interface RecorderEngine {
    /**
     * 开始录音
     *
     * @param path
     */
    public void startRecord(String path);

    /**
     * 暂停录音
     */
    public void pause();

    /**
     * 停止录音
     */
    public void stop();

    /**
     * 录音最大时常
     *
     * @param seconds
     */
    public void setDuring(int seconds);

    public int getDuring();

    public void setRecorderListener(RecorderEngineListener recorderEngineListener);

    public void setSoundAmplitudeListener(SoundAmplitudeListener soundAmplitudeListener);

    public interface RecorderEngineListener {
        public void onRecorderStart();

        public void onRecorderStop();

        public void onRecorderPrepared();

        public void onRecorderProgress(int seconds);

        public void onRecorderStreamError();
    }

    public interface SoundAmplitudeListener {
        public void amplitude(int ratio, int db, int value);
    }

}
