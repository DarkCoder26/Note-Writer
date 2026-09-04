package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.SaveStatus

@Composable
fun StatusBar(
    pageCount: Int,
    wordCount: Int,
    characterCount: Int,
    saveStatus: SaveStatus,
    zoomPercent: Int,
    onZoomChange: (Int) -> Unit,
    onWordCountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF1F5F9),
        contentColor = Color(0xFF334155),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .testTag("status_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            // Left Status Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onWordCountClick)
            ) {
                Text(
                    text = "Page 1 of $pageCount",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                )

                StatusBarDivider()

                Text(
                    text = "$wordCount words",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.testTag("status_word_count")
                )

                StatusBarDivider()

                Text(
                    text = "$characterCount characters",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                )

                StatusBarDivider()

                // Save Status Indicator Dot
                val dotColor = when (saveStatus) {
                    SaveStatus.SAVED -> Color(0xFF16A34A)
                    SaveStatus.SAVING -> Color(0xFF2563EB)
                    SaveStatus.UNSAVED -> Color(0xFFEA580C)
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = saveStatus.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = dotColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Right Zoom Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onZoomChange((zoomPercent - 10).coerceAtLeast(50)) },
                    modifier = Modifier.size(20.dp).testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = "$zoomPercent%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("zoom_indicator")
                )

                IconButton(
                    onClick = { onZoomChange((zoomPercent + 10).coerceAtMost(200)) },
                    modifier = Modifier.size(20.dp).testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(1.dp)
            .height(14.dp)
            .background(Color(0xFFCBD5E1))
    )
}
