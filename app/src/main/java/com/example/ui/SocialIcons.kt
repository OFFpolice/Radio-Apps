package com.example.ui

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
        ).path(fill = SolidColor(Color.Black)) {
            // Outer border
            moveTo(12f, 2.163f)
            curveTo(8.797f, 2.163f, 8.397f, 2.177f, 7.135f, 2.235f)
            curveTo(3.181f, 2.416f, 0.96f, 4.618f, 0.771f, 8.59f)
            curveTo(0.713f, 9.852f, 0.7f, 10.252f, 0.7f, 11.455f)
            curveTo(0.7f, 12.658f, 0.713f, 13.058f, 0.771f, 14.32f)
            curveTo(0.96f, 18.292f, 3.177f, 20.494f, 7.135f, 20.675f)
            curveTo(8.397f, 20.733f, 8.797f, 20.747f, 12f, 20.747f)
            curveTo(15.203f, 20.747f, 15.603f, 20.733f, 16.865f, 20.675f)
            curveTo(20.819f, 20.494f, 23.04f, 18.292f, 23.229f, 14.32f)
            curveTo(23.287f, 13.058f, 23.3f, 12.658f, 23.3f, 11.455f)
            curveTo(23.3f, 10.252f, 23.287f, 9.852f, 23.229f, 8.59f)
            curveTo(23.04f, 4.618f, 20.819f, 2.416f, 16.865f, 2.235f)
            curveTo(15.603f, 2.177f, 15.203f, 2.163f, 12f, 2.163f)
            close()
            
            // Hollow inner space of the outer border
            moveTo(12f, 3.84f)
            curveTo(15.148f, 3.84f, 15.52f, 3.852f, 16.762f, 3.908f)
            curveTo(19.648f, 4.04f, 21.096f, 5.484f, 21.228f, 8.372f)
            curveTo(21.284f, 9.613f, 21.296f, 9.986f, 21.296f, 13.134f)
            curveTo(21.296f, 16.282f, 21.284f, 16.655f, 21.228f, 17.896f)
            curveTo(21.096f, 20.784f, 19.648f, 22.228f, 16.762f, 22.36f)
            curveTo(15.52f, 22.416f, 15.148f, 22.428f, 12f, 22.428f)
            curveTo(8.852f, 22.428f, 8.48f, 22.416f, 7.238f, 22.36f)
            curveTo(4.352f, 22.228f, 2.904f, 20.784f, 2.772f, 17.896f)
            curveTo(2.716f, 16.655f, 2.704f, 16.282f, 2.704f, 13.134f)
            curveTo(2.704f, 9.986f, 2.716f, 9.613f, 2.772f, 8.372f)
            curveTo(2.904f, 5.484f, 4.352f, 4.04f, 7.238f, 3.908f)
            curveTo(8.48f, 3.852f, 8.852f, 3.84f, 12f, 3.84f)
            close()
            
            // Lens (Center Circle)
            moveTo(12f, 5.838f)
            curveTo(8.898f, 5.838f, 6.398f, 8.338f, 6.398f, 11.44f)
            curveTo(6.398f, 14.541f, 8.898f, 17.042f, 12f, 17.042f)
            curveTo(15.102f, 17.042f, 17.602f, 14.541f, 17.602f, 11.44f)
            curveTo(17.602f, 8.338f, 15.102f, 5.838f, 12f, 5.838f)
            close()
            
            // Inner space of lens
            moveTo(12f, 7.518f)
            curveTo(14.165f, 7.518f, 15.922f, 9.274f, 15.922f, 11.44f)
            curveTo(15.922f, 13.605f, 14.165f, 15.362f, 12f, 15.362f)
            curveTo(9.835f, 15.362f, 8.078f, 13.605f, 8.078f, 11.44f)
            curveTo(8.078f, 9.274f, 9.835f, 7.518f, 12f, 7.518f)
            close()
            
            // Flash dot
            moveTo(17.842f, 4.543f)
            curveTo(17.206f, 4.543f, 16.691f, 5.058f, 16.691f, 5.694f)
            curveTo(16.691f, 6.33f, 17.206f, 6.845f, 17.842f, 6.845f)
            curveTo(18.478f, 6.845f, 18.993f, 6.33f, 18.993f, 5.694f)
            curveTo(18.993f, 5.058f, 18.478f, 4.543f, 17.842f, 4.543f)
            close()
        }.build()
    }
}
