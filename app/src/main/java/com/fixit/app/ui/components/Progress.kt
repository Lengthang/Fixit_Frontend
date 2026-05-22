package com.fixit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fixit.app.ui.theme.C

@Composable
fun Progress(step: Int, total: Int) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp)
            .height(4.dp).clip(RoundedCornerShape(2.dp)).background(C.Line),
    ) {
        Box(
            Modifier.fillMaxWidth(step / total.toFloat()).fillMaxHeight()
                .clip(RoundedCornerShape(2.dp)).background(C.Blue),
        )
    }
}