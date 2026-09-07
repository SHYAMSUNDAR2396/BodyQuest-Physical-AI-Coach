package com.bodyquest.core

import kotlin.math.acos
import kotlin.math.sqrt

/** Interior angle at vertex [b], in degrees, formed by points a-b-c. Ignores z (image-plane angle). */
fun angleDegrees(a: Landmark, b: Landmark, c: Landmark): Float {
    val abx = a.x - b.x; val aby = a.y - b.y
    val cbx = c.x - b.x; val cby = c.y - b.y
    val dot = abx * cbx + aby * cby
    val magAB = sqrt(abx * abx + aby * aby)
    val magCB = sqrt(cbx * cbx + cby * cby)
    if (magAB == 0f || magCB == 0f) return 0f
    val cosAngle = (dot / (magAB * magCB)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cosAngle).toDouble()).toFloat()
}
