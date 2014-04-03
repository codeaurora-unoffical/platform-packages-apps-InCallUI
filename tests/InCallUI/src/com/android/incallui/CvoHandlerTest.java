/* Copyright (c) 2014, The Linux Foundation. All rights reserved.
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

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.test.AndroidTestCase;

import com.android.incallui.CvoHandler;

public class CvoHandlerTest extends AndroidTestCase {

    private static final int CVO_INFO_CHANGED = 2;

    private CvoHandler mCvoHandler;
    private boolean mCvoInfoChangedReceived = false;
    private int mOrientation = 0;
    private static Object mMonitor = new Object();

    private Handler mTestHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case CVO_INFO_CHANGED:
                    mCvoInfoChangedReceived = true;
                    ar = (AsyncResult) msg.obj;
                    if (ar != null && ar.result != null && ar.exception == null) {
                        mOrientation = (Integer) ar.result;
                    }
                    synchronized(mMonitor) {
                        mMonitor.notifyAll();
                    }
                    break;
            }
        }
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCvoHandler = new CvoHandler(getContext());
    }

    public void testNotifyCvoClients()  throws Exception {
        mCvoHandler.registerForCvoInfoChange(mTestHandler, CVO_INFO_CHANGED, null);

        mCvoHandler.doOnOrientationChanged(225);

        while (mCvoInfoChangedReceived == false) {
            synchronized(mMonitor) {
                mMonitor.wait();
            }
        }

        assertTrue(mCvoInfoChangedReceived);
        assertEquals(CvoHandler.ORIENTATION_ANGLE_270, mOrientation);
    }

    public void testConvertMediaOrientationToActualAngleOrientation0() {
        assertEquals(0,
                CvoHandler.convertMediaOrientationToActualAngle(CvoHandler.ORIENTATION_ANGLE_0));
    }

    public void testConvertMediaOrientationToActualAngleOrientation90() {
        assertEquals(90,
                CvoHandler.convertMediaOrientationToActualAngle(CvoHandler.ORIENTATION_ANGLE_90));
    }

    public void testConvertMediaOrientationToActualAngleOrientation180() {
        assertEquals(180,
                CvoHandler.convertMediaOrientationToActualAngle(CvoHandler.ORIENTATION_ANGLE_180));
    }

    public void testConvertMediaOrientationToActualAngleOrientation270() {
        assertEquals(270,
                CvoHandler.convertMediaOrientationToActualAngle(CvoHandler.ORIENTATION_ANGLE_270));
    }

    public void testConvertMediaOrientationToActualAngleOrientationNegative() {
        assertEquals(0, CvoHandler.convertMediaOrientationToActualAngle(-100));
    }

    public void testConvertMediaOrientationToActualAngleOrientation54() {
        assertEquals(0, CvoHandler.convertMediaOrientationToActualAngle(54));
    }

    public void testCalculateDeviceOrientation0() {
        assertEquals(CvoHandler.ORIENTATION_ANGLE_0, CvoHandler.calculateDeviceOrientation(0));
    }

    public void testCalculateDeviceOrientation44() {
        assertEquals(CvoHandler.ORIENTATION_ANGLE_0, CvoHandler.calculateDeviceOrientation(44));
    }

    public void testCalculateDeviceOrientation46() {
        assertEquals(CvoHandler.ORIENTATION_ANGLE_90, CvoHandler.calculateDeviceOrientation(46));
    }

    public void testCalculateDeviceOrientation90() {
        assertEquals(CvoHandler.ORIENTATION_ANGLE_90, CvoHandler.calculateDeviceOrientation(90));
    }

    public void testCalculateDeviceOrientation180() {
        assertEquals(CvoHandler.ORIENTATION_ANGLE_180, CvoHandler.calculateDeviceOrientation(180));
    }

    public void testCalculateDeviceOrientation270() {
        assertEquals(CvoHandler.ORIENTATION_ANGLE_270, CvoHandler.calculateDeviceOrientation(270));
    }

    public void testCalculateDeviceOrientation360() {
        assertEquals(CvoHandler.ORIENTATION_ANGLE_0, CvoHandler.calculateDeviceOrientation(360));
    }

    @Override
    protected void tearDown() throws Exception {
        mCvoHandler = null;
        super.tearDown();
    }
}