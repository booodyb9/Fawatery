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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InvoiceStatus
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassBlue
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.IosShadowColor
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusDraftContainer
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueContainer
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidContainer
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingContainer
import kotlin.math.abs

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 20.dp,
    borderColor: Color = GlassBorder,
    backgroundColor: Color = FrostedGlassCard,
    hasGoldHighlight: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val borderBrush = if (hasGoldHighlight) {
        Brush.linearGradient(
            colors = listOf(
                RoyalGold.copy(alpha = 0.5f),
                GlassBlue.copy(alpha = 0.35f),
                Color(0x30FFFFFF)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                borderColor.copy(alpha = 0.45f),
                Color(0x40FFFFFF),
                borderColor.copy(alpha = 0.2f)
            )
        )
    }

    Card(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(shapeRadius),
                ambientColor = IosShadowColor,
                spotColor = IosShadowColor
            )
            .clip(RoundedCornerShape(shapeRadius))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(shapeRadius),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderBrush),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(0.dp)) {
            // iOS frosted specular top highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.95f),
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
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(0.8.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
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
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
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
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x18007AFF))
            .border(0.8.dp, GlassBlue.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = "بيانات الفاتورة",
            tint = GlassBlue,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "فواتير بالريال السعودي",
            color = GlassBlue,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}


/** Standards-based QR bitmap with its full white quiet zone. */
@Composable
fun ZatcaQrCodeView(dataString: String, modifier: Modifier = Modifier, sizeDp: Dp = 150.dp) {
    val bitmap = androidx.compose.runtime.remember(dataString) {
        com.example.export.InvoiceQr.bitmap(dataString)
    }
    androidx.compose.foundation.Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR بيانات الفاتورة",
        modifier = modifier.size(sizeDp).background(Color.White)
    )
}
