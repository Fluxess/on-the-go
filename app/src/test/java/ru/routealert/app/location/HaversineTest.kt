package ru.routealert.app.location

import org.junit.Assert.assertTrue
import org.junit.Test

class HaversineTest {
    @Test
    fun nearbyPointsWithin200m() {
        // ~111 m north of origin-ish at equator scale; use known Moscow points ~150m apart-ish
        val aLat = 55.751244
        val aLon = 37.618423
        val bLat = 55.752244 // ~111 m north
        val bLon = 37.618423
        val d = Haversine.distanceMeters(aLat, aLon, bLat, bLon)
        assertTrue("expected ~111m, got $d", d in 90.0..130.0)
    }

    @Test
    fun farPointsOutside1km() {
        val d = Haversine.distanceMeters(55.75, 37.62, 55.76, 37.62)
        assertTrue("expected >1000m, got $d", d > 1000.0)
    }
}
