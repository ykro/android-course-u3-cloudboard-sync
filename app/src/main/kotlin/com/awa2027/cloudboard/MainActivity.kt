package com.awa2027.cloudboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.awa2027.cloudboard.ui.BoardRoute
import com.awa2027.cloudboard.ui.theme.CloudBoardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CloudBoardTheme { BoardRoute() } }
    }
}
