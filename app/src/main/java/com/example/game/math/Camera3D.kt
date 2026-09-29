package com.example.game.math

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val isVisible: Boolean
)

class Camera3D(
    var position: Vec3 = Vec3(0f, 6f, -10f),
    var target: Vec3 = Vec3(0f, 0f, 0f),
    var up: Vec3 = Vec3.UP,
    var fovScale: Float = 600f
) {
    private var forward: Vec3 = Vec3.FORWARD
    private var right: Vec3 = Vec3.RIGHT
    private var actualUp: Vec3 = Vec3.UP

    fun updateOrientation() {
        val dir = target - position
        val len = dir.length()
        forward = if (len > 0.001f) dir * (1f / len) else Vec3.FORWARD
        val r = forward.cross(up)
        val rLen = r.length()
        right = if (rLen > 0.001f) r * (1f / rLen) else Vec3.RIGHT
        actualUp = right.cross(forward).normalized()
    }

    fun project(point: Vec3, screenWidth: Float, screenHeight: Float): ProjectedPoint {
        val rel = point - position
        val zCam = rel.dot(forward)
        if (zCam < 0.2f) {
            return ProjectedPoint(0f, 0f, zCam, false)
        }
        val xCam = rel.dot(right)
        val yCam = rel.dot(actualUp)

        val factor = fovScale / zCam
        val sx = screenWidth * 0.5f + xCam * factor
        val sy = screenHeight * 0.5f - yCam * factor

        // Extra margin check for clipping
        val margin = 200f
        val isVis = sx >= -margin && sx <= screenWidth + margin &&
                    sy >= -margin && sy <= screenHeight + margin

        return ProjectedPoint(sx, sy, zCam, isVis)
    }

    /**
     * Projects a sphere radius at a given depth to screen pixels
     */
    fun projectRadius(radius: Float, depth: Float): Float {
        return if (depth > 0.2f) (radius / depth) * fovScale else 0f
    }
}
