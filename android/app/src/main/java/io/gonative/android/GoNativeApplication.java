package io.gonative.android;

import android.app.Application;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.ValueCallback;
import android.widget.Toast;

import com.adpdigital.push.AdpPushClient;
import com.adpdigital.push.ChabokNotification;
import com.adpdigital.push.NotificationHandler;
import com.adpdigital.push.PushMessage;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.onesignal.OSSubscriptionObserver;
import com.onesignal.OSSubscriptionState;
import com.onesignal.OSSubscriptionStateChanges;
import com.onesignal.OneSignal;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.gonative.android.library.AppConfig;

/**
 * Created by weiyin on 9/2/15.
 * Copyright 2014 GoNative.io LLC
 */
public class GoNativeApplication extends Application {
    private LoginManager loginManager;
    private RegistrationManager registrationManager;
    private WebViewPool webViewPool;
    private Message webviewMessage;
    private ValueCallback webviewValueCallback;
    private boolean oneSignalRegistered = false;
    private int numOneSignalChecks = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final String TAG = this.getClass().getName();
    private AdpPushClient chabok = null;

    @Override
    public void onCreate() {
        super.onCreate();

        initPushClient();

        String userId = chabok.getUserId();
        if (userId != null && !userId.isEmpty()) {
            chabok.register(userId);
        } else {
            chabok.register("Chabok-Starter-GoNative");
        }

        AppConfig appConfig = AppConfig.getInstance(this);
        if (appConfig.configError != null) {
            Toast.makeText(this, "Invalid appConfig json", Toast.LENGTH_LONG).show();
        }

        if (appConfig.oneSignalEnabled) {
            OneSignal.setRequiresUserPrivacyConsent(appConfig.oneSignalRequiresUserPrivacyConsent);
            OneSignal.init(this, "REMOTE", appConfig.oneSignalAppId,
                    new OneSignalNotificationHandler(this));
            OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);
        }

        this.loginManager = new LoginManager(this);

        if (appConfig.registrationEndpoints != null) {
            this.registrationManager = new RegistrationManager(this);
            registrationManager.processConfig(appConfig.registrationEndpoints);

            if (appConfig.oneSignalEnabled) {
                OneSignal.addSubscriptionObserver(new OSSubscriptionObserver() {
                    @Override
                    public void onOSSubscriptionChanged(OSSubscriptionStateChanges stateChanges) {
                        OSSubscriptionState to = stateChanges.getTo();
                        registrationManager.setOneSignalUserId(to.getUserId(), to.getPushToken(), to.getSubscribed());

                        if (to.getSubscribed()) {
                            oneSignalRegistered = true;
                        }
                    }
                });

                // sometimes the subscription observer doesn't get fired, so check a few times on a timer
                final Runnable checkOneSignal = new Runnable() {
                    @Override
                    public void run() {
                        if (oneSignalRegistered) {
                            scheduler.shutdown();
                            return;
                        }

                        OSSubscriptionState state = OneSignal.getPermissionSubscriptionState().getSubscriptionStatus();
                        registrationManager.setOneSignalUserId(state.getUserId(), state.getPushToken(), state.getSubscribed());

                        if (state.getSubscribed()) {
                            scheduler.shutdown();
                            oneSignalRegistered = true;

                        } else if (numOneSignalChecks++ > 10) {
                            scheduler.shutdown();
                        }
                    }
                };
                scheduler.scheduleAtFixedRate(checkOneSignal, 2, 2, TimeUnit.SECONDS);
            }
        }

        // some global webview setup
        WebViewSetup.setupWebviewGlobals(this);

        webViewPool = new WebViewPool();

        Iconify.with(new FontAwesomeModule());
    }

    public LoginManager getLoginManager() {
        return loginManager;
    }

    public RegistrationManager getRegistrationManager() {
        return registrationManager;
    }

    public WebViewPool getWebViewPool() {
        return webViewPool;
    }

    public Message getWebviewMessage() {
        return webviewMessage;
    }

    public void setWebviewMessage(Message webviewMessage) {
        this.webviewMessage = webviewMessage;
    }

    // Needed for Crosswalk
    @SuppressWarnings("unused")
    public ValueCallback getWebviewValueCallback() {
        return webviewValueCallback;
    }

    public void setWebviewValueCallback(ValueCallback webviewValueCallback) {
        this.webviewValueCallback = webviewValueCallback;
    }

    private synchronized void initPushClient() {
        if (chabok == null) {
            chabok = AdpPushClient.init(
                    getApplicationContext(),
                    MainActivity.class,
                    "chabok-starter/839879285435",
                    "70df4ae2e1fd03518ce3e3b21ee7ca7943577749",
                    "chabok-starter",
                    "chabok-starter"
            );
            chabok.setDevelopment(true);
            chabok.addListener(this);
            chabok.addNotificationHandler(getNotificationHandler());
        }
    }

    private NotificationHandler getNotificationHandler(){
        return new NotificationHandler(){

            @Override
            public Class getActivityClass(ChabokNotification chabokNotification) {
                // return preferred activity class to be opened on this message's notification
                return MainActivity.class;
            }

            @Override
            public boolean buildNotification(ChabokNotification chabokNotification, NotificationCompat.Builder builder) {
                // use builder to customize the notification object
                // return false to prevent this notification to be shown to the user, otherwise true
                getDataFromChabokNotification(chabokNotification);
                return true;
            }
        };
    }

    private void getDataFromChabokNotification(ChabokNotification chabokNotification) {
        if (chabokNotification != null) {
            if (chabokNotification.getExtras() != null) {
                Bundle payload = chabokNotification.getExtras();

                //FCM message data is here
                Object data = payload.get("data");
                if (data != null) {
                    Log.d(TAG, "getDataFromChabokNotification: The ChabokNotification data is : " + String.valueOf(data));
                }
            } else if (chabokNotification.getMessage() != null) {
                PushMessage payload = chabokNotification.getMessage();

                //Chabok message data is here
                JSONObject data = payload.getData();
                if (data != null) {
                    Log.d(TAG, "getDataFromChabokNotification: The ChabokNotification data is : " + data);
                }
            }
        }
    }

    public void onEvent(PushMessage message) {
        Log.d(TAG, "\n\n--------------------\n\nGOT MESSAGE " + message + "\n\n");
        JSONObject data = message.getData();
        if (data != null){
            Log.d(TAG, "--------------------\n\n The message data is : " + data + "\n\n");
        }
    }

    @Override
    public void onTerminate() {
        if (chabok != null)
            chabok.dismiss();

        super.onTerminate();
    }

}
