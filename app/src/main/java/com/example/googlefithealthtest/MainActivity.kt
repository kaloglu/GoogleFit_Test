package com.example.googlefithealthtest

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_CUMULATIVE
import com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_DELTA
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.android.gms.fitness.result.DataSourcesResult
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, OnDataPointListener {
    private var authInProgress: Boolean = false
    private var lastSignedInAccount: GoogleSignInAccount? = null
    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE: Int = 1500
    private val REQUEST_OAUTH = 1001
    private lateinit var mClient: GoogleApiClient
    private lateinit var googleSignInAccount: GoogleSignInAccount
    private var expendedCalories: Float = 0F
    private var total: Int = 0
    // Date format
    private val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)
        buildFitnessClient()

    }

    private fun buildFitnessClient() {
//        checkPermissions()
        mClient = GoogleApiClient.Builder(this)
            .addApi(Fitness.SENSORS_API)  // Required for SensorsApi calls
            .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
            .addConnectionCallbacks(this)
            .enableAutoManage(this, 0, this)
            .build()
    }

    override fun onStart() {
        super.onStart()
        mClient.connect()
    }

    override fun onConnected(connectionHint: Bundle?) {
        val dataSourceRequest = DataSourcesRequest.Builder()
            .setDataTypes(
                TYPE_STEP_COUNT_CUMULATIVE, TYPE_STEP_COUNT_DELTA
            )
            .setDataSourceTypes(DataSource.TYPE_DERIVED)
            .build()

        val dataSourcesResultCallback =
            ResultCallback<DataSourcesResult> { dataSourcesResult ->
                for (dataSource in dataSourcesResult.dataSources) {
                    Log.d("GoogleFit", dataSource.dataType.name + " = " + dataSource.streamName)
                    registerFitnessDataListener(dataSource, dataSource.dataType)
                }
            }

        Fitness.SensorsApi
            .findDataSources(mClient, dataSourceRequest)
            .setResultCallback(dataSourcesResultCallback)
    }


    private fun registerFitnessDataListener(dataSource: DataSource, dataType: DataType) {

        val request = SensorRequest.Builder()
            .setDataSource(dataSource)
            .setDataType(dataType)
            .setSamplingRate(1, TimeUnit.SECONDS)
            .build()

        Fitness.SensorsApi.add(mClient, request, this)
            .setResultCallback { status ->
                if (status.isSuccess) {
                    Log.e("GoogleFit", "SensorApi successfull")
                } else {
                    Log.e("GoogleFit", "adding status: " + status.statusMessage!!)
                }
            }
    }


    override fun onDataPoint(dataPoint: DataPoint) {
        for (field in dataPoint.dataType.fields) {
            val value = dataPoint.getValue(field)
            Log.d("GoogleFit", "Field: " + field.name + " Value: " + value + "(${dataPoint.getTimestamp(TimeUnit.MINUTES)})")
            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    "Field: " + field.name + " Value: " + value + "(${dataPoint.getTimestamp(TimeUnit.MINUTES)})",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onConnectionSuspended(cause: Int) {
        // If your connection to the sensor gets lost at some point,
        // you'll be able to determine the reason and react to it here.
        when (cause) {
            GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST ->
                Toast.makeText(baseContext, "Connection lost.  Cause: Network Lost.", Toast.LENGTH_SHORT).show()
            GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED ->
                Toast.makeText(baseContext, "Connection lost.  Reason: Service Disconnected", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (!authInProgress) {
            try {
                authInProgress = true
                connectionResult.startResolutionForResult(this@MainActivity, REQUEST_OAUTH)
            } catch (e: IntentSender.SendIntentException) {

            }

        } else {
            Log.e("GoogleFit", "authInProgress")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                if (!mClient.isConnecting && !mClient.isConnected) {
                    mClient.connect()
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.e("GoogleFit", "RESULT_CANCELED")
            }
        } else {
            Log.e("GoogleFit", "requestCode NOT request_oauth")
        }
    }


    private fun calculateSteps(dataSetx: MutableList<DataSet>) {
        dataSetx.forEach { dataSet ->
            if (dataSet.dataType.name == "com.google.step_count.delta") {
                if (dataSet.dataPoints.size > 0) {
                    // total steps
                    total += dataSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()

                }
            }
        }
    }

    private fun calculateStepActionCalories(bucket: Bucket) {
        val bucketActivity = bucket.activity
        if (bucketActivity.contains(FitnessActivities.WALKING) || bucketActivity.contains(FitnessActivities.RUNNING)) {
            val dataSets = bucket.dataSets
            dataSets.forEach { dataSet ->
                if (dataSet.dataType.name == "com.google.calories.expended") {
                    dataSet.dataPoints.forEach { dp ->
                        if (dp.getEndTime(TimeUnit.MILLISECONDS) > dp.getStartTime(TimeUnit.MILLISECONDS)) {
                            dp.dataType.fields.forEach { field ->
                                // total calories burned
                                expendedCalories += dp.getValue(field).asFloat()
                            }

                        }
                    }

                }
            }
        }
    }

}
