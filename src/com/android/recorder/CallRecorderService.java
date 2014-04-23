/*
 * Copyright (C) 2014, The Linux Foundation. All rights reserved.
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

package com.android.recorder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class CallRecorderService {

    private static final boolean DBG = false;
    private static final String CALL_RECORDER_ACTION = "com.android.action.CALL_RECORD";
    private static final String DBG_CALL_RECORD = "CallRecorder";

    private static CallRecorderService mRecorderService;
    private ICallRecorder mCallRecorder;

    public static CallRecorderService getInstance() {
        if (mRecorderService == null) {
            mRecorderService = new CallRecorderService();
        }

        return mRecorderService;
    }

    public void init(Context context) {

        bindRecorderService(context);
    }

    private void bindRecorderService(Context context) {
        if (mCallRecorder == null) {
            final Intent intent = new Intent(CALL_RECORDER_ACTION);
            try {
                context.bindService(intent, connection, context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                logd(" bind call recorder failed : " + e);
            }
        }
    }

    private void unbindRecorderService(Context context) {

        if (mCallRecorder != null) {
            try {
                context.unbindService(connection);
            } catch (Exception e) {
                logd(" unbind call recorder failed : " + e);
            }
        }
    }

    protected ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCallRecorder = ICallRecorder.Stub.asInterface(service);
            logd("bind call record service:" + mCallRecorder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logd("call record service is unbind");
            mCallRecorder = null;
        }
    };

    public boolean isInCallRecorderReady() {
        if (mCallRecorder != null) {
            try {
                return mCallRecorder.isEnabled();
            } catch (RemoteException e) {
                mCallRecorder = null;

                logd("Call recorder not ready, error:" + e);
            }
        }

        return false;
    }

    public boolean isInCallRecording() {
        if (mCallRecorder != null) {
            try {
                return mCallRecorder.isRecording();
            } catch (RemoteException e) {
                mCallRecorder = null;

                logd("get recorder status error:" + e);
            }
        }

        return false;
    }

    public void startInCallRecorder() {
        if (mCallRecorder != null) {
            try {
                mCallRecorder.startInCallRecorder();
            } catch (RemoteException e) {
                mCallRecorder = null;

                logd("start recorder error:" + e);
            }
        }
    }

    public void stopInCallRecorder() {
        if (mCallRecorder != null) {
            try {
                mCallRecorder.stopInCallRecorder();
            } catch (RemoteException e) {
                mCallRecorder = null;

                logd("start recorder error:" + e);
            }
        }
    }

    public void logd(String msg) {
        if (DBG) {
            Log.d(DBG_CALL_RECORD, msg);
        }
    }
}
