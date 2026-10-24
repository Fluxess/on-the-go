package ru.routealert.app.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.routealert.app.R
import ru.routealert.app.data.RoutePointRepository
import ru.routealert.app.service.TripTrackingService
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.RouteAlertTheme
import ru.routealert.app.ui.theme.liquidGlass

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOn()

        val pointId = intent.getLongExtra(AlertExtras.EXTRA_POINT_ID, -1L)
        val pointName = intent.getStringExtra(AlertExtras.EXTRA_POINT_NAME).orEmpty().ifBlank { "Точка" }
        val distance = intent.getFloatExtra(AlertExtras.EXTRA_DISTANCE_M, 0f)

        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            IntentFilter(ACTION_CLOSE_ALARM),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        startAlarmSoundAndVibration()

        setContent {
            RouteAlertTheme {
                var closing by remember { mutableStateOf(false) }
                DisposableEffect(Unit) {
                    onDispose { stopAlarmSoundAndVibration() }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1A1010), Color(0xFF0B0B0D), Color(0xFF141018))
                            )
                        )
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = pointName,
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = NotificationHelper.formatDistance(distance),
                        color = Color(0xFFB5B5B0),
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(36.dp))
                    // Clean bell icon without frames
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .liquidGlass(shape = CircleShape, colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.06f)
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            painter = painterResource(R.drawable.ic_alarm_bell),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Box(
                        modifier = Modifier
                            .size(168.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                colors = Glass.Danger,
                                stroke = Color.White.copy(alpha = 0.45f)
                            )
                            .clickable {
                                if (closing) return@clickable
                                closing = true
                                if (pointId >= 0) {
                                    NotificationHelper(this@AlarmActivity).cancelPointNotification(pointId)
                                    TripTrackingService.markTriggered(pointId)
                                }
                                finish()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Стоп", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(20.dp))
                    TextButton(
                        onClick = {
                            if (closing) return@TextButton
                            closing = true
                            if (pointId >= 0) {
                                NotificationHelper(this@AlarmActivity).cancelPointNotification(pointId)
                                TripTrackingService.snooze(pointId, AlertExtras.SNOOZE_MINUTES)
                            }
                            finish()
                        }
                    ) {
                        Text(
                            "Отложить на ${AlertExtras.SNOOZE_MINUTES} мин",
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            if (closing) return@TextButton
                            closing = true
                            if (pointId >= 0) {
                                NotificationHelper(this@AlarmActivity).cancelPointNotification(pointId)
                                TripTrackingService.markTriggered(pointId)
                                lifecycleScope.launch(Dispatchers.IO) {
                                    RoutePointRepository.get(this@AlarmActivity).setEnabled(pointId, false)
                                }
                            }
                            finish()
                        }
                    ) {
                        Text(
                            "Убрать точку",
                            color = Color(0xFFFF6B6B),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    private fun turnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun startAlarmSoundAndVibration() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmActivity, uri)
            isLooping = true
            prepare()
            start()
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VibratorManager::class.java)
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 600, 400, 600, 400, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarmSoundAndVibration() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAlarmSoundAndVibration()
        runCatching { unregisterReceiver(closeReceiver) }
        super.onDestroy()
    }

    companion object {
        const val ACTION_CLOSE_ALARM = "ru.routealert.app.action.CLOSE_ALARM"
    }
}
