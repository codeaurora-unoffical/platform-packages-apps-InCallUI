/* Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.android.incallui;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.telecom.AudioState;

public class VideoCallTonePlayer {
    // Buffer time (in msec) to add on to the tone timeout value. Needed mainly when the timeout
    // value for a tone is exact duration of the tone itself.
    private static final int TIMEOUT_BUFFER_MILLIS = 20;
    private static final int RELATIVE_VOLUME_HIPRI = 80;

    public void playUpgradeToVideoRequestTone() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                ToneGenerator toneGenerator = null;
                try {
                    // Similar to the call waiting tone, but does not repeat.
                    final int toneType = ToneGenerator.TONE_SUP_CALL_WAITING;
                    final int toneVolume = RELATIVE_VOLUME_HIPRI;
                    final int toneLengthMillis = 4000;

                    int currentAudioMode = AudioModeProvider.getInstance().getAudioMode();
                    int stream = AudioManager.STREAM_VOICE_CALL;
                    if (isAudioRouteEnabled(currentAudioMode,
                            AudioState.ROUTE_BLUETOOTH)) {
                        stream = AudioManager.STREAM_BLUETOOTH_SCO;
                    }

                    Log.d(this, "playUpgradeToVideoRequestTone currentAudioMode: "
                            + currentAudioMode);

                    try {
                        Log.v(this, "Creating generator");
                        toneGenerator = new ToneGenerator(stream, toneVolume);
                    } catch (RuntimeException e) {
                        Log.w(this, "Failed to create ToneGenerator.");
                        return;
                    }

                    synchronized (this) {
                        toneGenerator.startTone(toneType);

                        try {
                            Log.v(this, "Starting tone ...waiting for %d ms.",
                                toneLengthMillis + TIMEOUT_BUFFER_MILLIS);
                            wait(toneLengthMillis + TIMEOUT_BUFFER_MILLIS);
                        } catch (InterruptedException e) {
                            Log.w(this, "wait interrupted");
                        }
                    }
                } finally {
                    if (toneGenerator != null) {
                        toneGenerator.release();
                    }
                }
            }
        };
        thread.start();
    }

    private boolean isAudioRouteEnabled(int audioRoute, int audioRouteMask) {
        return ((audioRoute & audioRouteMask) != 0);
    }
}
