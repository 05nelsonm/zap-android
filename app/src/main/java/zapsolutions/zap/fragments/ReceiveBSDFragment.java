package zapsolutions.zap.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.github.lightningnetwork.lnd.lnrpc.AddInvoiceResponse;
import com.github.lightningnetwork.lnd.lnrpc.Invoice;
import com.github.lightningnetwork.lnd.lnrpc.LightningGrpc;
import com.github.lightningnetwork.lnd.lnrpc.NewAddressRequest;
import com.github.lightningnetwork.lnd.lnrpc.NewAddressResponse;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

import zapsolutions.zap.GeneratedRequestActivity;
import zapsolutions.zap.R;
import zapsolutions.zap.channelManagement.ManageChannelsActivity;
import zapsolutions.zap.connection.establishConnectionToLnd.LndConnection;
import zapsolutions.zap.interfaces.UserGuardianInterface;
import zapsolutions.zap.util.ExecuteOnCaller;
import zapsolutions.zap.util.MonetaryUtil;
import zapsolutions.zap.util.OnSingleClickListener;
import zapsolutions.zap.util.PrefsUtil;
import zapsolutions.zap.util.UserGuardian;
import zapsolutions.zap.util.Wallet;
import zapsolutions.zap.util.ZapLog;


public class ReceiveBSDFragment extends BottomSheetDialogFragment implements UserGuardianInterface {

    private static final String LOG_TAG = ReceiveBSDFragment.class.getName();

    private View mBtnLn;
    private View mBtnOnChain;
    private View mChooseTypeView;
    private ImageView mIvBsdIcon;
    private ConstraintLayout mRootLayout;
    private ConstraintLayout mIconAnchor;
    private View mReceiveAmountView;
    private EditText mEtAmount;
    private EditText mEtMemo;
    private TextView mTvUnit;
    private View mMemoView;
    private TextView mTvTitle;
    private View mNumpad;
    private Button[] mBtnNumpad = new Button[10];
    private Button mBtnNumpadDot;
    private ImageButton mBtnNumpadBack;
    private Button mBtnNext;
    private Button mBtnGenerateRequest;
    private boolean mOnChain;
    private BottomSheetBehavior mBehavior;
    private FrameLayout mBottomSheet;
    private TextView mTvNoIncomingBalance;
    private Button mBtnManageChannels;
    private View mViewNoIncomingBalance;
    private UserGuardian mUG;
    private String mValueBeforeUnitSwitch;
    private boolean mUseValueBeforeUnitSwitch = true;
    private boolean mAmountValid = true;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_receive, container);

        // Apply FLAG_SECURE to dialog to prevent screen recording
        if (PrefsUtil.preventScreenRecording()) {
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        mUG = new UserGuardian(getActivity(), this);

        mBtnLn = view.findViewById(R.id.lnBtn);
        mBtnOnChain = view.findViewById(R.id.onChainBtn);
        mIvBsdIcon = view.findViewById(R.id.bsdIcon);
        mIconAnchor = view.findViewById(R.id.anchor);
        mRootLayout = view.findViewById(R.id.rootLayout);
        mChooseTypeView = view.findViewById(R.id.chooseTypeLayout);
        mReceiveAmountView = view.findViewById(R.id.receiveInputsView);
        mEtAmount = view.findViewById(R.id.receiveAmount);
        mTvUnit = view.findViewById(R.id.receiveUnit);
        mEtMemo = view.findViewById(R.id.receiveMemo);
        mMemoView = view.findViewById(R.id.receiveMemoTopLayout);
        mTvTitle = view.findViewById(R.id.bsdTitle);
        mNumpad = view.findViewById(R.id.Numpad);
        mBtnNext = view.findViewById(R.id.nextButton);
        mBtnGenerateRequest = view.findViewById(R.id.generateRequestButton);


        mTvNoIncomingBalance = view.findViewById(R.id.noIncomingChannelBalanceText);
        mViewNoIncomingBalance = view.findViewById(R.id.noIncomingChannelBalanceView);
        mBtnManageChannels = view.findViewById(R.id.manageChannels);


        // Get numpad buttons
        mBtnNumpad[0] = view.findViewById(R.id.Numpad1);
        mBtnNumpad[1] = view.findViewById(R.id.Numpad2);
        mBtnNumpad[2] = view.findViewById(R.id.Numpad3);
        mBtnNumpad[3] = view.findViewById(R.id.Numpad4);
        mBtnNumpad[4] = view.findViewById(R.id.Numpad5);
        mBtnNumpad[5] = view.findViewById(R.id.Numpad6);
        mBtnNumpad[6] = view.findViewById(R.id.Numpad7);
        mBtnNumpad[7] = view.findViewById(R.id.Numpad8);
        mBtnNumpad[8] = view.findViewById(R.id.Numpad9);
        mBtnNumpad[9] = view.findViewById(R.id.Numpad0);

        mBtnNumpadDot = view.findViewById(R.id.NumpadDot);
        mBtnNumpadBack = view.findViewById(R.id.NumpadBack);

        // Set action for numpad number buttons
        for (Button btn : mBtnNumpad) {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Add input
                    int start = Math.max(mEtAmount.getSelectionStart(), 0);
                    int end = Math.max(mEtAmount.getSelectionEnd(), 0);
                    mEtAmount.getText().replace(Math.min(start, end), Math.max(start, end),
                            btn.getText(), 0, btn.getText().length());

                }
            });
        }

        // Set action for numpad "." button
        mBtnNumpadDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add input
                int start = Math.max(mEtAmount.getSelectionStart(), 0);
                int end = Math.max(mEtAmount.getSelectionEnd(), 0);
                mEtAmount.getText().replace(Math.min(start, end), Math.max(start, end),
                        mBtnNumpadDot.getText(), 0, mBtnNumpadDot.getText().length());
            }
        });

        // Set action for numpad "delete" button
        mBtnNumpadBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // remove Input
                deleteAmountInput();
            }
        });

        // add "optional" hint to optional fields
        mEtAmount.setHint(getResources().getString(R.string.amount) + " (" + getResources().getString(R.string.optional) + ")");
        mEtMemo.setHint(getResources().getString(R.string.memo) + " (" + getResources().getString(R.string.optional) + ")");

        // deactivate default Keyboard for number input.
        mEtAmount.setShowSoftInputOnFocus(false);

        // set unit to current primary unit
        mTvUnit.setText(MonetaryUtil.getInstance().getPrimaryDisplayUnit());

        // Action when clicked on "x" (close) button
        ImageButton btnCloseBSD = view.findViewById(R.id.closeButton);
        btnCloseBSD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });


        // Action when clicked on "Lightning" Button
        mBtnLn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mBtnNext.setEnabled(false);
                mBtnNext.setTextColor(getResources().getColor(R.color.gray));
                mEtAmount.setHint(getResources().getString(R.string.amount));

                mOnChain = false;
                mIvBsdIcon.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_icon_modal_lightning));
                mTvTitle.setText(R.string.receive_lightning_request);


                // Animate bsd Icon size
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(mIvBsdIcon, "scaleX", 0f, 1f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(mIvBsdIcon, "scaleY", 0f, 1f);
                scaleUpX.setDuration(400);
                scaleUpY.setDuration(400);

                AnimatorSet scaleUpIcon = new AnimatorSet();
                scaleUpIcon.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
                scaleUpIcon.play(scaleUpX).with(scaleUpY);
                scaleUpIcon.start();

                // Check if we can receive anything over the lightning network
                boolean canReceiveLightningPayment;
                boolean hasActiveChannels = Wallet.getInstance().hasOpenActiveChannels();

                if (hasActiveChannels) {
                    if (Wallet.getInstance().getMaxChannelRemoteBalance() > 0L) {
                        // We have remote balances on at least one channel, so we can receive a lightning payment!
                        canReceiveLightningPayment = true;
                    } else {
                        mTvNoIncomingBalance.setText(R.string.receive_noIncomeBalance);
                        canReceiveLightningPayment = false;
                    }
                } else {
                    mTvNoIncomingBalance.setText(R.string.receive_noActiveChannels);
                    canReceiveLightningPayment = false;
                }

                // In Demo Mode, we want to show the working way...
                if (!PrefsUtil.isWalletSetup()) {
                    canReceiveLightningPayment = true;
                }

                // Animate Layout changes
                ConstraintSet csRoot = new ConstraintSet();
                csRoot.clone(mRootLayout);
                if (canReceiveLightningPayment) {
                    csRoot.constrainHeight(mNumpad.getId(), ConstraintSet.WRAP_CONTENT);
                    csRoot.constrainHeight(mReceiveAmountView.getId(), ConstraintSet.WRAP_CONTENT);
                    csRoot.constrainHeight(mBtnNext.getId(), ConstraintSet.WRAP_CONTENT);
                } else {
                    csRoot.constrainHeight(mTvNoIncomingBalance.getId(), ConstraintSet.WRAP_CONTENT);
                    csRoot.constrainHeight(mViewNoIncomingBalance.getId(), ConstraintSet.WRAP_CONTENT);
                }

                Transition transition = new ChangeBounds();
                transition.setInterpolator(new DecelerateInterpolator(3));
                transition.setDuration(400);
                TransitionManager.beginDelayedTransition(mRootLayout, transition);
                csRoot.applyTo(mRootLayout);


                FrameLayout bottomSheet = getDialog().findViewById(R.id.design_bottom_sheet);
                //CoordinatorLayout layout = (CoordinatorLayout) bottomSheet.getParent();
                mBehavior = BottomSheetBehavior.from(bottomSheet);
                mBehavior.setPeekHeight(bottomSheet.getHeight());
                //layout.getParent().requestLayout();

                // Expand bottom sheet after size has changed
                mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                // Manage visibilities
                mIvBsdIcon.setVisibility(View.VISIBLE);
                mChooseTypeView.setVisibility(View.GONE);
                mMemoView.setVisibility(View.GONE);
                if (canReceiveLightningPayment) {
                    mBtnNext.setVisibility(View.VISIBLE);
                    mTvNoIncomingBalance.setVisibility(View.GONE);
                    mBtnManageChannels.setVisibility(View.GONE);

                    // Request focus on amount input
                    mEtAmount.requestFocus();
                } else {
                    mBtnNext.setVisibility(View.GONE);
                    mViewNoIncomingBalance.setVisibility(View.VISIBLE);
                    mBtnManageChannels.setVisibility(View.VISIBLE);
                }

            }
        });

        // Action when clicked on "On-Chain" Button
        mBtnOnChain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mOnChain = true;
                mIvBsdIcon.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_icon_modal_on_chain));
                mTvTitle.setText(R.string.receive_on_chain_request);

                // Animate bsd Icon size
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(mIvBsdIcon, "scaleX", 0f, 1f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(mIvBsdIcon, "scaleY", 0f, 1f);
                scaleUpX.setDuration(400);
                scaleUpY.setDuration(400);

                AnimatorSet scaleUpIcon = new AnimatorSet();
                scaleUpIcon.setInterpolator(new AnticipateOvershootInterpolator(1.0f));
                scaleUpIcon.play(scaleUpX).with(scaleUpY);
                scaleUpIcon.start();

                // Animate Layout changes
                ConstraintSet csRoot = new ConstraintSet();
                csRoot.clone(mRootLayout);
                csRoot.constrainHeight(mNumpad.getId(), ConstraintSet.WRAP_CONTENT);
                csRoot.constrainHeight(mReceiveAmountView.getId(), ConstraintSet.WRAP_CONTENT);
                csRoot.constrainHeight(mBtnNext.getId(), ConstraintSet.WRAP_CONTENT);

                Transition transition = new ChangeBounds();
                transition.setInterpolator(new DecelerateInterpolator(3));
                transition.setDuration(400);
                TransitionManager.beginDelayedTransition(mRootLayout, transition);
                csRoot.applyTo(mRootLayout);


                FrameLayout bottomSheet = getDialog().findViewById(R.id.design_bottom_sheet);
                bottomSheet.setForegroundGravity(Gravity.BOTTOM);
                //CoordinatorLayout layout = (CoordinatorLayout) bottomSheet.getParent();
                mBehavior = BottomSheetBehavior.from(bottomSheet);
                mBehavior.setPeekHeight(bottomSheet.getHeight());
                //layout.getParent().requestLayout();

                // Expand bottom sheet after size has changed
                mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);


                mIvBsdIcon.setVisibility(View.VISIBLE);
                mChooseTypeView.setVisibility(View.GONE);
                mBtnNext.setVisibility(View.VISIBLE);
                mMemoView.setVisibility(View.GONE);


                // Request focus on amount input
                mEtAmount.requestFocus();
            }
        });

        // Action when clicked on "next" button
        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNumpad.setVisibility(View.GONE);
                mBtnNext.setVisibility(View.GONE);
                mMemoView.setVisibility(View.VISIBLE);
                mBtnGenerateRequest.setVisibility(View.VISIBLE);

                mEtAmount.setEnabled(false);
                mEtMemo.requestFocus();
                showKeyboard();
            }
        });

        // Action when clicked on "Generate Request" button
        mBtnGenerateRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Warn the user if his primary currency is not of type bitcoin and his exchange rate is older than 1 hour.
                if (!MonetaryUtil.getInstance().getPrimaryCurrency().isBitcoin() && MonetaryUtil.getInstance().getExchangeRateAge() > 3600) {
                    mUG.securityOldExchangeRate(MonetaryUtil.getInstance().getExchangeRateAge());
                } else {
                    generateRequest();
                }
            }
        });

        // Action when clicked on "manage Channels" button
        mBtnManageChannels.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                Intent intent = new Intent(getActivity(), ManageChannelsActivity.class);
                startActivity(intent);
                dismiss();
            }
        });


        // Action when clicked on receive unit
        LinearLayout llUnit = view.findViewById(R.id.receiveUnitLayout);
        llUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEtAmount.getText().toString().equals(".")) {
                    mEtAmount.setText("");
                }

                if (!mUseValueBeforeUnitSwitch) {
                    mValueBeforeUnitSwitch = mEtAmount.getText().toString();
                }

                String convertedAmount = MonetaryUtil.getInstance().convertPrimaryToSecondaryCurrency(mEtAmount.getText().toString());
                MonetaryUtil.getInstance().switchCurrencies();
                if (mUseValueBeforeUnitSwitch) {
                    mEtAmount.setText(mValueBeforeUnitSwitch);
                    mUseValueBeforeUnitSwitch = false;
                } else {
                    mEtAmount.setText(convertedAmount);
                    mUseValueBeforeUnitSwitch = true;
                }
                mTvUnit.setText(MonetaryUtil.getInstance().getPrimaryDisplayUnit());

            }
        });


        // Input validation for the amount field.
        mEtAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {

                // remove the last inputted character if not valid
                if (!mAmountValid) {
                    deleteAmountInput();
                }

                // make text red if input is too large
                if (mOnChain) {
                    // always make it white, we have no limit for on-chain
                    mEtAmount.setTextColor(getResources().getColor(R.color.white));
                    mUseValueBeforeUnitSwitch = false;
                } else {
                    long maxReceivable;
                    mUseValueBeforeUnitSwitch = false;
                    if (PrefsUtil.isWalletSetup()) {
                        maxReceivable = Wallet.getInstance().getMaxChannelRemoteBalance();
                    } else {
                        maxReceivable = 500000000000L;
                    }
                    if (!mEtAmount.getText().toString().equals(".")) {
                        long currentValue = Long.parseLong(MonetaryUtil.getInstance().convertPrimaryToSatoshi(mEtAmount.getText().toString()));
                        if (currentValue > maxReceivable) {
                            mEtAmount.setTextColor(getResources().getColor(R.color.superRed));
                            String maxAmount = getResources().getString(R.string.max_amount) + " " + MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(maxReceivable);
                            Toast.makeText(getActivity(), maxAmount, Toast.LENGTH_SHORT).show();
                            mBtnNext.setEnabled(false);
                            mBtnNext.setTextColor(getResources().getColor(R.color.gray));
                        } else if (currentValue == 0) {
                            // Disable 0 sat ln invoices
                            mBtnNext.setEnabled(false);
                            mBtnNext.setTextColor(getResources().getColor(R.color.gray));
                        } else {
                            mEtAmount.setTextColor(getResources().getColor(R.color.white));
                            mBtnNext.setEnabled(true);
                            mBtnNext.setTextColor(getResources().getColor(R.color.lightningOrange));
                        }
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before,
                                      int count) {
                if (arg0.length() == 0) {
                    // No entered text so will show hint
                    mEtAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                } else {
                    mEtAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                }

                // validate input
                mAmountValid = MonetaryUtil.getInstance().validateCurrencyInput(arg0.toString(), MonetaryUtil.getInstance().getPrimaryCurrency());
            }
        });


        return view;
    }

    @Override
    public int getTheme() {
        return R.style.ZapBottomSheetDialogTheme;
    }

    private void deleteAmountInput() {
        boolean selection = mEtAmount.getSelectionStart() != mEtAmount.getSelectionEnd();

        int start = Math.max(mEtAmount.getSelectionStart(), 0);
        int end = Math.max(mEtAmount.getSelectionEnd(), 0);

        String before = mEtAmount.getText().toString().substring(0, start);
        String after = mEtAmount.getText().toString().substring(end);

        if (selection) {
            String outputText = before + after;
            mEtAmount.setText(outputText);
            mEtAmount.setSelection(start);
        } else {
            if (before.length() >= 1) {
                String newBefore = before.substring(0, before.length() - 1);
                String outputText = newBefore + after;
                mEtAmount.setText(outputText);
                mEtAmount.setSelection(start - 1);
            }
        }
    }

    private void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    private void generateRequest() {
        if (PrefsUtil.isWalletSetup()) {
            // The wallet is setup. Communicate with LND and generate the request.
            if (mOnChain) {

                // generate onChain request

                int addressType;
                if (PrefsUtil.getPrefs().getString("btcAddressType", "p2psh").equals("bech32")) {
                    addressType = 0;
                } else {
                    addressType = 1;
                }

                // non blocking stub
                LightningGrpc.LightningFutureStub asyncAddressClient = LightningGrpc
                        .newFutureStub(LndConnection.getInstance().getSecureChannel())
                        .withCallCredentials(LndConnection.getInstance().getMacaroon());

                NewAddressRequest asyncNewAddressRequest = NewAddressRequest.newBuilder()
                        .setTypeValue(addressType) // 0 = bech32 (native segwit) , 1 = Segwit compatibility address
                        .build();

                final ListenableFuture<NewAddressResponse> addressFuture = asyncAddressClient.newAddress(asyncNewAddressRequest);

                ZapLog.debug(LOG_TAG, "OnChain generating...");

                addressFuture.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            NewAddressResponse addressResponse = addressFuture.get();
                            ZapLog.debug(LOG_TAG, addressResponse.toString());

                            String value;
                            if (mUseValueBeforeUnitSwitch) {
                                value = MonetaryUtil.getInstance().convertSecondaryToBitcoin(mValueBeforeUnitSwitch);
                            } else {
                                value = MonetaryUtil.getInstance().convertPrimaryToBitcoin(mEtAmount.getText().toString());
                            }

                            Intent intent = new Intent(getActivity(), GeneratedRequestActivity.class);
                            intent.putExtra("onChain", mOnChain);
                            intent.putExtra("address", addressResponse.getAddress());
                            intent.putExtra("amount", value);
                            intent.putExtra("memo", mEtMemo.getText().toString());
                            startActivity(intent);
                            dismiss();

                        } catch (InterruptedException e) {
                            ZapLog.debug(LOG_TAG, "Interrupted");
                            Toast.makeText(getActivity(), R.string.receive_generateRequest_failed, Toast.LENGTH_SHORT).show();
                        } catch (ExecutionException e) {
                            ZapLog.debug(LOG_TAG, "Exception in task");
                            Toast.makeText(getActivity(), R.string.receive_generateRequest_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new ExecuteOnCaller());

            } else {
                // generate lightning request

                // non blocking stub
                LightningGrpc.LightningFutureStub asyncInvoiceClient = LightningGrpc
                        .newFutureStub(LndConnection.getInstance().getSecureChannel())
                        .withCallCredentials(LndConnection.getInstance().getMacaroon());

                long value;
                if (mUseValueBeforeUnitSwitch) {
                    value = Long.parseLong(MonetaryUtil.getInstance().convertSecondaryToSatoshi(mValueBeforeUnitSwitch));
                } else {
                    value = Long.parseLong(MonetaryUtil.getInstance().convertPrimaryToSatoshi(mEtAmount.getText().toString()));
                }

                Invoice asyncInvoiceRequest = Invoice.newBuilder()
                        .setValue(value)
                        .setMemo(mEtMemo.getText().toString())
                        .setExpiry(Long.parseLong(PrefsUtil.getPrefs().getString("lightning_expiry", "86400"))) // in seconds
                        .build();

                final ListenableFuture<AddInvoiceResponse> invoiceFuture = asyncInvoiceClient.addInvoice(asyncInvoiceRequest);


                invoiceFuture.addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AddInvoiceResponse invoiceResponse = invoiceFuture.get();
                            ZapLog.debug(LOG_TAG, invoiceResponse.toString());

                            Intent intent = new Intent(getActivity(), GeneratedRequestActivity.class);
                            intent.putExtra("onChain", mOnChain);
                            intent.putExtra("lnInvoice", invoiceResponse.getPaymentRequest());
                            intent.putExtra("lnInvoiceAddIndex", invoiceResponse.getAddIndex());
                            startActivity(intent);
                            dismiss();

                        } catch (InterruptedException e) {
                            ZapLog.debug(LOG_TAG, "Interrupted");
                            Toast.makeText(getActivity(), R.string.receive_generateRequest_failed, Toast.LENGTH_SHORT).show();
                        } catch (ExecutionException e) {
                            ZapLog.debug(LOG_TAG, "Exception in task");
                            Toast.makeText(getActivity(), R.string.receive_generateRequest_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new ExecuteOnCaller());

            }
        } else {
            // The wallet is not setup. Show setup wallet message.
            Toast.makeText(getActivity(), R.string.demo_setupWalletFirst, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void guardianDialogConfirmed(String DialogName) {
        switch (DialogName) {
            case UserGuardian.OLD_EXCHANGE_RATE:
                generateRequest();
                break;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FrameLayout bottomSheet = getDialog().findViewById(R.id.design_bottom_sheet);
        mBehavior = BottomSheetBehavior.from(bottomSheet);


        mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }
}
