package com.izzan.sensoryshi

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izzan.sensoryshi.ui.theme.SensoryshiTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null

    // Sensor Value States for Compose UI
    private val luxValue = mutableFloatStateOf(0f)
    private val proximityValue = mutableFloatStateOf(0f)
    private val isProximityNear = mutableStateOf(false)
    private val accelX = mutableFloatStateOf(0f)
    private val accelY = mutableFloatStateOf(0f)
    private val accelZ = mutableFloatStateOf(0f)

    // Sensor availability flags
    private val hasLightSensor = mutableStateOf(false)
    private val hasProximitySensor = mutableStateOf(false)
    private val hasAccelerometer = mutableStateOf(false)

    // Lifecycle Status Indicator
    private val lifecycleStatus = mutableStateOf("INITIALIZED")

    // Sound Feedback Generator
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SensorManager safely
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Detect Available Sensors
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        hasLightSensor.value = lightSensor != null
        hasProximitySensor.value = proximitySensor != null
        hasAccelerometer.value = accelerometer != null

        initToneGenerator()

        setContent {
            SensoryshiTheme {
                MainScreen(
                    lux = luxValue.floatValue,
                    proximityDistance = proximityValue.floatValue,
                    isNear = isProximityNear.value,
                    accelX = accelX.floatValue,
                    accelY = accelY.floatValue,
                    accelZ = accelZ.floatValue,
                    hasLight = hasLightSensor.value,
                    hasProximity = hasProximitySensor.value,
                    hasAccel = hasAccelerometer.value,
                    lifecycleState = lifecycleStatus.value,
                    onPlaySoundClick = { playBeepSound() }
                )
            }
        }
    }

    private fun initToneGenerator() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            toneGenerator = null
        }
    }

    private fun playBeepSound() {
        try {
            initToneGenerator()
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // MANDATORY REQUIREMENT: Register sensors in onResume
    override fun onResume() {
        super.onResume()
        lifecycleStatus.value = "RESUMED (Sensor Aktif)"

        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    // MANDATORY REQUIREMENT: Unregister sensors in onPause
    override fun onPause() {
        super.onPause()
        lifecycleStatus.value = "PAUSED (Sensor Nonaktif)"
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                luxValue.floatValue = event.values.getOrElse(0) { 0f }
            }
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values.getOrElse(0) { 0f }
                proximityValue.floatValue = distance
                val maxRange = proximitySensor?.maximumRange ?: 5f
                val near = (distance < maxRange) && (distance < 5f)
                isProximityNear.value = near
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelX.floatValue = event.values.getOrElse(0) { 0f }
                accelY.floatValue = event.values.getOrElse(1) { 0f }
                accelZ.floatValue = event.values.getOrElse(2) { 0f }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Unused
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    lux: Float,
    proximityDistance: Float,
    isNear: Boolean,
    accelX: Float,
    accelY: Float,
    accelZ: Float,
    hasLight: Boolean,
    hasProximity: Boolean,
    hasAccel: Boolean,
    lifecycleState: String,
    onPlaySoundClick: () -> Unit
) {
    // Play sound safely when isNear state changes to true
    LaunchedEffect(isNear) {
        if (isNear) {
            onPlaySoundClick()
        }
    }

    // Dynamic Theme Color transition based on Light Sensor (Lux)
    val targetBgColor = when {
        lux <= 10f -> Color(0xFF1E1B4B) // Midnight Indigo (Night Mode)
        lux <= 200f -> Color(0xFF064E3B) // Forest Emerald (Dim Light)
        lux <= 1000f -> Color(0xFF1E293B) // Royal Slate (Indoor)
        else -> Color(0xFF78350F) // Warm Amber (Sunlight)
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 600),
        label = "BgColorAnimation"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = animatedBgColor
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Sensoryshi Explorer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Multi-Sensor Dashboard",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    },
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (lifecycleState.contains("RESUMED")) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (lifecycleState.contains("RESUMED")) Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (lifecycleState.contains("RESUMED")) "Aktif" else "Pause",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.35f)
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Lifecycle Status Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Lifecycle Status",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lifecycle Status: $lifecycleState",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // SENSOR 1: ENVIRONMENT - Light Sensor
                EnvironmentSensorCard(
                    lux = lux,
                    hasSensor = hasLight
                )

                // SENSOR 2: POSITION - Proximity Sensor
                PositionSensorCard(
                    distance = proximityDistance,
                    isNear = isNear,
                    hasSensor = hasProximity,
                    onPlaySound = onPlaySoundClick
                )

                // SENSOR 3: MOTION - Accelerometer
                MotionSensorCard(
                    accelX = accelX,
                    accelY = accelY,
                    accelZ = accelZ,
                    hasSensor = hasAccel
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ----------------------------------------------------------------------
// SENSOR 1: ENVIRONMENT CATEGORY (LIGHT SENSOR)
// ----------------------------------------------------------------------
@Composable
fun EnvironmentSensorCard(
    lux: Float,
    hasSensor: Boolean
) {
    val statusLabel: String
    val iconColor: Color

    when {
        lux <= 10f -> {
            statusLabel = "Sangat Gelap (Night Mode)"
            iconColor = Color(0xFF818CF8)
        }
        lux <= 200f -> {
            statusLabel = "Cahaya Redup (Dim Light)"
            iconColor = Color(0xFFFBBF24)
        }
        lux <= 1000f -> {
            statusLabel = "Cahaya Ruangan (Normal)"
            iconColor = Color(0xFF34D399)
        }
        else -> {
            statusLabel = "Cahaya Terang (Outdoor Sun)"
            iconColor = Color(0xFFF43F5E)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SensorHeader(
                category = "KATEGORI: ENVIRONMENT",
                title = "Light Sensor (Sensor Cahaya)",
                icon = Icons.Default.Star,
                iconTint = iconColor,
                isAvailable = hasSensor
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasSensor) {
                SensorNotAvailableMessage("Sensor Cahaya tidak terdeteksi pada emulator/device ini.")
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "${lux.toInt()} Lux",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = statusLabel,
                            fontSize = 13.sp,
                            color = iconColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar representation of Lux
                val progress = (lux / 2000f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = iconColor,
                    trackColor = Color(0xFF334155)
                )

                Text(
                    text = "Efek UI: Latar belakang aplikasi otomatis berganti warna & tema sesuai tingkat cahaya ruangan.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------
// SENSOR 2: POSITION CATEGORY (PROXIMITY SENSOR)
// ----------------------------------------------------------------------
@Composable
fun PositionSensorCard(
    distance: Float,
    isNear: Boolean,
    hasSensor: Boolean,
    onPlaySound: () -> Unit
) {
    val cardBg = if (isNear) Color(0xFF881337) else Color(0xFF0F766E)
    val accentColor = if (isNear) Color(0xFFF43F5E) else Color(0xFF2DD4BF)
    val statusText = if (isNear) "OBJEK SANGAT DEKAT (< 5 cm)" else "OBJEK JAUH (NORMAL)"
    val statusIcon = if (isNear) Icons.Default.Warning else Icons.Default.Check

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SensorHeader(
                category = "KATEGORI: POSITION",
                title = "Proximity Sensor (Sensor Jarak)",
                icon = Icons.Default.Place,
                iconTint = accentColor,
                isAvailable = hasSensor
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasSensor) {
                SensorNotAvailableMessage("Sensor Proximity tidak terdeteksi pada emulator/device ini.")
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Jarak Terukur: ${distance.toInt()} cm",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Efek UI: Merubah warna card alert & membunyikan suara BEEP ketika objek mendekat.",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onPlaySound,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Test Sound",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tes Suara", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// SENSOR 3: MOTION CATEGORY (ACCELEROMETER)
// ----------------------------------------------------------------------
@Composable
fun MotionSensorCard(
    accelX: Float,
    accelY: Float,
    accelZ: Float,
    hasSensor: Boolean
) {
    // Determine tilt status
    val tiltStatus = when {
        accelX > 3.0f -> "Miring Kiri (Tilt Left)"
        accelX < -3.0f -> "Miring Kanan (Tilt Right)"
        accelY > 3.0f -> "Miring Bawah (Tilt Down)"
        accelY < -3.0f -> "Miring Atas (Tilt Up)"
        else -> "Posisi Datar (Balanced)"
    }

    var ballPosX by remember { mutableFloatStateOf(0.5f) }
    var ballPosY by remember { mutableFloatStateOf(0.5f) }

    // Smoothly adjust ball position based on acceleration
    LaunchedEffect(accelX, accelY) {
        val targetX = (0.5f - (accelX / 18.0f)).coerceIn(0.08f, 0.92f)
        val targetY = (0.5f + (accelY / 18.0f)).coerceIn(0.08f, 0.92f)

        ballPosX += (targetX - ballPosX) * 0.25f
        ballPosY += (targetY - ballPosY) * 0.25f
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SensorHeader(
                category = "KATEGORI: MOTION",
                title = "Accelerometer (Sensor Akselerasi)",
                icon = Icons.Default.PlayArrow,
                iconTint = Color(0xFF38BDF8),
                isAvailable = hasSensor
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasSensor) {
                SensorNotAvailableMessage("Accelerometer tidak terdeteksi pada emulator/device ini.")
            } else {
                // Interactive Physics Canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Grid lines
                        drawLine(
                            color = Color(0xFF334155),
                            start = Offset(width / 2, 0f),
                            end = Offset(width / 2, height),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFF334155),
                            start = Offset(0f, height / 2),
                            end = Offset(width, height / 2),
                            strokeWidth = 1f
                        )

                        // Ball
                        val ballX = width * ballPosX
                        val ballY = height * ballPosY
                        val radius = 22.dp.toPx()

                        // Glow background for ball
                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = 0.35f),
                            radius = radius * 1.5f,
                            center = Offset(ballX, ballY)
                        )

                        // Main Ball
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF7DD3FC), Color(0xFF0284C7)),
                                center = Offset(ballX - radius * 0.3f, ballY - radius * 0.3f),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(ballX, ballY)
                        )
                    }

                    // Overlay Tilt Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = tiltStatus,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Reset position button
                    IconButton(
                        onClick = {
                            ballPosX = 0.5f
                            ballPosY = 0.5f
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Ball",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // X, Y, Z Live Readings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ReadingBadge(label = "X (Kiri/Kanan)", value = "%.2f".format(accelX), color = Color(0xFF38BDF8))
                    ReadingBadge(label = "Y (Atas/Bawah)", value = "%.2f".format(accelY), color = Color(0xFF818CF8))
                    ReadingBadge(label = "Z (Depan/Belakang)", value = "%.2f".format(accelZ), color = Color(0xFFC084FC))
                }

                Text(
                    text = "Efek UI: Memindahkan posisi bola 2D di atas Canvas secara real-time saat HP/emulator dimiringkan.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------------------------
// HELPER COMPONENTS
// ----------------------------------------------------------------------
@Composable
fun SensorHeader(
    category: String,
    title: String,
    icon: ImageVector,
    iconTint: Color,
    isAvailable: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = iconTint
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isAvailable) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAvailable) Color(0xFF10B981) else Color(0xFFEF4444))
        ) {
            Text(
                text = if (isAvailable) "Tersedia" else "Tidak Ada",
                color = if (isAvailable) Color(0xFF34D399) else Color(0xFFF87171),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ReadingBadge(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, color = Color(0xFF94A3B8))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun SensorNotAvailableMessage(message: String) {
    Surface(
        color = Color(0xFF334155),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = Color.LightGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
        )
    }
}
