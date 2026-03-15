package com.syncrime.android.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {
    
    const val NOTIFICATION_PERMISSION_CODE = 1001
    const val STORAGE_PERMISSION_CODE = 1002
    const val LOCATION_PERMISSION_CODE = 1003
    
    enum class PermissionType {
        NOTIFICATIONS,
        STORAGE,
        LOCATION
    }
    
    data class PermissionState(
        val type: PermissionType,
        val isGranted: Boolean,
        val shouldShowRationale: Boolean
    )
    
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        }
    }
    
    fun requestStoragePermission(activity: Activity) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        ActivityCompat.requestPermissions(activity, permissions, STORAGE_PERMISSION_CODE)
    }
    
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_CODE
        )
    }
    
    fun shouldShowNotificationRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            false
        }
    }
    
    fun shouldShowStorageRationale(activity: Activity): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    fun shouldShowLocationRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    fun getPermissionState(context: Context, type: PermissionType): PermissionState {
        val activity = context as? Activity
        return when (type) {
            PermissionType.NOTIFICATIONS -> PermissionState(
                type = type,
                isGranted = hasNotificationPermission(context),
                shouldShowRationale = activity?.let { shouldShowNotificationRationale(it) } ?: false
            )
            PermissionType.STORAGE -> PermissionState(
                type = type,
                isGranted = hasStoragePermission(context),
                shouldShowRationale = activity?.let { shouldShowStorageRationale(it) } ?: false
            )
            PermissionType.LOCATION -> PermissionState(
                type = type,
                isGranted = hasLocationPermission(context),
                shouldShowRationale = activity?.let { shouldShowLocationRationale(it) } ?: false
            )
        }
    }
    
    fun getAllPermissionStates(context: Context): List<PermissionState> {
        return PermissionType.values().map { getPermissionState(context, it) }
    }
    
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasNotificationPermission(context)
    }
}
