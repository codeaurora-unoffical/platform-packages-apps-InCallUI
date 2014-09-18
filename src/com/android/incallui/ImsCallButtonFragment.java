/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.incallui.CallButtonPresenter.CallButtonUi;
import com.android.recorder.CallRecorderService;
import com.android.services.telephony.common.CallDetails;

public class ImsCallButtonFragment extends CallButtonFragment {
    
    private View mImsControl;
    private View mImsModify;
    private View mImsSwitch;
    
    private ImageView mModifyDrawable;
    private TextView mModifyText;  

    private ImageView mSwitchDrawable;
    private TextView mSwitchText;
    
    private boolean mVTOn = true;
    private boolean mCameraOn = true;
    @Override
    CallButtonPresenter createPresenter() {
        // TODO Auto-generated method stub
        return super.createPresenter();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View parent = inflater.inflate(R.layout.ims_call_button_fragment, container, false);

        mEndCallButton = parent.findViewById(R.id.imsEndButton);
        mEndCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().endCallClicked();
            }
        });

        // make the hit target smaller for the end button so that is creates a deadzone
        // along the inside perimeter of the button.
        mEndCallButton.setOnTouchListener(new SmallerHitTargetTouchListener());
        
        mManageConferenceButton = parent.findViewById(R.id.imsManageConf);
        mManageConferenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().manageConferenceButtonClicked();
            }
        });
        
        mMuteButton = (ImageButton) parent.findViewById(R.id.imsMute);
        mMuteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final ImageButton button = (ImageButton) v;
                getPresenter().muteClicked(!button.isSelected());
            }
        });

        mAudioButton = (ImageButton) parent.findViewById(R.id.imsSpeaker);
        mAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAudioButtonClicked();
            }
        });

        mHoldButton = (ImageButton) parent.findViewById(R.id.imsHold);
        mHoldButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final ImageButton button = (ImageButton) v;
                getPresenter().holdClicked(!button.isSelected());
            }
        });

        mShowDialpadButton = (ToggleButton) parent.findViewById(R.id.imsKeyboard);
        mShowDialpadButton.setOnClickListener(this);

        mImsModify = parent.findViewById(R.id.imsModify);
        mImsModify.setOnClickListener(this);
        
        mModifyDrawable = (ImageView)parent.findViewById(R.id.imsModifyDrawable);
        mModifyText = (TextView)parent.findViewById(R.id.imsModifytxt);

        mImsSwitch = parent.findViewById(R.id.imsSwitch);
        mImsSwitch.setOnClickListener(this);

        mSwitchDrawable = (ImageView)parent.findViewById(R.id.imsSwitchDrawable);
        mSwitchText = (TextView)parent.findViewById(R.id.imsSwitchtxt);

        mSwapButton = (ImageButton) parent.findViewById(R.id.imsSwap);
        mSwapButton.setOnClickListener(this);
        
        mImsControl = parent.findViewById(R.id.imsControl);
        
        mAddCallButton = (ImageButton) parent.findViewById(R.id.imsAdd);
        mAddCallButton.setOnClickListener(this);
        mMergeButton = (ImageButton) parent.findViewById(R.id.imsMerge);
        mMergeButton.setOnClickListener(this);

        return parent;
    }
    

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    private void handleClickModifyButton(){
        Log.d(this, "handleClickModifyButton mVTOn = " + mVTOn);
        getPresenter().modifyWithCallType(mVTOn);
        //mVTOn = !mVTOn;
        mImsModify.setEnabled(false);
        mModifyDrawable.setEnabled(false);
        mModifyText.setEnabled(false);
        Toast.makeText(getView().getContext(), R.string.ims_connecting ,Toast.LENGTH_SHORT).show();
    }
    
    
    private void handleSwitchCameraButton(){
        ((InCallActivity)getActivity()).handleSwitchCamera();
    }
    
    
    public void handleUpdateModifyButtons(int callType) {
        Log.d(this, "enter handleUpdateModifyButtons callType = " + callType);
        switch (callType) {
            case CallDetails.CALL_TYPE_VT_TX:
                break;
            case CallDetails.CALL_TYPE_VT_RX:
                break;
            case CallDetails.CALL_TYPE_VT:
                mModifyText.setText(R.string.ims_modify_volte);
                mModifyDrawable.setImageResource(R.drawable.ims_voicecall_view);
                mModifyDrawable.setEnabled(true);
                mImsModify.setEnabled(true);
                mModifyText.setEnabled(true);
                mImsSwitch.setEnabled(true);
                mSwitchDrawable.setEnabled(true);
                mSwitchText.setEnabled(true);
                mVTOn = true;
                break;
            case CallDetails.CALL_TYPE_VOICE:
                mModifyText.setText(R.string.ims_modify_vt);
                mModifyDrawable.setImageResource(R.drawable.ims_videocall_view);
                mModifyDrawable.setEnabled(true);
                mImsModify.setEnabled(true);
                mModifyText.setEnabled(true);
                mImsSwitch.setEnabled(false);
                mSwitchDrawable.setEnabled(false);
                mSwitchText.setEnabled(false);
                mVTOn = false;
                break;

            default:
                Log.e(this, "handleUpdateModifyButtons callType = " + callType);
        }
    }
    
    
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        Log.d(this, "onClick(View " + view + ", id " + id + ")...");

        switch(id) {
            case R.id.imsKeyboard:
                getPresenter().showDialpadClicked(mShowDialpadButton.isChecked());
                break;
            case R.id.addParticipant:
                InCallPresenter.getInstance().sendAddParticipantIntents();
                break;
            case R.id.imsModify:
                handleClickModifyButton();
                break;
            case R.id.imsSwitch:
                handleSwitchCameraButton();
                break;
            
            case R.id.imsSwap:
                getPresenter().swapClicked();
                break;
                
            case R.id.imsAdd:
                getPresenter().addCallClicked();
                break;
            case R.id.imsMerge:
                getPresenter().mergeClicked();
                break;
                
            default:
                Log.wtf(this, "onClick: unexpected");
                break;
        }
    }


    @Override
    public void setEnabled(boolean isEnabled) {
        View view = getView();
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }

        // The main end-call button spanning across the screen.
        mEndCallButton.setEnabled(isEnabled);

        // The smaller buttons laid out horizontally just below the end-call button.
        mMuteButton.setEnabled(isEnabled);
        mAudioButton.setEnabled(isEnabled);
        mHoldButton.setEnabled(isEnabled);
        mShowDialpadButton.setEnabled(isEnabled);

        mImsModify.setEnabled(isEnabled);
        mAddCallButton.setEnabled(isEnabled);
        mMergeButton.setEnabled(isEnabled);

    }

    @Override
    public void setMute(boolean value) {
        // TODO Auto-generated method stub
        super.setMute(value);
    }

    @Override
    public void enableMute(boolean enabled) {
        // TODO Auto-generated method stub
        super.enableMute(enabled);
    }

    @Override
    public void setHold(boolean value) {
        // TODO Auto-generated method stub
        super.setHold(value);
    }

    @Override
    public void showHold(boolean show) {
        // TODO Auto-generated method stub
        super.showHold(show);
    }

    @Override
    public void enableHold(boolean enabled) {
        // TODO Auto-generated method stub
        super.enableHold(enabled);
    }

    @Override
    public void showMerge(boolean show) {
        mMergeButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    @Override
    public void showAddCall(boolean show) {
        mAddCallButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void enableAddCall(boolean enabled) {
        mAddCallButton.setEnabled(enabled);
    }

    @Override
    public void enableAddParticipant(boolean show) {
        
    }

    @Override
    public void setAudio(int mode) {
        // TODO Auto-generated method stub
        super.setAudio(mode);
    }

    @Override
    public void setSupportedAudio(int modeMask) {
        // TODO Auto-generated method stub
        super.setSupportedAudio(modeMask);
    }


    @Override
    public void displayModifyCallOptions(int callId) {
        super.displayModifyCallOptions(callId);
    }

    @Override
    public void enableModifyCall(boolean enabled) {
        mImsModify.setEnabled(enabled);
    }

    @Override
    public void showModifyCall(boolean show) {
        mImsModify.setEnabled(show);
    }

    @Override
    public void onDismiss(PopupMenu menu) {
        // TODO Auto-generated method stub
        super.onDismiss(menu);
    }

    @Override
    public void refreshAudioModePopup() {
        // TODO Auto-generated method stub
        super.refreshAudioModePopup();
    }

    @Override
    public void displayDialpad(boolean value) {
        mImsControl.setVisibility(value ? View.GONE : View.VISIBLE);
        super.displayDialpad(value);
    }

    @Override
    public boolean isDialpadVisible() {
        // TODO Auto-generated method stub
        return super.isDialpadVisible();
    }

    @Override
    public void displayManageConferencePanel(boolean value) {
        // TODO Auto-generated method stub
        super.displayManageConferencePanel(value);
    }

    @Override
    public void showManageConferenceCallButton() {
        // TODO Auto-generated method stub
        mImsModify.setVisibility(View.GONE);
        mImsSwitch.setVisibility(View.GONE);
        mManageConferenceButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void showGenericMergeButton() {
        // TODO Auto-generated method stub
        mManageConferenceButton.setVisibility(View.GONE);
        mMergeButton.setVisibility(View.VISIBLE);
        mImsModify.setVisibility(View.VISIBLE);
        mImsSwitch.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideExtraRow() {
        //we need this function to hide manageConference button
        mManageConferenceButton.setVisibility(View.GONE);
        mImsModify.setVisibility(View.VISIBLE);
        mImsSwitch.setVisibility(View.VISIBLE);
    }
    
    public void hideExtraRow(boolean inConference){
        if (inConference){
            mManageConferenceButton.setVisibility(View.GONE);
            mImsModify.setVisibility(View.GONE);
            mImsSwitch.setVisibility(View.GONE);
        }else{
            mManageConferenceButton.setVisibility(View.GONE);
            mImsModify.setVisibility(View.VISIBLE);
            mImsSwitch.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public CallButtonPresenter getPresenter() {
        // TODO Auto-generated method stub
        return super.getPresenter();
    }

    @Override
    public void onDestroyView() {
        // TODO Auto-generated method stub
        super.onDestroyView();
    }
}
