package com.lonx.lyrico.ui.components.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    height: Dp = 30.dp,
    shape: Shape = RoundedCornerShape(percent = 50),
    contentPadding: PaddingValues = PaddingValues(horizontal = 13.dp, vertical = 5.dp),
    iconSpacing: Dp = 5.dp,
    containerColor: Color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    contentColor: Color = MiuixTheme.colorScheme.onSurface,
    disabledContainerColor: Color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    disabledContentColor: Color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    textStyle: TextStyle = MiuixTheme.textStyles.body2.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
) {
    val actualContainerColor = if (enabled) containerColor else disabledContainerColor
    val actualContentColor = if (enabled) contentColor else disabledContentColor

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(actualContainerColor)
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides actualContentColor
                ) {
                    leadingIcon()
                }

                Spacer(modifier = Modifier.width(iconSpacing))
            }

            Text(
                text = text,
                style = textStyle,
                color = actualContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(iconSpacing))

                CompositionLocalProvider(
                    LocalContentColor provides actualContentColor
                ) {
                    trailingIcon()
                }
            }
        }
    }
}