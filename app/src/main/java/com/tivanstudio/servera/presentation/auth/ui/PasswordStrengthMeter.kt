package com.tivanstudio.servera.presentation.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tivanstudio.servera.R
import com.tivanstudio.servera.presentation.auth.PasswordStrength
import com.tivanstudio.servera.presentation.theme.DangerRed
import com.tivanstudio.servera.presentation.theme.PrimaryGreen
import com.tivanstudio.servera.presentation.theme.WarningAmber

/** Three segments filled up to the reached strength, with the matching label beside them. */
@Composable
fun PasswordStrengthMeter(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    val filled = when (strength) {
        PasswordStrength.WEAK -> 1
        PasswordStrength.MEDIUM -> 2
        PasswordStrength.STRONG -> 3
    }
    val color = when (strength) {
        PasswordStrength.WEAK -> DangerRed
        PasswordStrength.MEDIUM -> WarningAmber
        PasswordStrength.STRONG -> PrimaryGreen
    }
    val label = when (strength) {
        PasswordStrength.WEAK -> R.string.pwd_weak
        PasswordStrength.MEDIUM -> R.string.pwd_medium
        PasswordStrength.STRONG -> R.string.pwd_strong
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->
            Segment(
                color = if (index < filled) color else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = stringResource(label),
            color = color,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

@Composable
private fun Segment(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    ) {}
}
