package com.asme.receiving.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asme.receiving.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        phase = 0
        alpha.snapTo(0f)
        alpha.animateTo(1f, animationSpec = tween(350))
        delay(450)
        alpha.animateTo(0f, animationSpec = tween(250))
        delay(150)
        phase = 1
        alpha.snapTo(0f)
        alpha.animateTo(1f, animationSpec = tween(450))
        delay(700)
        alpha.animateTo(0f, animationSpec = tween(350))
        delay(100)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (phase == 0) {
            Text(
                text = "Brought to you by:",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialGuardianColors.Title,
                modifier = Modifier.alpha(alpha.value),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.welders_helper_512),
                    contentDescription = "Welders Helper Logo",
                    modifier = Modifier
                        .size(220.dp)
                        .alpha(alpha.value),
                )
            }
        }
    }
}
