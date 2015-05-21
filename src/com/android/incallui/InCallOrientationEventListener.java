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

import android.content.Context;
import android.content.res.Configuration;
import android.view.OrientationEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

/**
 * This class listens to Orientation events and overrides onOrientationChanged which gets
 * invoked when an orientation change occurs. When that happens, we notify InCallUI registrants
 * of the change.
 */
public class InCallOrientationEventListener extends OrientationEventListener {

    /**
     * Screen orientation angles one of 0, 90, 180, 270, 360 in degrees.
     */
    public static int SCREEN_ORIENTATION_0 = 0;
    public static int SCREEN_ORIENTATION_90 = 90;
    public static int SCREEN_ORIENTATION_180 = 180;
    public static int SCREEN_ORIENTATION_270 = 270;
    public static int SCREEN_ORIENTATION_360 = 360;

    /**
     * This is to identify dead zones where we won't notify others of orientation changed.
     * Say for e.g our threshold is x degrees. We will only notify UI when our current rotation is
     * within x degrees right or left of the screen orientation angles. If it's not within those
     * ranges, we return SCREEN_ORIENTATION_UNKNOWN and ignore it.
     */
    private static int SCREEN_ORIENTATION_UNKNOWN = -1;

    // Rotation threshold is 10 degrees. So if the rotation angle is within 10 degrees of any of
    // the above angles, we will notify orientation changed.
    private static int ROTATION_THRESHOLD = 10;

    /**
     * Cache the current rotation of the device.
     */
    private static int mCurrentOrientation = SCREEN_ORIENTATION_0;

    public InCallOrientationEventListener(Context context) {
        super(context);
    }

    /**
     * Handles changes in device orientation. Notifies InCallPresenter of orientation changes.
     *
     * Note that this API receives sensor rotation in degrees as a param and we convert that to
     * one of our screen orientation constants - (one of: {@link SCREEN_ORIENTATION_0},
     * {@link SCREEN_ORIENTATION_90}, {@link SCREEN_ORIENTATION_180},
     * {@link SCREEN_ORIENTATION_270}).
     *
     * @param rotation The new device sensor rotation in degrees
     */
    @Override
    public void onOrientationChanged(int rotation) {
        if (rotation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            Log.e(this, "onOrientationChanged - Unknown orientation. Return");
            return;
        }

        final int orientation = toScreenOrientation(rotation);

        Log.d(this, "onOrientationChanged currentOrientation = " + mCurrentOrientation +
                " newOrientation = " + orientation);
        if (orientation != SCREEN_ORIENTATION_UNKNOWN && mCurrentOrientation != orientation) {
            mCurrentOrientation = orientation;
            InCallPresenter.getInstance().onDeviceOrientationChange(mCurrentOrientation);
        }
    }

    /**
     * Enables the OrientationEventListener and notifies listeners of current orientation if
     * notify flag is true
     * @param notify true or false. Notify device orientation changed if true.
     */
    public void enable(boolean notify) {
        super.enable();
        if (notify) {
            InCallPresenter.getInstance().onDeviceOrientationChange(mCurrentOrientation);
        }
    }

    /**
     * Enables the OrientationEventListener with notify flag defaulting to false.
     */
    public void enable() {
        enable(false);
    }

    /**
     * Converts sensor rotation in degrees to screen orientation constants.
     * @param rotation sensor rotation angle in degrees
     * @return Screen orientation angle in degrees (0, 90, 180, 270). Returns -1 for degrees not
     * within threshold to identify zones where orientation change should not be trigerred.
     */
    private int toScreenOrientation(int rotation) {
        // Sensor orientation 90 is equivalent to screen orientation 270 and vice versa. This
        // function returns the screen orientation. Se we convert sensor rotation 90 to 270 and
        // vice versa here.
        if (isInLeftRange(rotation, SCREEN_ORIENTATION_360, ROTATION_THRESHOLD) ||
                isInRightRange(rotation, SCREEN_ORIENTATION_0, ROTATION_THRESHOLD)) {
            return SCREEN_ORIENTATION_0;
        } else if (isWithinThreshold(rotation, SCREEN_ORIENTATION_90, ROTATION_THRESHOLD)) {
            return SCREEN_ORIENTATION_270;
        } else if (isWithinThreshold(rotation, SCREEN_ORIENTATION_180, ROTATION_THRESHOLD)) {
            return SCREEN_ORIENTATION_180;
        } else if (isWithinThreshold(rotation, SCREEN_ORIENTATION_270, ROTATION_THRESHOLD)) {
            return SCREEN_ORIENTATION_90;
        }
        return SCREEN_ORIENTATION_UNKNOWN;
    }

    private static boolean isWithinRange(int value, int begin, int end) {
        return value >= begin && value < end;
    }

    private static boolean isWithinThreshold(int value, int center, int threshold) {
        return isWithinRange(value, center - threshold, center + threshold);
    }

    private static boolean isInLeftRange(int value, int center, int threshold) {
        return isWithinRange(value, center - threshold, center);
    }

    private static boolean isInRightRange(int value, int center, int threshold) {
        return isWithinRange(value, center, center + threshold);
    }
}
