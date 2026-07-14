package com.offpolice.webradiobot.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object SocialIcons {
    val Telegram: ImageVector by lazy {
        ImageVector.Builder(
            name = "Telegram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(19.07f, 4.97f)
            lineTo(2.47f, 11.38f)
            quadTo(1.72f, 11.69f, 1.76f, 12.56f)
            quadTo(1.81f, 13.42f, 2.58f, 13.66f)
            lineTo(6.84f, 14.99f)
            lineTo(16.71f, 8.77f)
            quadTo(17.18f, 8.44f, 16.8f, 8.78f)
            lineTo(8.81f, 15.99f)
            lineTo(8.81f, 19.5f)
            quadTo(8.81f, 20.25f, 9.47f, 19.88f)
            lineTo(11.91f, 17.54f)
            lineTo(16.97f, 21.28f)
            quadTo(17.89f, 21.96f, 18.67f, 21.08f)
            lineTo(22.02f, 5.28f)
            quadTo(21.25f, 4.25f, 21.25f, 4.07f)
            close()
        }.build()
    }

    val X: ImageVector by lazy {
        ImageVector.Builder(
            name = "X",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(18.244f, 2.25f)
            horizontalLineTo(21.552f)
            lineTo(14.325f, 10.51f)
            lineTo(22.827f, 21.75f)
            horizontalLineTo(16.19f)
            lineTo(10.976f, 14.933f)
            lineTo(5.002f, 21.75f)
            horizontalLineTo(1.68f)
            lineTo(9.41f, 12.915f)
            lineTo(1.254f, 2.25f)
            horizontalLineTo(8.08f)
            lineTo(12.793f, 8.481f)
            close()
            moveTo(17.083f, 19.77f)
            horizontalLineTo(18.916f)
            lineTo(7.084f, 4.126f)
            horizontalLineTo(5.117f)
            close()
        }.build()
    }

    val Instagram: ImageVector by lazy {
        ImageVector.Builder(
            name = "Instagram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            pathFillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        ) {
            // Perfect Squircle Outer Box (x=3 to 21, y=3 to 21, corner radius=6)
            moveTo(15f, 3f)
            lineTo(9f, 3f)
            curveTo(5.686f, 3f, 3f, 5.686f, 3f, 9f)
            lineTo(3f, 15f)
            curveTo(3f, 18.314f, 5.686f, 21f, 9f, 21f)
            lineTo(15f, 21f)
            curveTo(18.314f, 21f, 21f, 18.314f, 21f, 15f)
            lineTo(21f, 9f)
            curveTo(21f, 5.686f, 18.314f, 3f, 15f, 3f)
            close()
            
            // Perfect Squircle Inner Box (x=5 to 19, y=5 to 19, corner radius=4) (Hollows outer box under EvenOdd)
            moveTo(15f, 5f)
            lineTo(9f, 5f)
            curveTo(6.791f, 5f, 5f, 6.791f, 5f, 9f)
            lineTo(5f, 15f)
            curveTo(5f, 17.209f, 6.791f, 19f, 9f, 19f)
            lineTo(15f, 19f)
            curveTo(17.209f, 19f, 19f, 17.209f, 19f, 15f)
            lineTo(19f, 9f)
            curveTo(19f, 6.791f, 17.209f, 5f, 15f, 5f)
            close()
            
            // Center Circle Outer Path (radius=5, centered at 12,12)
            moveTo(12f, 7f)
            curveTo(9.239f, 7f, 7f, 9.239f, 7f, 12f)
            curveTo(7f, 14.761f, 9.239f, 17f, 12f, 17f)
            curveTo(14.761f, 17f, 17f, 14.761f, 17f, 12f)
            curveTo(17f, 9.239f, 14.761f, 7f, 12f, 7f)
            close()
            
            // Center Circle Inner Path (radius=3, centered at 12,12) (Hollows center circle outline under EvenOdd)
            moveTo(12f, 9f)
            curveTo(13.657f, 9f, 15f, 10.343f, 15f, 12f)
            curveTo(15f, 13.657f, 13.657f, 15f, 12f, 15f)
            curveTo(10.343f, 15f, 9f, 13.657f, 9f, 12f)
            curveTo(9f, 10.343f, 10.343f, 9f, 12f, 9f)
            close()
            
            // Top-right flash dot (radius=1.2, centered at 16.5,7.5)
            moveTo(16.5f, 6.3f)
            curveTo(15.837f, 6.3f, 15.3f, 6.837f, 15.3f, 7.5f)
            curveTo(15.3f, 8.163f, 15.837f, 8.7f, 16.5f, 8.7f)
            curveTo(17.163f, 8.7f, 17.7f, 8.163f, 17.7f, 7.5f)
            curveTo(17.7f, 6.837f, 17.163f, 6.3f, 16.5f, 6.3f)
            close()
        }.build()
    }
}
