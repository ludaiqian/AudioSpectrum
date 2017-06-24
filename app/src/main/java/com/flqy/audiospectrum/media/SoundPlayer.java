/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flqy.audiospectrum.media;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;

import java.io.IOException;

/**
 * Plays an AssetFileDescriptor, but does all the hard work on another thread so
 * that any slowness with preparing or loading doesn't block the calling thread.
 */
public class SoundPlayer implements Runnable {
	private static final String TAG = "SoundPlayer";
	private Thread mThread;
	private MediaPlayer mPlayer;
	private int mPlayCount = 0;
	private boolean mExit;
	private AssetFileDescriptor mAfd;
	private int mAudioStreamType;
	private String path;
	private Handler handler = new Handler();
	private boolean isPlayEnd = false;

	@Override
	public void run() {
		while (true) {
			try {
				if (mPlayer == null) {
					MediaPlayer player = new MediaPlayer();
					player.setAudioStreamType(mAudioStreamType);
					if (mAfd != null)
						player.setDataSource(mAfd.getFileDescriptor(), mAfd.getStartOffset(), mAfd.getLength());
					else if (path != null)
						player.setDataSource(path);
					player.setLooping(false);
					player.prepare();
					mPlayer = player;
					if (mAfd != null) {
						mAfd.close();
						mAfd = null;
					}
				}
				synchronized (this) {
					while (true) {
						if (mExit) {
							return;
						} else if (mPlayCount <= 0) {
							wait();
						} else {
							mPlayCount--;
							break;
						}
					}
				}
				mPlayer.setOnCompletionListener(new OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer arg0) {
						handler.post(new Runnable() {

							@Override
							public void run() {
								isPlayEnd = true;
								if (onPlayEndListener != null)
									onPlayEndListener.onPlayEnd();
							}
						});

					}
				});
				mPlayer.start();

			} catch (Exception e) {
				e.printStackTrace();
//				Log.e(TAG, "Error playing sound", e);
				if (mPlayer != null) {
					mPlayer.release();
					mPlayer = null;
				}
				break;
			}
		}
	}

	public SoundPlayer(String path) {
		super();
		this.path = path;
		mAudioStreamType = AudioManager.STREAM_MUSIC;
	}

	public String getSource() {
		return path;
	}

	public SoundPlayer(AssetFileDescriptor afd) {
		mAfd = afd;
		mAudioStreamType = AudioManager.STREAM_MUSIC;
	}

	public SoundPlayer(AssetFileDescriptor afd, boolean enforceAudible) {
		mAfd = afd;
		if (enforceAudible) {
			mAudioStreamType = 7; // AudioManager.STREAM_SYSTEM_ENFORCED;
									// currently hidden API.
		} else {
			mAudioStreamType = AudioManager.STREAM_MUSIC;
		}
	}

	public void play() {
		isPlayEnd = false;
		if (mThread == null) {
			mThread = new Thread(this);
			mThread.start();
		}
		synchronized (this) {
			mPlayCount++;
			notifyAll();
		}
	}

	public void toggle() {
		if (getDuration() > 0) {
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
			} else {
				mPlayer.start();
			}
		}
	}

	public boolean isPlayEnd() {
		return isPlayEnd;
	}

	public int getDuration() {
		try {
			if (mPlayer != null)
				return mPlayer.getDuration();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public int getCurrent() {
		try {
			if (mPlayer != null)
				return mPlayer.getCurrentPosition();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public void release() {
		if (mThread != null) {
			synchronized (this) {
				mExit = true;
				notifyAll();
			}
			mThread.interrupt();
			mThread = null;
		}
		if (mAfd != null) {
			try {
				mAfd.close();
			} catch (IOException e) {
			}
			mAfd = null;
		}
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	private OnPlayEndListener onPlayEndListener;

	public OnPlayEndListener getOnPlayEndListener() {
		return onPlayEndListener;
	}

	public void setOnPlayEndListener(OnPlayEndListener onPlayEndListener) {
		this.onPlayEndListener = onPlayEndListener;
	}

	public interface OnPlayEndListener {
		public void onPlayEnd();
	}

	public boolean isPlaying() {
		try {
			return mPlayer != null && mPlayer.isPlaying();
		} catch (Exception e) {
			return false;
		}
	}
}
