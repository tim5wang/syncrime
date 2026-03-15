package com.syncrime.android.permission

import org.junit.Assert.*
import org.junit.Test

class PermissionManagerTest {

    @Test
    fun `test notification permission code is correct`() {
        assertEquals("Permission code should be 1001", 1001, PermissionManager.NOTIFICATION_PERMISSION_CODE)
    }

    @Test
    fun `test permission manager constants defined`() {
        assertTrue("Permission code should be positive", PermissionManager.NOTIFICATION_PERMISSION_CODE > 0)
    }

    @Test
    fun `test permission code is within valid range`() {
        val code = PermissionManager.NOTIFICATION_PERMISSION_CODE
        assertTrue("Permission code should be between 1 and 65535", code in 1..65535)
    }

    @Test
    fun `test permission code is odd`() {
        assertTrue("Permission code should be odd", PermissionManager.NOTIFICATION_PERMISSION_CODE % 2 == 1)
    }
}
