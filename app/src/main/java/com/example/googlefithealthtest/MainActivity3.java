package com.example.googlefithealthtest;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import org.jetbrains.annotations.Nullable;


public class MainActivity3 extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    //    RecordingApi
    private Button mCancelSubscriptionsBtn;
    private Button mShowSubscriptionsBtn;

    //    HistoryApi
    private Button mButtonViewWeek;
    private Button mButtonViewToday;
    private Button mButtonAddSteps;
    private Button mButtonUpdateSteps;
    private Button mButtonDeleteSteps;

    private ResultCallback<Status> mSubscribeResultCallback;
    private ResultCallback<Status> mCancelSubscriptionResultCallback;
    private ResultCallback<ListSubscriptionsResult> mListSubscriptionsResultCallback;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initCallbacks();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.RECORDING_API)
                .addApi(Fitness.HISTORY_API)
//                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .build();
    }

    private void initViews() {
//        RecordingApi
        mCancelSubscriptionsBtn = (Button) findViewById(R.id.btn_cancel_subscriptions);
        mShowSubscriptionsBtn = (Button) findViewById(R.id.btn_show_subscriptions);

        mCancelSubscriptionsBtn.setOnClickListener(this);
        mShowSubscriptionsBtn.setOnClickListener(this);


//        HistoryApi
        mButtonViewWeek = (Button) findViewById(R.id.btn_view_week);
        mButtonViewToday = (Button) findViewById(R.id.btn_view_today);
        mButtonAddSteps = (Button) findViewById(R.id.btn_add_steps);
        mButtonUpdateSteps = (Button) findViewById(R.id.btn_update_steps);
        mButtonDeleteSteps = (Button) findViewById(R.id.btn_delete_steps);

        mButtonViewWeek.setOnClickListener(this);
        mButtonViewToday.setOnClickListener(this);
        mButtonAddSteps.setOnClickListener(this);
        mButtonUpdateSteps.setOnClickListener(this);
        mButtonDeleteSteps.setOnClickListener(this);
    }

    private void initCallbacks() {
        mSubscribeResultCallback = new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                        Log.e("RecordingAPI", "Already subscribed to the Recording API");
                    } else {
                        Log.e("RecordingAPI", "Subscribed to the Recording API");
                    }
                }
            }
        };

        mCancelSubscriptionResultCallback = new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Log.e("RecordingAPI", "Canceled subscriptions!");
                } else {
                    // Subscription not removed
                    Log.e("RecordingAPI", "Failed to cancel subscriptions");
                }
            }
        };

        mListSubscriptionsResultCallback = new ResultCallback<ListSubscriptionsResult>() {
            @Override
            public void onResult(@NonNull ListSubscriptionsResult listSubscriptionsResult) {
                for (Subscription subscription : listSubscriptionsResult.getSubscriptions()) {
                    DataType dataType = subscription.getDataType();
                    Log.e("RecordingAPI", dataType.getName());
                    for (Field field : dataType.getFields()) {
                        Log.e("RecordingAPI", field.toString() + " = ");
                    }
                }
            }
        };
    }

    private void showSubscriptions() {
        Fitness.RecordingApi.listSubscriptions(mGoogleApiClient)
                .setResultCallback(mListSubscriptionsResultCallback);
    }

    private void cancelSubscriptions() {
        Fitness.RecordingApi.unsubscribe(mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(mCancelSubscriptionResultCallback);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_view_today:{
                showToday();
            }
            case R.id.btn_cancel_subscriptions: {
                cancelSubscriptions();
                break;
            }
            case R.id.btn_show_subscriptions: {
                showSubscriptions();
                break;
            }
        }
    }

    private void showToday() {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("RecordingAPI", "onConnected");
        Fitness.RecordingApi.subscribe(mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(mSubscribeResultCallback);

        Log.e("HistoryAPI", "onConnected");

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("HistoryAPI", "onConnectionSuspended");

        Log.e("RecordingAPI", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("HistoryAPI", "onConnectionFailed");

        Log.e("RecordingAPI", "onConnectionFailed");
    }

}
