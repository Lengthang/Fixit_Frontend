package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixit.app.ui.theme.C

@Composable
fun FixItTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    var focused by rememberSaveable { mutableStateOf(false) }
    val borderColor = if (focused) C.Blue else C.Line
    val labelColor = if (focused) C.Blue else C.Slate

    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
        Text(label, fontSize = 12.sp, color = labelColor, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(color = C.Ink, fontSize = 15.sp),
                cursorBrush = SolidColor(C.Blue),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = C.Mute, fontSize = 15.sp)
                    }
                    inner()
                },
            )
        }
    }
}