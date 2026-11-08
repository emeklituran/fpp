package com.galaxyoffline.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class PermissionUse(val permission: String, val reason: String)

/** Hakkında ekranında gösterilen uygulama kimliği. */
@Immutable
data class AppInfo(
    val name: String,
    val description: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val icon: ImageVector,
    val permissions: List<PermissionUse> = emptyList(),
    val limitations: List<String> = emptyList(),
)
