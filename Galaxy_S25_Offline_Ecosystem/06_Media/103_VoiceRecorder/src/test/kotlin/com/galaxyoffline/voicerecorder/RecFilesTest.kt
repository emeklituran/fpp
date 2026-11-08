package com.galaxyoffline.voicerecorder

import com.galaxyoffline.voicerecorder.data.RecFiles
import org.junit.Assert.assertEquals
import org.junit.Test

class RecFilesTest {
    @Test fun formats() {
        assertEquals("00:05", RecFiles.duration(5_400))
        assertEquals("1:01:01", RecFiles.duration(3_661_000))
        assertEquals("2 KB", RecFiles.size(1500))
        assertEquals("1,5 MB", RecFiles.size(1_572_864))
    }

    @Test fun safeNames() {
        assertEquals("Toplantı_ 1_2", RecFiles.safeName("Toplantı: 1/2"))
        assertEquals("Kayıt", RecFiles.safeName("   "))
    }
}
