/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui;

import java.util.Map;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.android.incallui.ContactInfoCache.ContactCacheEntry;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.CallDetails;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Logic for call buttons.
 */
public class ConferenceManagerPresenter
        extends Presenter<ConferenceManagerPresenter.ConferenceManagerUi>
        implements InCallStateListener {

    private static final int MAX_CALLERS_IN_CONFERENCE = 5;

    private int mNumCallersInConference;
    private Integer[] mCallerIds;
    private String[] mParticipantList;
    private Map<String, String[]> mConferenceDetails;
    private Context mContext;
    private StringBuffer mExistedParticipants;
    private static String LOG_TAG = "ConferenceManagerPresenter";

    @Override
    public void onUiReady(ConferenceManagerUi ui) {
        super.onUiReady(ui);

        // register for call state changes last
        InCallPresenter.getInstance().addListener(this);
    }

    @Override
    public void onUiUnready(ConferenceManagerUi ui) {
        super.onUiUnready(ui);

        InCallPresenter.getInstance().removeListener(this);
    }

    @Override
    public void onStateChange(InCallState state, CallList callList) {
        if (getUi().isFragmentVisible()) {
            Log.v(this, "onStateChange" + state);
            if (state == InCallState.INCALL) {
                final Call call = callList.getActiveOrBackgroundCall();
                if (call != null && call.isConferenceCall()) {
                    Log.v(this, "Number of existing calls is " +
                            String.valueOf(call.getChildCallIds().size()));
                    update(callList);
                } else {
                    getUi().setVisible(false);
                }
            } else {
                getUi().setVisible(false);
            }
        }
    }

    public void init(Context context, CallList callList) {
        mContext = Preconditions.checkNotNull(context);
        mContext = context;
        update(callList);
    }

    private boolean isImsCall(Call call) {
        return call != null && call.getCallDetails() != null
                && call.getCallDetails().getCallDomain() == CallDetails.CALL_DOMAIN_PS;
    }

    private void initParticipantList(CallList callList) {
        mParticipantList = null;
        mExistedParticipants = new StringBuffer();
        Call call = callList.getActiveOrBackgroundCall();

        if (isImsCall(call)) {
            String[] confParticipantList = call.getCallDetails().getConfParticipantList();
            Log.v(this, "initParticipantList call=" + call);
            // If conference refresh info xml is present use that information
            if (confParticipantList != null
                    && confParticipantList.length > 0) {
                mParticipantList = confParticipantList;
                mNumCallersInConference = mParticipantList.length;
                mConferenceDetails = call.getCallDetails().getConfDetails();
                Log.v(this, "mConferenceDetails = " + mConferenceDetails);
                return;
            }
        }
        mCallerIds = callList.getActiveOrBackgroundCall().getChildCallIds().toArray(new Integer[0]);
        mNumCallersInConference = mCallerIds.length;
    }

    private void update(CallList callList) {
        mCallerIds = null;
        // set mNumCallersInConference and mParticipantList
        initParticipantList(callList);

        Log.v(this, "Number of calls is " + String.valueOf(mNumCallersInConference));

        // Users can split out a call from the conference call if there either the active call
        // or the holding call is empty. If both are filled at the moment, users can not split out
        // another call.
        final boolean hasActiveCall = (callList.getActiveCall() != null);
        final boolean hasHoldingCall = (callList.getBackgroundCall() != null);
        boolean canSeparate = !(hasActiveCall && hasHoldingCall);

        for (int i = 0; i < MAX_CALLERS_IN_CONFERENCE; i++) {
            if (i < mNumCallersInConference) {
                // Fill in the row in the UI for this caller.
                if (mParticipantList == null) {
                    final ContactCacheEntry contactCache = ContactInfoCache.getInstance(mContext).
                            getInfo(mCallerIds[i]);
                    updateManageConferenceRow(i, contactCache, canSeparate);
                } else {
                    updateManageConferenceRow(i, mParticipantList[i]);
                }
            } else {
                // Blank out this row in the UI
                updateManageConferenceRow(i, null, false);
            }
        }
        getUi().hideAddParticipant(mNumCallersInConference == MAX_CALLERS_IN_CONFERENCE);
    }

    /**
      * Updates a single row of the "Manage conference" UI.  (One row in this
      * UI represents a single caller in the conference.)
      *
      * @param i the row to update
      * @param contactCacheEntry the contact details corresponding to this caller.
      *        If null, that means this is an "empty slot" in the conference,
      *        so hide this row in the UI.
      * @param canSeparate if true, show a "Separate" (i.e. "Private") button
      *        on this row in the UI.
      */
    public void updateManageConferenceRow(final int i,
                                          final ContactCacheEntry contactCacheEntry,
                                          boolean canSeparate) {

        if (contactCacheEntry != null) {
            // Activate this row of the Manage conference panel:
            getUi().setRowVisible(i, true);

            final String name = contactCacheEntry.name;
            final String number = contactCacheEntry.number;
            mExistedParticipants.append(number).append(";");

            if (canSeparate) {
                getUi().setCanSeparateButtonForRow(i, canSeparate);
            }
            // display the CallerInfo.
            getUi().setupEndButtonForRow(i);
            getUi().displayCallerInfoForConferenceRow(i, name, number, contactCacheEntry.label,
                    contactCacheEntry.photo, "");

        } else {
            // Disable this row of the Manage conference panel:
            getUi().setRowVisible(i, false);
        }
    }

    public void updateManageConferenceRow(final int i, final String url) {
        if (url != null) {
            getUi().setRowVisible(i, true);
            getUi().setupEndButtonForRowWithUrl(i, url);
            String state;
            if (mConferenceDetails != null){
                state = getValueForKeyFromEntry(mConferenceDetails.get(url),
                    CallDetails.CONFERENCE_DETATILS_STATE);
            }else{
                state = "";
            }
            getUi().displayCallerInfoForConferenceRow(i, "", url, "", null, state);
            mExistedParticipants.append(url).append(";");
        } else {
            // Disable this row of the Manage conference panel:
            getUi().setRowVisible(i, false);
        }
    }

    private String getValueForKeyFromEntry(String[] entry, String key) {
        for (int i = 0; entry != null && i < entry.length; i++) {
            if (entry[i] != null) {
                String[] currKey = entry[i].split("=");
                if (currKey.length == 2 && currKey[0].equals(key)) {
                    return currKey[1];
                }
            }
        }
        return null;
    }

    public void manageConferenceDoneClicked() {
        getUi().setVisible(false);
    }

    public void manageAddParticipants() {
        Intent intent = new Intent("android.intent.action.ADDPARTICIPANT");
        intent.putExtra(InCallApp.ADD_PARTICIPANT_KEY, true);

        if (mExistedParticipants != null) {
            Log.d(LOG_TAG,
                    "manageAddParticipants existingParticipants=" + mExistedParticipants.toString());
            intent.putExtra(InCallApp.CURRENT_PARTICIPANT_LIST, mExistedParticipants.toString());
        }
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // This is rather rare but possible.
            // Note: this method is used even when the phone is encrypted. At
            // that moment
            // the system may not find any Activity which can accept this Intent
            Log.e(LOG_TAG, "Activity for adding calls isn't found.");
        }
    }

    public int getMaxCallersInConference() {
        return MAX_CALLERS_IN_CONFERENCE;
    }

    public void separateConferenceConnection(int rowId) {
        CallCommandClient.getInstance().separateCall(mCallerIds[rowId]);
    }

    public void endConferenceConnection(int rowId) {
        CallCommandClient.getInstance().disconnectCall(mCallerIds[rowId]);
    }

    public void endConferenceConnectionUrl(int rowId , String url) {
        CallCommandClient.getInstance().hangupWithReason(-1, url,
                true, Call.DisconnectCause.NORMAL.ordinal(), "");
    }

    public interface ConferenceManagerUi extends Ui {
        void setVisible(boolean on);
        boolean isFragmentVisible();
        void setRowVisible(int rowId, boolean on);
        void displayCallerInfoForConferenceRow(int rowId, String callerName, String callerNumber,
                String callerNumberType, Drawable photo, String state);
        void setCanSeparateButtonForRow(int rowId, boolean canSeparate);
        void setupEndButtonForRow(int rowId);

        void setupEndButtonForRowWithUrl(int rowId, String url);
        void startConferenceTime(long base);
        void stopConferenceTime();
        void hideAddParticipant(boolean hide);
    }
}
