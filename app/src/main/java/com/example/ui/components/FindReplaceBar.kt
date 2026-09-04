package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.SearchMatch

@Composable
fun FindReplaceBar(
    searchQuery: String,
    replaceQuery: String,
    matches: List<SearchMatch>,
    currentMatchIndex: Int,
    onSearchChange: (String) -> Unit,
    onReplaceChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFFFFFFF),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("find_replace_bar")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Row 1: Search Query + Match Count + Prev / Next + Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Find in document...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF0F4C81),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("find_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Match count badge
                val matchLabel = if (searchQuery.isBlank()) {
                    "0 results"
                } else if (matches.isEmpty()) {
                    "No match"
                } else {
                    "${currentMatchIndex + 1} of ${matches.size}"
                }

                Text(
                    text = matchLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        color = if (matches.isEmpty() && searchQuery.isNotBlank()) Color(0xFFDC2626) else Color(0xFF64748B)
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = onPreviousMatch,
                    enabled = matches.isNotEmpty(),
                    modifier = Modifier.size(32.dp).testTag("find_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Match",
                        tint = if (matches.isNotEmpty()) Color(0xFF0F4C81) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onNextMatch,
                    enabled = matches.isNotEmpty(),
                    modifier = Modifier.size(32.dp).testTag("find_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Match",
                        tint = if (matches.isNotEmpty()) Color(0xFF0F4C81) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("find_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Find & Replace",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Replace Query + Replace / Replace All buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceChange,
                    placeholder = { Text("Replace with...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = null,
                            tint = Color(0xFF0F4C81),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("replace_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onReplaceCurrent,
                    enabled = matches.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F4C81)),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("replace_one_button")
                ) {
                    Text("Replace", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onReplaceAll,
                    enabled = matches.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81)),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("replace_all_button")
                ) {
                    Text("Replace All", fontSize = 12.sp)
                }
            }
        }
    }
}
