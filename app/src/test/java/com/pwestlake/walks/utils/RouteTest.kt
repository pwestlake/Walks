package com.pwestlake.walks.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTest {
    @Test
    fun testFormatAsGPX(): Unit {
        val points = LinkedHashSet<Trkpt>()
        points.add(Trkpt(0.0, 1.0, 1.0))
        points.add(Trkpt(0.0, 2.0, 2.0))

        val result = formatAsGPX(points)
        assertEquals("", result)
    }
}