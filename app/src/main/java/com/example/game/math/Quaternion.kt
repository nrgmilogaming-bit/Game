package com.example.game.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Quaternion(
    val w: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun times(q: Quaternion): Quaternion {
        return Quaternion(
            w = w * q.w - x * q.x - y * q.y - z * q.z,
            x = w * q.x + x * q.w + y * q.z - z * q.y,
            y = w * q.y - x * q.z + y * q.w + z * q.x,
            z = w * q.z + x * q.y - y * q.x + z * q.w
        ).normalized()
    }

    fun rotate(v: Vec3): Vec3 {
        // v' = q * (0, v) * q^-1
        val qv = Vec3(x, y, z)
        val uv = qv.cross(v)
        val uuv = qv.cross(uv)
        return v + (uv * (2.0f * w)) + (uuv * 2.0f)
    }

    fun normalized(): Quaternion {
        val mag = sqrt(w * w + x * x + y * y + z * z)
        return if (mag > 0.0001f) {
            val inv = 1f / mag
            Quaternion(w * inv, x * inv, y * inv, z * inv)
        } else {
            IDENTITY
        }
    }

    companion object {
        val IDENTITY = Quaternion(1f, 0f, 0f, 0f)

        fun fromAxisAngle(axis: Vec3, angleRadians: Float): Quaternion {
            val normAxis = axis.normalized()
            val halfAngle = angleRadians * 0.5f
            val s = sin(halfAngle)
            return Quaternion(
                w = cos(halfAngle),
                x = normAxis.x * s,
                y = normAxis.y * s,
                z = normAxis.z * s
            ).normalized()
        }
    }
}
