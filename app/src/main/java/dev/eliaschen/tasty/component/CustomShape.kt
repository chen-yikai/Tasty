package dev.eliaschen.tasty.component

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

class EightLeafShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            // Use the smaller dimension as the bounding radius to keep it safe from clipping
            val maxRadius = minOf(size.width, size.height) / 2f

            val steps = 360
            for (i in 0..steps) {
                // Convert degrees to radians
                val theta = Math.toRadians(i.toDouble())

                // k = 4 creates an 8-leaf rose curve when k is even
                // r = cos(4 * theta)
                val r = maxRadius * cos(4 * theta)

                // Convert polar coordinates (r, theta) to Cartesian (x, y)
                val x = (centerX + r * cos(theta)).toFloat()
                val y = (centerY + r * sin(theta)).toFloat()

                if (i == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            close()
        }
        return Outline.Generic(path)
    }
}