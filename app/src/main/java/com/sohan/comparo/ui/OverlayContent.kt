package com.sohan.comparo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.State

@Composable
fun OverlayContent(
    textState: State<String>,
    onCompareClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xDDFFFFFF)), // Semi-transparent white
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.width(220.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Comparo Active",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            
            Text(
                text = textState.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            
            androidx.compose.material3.Button(
                onClick = onCompareClick,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                 Text("Compare Now", fontSize = 12.sp)
            }
        }
    }
}
