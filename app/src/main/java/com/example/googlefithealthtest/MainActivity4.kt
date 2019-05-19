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
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.*
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType.*
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main4.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity4 : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {
    private lateinit var sensorClient: SensorsClient
    private lateinit var recordingClient: RecordingClient
    private lateinit var historyClient: HistoryClient
    private var lastSignedInAccount: GoogleSignInAccount? = null
    private val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE: Int = 1500
    private val REQUEST_OAUTH = 1001
    private lateinit var mClient: GoogleApiClient
    private var expendedCalories: Float = 0F
    private var total: Int = 0
    // Date format
    private val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale("tr", "TR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main4)

        lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)

        buildFitnessClient()

    }

    private fun checkPermissions() {
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
            .addDataType(TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .build()


        if (!GoogleSignIn.hasPermissions(lastSignedInAccount, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                lastSignedInAccount,
                fitnessOptions
            )
        } else {
            accessGoogleFitData()
        }
    }

    private fun buildFitnessClient() {
        checkPermissions()
    }

    override fun onConnected(connectionHint: Bundle?) {
        Toast.makeText(baseContext, "connected: $connectionHint", Toast.LENGTH_SHORT).show()

        // selectedDate in format yyyy-MM-dd
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

    override fun onConnectionFailed(result: ConnectionResult) {
        Toast.makeText(baseContext, "suspended: ${result.errorCode} : ${result.errorMessage}", Toast.LENGTH_SHORT)
            .show()
        // Error while connecting. Try to resolve using the pending intent returned.
        if (result.errorCode == FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS || result.hasResolution()) {
            try {
                result.startResolutionForResult(this, REQUEST_OAUTH)
            } catch (e: IntentSender.SendIntentException) {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
//                fetchUserGoogleFitData("2019-05-17")
                accessGoogleFitData()
            }
            if (requestCode == REQUEST_OAUTH) {
                mClient.connect()
            }
        }
    }


    private fun accessGoogleFitData() {

        lastSignedInAccount?.let { lastSign ->
            recordingClient = Fitness.getRecordingClient(this, lastSign)
            sensorClient = Fitness.getSensorsClient(this, lastSign)
            historyClient = Fitness.getHistoryClient(this, lastSign)

            val dataSourceRequest = DataSourcesRequest.Builder()
                .setDataTypes(TYPE_STEP_COUNT_DELTA)
                .setDataSourceTypes(DataSource.TYPE_DERIVED)
                .build()


            sensorClient
                .findDataSources(dataSourceRequest)
                .addOnSuccessListener { datasources ->
                    datasources.forEach { dataSource ->
                        Log.w("SensorAPI", dataSource.dataType.name + " = " + dataSource.streamName)

                        subscribeRecording(dataSource)
                        readHistoryClient(dataSource)
                    }
                }
                .addOnCompleteListener {
                    Log.w("checkAPI", "Chgecking subscribed List")

                    recordingClient
                        .listSubscriptions()
                        .addOnCompleteListener {
                            it.result
                                ?.forEach { subscription ->
                                    Log.i(
                                        "RecordingAPI list",
                                        subscription.toDebugString() + " = " + subscription.dataSource
                                    )
                                    subscription.dataSource?.let { dataSource ->
                                        Log.w(
                                            "RecordingAPI list",
                                            dataSource.dataType.name + " = " + dataSource.streamName
                                        )
                                    }
                                }
                        }


                }
                .addOnFailureListener {
                    Log.e("SensorAPI", "Error: ", it)
                }
        }
    }

    private fun readHistoryClient(dataSource: DataSource): Task<DataReadResponse> {
//        val readRequest = queryDateFitnessData()

//        var d1: Date? = null
//        try {
//            d1 = originalFormat.parse("2019-05-19")
//        } catch (ignored: Exception) {
//        }

        val calendar = Calendar.getInstance()

//        try {
//            calendar.time = d1
//        } catch (e: Exception) {
        calendar.time = Date()
//        }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy")

        date.text = dateFormat.format(calendar.time)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day_of_Month = calendar.get(Calendar.DAY_OF_MONTH)
        val startCalendar = Calendar.getInstance(Locale.getDefault())
        val endTime = getEndTime(startCalendar, year, month, day_of_Month)
        val startTime = getStartTime(startCalendar)
        val readRequest = DataReadRequest.Builder()
            .read(dataSource.dataType)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .bucketByTime(1, TimeUnit.DAYS)
            .enableServerQueries()
            .build()

        return historyClient
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                Log.w(
                    "HistoryAPI",
                    dataReadResponse.status.statusMessage + " = dataset size: " + dataReadResponse.dataSets.size + " = bucket size: " + dataReadResponse.buckets.size
                )
                dataReadResponse.buckets.forEach { bucket ->
                    val dataSetx = bucket.dataSets

                    Log.i(
                        "HistoryAPI",
                        bucket.activity + " = dataset size: " + dataSetx.size
                    )
                    calculateSteps(dataSetx)

//                    calculateStepActionCalories(bucket)
                }
            }
            .addOnFailureListener {
                Log.e("HistoryAPI", "onException()", it)
            }
            .addOnCompleteListener {
                Log.i("HistoryAPI", "History Alma işlemi tamamlandı! Atılan adım sayısı:" + total)
                stepCount.text = total.toString()
            }
    }

    private fun subscribeRecording(dataSource: DataSource) {
        recordingClient
            .subscribe(dataSource.dataType)
            .addOnSuccessListener {
                Log.i("RecordingAPI", "Subscribed to the Recording API for " + TYPE_STEP_COUNT_DELTA.name)
            }
            .addOnFailureListener {
                Log.e("RecordingAPI", it.message)
            }


    }

//    private fun queryDateFitnessData(): DataReadRequest {
//
////        val startCalendar = Calendar.getInstance(Locale.getDefault())
////        val endTime = getEndTime(startCalendar, year, month, day_of_Month)
////        val startTime = getStartTime(startCalendar)
////
////        return DataReadRequest.Builder()
////            .aggregate(TYPE_CALORIES_EXPENDED, AGGREGATE_CALORIES_EXPENDED)
////            .aggregate(TYPE_STEP_COUNT_DELTA, AGGREGATE_STEP_COUNT_DELTA)
////            .bucketByActivitySegment(1, TimeUnit.MILLISECONDS)
////            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
////            .build()
//
//    }

    //get first hour in given calendar date
    private fun getStartTime(startCalendar: Calendar): Long {
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)
        return startCalendar.timeInMillis
    }

    //get last hour in given calendar date
    private fun getEndTime(
        startCalendar: Calendar,
        year: Int,
        month: Int,
        day_of_Month: Int
    ): Long {

        startCalendar.set(Calendar.YEAR, year)
        startCalendar.set(Calendar.MONTH, month)
        startCalendar.set(Calendar.DAY_OF_MONTH, day_of_Month)
        startCalendar.set(Calendar.HOUR_OF_DAY, 23)
        startCalendar.set(Calendar.MINUTE, 59)
        startCalendar.set(Calendar.SECOND, 59)
        startCalendar.set(Calendar.MILLISECOND, 999)
        return startCalendar.timeInMillis
    }

    private fun calculateSteps(dataSetx: MutableList<DataSet>) {
        dataSetx.forEach { dataSet ->
            Log.i(
                "HistoryAPI",
                dataSet.dataType.name + " = dataPoint size: " + dataSet.dataPoints.size
            )
            if (dataSet.dataType.name == "com.google.step_count.delta") {
                if (dataSet.dataPoints.size > 0) {
                    Log.i(
                        "HistoryAPI",
                        "step = " + dataSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
                    )
                    // total steps
                    total += dataSet.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()

                }
            }
        }
    }

//    private fun calculateStepActionCalories(bucket: Bucket) {
//        val bucketActivity = bucket.activity
////        if (bucketActivity.contains(FitnessActivities.WALKING) || bucketActivity.contains(FitnessActivities.RUNNING)) {
//        val dataSets = bucket.dataSets
//        Log.i(
//            "HistoryAPI",
//            "calories = dataPoint size: " + dataSets.size
//        )
//        dataSets.forEach { dataSet ->
//            Log.i(
//                "HistoryAPI",
//                dataSet.dataType.name + " = dataPoint size: " + dataSet.dataPoints.size
//            )
//            if (dataSet.dataType.name == "com.google.calories.expended") {
//                dataSet.dataPoints.forEach { dp ->
//                    Log.i(
//                        "HistoryAPI",
//                        "" + dp.dataType + " = dataField size: " + dp.dataType.fields.size
//                    )
//                    if (dp.getEndTime(TimeUnit.MILLISECONDS) > dp.getStartTime(TimeUnit.MILLISECONDS)) {
//                        dp.dataType.fields.forEach { field ->
//                            // total calories burned
//                            expendedCalories += dp.getValue(field).asFloat()
//                        }
//
//                    }
//                }
//
//            }
//        }
////        }
//    }


}
