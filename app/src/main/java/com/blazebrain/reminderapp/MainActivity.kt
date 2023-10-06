package com.blazebrain.reminderapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.blazebrain.reminderapp.databinding.ActivityMainBinding
import com.blazebrain.reminderapp.util.ReminderWorker
import com.blazebrain.reminderapp.util.isPermissionGranted
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding get() = _binding!!

    private val permissionResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permission ->
        // Handle Permission granted/rejected
        /* no op*/
        if(!permission){
            Toast.makeText(this, "Please allow permission to get notification", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkPermission() {
        if (!isPermissionGranted(this)) {
              permissionResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            return
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val today = Calendar.getInstance()
        // 1 Create Variables to hold user's selection
        var chosenYear = 0
        var chosenMonth = 0
        var chosenDay = 0
        var chosenHour = 0
        var chosenMin = 0

        checkPermission()
        binding.btnSelectDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                // on below line we are passing context.
                this,
                { _, year, monthOfYear, dayOfMonth ->
                    // on below line we are setting
                    // date to our text view.
                    chosenYear = year
                    chosenMonth = monthOfYear
                    chosenDay = dayOfMonth
                },
                // on below line we are passing year, month
                // and day for the selected date in our date picker.
                today.get(Calendar.YEAR), today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            )
            // at last we are calling show
            // to display our date picker dialog.
            datePickerDialog.show()
        }
        // 4 Add the Listener to gain access to user selection in the TimePicker and
        // then assign the selected values to the variables created above
        binding.timePicker.setOnTimeChangedListener { _, hour, minute ->
            chosenHour = hour
            chosenMin = minute
        }

        // 5 Add the Listener to listen to click events and execute the code to setNotification
        binding.btnSet.setOnClickListener {
            if (chosenYear == 0) {
                Toast.makeText(this, "Please select date", Toast.LENGTH_SHORT).show()
            } else if (binding.etMsg.text.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter message", Toast.LENGTH_SHORT).show()
            } else {
                val userSelectedDateTime = Calendar.getInstance()
                userSelectedDateTime.set(chosenYear, chosenMonth, chosenDay, chosenHour, chosenMin)
                val todayDateTime = Calendar.getInstance()
                val delayInSeconds =
                    (userSelectedDateTime.timeInMillis / 1000L) - (todayDateTime.timeInMillis / 1000L)
                createWorkRequest(binding.etMsg.text.toString(), delayInSeconds)
                Toast.makeText(this, "Reminder set", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Private Function to create the OneTimeWorkRequest
    private fun createWorkRequest(message: String, timeDelayInSeconds: Long) {
        val myWorkRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(timeDelayInSeconds, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    "title" to "Reminder",
                    "message" to message,
                )
            )
            .build()

        WorkManager.getInstance(this).enqueue(myWorkRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}