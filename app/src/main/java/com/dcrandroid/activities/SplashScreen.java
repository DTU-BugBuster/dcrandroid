package com.dcrandroid.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.dcrandroid.MainActivity;
import com.dcrandroid.MainApplication;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.dialog.InfoDialog;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.PreferenceUtil;

import java.io.File;

import mobilewallet.LibWallet;

/**
 * Created by Macsleven on 24/12/2017.
 */

public class SplashScreen extends AppCompatActivity implements Animation.AnimationListener {

    private ImageView imgAnim;
    private PreferenceUtil util;
    private TextView tvLoading;
    private Thread loadThread;
    private DcrConstants constants;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!isTaskRoot()){
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if(intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction.equals(Intent.ACTION_MAIN)){
                finish();
                return;
            }
        }
        util = new PreferenceUtil(this);
        setContentView(R.layout.splash_page);
        imgAnim = findViewById(R.id.splashscreen_icon);

        imgAnim.setOnClickListener(new DoubleClickListener() {
            @Override
            public void onSingleClick(View v) {

            }
            @Override
            public void onDoubleClick(View v) {
                if(loadThread != null) {
                    loadThread.interrupt();
                }
                Intent intent = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivityForResult(intent,2);
            }
        });

        imgAnim.setBackgroundResource(R.drawable.load_animation);
        imgAnim.post(new Runnable() {
            @Override
            public void run() {
                AnimationDrawable loadAnimation = (AnimationDrawable) imgAnim.getBackground();
                loadAnimation.start();
            }
        });

        tvLoading = findViewById(R.id.loading_status);
        startup();
    }

    private void startup(){
        MainApplication application = (MainApplication) getApplication();

        constants = DcrConstants.getInstance();

        String netType = application.isTestNet() ? "testnet3" : "mainnet";

        String homeDir = getFilesDir()+"/wallet";

        constants.wallet = new LibWallet(homeDir, Constants.BADGER_DB, netType);
        constants.wallet.setLogLevel(util.get(Constants.LOGGING_LEVEL));
        constants.wallet.initLoader();

        String walletDB;

        if(application.isTestNet()){
            walletDB = "/testnet3/wallet.db";
        }else{
            walletDB = "/mainnet/wallet.db";
        }

        System.out.println("net type: "+ netType+ " db: "+walletDB);

        File f = new File(homeDir, walletDB);
        if(!f.exists()){
            System.out.println("Doesn't exists: "+ f.getAbsolutePath());
            loadThread = new Thread(){
                public void run(){
                    try{
                        sleep(3000);
                        createWallet();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            };
            loadThread.start();
        }else{
            load();
        }
    }

    private void setText(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLoading.setText(str);
            }
        });
    }

    private void createWallet(){
        Intent i = new Intent(SplashScreen.this, SetupWalletActivity.class);
        startActivity(i);
        finish();
    }

    public void load(){
        loadThread = new Thread(){
            public void run() {
                try {
                    setText(getString(R.string.opening_wallet));
                    constants.wallet.openWallet();
                    Intent i = new Intent(SplashScreen.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    //Finish all the activities before this
                    ActivityCompat.finishAffinity(SplashScreen.this);
                }catch (final Exception e){
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            InfoDialog infoDialog = new InfoDialog(SplashScreen.this)
                                    .setDialogTitle(getString(R.string.failed_to_open_wallet))
                                    .setMessage(e.getMessage())
                                    .setIcon(R.drawable.np_amount_withdrawal) //Temporary Icon
                                    .setPositiveButton(getString(R.string.exit_cap), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    });
                            infoDialog.setCancelable(false);
                            infoDialog.setCanceledOnTouchOutside(false);
                            infoDialog.show();

                        }
                    });
                    //System.out.println("Restoring Wallet");
                    //Utils.restoreWalletDB(SplashScreen.this);
                    //load();
                }
            }};
        loadThread.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 2){
            startup();
        }
    }

    @Override
    public void onBackPressed() {}

    @Override
    public void onAnimationStart(Animation animation) {}

    @Override
    public void onAnimationEnd(Animation animation) {}

    @Override
    public void onAnimationRepeat(Animation animation) {}

    public abstract class DoubleClickListener implements View.OnClickListener {

        private static final long DOUBLE_CLICK_TIME_DELTA = 300;//milliseconds

        long lastClickTime = 0;

        @Override
        public void onClick(View v) {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA){
                onDoubleClick(v);
                lastClickTime = 0;
            } else {
                onSingleClick(v);
            }
            lastClickTime = clickTime;
        }

        public abstract void onSingleClick(View v);
        public abstract void onDoubleClick(View v);
    }
}