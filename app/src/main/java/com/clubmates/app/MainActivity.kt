package com.clubmates.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.clubmates.app.navigation.ClubMatesNavGraph
import com.clubmates.app.ui.theme.ClubMatesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClubMatesTheme {
                ClubMatesNavGraph()
            }
        }
    }
}
