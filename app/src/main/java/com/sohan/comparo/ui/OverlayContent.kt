package com.sohan.comparo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.State

import com.sohan.comparo.data.ScannedItem

@Composable
fun OverlayContent(
    textState: State<String>, // Legacy status like "Scanning..."
    scannedItems: List<ScannedItem>,
    onCompareClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEEEEEEEE)), 
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.width(260.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Text(
                text = "Comparo Live",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status Text
            if (textState.value.isNotEmpty()) {
                Text(
                    text = textState.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Blue
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Comparison Table
            if (scannedItems.isNotEmpty()) {
                scannedItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.appName, fontWeight = FontWeight.Medium)
                        Text("₹${item.price}", fontWeight = FontWeight.Bold, color = Color(0xFF006400))
                    }
                    Divider(color = Color.LightGray, thickness = 0.5.dp)
                }
            } else {
                 Text("No prices found yet.", style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onCompareClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                 Text("Compare Now", fontSize = 14.sp)
            }
        }
    }
}
