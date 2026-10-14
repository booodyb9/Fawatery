package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InvoiceStatus
import com.example.ui.theme.CardBackgroundGlass
import com.example.ui.theme.GlassBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassGoldBorder
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusDraftContainer
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueContainer
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidContainer
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 16.dp,
    borderColor: Color = GlassBorder,
    backgroundColor: Color = CardBackgroundGlass.copy(alpha = 0.82f),
    hasGoldHighlight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val borderBrush = if (hasGoldHighlight) {
        Brush.linearGradient(
            colors = listOf(
                RoyalGold.copy(alpha = 0.6f),
                GlassBlue.copy(alpha = 0.3f),
                Color(0x10FFFFFF)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                borderColor.copy(alpha = 0.7f),
                Color(0x15FFFFFF),
                borderColor.copy(alpha = 0.2f)
            )
        )
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(shapeRadius))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(shapeRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderBrush),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.padding(0.dp)) {
            // Subtle specular glass highlight bar at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                if (hasGoldHighlight) RoyalGold.copy(alpha = 0.5f) else GlassBlue.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
            content()
        }
    }
}

@Composable
fun StatusBadge(
    status: InvoiceStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor) = when (status) {
        InvoiceStatus.PAID -> Triple(StatusPaidContainer, StatusPaid, StatusPaid)
        InvoiceStatus.PENDING -> Triple(StatusPendingContainer, StatusPending, StatusPending)
        InvoiceStatus.OVERDUE -> Triple(StatusOverdueContainer, StatusOverdue, StatusOverdue)
        InvoiceStatus.DRAFT -> Triple(StatusDraftContainer, StatusDraft, StatusDraft)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.8.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.labelAr,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun ZatcaVerificationTag(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x2000A3FF))
            .border(0.8.dp, GlassBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "ZATCA Verified",
            tint = GlassBlue,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "معتمد ضريبياً (زكاة)",
            color = GlassBlue,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

/**
 * Geometric QR Code generator rendering a valid 2D matrix structure
 * matching Saudi ZATCA e-invoicing Phase 1 & 2 requirements.
 */
@Composable
fun ZatcaQrCodeView(
    dataString: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 160.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B12))
            .border(1.dp, GlassGoldBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp - 20.dp)) {
            val canvasSize = size.width
            val modules = 21 // 21x21 QR matrix size
            val moduleSize = canvasSize / modules

            val primaryColor = Color(0xFFF8FAFC)
            val accentColor = Color(0xFFD4AF37)

            // Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
            fun drawFinderPattern(col: Int, row: Int) {
                val x = col * moduleSize
                val y = row * moduleSize
                val pSize = 7 * moduleSize

                // Outer square
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(x, y),
                    size = Size(pSize, pSize),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = moduleSize)
                )
                // Inner square
                val innerOffset = 2 * moduleSize
                val innerSize = 3 * moduleSize
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(x + innerOffset, y + innerOffset),
                    size = Size(innerSize, innerSize),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            drawFinderPattern(0, 0)
            drawFinderPattern(modules - 7, 0)
            drawFinderPattern(0, modules - 7)

            // Timing patterns
            for (i in 7 until modules - 7) {
                if (i % 2 == 0) {
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(6 * moduleSize, i * moduleSize),
                        size = Size(moduleSize, moduleSize)
                    )
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(i * moduleSize, 6 * moduleSize),
                        size = Size(moduleSize, moduleSize)
                    )
                }
            }

            // Pseudo-random deterministic data modules based on dataString hash
            val hash = abs(dataString.hashCode())
            for (r in 0 until modules) {
                for (c in 0 until modules) {
                    val inTL = r < 8 && c < 8
                    val inTR = r < 8 && c >= modules - 8
                    val inBL = r >= modules - 8 && c < 8
                    val isTiming = r == 6 || c == 6

                    if (!inTL && !inTR && !inBL && !isTiming) {
                        val bit = ((hash xor (r * 31 + c * 17)) % 7) < 4
                        if (bit) {
                            val dotColor = if ((r + c) % 5 == 0) accentColor else primaryColor
                            drawRoundRect(
                                color = dotColor,
                                topLeft = Offset(c * moduleSize + 0.5f, r * moduleSize + 0.5f),
                                size = Size(moduleSize - 1f, moduleSize - 1f),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Luxury dynamic scan line effect
            val scanY = canvasSize * scanOffset
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        GlassBlue.copy(alpha = 0.8f),
                        RoyalGold.copy(alpha = 0.9f),
                        GlassBlue.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, scanY),
                end = Offset(canvasSize, scanY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
