package com.viettel.appbase.feature.scan

import kotlinx.serialization.Serializable

/**
 * Demo AI task. A real app defines its own task id + result model, the Worker holds the matching
 * prompt and JSON schema, and the fake sample keeps the mock flavor working offline.
 */
object DemoScan {
    const val TASK = "identify_object"

    val fakeSamples = mapOf(
        TASK to """
            {
              "recognized": true,
              "name": "Bọ rùa bảy chấm",
              "scientificName": "Coccinella septempunctata",
              "confidence": 0.92,
              "summary": "Côn trùng có ích, ăn rệp và sâu non hại cây. Không gây hại cho người.",
              "advice": [
                "Không cần diệt: bọ rùa giúp kiểm soát rệp tự nhiên.",
                "Hạn chế phun thuốc trừ sâu phổ rộng ở khu vực có bọ rùa."
              ]
            }
        """.trimIndent(),
    )
}

/** Mirrors the JSON schema of the identify_object task on the Worker. */
@Serializable
data class DemoScanResult(
    val recognized: Boolean,
    val name: String? = null,
    val scientificName: String? = null,
    val confidence: Double? = null,
    val summary: String? = null,
    val advice: List<String> = emptyList(),
)
