package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    qrColor: Color = Color(0xFF0F172A),
    backgroundColor: Color = Color.White
) {
    // Generate genuine standard QR Code BitMatrix via ZXing for real camera scanning
    val matrix = remember(data) {
        try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(
                data.ifBlank { "HOMEo-AI" },
                BarcodeFormat.QR_CODE,
                29,
                29,
                hints
            )
            val w = bitMatrix.width
            val h = bitMatrix.height
            Array(h) { y ->
                BooleanArray(w) { x ->
                    bitMatrix.get(x, y)
                }
            }
        } catch (_: Exception) {
            Array(21) { BooleanArray(21) { (it % 2 == 0) } }
        }
    }

    val rows = matrix.size
    val cols = if (rows > 0) matrix[0].size else 1

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(2.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = this.size.width / cols
                val cellH = this.size.height / rows
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (matrix[r][c]) {
                            drawRect(
                                color = qrColor,
                                topLeft = Offset(c * cellW, r * cellH),
                                size = Size(cellW * 1.05f, cellH * 1.05f)
                            )
                        }
                    }
                }
            }
        }
    }
}
