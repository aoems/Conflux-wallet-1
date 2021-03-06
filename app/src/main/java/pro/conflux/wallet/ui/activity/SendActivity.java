package pro.conflux.wallet.ui.activity;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import pro.conflux.wallet.C;
import pro.conflux.wallet.R;
import pro.conflux.wallet.base.BaseActivity;
import pro.conflux.wallet.entity.Address;
import pro.conflux.wallet.entity.ConfirmationType;
import pro.conflux.wallet.entity.ErrorEnvelope;
import pro.conflux.wallet.entity.GasSettings;
import pro.conflux.wallet.utils.BalanceUtils;
import pro.conflux.wallet.utils.ToastUtils;
import pro.conflux.wallet.view.ConfirmTransactionView;
import pro.conflux.wallet.view.InputPwdView;
import pro.conflux.wallet.viewmodel.ConfirmationViewModel;
import pro.conflux.wallet.viewmodel.ConfirmationViewModelFactory;

import org.cfx.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;


import butterknife.BindView;
import butterknife.OnClick;


public class SendActivity extends BaseActivity {

    ConfirmationViewModelFactory confirmationViewModelFactory;
    ConfirmationViewModel viewModel;

    @BindView(R.id.tv_title)
    TextView tvTitle;

    @BindView(R.id.iv_btn)
    ImageView ivBtn;
    @BindView(R.id.rl_btn)
    LinearLayout rlBtn;

    @BindView(R.id.et_transfer_address)
    EditText etTransferAddress;

    @BindView(R.id.send_amount)
    EditText amountText;

    @BindView(R.id.lly_contacts)
    LinearLayout llyContacts;

    @BindView(R.id.seekbar)
    SeekBar seekbar;

    @BindView(R.id.tv_gas_cost)
    TextView tvGasCost;

    @BindView(R.id.gas_price)
    TextView tvGasPrice;

    @BindView(R.id.lly_gas)
    LinearLayout llyGas;
    @BindView(R.id.et_hex_data)
    EditText etHexData;
    @BindView(R.id.lly_advance_param)
    LinearLayout llyAdvanceParam;


    @BindView(R.id.advanced_switch)
    Switch advancedSwitch;

    @BindView(R.id.custom_gas_price)
    EditText customGasPrice;

    @BindView(R.id.custom_gas_limit)
    EditText customGasLimit;


    private String walletAddr;
    private String contractAddress;
    private int decimals;
    private String balance;
    private String symbol;

    private String netCost;
    private  BigInteger gasPrice;
    private BigInteger gasLimit;


    private boolean sendingTokens = false;

    private Dialog dialog;

    private static final int QRCODE_SCANNER_REQUEST = 1100;

    private static final double miner_min = 1 ;
    private static final double miner_max = 20;

    private String scanResult;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_transfer;
    }

    @Override
    public void initToolBar() {
        ivBtn.setImageResource(R.drawable.ic_transfer_scanner);
        rlBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void initDatas() {


        Intent intent = getIntent();
        walletAddr = intent.getStringExtra(C.EXTRA_ADDRESS);

        contractAddress = intent.getStringExtra(C.EXTRA_CONTRACT_ADDRESS);

        decimals = intent.getIntExtra(C.EXTRA_DECIMALS, C.CFX_DECIMALS);
        symbol = intent.getStringExtra(C.EXTRA_SYMBOL);

        symbol = symbol == null ? C.CFX_SYMBOL : symbol.trim().toUpperCase();
        sendingTokens = symbol.equals(C.CFX_SYMBOL.trim().toUpperCase()) ? false :true;//判断是否为20token

        tvTitle.setText(symbol + getString(R.string.transfer_title));




        confirmationViewModelFactory = new ConfirmationViewModelFactory();
        viewModel = ViewModelProviders.of(this, confirmationViewModelFactory)
                .get(ConfirmationViewModel.class);

        viewModel.sendTransaction().observe(this, this::onTransaction);
        viewModel.gasSettings().observe(this, this::onGasSettings);
        viewModel.progress().observe(this, this::onProgress);
        viewModel.error().observe(this, this::onError);

        // 首页直接扫描进入
        scanResult = intent.getStringExtra("scan_result");
        if (!TextUtils.isEmpty(scanResult)) {
            parseScanResult(scanResult);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(this, sendingTokens? ConfirmationType.CFX: ConfirmationType.ERC20);
    }

    @Override
    public void configViews() {
        advancedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    llyAdvanceParam.setVisibility(View.VISIBLE);
                    llyGas.setVisibility(View.GONE);


                    BigDecimal value = Convert.fromWei(new BigDecimal(gasPrice), Convert.Unit.GWEI);
                    customGasPrice.setText(new DecimalFormat("0.0000000").format(value));
                    customGasLimit.setText(gasLimit.toString());

                } else {
                    llyAdvanceParam.setVisibility(View.GONE);
                    llyGas.setVisibility(View.VISIBLE);

                }
            }
        });


        final DecimalFormat gasformater = new DecimalFormat();
        //保留几位小数
        gasformater.setMaximumFractionDigits(2);
        //模式  四舍五入
        gasformater.setRoundingMode(RoundingMode.CEILING);


        final String cfxUnit = getString(R.string.transfer_cfx_unit);


        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                    double p = progress / 1000000f;
                    double d = (miner_max - miner_min) * p + 0.00001;

                   gasPrice = BalanceUtils.gweiToWei(BigDecimal.valueOf(d));
                   tvGasPrice.setText(new DecimalFormat("0.000000").format(d) + " " + C.GWEI_UNIT);

                updateNetworkFee();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekbar.setProgress(1);
        try {
            netCost = BalanceUtils.weiToCfx(gasPrice.multiply(gasLimit), 4) + cfxUnit;
        } catch (Exception e) {
        }

        customGasPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    return;
                }
                gasPrice = BalanceUtils.gweiToWei(new BigDecimal(s.toString()));

                try {
                    BigDecimal value = BalanceUtils.weiToCfx(gasPrice.multiply(gasLimit));

                    tvGasCost.setText(new DecimalFormat("0.00000000000").format(value)+ " " + cfxUnit);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        customGasLimit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                gasLimit = new BigInteger(s.toString());

                updateNetworkFee();
            }
        });
    }

    private void updateNetworkFee() {

        try {
            BigDecimal value = BalanceUtils.weiToCfx(gasPrice.multiply(gasLimit));

            tvGasCost.setText(new DecimalFormat("0.00000000000").format(value)+ " " + C.CFX_SYMBOL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void onGasSettings(GasSettings gasSettings) {
        gasPrice = gasSettings.gasPrice;
        gasLimit = gasSettings.gasLimit;

    }

    private boolean verifyInfo(String address, String amount) {

            try {
                new Address(address);
            } catch (Exception e) {
                ToastUtils.showToast(R.string.addr_error_tips);
                return false;
            }

            try {
                String wei = BalanceUtils.CfxToWei(amount);
                return wei != null;
            } catch (Exception e) {
                ToastUtils.showToast(R.string.amount_error_tips);

                return false;
            }
        }



    @OnClick({R.id.rl_btn, R.id.btn_next})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_btn:
                Intent intent = new Intent(SendActivity.this, QRCodeScannerActivity.class);
                startActivityForResult(intent, QRCODE_SCANNER_REQUEST);
                break;
            case R.id.btn_next:

                // confirm info;
                String toAddr = etTransferAddress.getText().toString().trim();
                String amount = amountText.getText().toString().trim();

                if (verifyInfo(toAddr, amount)) {
                    ConfirmTransactionView confirmView = new ConfirmTransactionView(this, this::onClick);
                    confirmView.fillInfo(walletAddr, toAddr, " - " + amount + " " +  symbol, netCost, gasPrice, gasLimit);

                    dialog = new BottomSheetDialog(this, R.style.BottomSheetDialog);
                    dialog.setContentView(confirmView);
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                }

                break;
            case R.id.confirm_button:
                // send
                dialog.hide();

                InputPwdView pwdView = new InputPwdView(this, pwd -> {
                    if (sendingTokens) {
                        viewModel.createTokenTransfer(pwd,
                                etTransferAddress.getText().toString().trim(),
                                contractAddress,
                                BalanceUtils.tokenToWei(new BigDecimal(amountText.getText().toString().trim()), decimals).toBigInteger(),
                                gasPrice,
                                gasLimit
                        );
                    } else {
                        viewModel.createTransaction(pwd, etTransferAddress.getText().toString().trim(),
                                Convert.toWei(amountText.getText().toString().trim(), Convert.Unit.ETHER).toBigInteger(),
                                gasPrice,
                                gasLimit );
                    }
                });

                dialog = new BottomSheetDialog(this);
                dialog.setContentView(pwdView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();

                break;
        }
    }


    private void onProgress(boolean shouldShowProgress) {
        hideDialog();
        if (shouldShowProgress) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_dialog_sending)
                    .setView(new ProgressBar(this))
                    .setCancelable(false)
                    .create();
            dialog.show();
        }
    }

    private void onError(ErrorEnvelope error) {
        hideDialog();
        String message = "";
        if(error.code==1){
            message = "错误！";
        }else{
            message = error.message;
        }
        System.out.println(error.toString());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.error_transaction_failed)
                .setMessage(message)
                .setPositiveButton(R.string.button_ok, (dialog1, id) -> {
                    // Do nothing
//                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//                    ClipData clip = ClipData.newPlainText("transaction hash", "");
//                    clipboard.setPrimaryClip(clip);
                    finish();
                })
                .create();
        dialog.show();
    }

    private void onTransaction(String hash) {
        hideDialog();

            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.transaction_succeeded)
                    .setMessage(hash)
                    .setPositiveButton(R.string.button_ok, (dialog1, id) -> {
                        finish();
                    })
                    .setNeutralButton(R.string.copy, (dialog1, id) -> {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("transaction hash", hash);
                        clipboard.setPrimaryClip(clip);
                        finish();
                    })
                    .create();
            dialog.show();


    }

    private void fillAddress(String addr) {
        try {
            new Address(addr);
            etTransferAddress.setText(addr);
        } catch (Exception e) {
            ToastUtils.showToast(R.string.addr_error_tips);
        }
    }

    private void parseScanResult(String result) {
        if (result.contains(":") && result.contains("?")) {  // 符合协议格式
            String[] urlParts = result.split(":");
            if (urlParts[0].equals("conflux")) {
                urlParts =  urlParts[1].split("\\?");

                fillAddress(urlParts[0]);

                // ?contractAddress=0xdxx & decimal=1 & value=100000
//                 String[] params = urlParts[1].split("&");
//                for (String param : params) {
//                    String[] keyValue = param.split("=");
//                }

            }


        } else {  // 无格式， 只有一个地址
            fillAddress(result);
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QRCODE_SCANNER_REQUEST) {
            if (data != null) {
                String scanResult = data.getStringExtra("scan_result");
                // 对扫描结果进行处理
                parseScanResult(scanResult);
//                ToastUtils.showLongToast(scanResult);
            }
        }
    }

}
