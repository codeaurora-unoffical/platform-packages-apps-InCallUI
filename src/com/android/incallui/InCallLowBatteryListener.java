
/* Copyright (c) 2016, The Linux Foundation. All rights reserved.
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

import android.app.AlertDialog;
import android.graphics.Color;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.telecom.InCallService.VideoCall;
import android.telecom.VideoProfile;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import com.android.incallui.InCallPresenter.InCallDetailsListener;
import com.android.incallui.InCallPresenter.InCallUiListener;
import org.codeaurora.ims.QtiCallConstants;

public class InCallLowBatteryListener implements CallList.Listener, InCallDetailsListener,
        InCallUiListener, CallList.CallUpdateListener {

    private static InCallLowBatteryListener sInCallLowBatteryListener;
    private PrimaryCallTracker mPrimaryCallTracker;
    private CallList mCallList = null;
    private AlertDialog mAlert = null;
    private List <Call> mLowBatteryCalls = new CopyOnWriteArrayList<>();
    // Holds TRUE if there is a user action to answer low battery video call as video else FALSE
    private boolean mIsAnswered = false;
    /**
     * Private constructor. Must use getInstance() to get this singleton.
     */
    private InCallLowBatteryListener() {
    }

    /**
     * Handles set up of the {@class InCallLowBatteryListener}.
     */
    public void setUp(Context context) {
        mPrimaryCallTracker = new PrimaryCallTracker();
        mCallList = CallList.getInstance();
        mCallList.addListener(this);
        InCallPresenter.getInstance().addListener(mPrimaryCallTracker);
        InCallPresenter.getInstance().addIncomingCallListener(mPrimaryCallTracker);
        InCallPresenter.getInstance().addDetailsListener(this);
        InCallPresenter.getInstance().addInCallUiListener(this);
    }

    /**
     * Handles tear down of the {@class InCallLowBatteryListener}.
     */
    public void tearDown() {
        if (mCallList != null) {
            mCallList.removeListener(this);
            mCallList = null;
        }
        InCallPresenter.getInstance().removeListener(mPrimaryCallTracker);
        InCallPresenter.getInstance().removeIncomingCallListener(mPrimaryCallTracker);
        InCallPresenter.getInstance().removeDetailsListener(this);
        InCallPresenter.getInstance().removeInCallUiListener(this);
        mPrimaryCallTracker = null;
        mIsAnswered = false;
    }

     /**
     * This method returns a singleton instance of {@class InCallLowBatteryListener}
     */
    public static synchronized InCallLowBatteryListener getInstance() {
        if (sInCallLowBatteryListener == null) {
            sInCallLowBatteryListener = new InCallLowBatteryListener();
        }
        return sInCallLowBatteryListener;
    }

    /**
     * This method overrides onIncomingCall method of {@interface CallList.Listener}
     */
    @Override
    public void onIncomingCall(Call call) {
        mIsAnswered = false;
        // if low battery dialog is already visible to user, dismiss it
        dismissPendingDialogs();
        /* On receiving MT call, disconnect pending MO low battery video call
           that is waiting for user input */
        maybeDisconnectPendingMoCall(mCallList.getPendingOutgoingCall());
    }

    /**
     * This method overrides onCallListChange method of {@interface CallList.Listener}
     * Added for completeness. No implementation yet.
     */
    @Override
    public void onCallListChange(CallList list) {
        // no-op
    }

    /**
     * This method overrides onUpgradeToVideo method of {@interface CallList.Listener}
     */
    @Override
    public void onUpgradeToVideo(Call call) {
        //if low battery dialog is visible to user, dismiss it
        dismissPendingDialogs();
    }

    /**
     * This method overrides onDisconnect method of {@interface CallList.Listener}
     */
    @Override
    public void onDisconnect(Call call) {
        Log.d(this, "onDisconnect call: " + call);
        updateCallInMap(call, false);

        if (mPrimaryCallTracker.getPrimaryCall() == null) {
            /* primarycall is null may signal the possibility that there is only a single call and
               is getting disconnected. So, try to dismiss low battery alert dialogue (if any). This
               is to handle unintentional dismiss for add VT call use-cases wherein low battery
               alert dialog is waiting for user input and the held call is remotely disconnected */
            dismissPendingDialogs();
        }
    }

    /**
      * This API handles InCallActivity destroy when low battery dialog is showing
      */
    public void onDestroyInCallActivity() {
        if (dismissPendingDialogs()) {
            Log.i(this, "onDestroyInCallActivity dismissed low battery dialog");

            /* Activity is destroyed when low battery dialog is showing, possibly
               by removing the activity from recent tasks list etc. Handle this by
               dismissing the existing low battery dialog and marking the entry
               against the call in low battery map that the low battery indication
               needs to be reprocessed for eg. when user brings back the call to
               foreground by pulling it from notification bar */
            Call call = mPrimaryCallTracker.getPrimaryCall();
            if (call == null) {
                Log.w(this, "onDestroyInCallActivity call is null");
                return;
            }

            if (mLowBatteryCalls.contains(call)) {
                Log.d(this, "remove the call from map due to activity destroy");
                mLowBatteryCalls.remove(call);

                /* Route the audio based on current call video state since there is
                   a possibility that user removed the activity from recent tasks
                   with low battery dialog showing soon after user clicked on
                   upgrade icon in which case audio needs to be routed back to ear piece
                 */
                InCallAudioManager.getInstance().onModifyCallClicked(call,
                        call.getVideoState());
            }
        }
    }

    /**
     * This API conveys if incall experience is showing or not.
     *
     * @param showing TRUE if incall experience is showing else FALSE
     */
    @Override
    public void onUiShowing(boolean showing) {
        Call call = mPrimaryCallTracker.getPrimaryCall();
        Log.d(this, "onUiShowing showing: " + showing + " call = " + call +
                " mIsAnswered = " + mIsAnswered);

        if (call == null) {
            return;
        }

        if (!showing) {
            if (InCallPresenter.getInstance().isChangingConfigurations()) {
                handleConfigurationChange(call);
            }
            return;
        }

        boolean isUnAnsweredMtCall = CallUtils.isIncomingVideoCall(call) && !mIsAnswered;
        // Low battery handling for MT video calls kicks-in only after user decides to
        // answer the call as Video. So, do not process unanswered incoming video call.
        if (isUnAnsweredMtCall) {
            return;
        }

        /* There can be chances to miss displaying low battery dialog when user tries to
         * accept incoming VT upgrade request from HUN due to absence of InCallActivity.
         * In such cases, show low battery dialog when InCallActivity is visible
         */
        maybeProcessLowBatteryIndication(call, call.getTelecommCall().getDetails(),
                isIncomingUpgradeReq(call.getSessionModificationState()));
    }

    /**
     * This method overrides onCallChanged method of {@interface CallList.CallUpdateListener}
     * Added for completeness. No implementation yet.
     */
     @Override
     public void onCallChanged(Call call) {

     }

    /**
     * This method overrides onLastForwardedNumberChange method of
     * {@interface CallList.CallUpdateListener}. Added for completeness.
     *  No implementation yet.
     */
     @Override
     public void onLastForwardedNumberChange() {
     }

    /**
     * This method overrides onChildNumberChange method of {@interface CallList.CallUpdateListener}
     * Added for completeness. No implementation yet.
     */
     @Override
     public void onChildNumberChange() {
     }

    /**
     * This method overrides onSessionModificationStateChange method of
     * {@interface CallList.CallUpdateListener}
     * @param call The call object for which session modify change occurred
     * @param sessionModificationState contains the session modification state change
     */
     @Override
     public void onSessionModificationStateChange(Call call, int sessionModificationState) {
         Log.d(this, "onSessionModificationStateChange call = " + call);

         if (call == null || !mPrimaryCallTracker.isPrimaryCall(call)) {
             return;
         }

         if (!isIncomingUpgradeReq(sessionModificationState)) {
             Log.d(this, "onSessionModificationStateChange removing call update listener");
             CallList.getInstance().removeCallUpdateListener(call.getId(), this);
             /*
              * Dismiss low battery dialog (if any) for eg. when incoming upgrade request
              * times-out possibly because there is no user input on low battery dialog or
              * when user dismisses the MT upgrade request from HUN with low battery dialog
              * showing etc.
              */
             dismissPendingDialogs();
         }
     }

     private boolean isIncomingUpgradeReq(int sessionModificationState) {
         return (sessionModificationState ==
                 Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST);
     }

    /**
     * When there is user action to modify call as Video call (for eg click on upgrade
     * button or accepting incoming upgrade request as video), this API checks to see
     * if UE is under low battery or not and accordingly processes the callType change
     * to video and returns TRUE if the callType change is handled by this API else FALSE
     *
     * @param call The call that is undergoing a change to video call
     */
    public boolean onChangeToVideoCall(Call call) {
        Log.d(this, "onChangeToVideoCall call = " + call);
        if (call == null || !mPrimaryCallTracker.isPrimaryCall(call)) {
            return false;
        }

        final android.telecom.Call.Details details = call.getTelecommCall().getDetails();
        if (!isLowBattery(details)) {
           // Do not process if the call is changing to Video when UE is NOT under low battery
           return false;
        }

        /* If user tries to change to Video call again, then remove the call from
         * from lowbatterymap to ensure that the dialog will be shown again
         */
        if (mLowBatteryCalls.contains(call)) {
            Log.d(this, "remove the call from map as user tried to change to Video call again");
            mLowBatteryCalls.remove(call);
        }

        /* Listen to call updates only when there is a incoming upgrade request so that
           low battery dialog can be dismissed if MT upgrade request times out */
        if (isIncomingUpgradeReq(call.getSessionModificationState())) {
            CallList.getInstance().addCallUpdateListener(call.getId(), this);
        }

        maybeProcessLowBatteryIndication(call, details, true);
        return true;
    }

    /**
     * When call is answered, this API checks to see if UE is under low battery or not
     * and accordingly processes the low battery video call and returns TRUE if
     * user action to answer the call is handled by this API else FALSE.
     *
     * @param call The call that is being answered
     * @param videoState The videoState type with which user answered the MT call
     */
    public boolean handleAnswerIncomingCall(Call call, int videoState) {
        Log.d(this, "handleAnswerIncomingCall = " + call + " videoState = " + videoState);
        if (call == null || !mPrimaryCallTracker.isPrimaryCall(call) ||
                !CallUtils.isVideoCall(call)) {
            return false;
        }

        final android.telecom.Call.Details details = call.getTelecommCall().getDetails();

        if (isLowBatteryDialogShowing() && VideoProfile.isAudioOnly(videoState)) {
            /* Dismiss pending low battery dialog, if user accepts the Video call as audio only
               from HUN without giving an input on low battery dialog */
            dismissPendingDialogs();
            return false;
        }

        if (!(isLowBattery(details)
                && CallUtils.isBidirectionalVideoCall(videoState))) {
           //return false if low battery MT VT call isn't accepted as Video
           return false;
        }

        //There is a user action to answer low battery MT Video call as Video
        mIsAnswered = true;
        maybeProcessLowBatteryIndication(call, details, false);
        return true;
    }

    /**
     * This API handles configuration changes done on low battery video call
     *
     * @param call The call on which configuration changes happened
     */
    private void handleConfigurationChange(Call call) {
        Log.d(this, "handleConfigurationChange Call = " + call);
        if (call == null || !mPrimaryCallTracker.isPrimaryCall(call)) {
           return;
        }

        /* If UE orientation changes with low battery dialog showing, then remove
           the call from lowbatterymap to ensure that the dialog will be shown to
           user when the InCallActivity is recreated */
        if (isLowBatteryDialogShowing()) {
            dismissPendingDialogs();
            if (mLowBatteryCalls.contains(call)) {
                Log.d(this, "remove the call from map due to orientation change");
                mLowBatteryCalls.remove(call);
            }
        }
    }

    /**
     * Handles changes to the details of the call.
     *
     * @param call The call for which the details changed.
     * @param details The new call details.
     */
    @Override
    public void onDetailsChanged(Call call, android.telecom.Call.Details details) {
        Log.d(this, " onDetailsChanged call=" + call + " details=" + details);

        if (call == null || !mPrimaryCallTracker.isPrimaryCall(call)) {
            Log.d(this," onDetailsChanged: call is null/Details not for primary call");
            return;
        }
        /* Low Battery handling for MT Video call kicks in only when user decides
           to answer the call as Video call so ignore the incoming video call
           processing here for now */
       if (CallUtils.isIncomingVideoCall(call)) {
            return;
       }

        maybeProcessLowBatteryIndication(call, details, false);
    }

    /**
      * disconnects pending MO video call that is waiting for user confirmation on
      * low battery dialog
      * @param call The probable call that may need to be disconnected
      **/
    private void maybeDisconnectPendingMoCall(Call call) {
        if (call == null) {
            return;
        }

        if (call.getState() == Call.State.CONNECTING && CallUtils.isVideoCall(call)
                && isLowBattery(call.getTelecommCall().getDetails())) {
            // dismiss the low battery dialog that is waiting for user input
            dismissPendingDialogs();

            String callId = call.getId();
            Log.d(this, "disconnect pending MO call");
            TelecomAdapter.getInstance().disconnectCall(callId);
        }
    }

    public boolean isLowBattery(android.telecom.Call.Details details) {
        final Bundle extras =  (details != null) ? details.getExtras() : null;
        final boolean isLowBattery = (extras != null) ? extras.getBoolean(
                QtiCallConstants.LOW_BATTERY_EXTRA_KEY, false) : false;
        Log.d(this, "isLowBattery : " + isLowBattery);
        return isLowBattery;
    }

    private void maybeProcessLowBatteryIndication(Call call,
            android.telecom.Call.Details details, boolean isChangeToVideoCall) {
        /*
         * Low battery indication is processed for Video calls as well as
         * for VoLTE calls that are undergoing a change to Video
         */
        if (!isChangeToVideoCall && !CallUtils.isVideoCall(call)) {
            return;
        }

        if (isLowBattery(details) && updateCallInMap(call, isChangeToVideoCall)) {
            processLowBatteryIndication(call, isChangeToVideoCall);
        }
    }

    /*
     * processes the low battery indication for video call
     */
    private void processLowBatteryIndication(Call call, boolean isChangeToVideoCall) {
        Log.d(this, "processLowBatteryIndication call: " + call);
        //if low battery dialog is already visible to user, dismiss it
        dismissPendingDialogs();
        displayLowBatteryAlert(call, isChangeToVideoCall);
    }

    /*
     * Adds/Removes the call to mLowBatteryCalls
     * Returns TRUE if call is added to mLowBatteryCalls else FALSE
     */
    private boolean updateCallInMap(Call call, boolean isChangeToVideoCall) {
        if (call == null) {
            Log.e(this, "call is null");
            return false;
        }

        final boolean isPresent = mLowBatteryCalls.contains(call);
        if (!Call.State.isConnectingOrConnected(call.getState())) {
            if (isPresent) {
                //we are done with the call so remove from callmap
                mLowBatteryCalls.remove(call);
                return false;
            }
        } else if (InCallPresenter.getInstance().getActivity() == null) {
            /*
             * Displaying Low Battery alert dialog requires incallactivity context
             * so return false if there is no incallactivity context
             */
            Log.i(this, "incallactivity is null");
            return false;
        } else if ((CallUtils.isVideoCall(call) || isChangeToVideoCall)
                && !isPresent
                && call.getParentId() == null) {
            /*
             * call will be added to call map only if below conditions are satisfied:
             * 1. call is not a child call
             * 2. call is a video call or is undergoing a change to video call
             * 3. low battery indication for that call is not yet processed
             */
            mLowBatteryCalls.add(call);
            return true;
        }
        return false;
    }

    /*
     * This method displays one of below alert dialogs when UE is in low battery
     * For Active Video Calls:
     *     1. hangup alert dialog in absence of voice capabilities
     *     2. downgrade to voice call alert dialog in the presence of voice
     *        capabilities
     * For MT Video calls wherein user decided to accept the call as Video and for MO Video Calls:
     *     1. alert dialog asking user confirmation to convert the video call to voice call or
     *        to continue the call as video call
     * For MO Video calls, seek user confirmation to continue the video call as is or convert the
     * video call to voice call
     */
    private void displayLowBatteryAlert(final Call call, final boolean isChangeToVideoCall) {
        final InCallActivity inCallActivity = InCallPresenter.getInstance().getActivity();
        if (inCallActivity == null) {
            Log.e(this, "displayLowBatteryAlert inCallActivity is NULL");
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(inCallActivity);
        alertDialog.setTitle(R.string.low_battery);
        alertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(final DialogInterface dialog) {
                Log.i(this, "displayLowBatteryAlert onDismiss");
                //mAlert = null;
            }
        });

        if (CallUtils.isIncomingVideoCall(call)) {
            alertDialog.setPositiveButton(R.string.low_battery_convert, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                     Log.d(this, "displayLowBatteryAlert answer as Voice Call");
                     TelecomAdapter.getInstance().answerCall(call.getId(),
                             VideoProfile.STATE_AUDIO_ONLY);
                }
            });

            alertDialog.setMessage(R.string.low_battery_msg);
            alertDialog.setNegativeButton(R.string.low_battery_yes, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                     Log.d(this, "displayLowBatteryAlert answer as Video Call");
                     TelecomAdapter.getInstance().answerCall(call.getId(),
                             VideoProfile.STATE_BIDIRECTIONAL);
                }
            });
        } else if (CallUtils.isOutgoingVideoCall(call)) {
            alertDialog.setPositiveButton(R.string.low_battery_convert, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(this, "displayLowBatteryAlert place Voice Call");
                    //Change the audio route to earpiece
                    InCallAudioManager.getInstance().onModifyCallClicked(call,
                            VideoProfile.STATE_AUDIO_ONLY);

                    TelecomAdapter.getInstance().continueCallWithVideoState(
                            call, VideoProfile.STATE_AUDIO_ONLY);
                }
            });

            alertDialog.setMessage(R.string.low_battery_msg);
            alertDialog.setNegativeButton(R.string.low_battery_yes, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                     Log.d(this, "displayLowBatteryAlert place Video Call");
                     TelecomAdapter.getInstance().continueCallWithVideoState(
                             call, VideoProfile.STATE_BIDIRECTIONAL);
                }
            });
        } else if (CallUtils.isActiveUnPausedVideoCall(call)) {
            if (QtiCallUtils.hasVoiceCapabilities(call)) {
                //active video call can be downgraded to voice
                alertDialog.setMessage(R.string.low_battery_msg);
                alertDialog.setNegativeButton(R.string.low_battery_yes, null);
                alertDialog.setPositiveButton(R.string.low_battery_convert, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(this, "displayLowBatteryAlert downgrading to voice call");
                        QtiCallUtils.downgradeToVoiceCall(call);
                    }
                });
            } else {
                /* video call doesn't have downgrade capabilities, so alert the user
                   with a hangup dialog*/
                alertDialog.setMessage(R.string.low_battery_hangup_msg);
                alertDialog.setNegativeButton(R.string.low_battery_no, null);
                alertDialog.setPositiveButton(R.string.low_battery_yes, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(this, "displayLowBatteryAlert hanging up the call: " + call);
                        final String callId = call.getId();
                        call.setState(Call.State.DISCONNECTING);
                        CallList.getInstance().onUpdate(call);
                        TelecomAdapter.getInstance().disconnectCall(callId);
                    }
                });
            }
        } else if (isIncomingUpgradeReq(call.getSessionModificationState())) {
            // Incoming upgrade request handling
            alertDialog.setPositiveButton(R.string.low_battery_convert, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(this, "displayLowBatteryAlert decline upgrade request");
                    CallList.getInstance().removeCallUpdateListener(call.getId(),
                            InCallLowBatteryListener.this);
                    InCallPresenter.getInstance().declineUpgradeRequest();
                }
            });

            alertDialog.setMessage(R.string.low_battery_msg);
            alertDialog.setNegativeButton(R.string.low_battery_yes, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(this, "displayLowBatteryAlert accept upgrade request as Video Call");
                    /* Control reached here means upgrade req can be accepted only as
                       bidirectional video call */
                    VideoProfile videoProfile = new VideoProfile(VideoProfile.STATE_BIDIRECTIONAL);
                    call.getVideoCall().sendSessionModifyResponse(videoProfile);
                    call.setSessionModificationState(Call.SessionModificationState.NO_REQUEST);
                    InCallAudioManager.getInstance().onAcceptUpgradeRequest(call,
                            VideoProfile.STATE_BIDIRECTIONAL);
                }
            });
        } else if (isChangeToVideoCall) {
            // Outgoing upgrade request handling
            alertDialog.setPositiveButton(R.string.low_battery_convert, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(this, "displayLowBatteryAlert change to Voice Call");

                    // Change the audio route to earpiece
                    InCallAudioManager.getInstance().onModifyCallClicked(call,
                            VideoProfile.STATE_AUDIO_ONLY);
                }
            });

            alertDialog.setMessage(R.string.low_battery_msg);
            alertDialog.setNegativeButton(R.string.low_battery_yes, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(this, "displayLowBatteryAlert change to Video Call");

                    VideoCall videoCall = call.getVideoCall();
                    if (videoCall == null) {
                        Log.w(this, "displayLowBatteryAlert videocall is null");
                        return;
                    }

                    int currUnpausedVideoState = CallUtils.getUnPausedVideoState(
                            call.getVideoState());
                    // Send bidirectional modify request
                    currUnpausedVideoState |= VideoProfile.STATE_BIDIRECTIONAL;

                    VideoProfile videoProfile = new VideoProfile(currUnpausedVideoState);
                    videoCall.sendSessionModifyRequest(videoProfile);
                    call.setSessionModificationState(
                            Call.SessionModificationState.WAITING_FOR_RESPONSE);
                }
            });
        }

        mAlert = alertDialog.create();
        mAlert.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                Log.d(this, "on Alert displayLowBattery keyCode = " + keyCode);
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                   // On Back key press, disconnect pending MO low battery video call
                   // that is waiting for user input
                    maybeDisconnectPendingMoCall(call);
                    return true;
                }
                return false;
            }
        });
        mAlert.setCanceledOnTouchOutside(false);
        mAlert.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mAlert.show();
        /*
         * By default both the buttons will have same color. In case we want to have different color
         * we need to set specifically.
         */
        mAlert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }

    /*
     * This method returns true if dialog is showing else false
     */
    private boolean isLowBatteryDialogShowing() {
        return mAlert != null && mAlert.isShowing();
    }

    /*
     * This method dismisses the low battery dialog and
     * returns true if dialog is dimissed else false
     */
    public boolean dismissPendingDialogs() {
        if (isLowBatteryDialogShowing()) {
            mAlert.dismiss();
            mAlert = null;
            return true;
        }
        return false;
    }
}
