package com.agg.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agg.app.domain.model.CategoryIcon

/** Shared visual primitives for the approved compact, pastel AGG prototype. */
@Composable
fun PrototypePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 44.dp).semantics { this.selected = selected },
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun CategoryBadge(icon: CategoryIcon, color: Color, size: Dp = 42.dp, description: String? = null) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f), modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon.toImageVector(), description, Modifier.size(size * 0.56f), tint = color)
        }
    }
}

@Composable
fun prototypeFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = MaterialTheme.colorScheme.error,
)

val PrototypeFieldShape = RoundedCornerShape(12.dp)
