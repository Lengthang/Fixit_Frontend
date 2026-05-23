package com.fixit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.OutlineButton
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import com.fixit.app.ui.welcome.RootViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fixit.app.nav.FixItNavGraph
import com.fixit.app.ui.theme.FixItTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FixItTheme {
                val root: RootViewModel = hiltViewModel()
                val startDest by root.startDestination.collectAsState()
                startDest?.let { FixItNavGraph(startDestination = it) }
            }
        }
    }
}
@Composable
fun UserPlaceholderScreen() {

    FixItScreen {
        TopBar(showBack = false)
        Column(
            Modifier.weight(1f).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("You're in!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            Spacer(Modifier.height(8.dp))
            Text("Signed in as Thang ?: — (user)",
                fontSize = 14.sp, color = C.Slate)
            Spacer(Modifier.height(4.dp))
            Text( "070", fontSize = 13.sp, color = C.Mute)
        }
        OutlineButton("Sign out") { }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FixItTheme {
        UserPlaceholderScreen()
    }
}