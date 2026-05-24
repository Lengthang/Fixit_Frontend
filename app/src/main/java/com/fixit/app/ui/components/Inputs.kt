package com.fixit.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C
import kotlin.text.isNotEmpty

/**
 * Presentational input field — renders the FixIt input style but does NOT
 * accept user typing. Use this in design previews and read-only displays.
 *
 * For real text entry (PhoneEntry/OTP), we'll add a sibling component
 * `FixItTextField` that wraps BasicTextField with the same chrome.
 */
@Composable
fun InputField(
    label: String,
    value: String,
    placeholder: String = "",
    focused: Boolean = false
) {
    val borderColor = if (focused) C.Blue else C.Line
    val labelColor  = if (focused) C.Blue else C.Slate
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
        Text(label, fontSize = 12.sp, color = labelColor, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .heightIn(min = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (value.isNotEmpty()) value else placeholder,
                color = if (value.isNotEmpty()) C.Ink else C.Mute,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            if (focused) {
                Box(Modifier.size(width = 1.5.dp, height = 18.dp).background(C.Blue))
            }
        }
    }
}