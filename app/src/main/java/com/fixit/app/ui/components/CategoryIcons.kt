package com.fixit.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Heuristic mapping from category name → built-in icon. The backend's
 * Category model has an optional icon_url, but seed data may not populate
 * it; this fallback keeps the home screen looking right regardless.
 */
fun iconForCategory(name: String?): ImageVector {
    val key = name?.lowercase()?.trim().orEmpty()
    return when {
        "plumb"   in key                         -> Icons.Filled.Plumbing
        "electric" in key                        -> Icons.Filled.ElectricBolt
        "clean"   in key                         -> Icons.Filled.CleaningServices
        "hvac"    in key || "air"  in key
                || "heating" in key || "cool" in key -> Icons.Filled.AcUnit
        "paint"   in key                         -> Icons.Filled.FormatPaint
        "carpenter" in key || "wood" in key      -> Icons.Filled.Carpenter
        "appliance" in key                       -> Icons.Filled.HomeRepairService
        "garden" in key || "landscape" in key
                || "lawn" in key                     -> Icons.Filled.Yard
        "moving" in key || "haul" in key         -> Icons.Filled.LocalShipping
        else                                     -> Icons.Filled.Build
    }
}