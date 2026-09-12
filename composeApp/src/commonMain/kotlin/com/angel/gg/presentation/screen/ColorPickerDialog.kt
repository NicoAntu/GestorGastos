package com.angel.gg.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.gg.presentation.AppTheme
import com.angel.gg.presentation.i18n.tr
import kotlin.math.*

@Composable
fun ColorPickerDialog(
    colorInicial   : String = "#4F46E5",
    onColorSelected: (String) -> Unit,
    onDismiss      : () -> Unit
) {
    var hue        by remember { mutableStateOf(250f) }
    var saturation by remember { mutableStateOf(0.7f) }
    var brightness by remember { mutableStateOf(0.9f) }

    // Estado separado para el texto del campo hex
    // Esto es lo que el usuario ve y edita — independiente del valor calculado
    var hexInput   by remember { mutableStateOf(colorInicial.uppercase()) }

    // Sincronizar desde el color inicial al abrir
    LaunchedEffect(colorInicial) {
        try {
            val hex = colorInicial.removePrefix("#")
            val r   = hex.substring(0, 2).toInt(16) / 255f
            val g   = hex.substring(2, 4).toInt(16) / 255f
            val b   = hex.substring(4, 6).toInt(16) / 255f
            val hsv = rgbToHsv(r, g, b)
            hue        = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
            hexInput   = colorInicial.uppercase()
        } catch (e: Exception) { }
    }

    val colorActual = remember(hue, saturation, brightness) {
        hsvToColor(hue, saturation, brightness)
    }

    // Cuando la rueda cambia, actualizamos también el hexInput
    // para que el campo siempre refleje la posición actual de la rueda
    val hexCalculado = remember(colorActual) {
        "#%02X%02X%02X".format(
            (colorActual.red   * 255).toInt(),
            (colorActual.green * 255).toInt(),
            (colorActual.blue  * 255).toInt()
        )
    }

    // Mantener hexInput sincronizado cuando la rueda se mueve
    // pero NO cuando el usuario está escribiendo (para no pisarle el texto)
    var usuarioEscribiendo by remember { mutableStateOf(false) }

    LaunchedEffect(hexCalculado) {
        if (!usuarioEscribiendo) {
            hexInput = hexCalculado
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = AppTheme.CardDark,
        title = {
            Text(tr("colorpicker.titulo"), color = AppTheme.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rueda de color
                Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    ColorWheel(
                        hue        = hue,
                        saturation = saturation,
                        brightness = brightness,
                        modifier   = Modifier.fillMaxSize(),
                        onColorChange = { h, s ->
                            usuarioEscribiendo = false   // la rueda tomó el control
                            hue        = h
                            saturation = s
                        }
                    )
                }

                // Slider de brillo
                Column {
                    Text(tr("colorpicker.brillo"), color = AppTheme.TextMuted, fontSize = 12.sp)
                    Slider(
                        value         = brightness,
                        onValueChange = {
                            usuarioEscribiendo = false
                            brightness = it
                        },
                        valueRange = 0.2f..1f,
                        colors     = SliderDefaults.colors(
                            thumbColor       = colorActual,
                            activeTrackColor = colorActual
                        )
                    )
                }

                // Preview + campo hex editable
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Círculo de preview con el color actual
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorActual)
                    )

                    // Campo hex editable
                    OutlinedTextField(
                        value         = hexInput,
                        onValueChange = { nuevo ->
                            // Permitir edición libre mientras escribe
                            if (nuevo.length <= 7) {
                                usuarioEscribiendo = true
                                hexInput = nuevo.uppercase()

                                // Intentar parsear el hex y mover la rueda
                                // Solo cuando el hex está completo (#RRGGBB)
                                val limpio = nuevo.removePrefix("#")
                                if (limpio.length == 6) {
                                    try {
                                        val r   = limpio.substring(0, 2).toInt(16) / 255f
                                        val g   = limpio.substring(2, 4).toInt(16) / 255f
                                        val b   = limpio.substring(4, 6).toInt(16) / 255f
                                        val hsv = rgbToHsv(r, g, b)
                                        hue        = hsv[0]
                                        saturation = hsv[1]
                                        brightness = hsv[2]
                                        // El hex es válido — ya no está "escribiendo"
                                        usuarioEscribiendo = false
                                    } catch (e: Exception) {
                                        // Hex inválido — seguir escribiendo
                                    }
                                }
                            }
                        },
                        modifier   = Modifier.width(130.dp),
                        textStyle  = TextStyle(
                            color      = AppTheme.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp
                        ),
                        singleLine = true,
                        label      = { Text("Hex", color = AppTheme.TextMuted, fontSize = 11.sp) },
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = AppTheme.Indigo,
                            unfocusedBorderColor    = AppTheme.BorderColor,
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor        = AppTheme.TextPrimary,
                            unfocusedTextColor      = AppTheme.TextPrimary,
                            cursorColor             = AppTheme.Indigo
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(hexCalculado); onDismiss() },
                colors  = ButtonDefaults.buttonColors(containerColor = AppTheme.Indigo)
            ) { Text(tr("colorpicker.seleccionar")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("comun.cancelar"), color = AppTheme.TextMuted)
            }
        }
    )
}

@Composable
private fun ColorWheel(
    hue          : Float,
    saturation   : Float,
    brightness   : Float,
    modifier     : Modifier,
    onColorChange: (Float, Float) -> Unit
) {
    var size by remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size / 2, size / 2)
                    val radius = size / 2
                    val dx     = offset.x - center.x
                    val dy     = offset.y - center.y
                    val dist   = sqrt(dx * dx + dy * dy)
                    if (dist <= radius) {
                        val angle = (atan2(dy, dx) * (180 / PI).toFloat() + 360) % 360
                        val sat   = (dist / radius).coerceIn(0f, 1f)
                        onColorChange(angle, sat)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val offset = change.position
                    val center = Offset(size / 2, size / 2)
                    val radius = size / 2
                    val dx     = offset.x - center.x
                    val dy     = offset.y - center.y
                    val angle  = (atan2(dy, dx) * (180 / PI).toFloat() + 360) % 360
                    val sat    = (sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
                    onColorChange(angle, sat)
                }
            }
    ) {
        size = this.size.minDimension
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val radius = size / 2

        // Sectores de color
        val steps = 360
        val sweep  = 360f / steps
        for (i in 0 until steps) {
            val angle = i.toFloat()
            val color = hsvToColor(angle, 1f, brightness)
            drawArc(
                color      = color,
                startAngle = angle - sweep / 2,
                sweepAngle = sweep + 1f,
                useCenter  = true,
                topLeft    = Offset(center.x - radius, center.y - radius),
                size       = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        // Gradiente blanco al centro para representar saturación
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Indicador de posición actual
        val rad        = (hue * PI / 180).toFloat()
        val indicadorX = center.x + cos(rad) * (saturation * radius)
        val indicadorY = center.y + sin(rad) * (saturation * radius)
        drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(indicadorX, indicadorY))
        drawCircle(
            color  = Color.Black.copy(alpha = 0.3f),
            radius = 10.dp.toPx(),
            center = Offset(indicadorX, indicadorY),
            style  = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c  = v * s
    val x  = c * (1 - abs((h / 60) % 2 - 1))
    val m  = v - c
    val (r1, g1, b1) = when {
        h < 60  -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else    -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m)
}

private fun rgbToHsv(r: Float, g: Float, b: Float): FloatArray {
    val max  = maxOf(r, g, b)
    val min  = minOf(r, g, b)
    val diff = max - min
    val h = when {
        diff == 0f -> 0f
        max == r   -> (60 * ((g - b) / diff) + 360) % 360
        max == g   -> 60 * ((b - r) / diff) + 120
        else       -> 60 * ((r - g) / diff) + 240
    }
    return floatArrayOf(h, if (max == 0f) 0f else diff / max, max)
}