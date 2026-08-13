package com.example.veiltalk.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.veiltalk.ui.theme.WaChatBg

@Composable
fun ChatBaseLayout(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = topBar,
        bottomBar = {
            // این بخش باعث می‌شود قسمت پایین صفحه با باز شدن کیبورد بالا بیاید
            Box(
                modifier = Modifier
                    .background(WaChatBg)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                bottomBar()
            }
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(WaChatBg)
            ) {
                content(PaddingValues(horizontal = 8.dp, vertical = 8.dp))
            }
        }
    )
}