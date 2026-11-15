package com.romoreno.malagapp.data.network.mapper

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * El geojson de Malaga viene en ETRS89 / UTM zona 30N (EPSG:25830), no en grados. Google Maps
 * necesita latitud/longitud, asi que se convierte aqui (ETRS89 ~ WGS84 para esta precision).
 */
object UtmConverter {

    private const val A = 6378137.0                 // semieje mayor (GRS80)
    private const val F = 1 / 298.257222101         // aplanamiento (GRS80)
    private const val K0 = 0.9996
    private const val CENTRAL_MERIDIAN_DEG = -3.0   // zona 30

    /** Devuelve (latitud, longitud). Si ya vienen en grados los respeta. */
    fun toLatLng(first: Double, second: Double): Pair<Double, Double> {
        // Un valor en grados nunca supera 180; en UTM son cientos de miles de metros
        if (abs(first) <= 180 && abs(second) <= 90) return second to first
        return utmToLatLng(first, second)
    }

    private fun utmToLatLng(easting: Double, northing: Double): Pair<Double, Double> {
        val e2 = F * (2 - F)
        val ep2 = e2 / (1 - e2)
        val x = easting - 500000.0
        val m = northing / K0
        val mu = m / (A * (1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2.pow(3) / 256))
        val e1 = (1 - sqrt(1 - e2)) / (1 + sqrt(1 - e2))

        val phi1 = mu +
                (3 * e1 / 2 - 27 * e1.pow(3) / 32) * sin(2 * mu) +
                (21 * e1 * e1 / 16 - 55 * e1.pow(4) / 32) * sin(4 * mu) +
                (151 * e1.pow(3) / 96) * sin(6 * mu) +
                (1097 * e1.pow(4) / 512) * sin(8 * mu)

        val sinPhi = sin(phi1)
        val cosPhi = cos(phi1)
        val tanPhi = tan(phi1)
        val n1 = A / sqrt(1 - e2 * sinPhi * sinPhi)
        val t1 = tanPhi * tanPhi
        val c1 = ep2 * cosPhi * cosPhi
        val r1 = A * (1 - e2) / (1 - e2 * sinPhi * sinPhi).pow(1.5)
        val d = x / (n1 * K0)

        val lat = phi1 - (n1 * tanPhi / r1) * (
                d * d / 2 -
                        (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * ep2) * d.pow(4) / 24 +
                        (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 - 252 * ep2 - 3 * c1 * c1) *
                        d.pow(6) / 720)

        val lon = (d -
                (1 + 2 * t1 + c1) * d.pow(3) / 6 +
                (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * ep2 + 24 * t1 * t1) * d.pow(5) / 120) /
                cosPhi

        return Math.toDegrees(lat) to (CENTRAL_MERIDIAN_DEG + Math.toDegrees(lon))
    }
}
