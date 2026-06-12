package com.example.ui.screens

import com.example.ui.theme.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel

@Composable
fun SplashScreen(
    viewModel: MainViewModel,
    onSplashFinished: (isLoggedIn: Boolean) -> Unit
) {
    val isFinished by viewModel.isSplashFinished.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    // Trigger navigation when finished
    LaunchedEffect(isFinished) {
        if (isFinished) {
            onSplashFinished(preferences.isLoggedIn)
        }
    }

    // Glowing/pulse ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic Star Grid / Lines in Background
        val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cols = 10
            val rows = 15
            for (i in 0..cols) {
                val x = (width / cols) * i
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            for (i in 0..rows) {
                val y = (height / rows) * i
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Neon Logo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing outer ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = CyberNeonCyan,
                        radius = (size.minDimension / 2) * pulseScale,
                        style = Stroke(width = 2.dp.toPx()),
                        alpha = 0.35f
                    )
                }

                // Rotating cyber rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = CyberNeonMagenta,
                        startAngle = rotateAngle,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawArc(
                        color = CyberNeonCyan,
                        startAngle = rotateAngle + 180f,
                        sweepAngle = 140f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                // Inner core Star Symbol
                Canvas(modifier = Modifier.size(40.dp)) {
                    val midX = size.width / 2f
                    val midY = size.height / 2f
                    // Draw neon core rhombus
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(midX, 10f)
                        lineTo(midX + 20f, midY)
                        lineTo(midX, size.height - 10f)
                        lineTo(midX - 20f, midY)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(CyberNeonCyan, CyberNeonMagenta)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Text Typography Setup with Sci-Fi naming conventions
            Text(
                text = "HELLO NOVA",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("splash_title")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SYS_BOOT SEQUENCE OPERATIONAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("splash_subtitle")
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Scanning Progress line
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                val progressAnim = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    progressAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(1900, easing = LinearOutSlowInEasing)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnim.value)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(CyberNeonCyan, CyberNeonMagenta)
                            )
                        )
                )
            }
        }
    }
}
