package ch.planetebowl.printagent.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.planetebowl.printagent.service.AgentStatus

@Composable
fun AgentStatusBanner(status: AgentStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        AgentStatus.RUNNING -> MaterialTheme.colorScheme.primary to "Agent actif"
        AgentStatus.STOPPED -> MaterialTheme.colorScheme.outline to "Agent à l'arrêt"
        AgentStatus.ERROR -> MaterialTheme.colorScheme.error to "Agent actif — erreur récente"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusDot(color = color)
        Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatusDot(color: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(16.dp)
            .background(color, CircleShape),
    )
}
