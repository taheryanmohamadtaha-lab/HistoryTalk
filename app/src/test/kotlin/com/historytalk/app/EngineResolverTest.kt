package com.historytalk.app

import com.historytalk.app.data.repo.AppSettings
import com.historytalk.app.data.repo.EngineModes
import com.historytalk.app.engine.DeviceInfo
import com.historytalk.app.engine.EngineResolver
import com.historytalk.app.engine.RuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineResolverTest {
    private val strong = DeviceInfo(totalRamMb = 6000, is64Bit = true)
    private val weak = DeviceInfo(totalRamMb = 2000, is64Bit = false)

    @Test fun noModelMeansBaseMode() {
        val s = EngineResolver.compute(AppSettings(), RuntimeState.Unloaded, null, strong)
        assertFalse(s.useModel)
        assertEquals(EngineResolver.BASE_LABEL, s.label)
    }

    @Test fun weakDeviceFallsBackToBaseInAutoMode() {
        val s = EngineResolver.compute(AppSettings(), RuntimeState.Ready, "m.task", weak)
        assertFalse(s.useModel)
        assertTrue(s.detail.contains("مناسب نیست"))
    }

    @Test fun readyModelOnStrongDeviceIsUsed() {
        val s = EngineResolver.compute(AppSettings(), RuntimeState.Ready, "m.task", strong)
        assertTrue(s.useModel)
        assertEquals(EngineResolver.MODEL_LABEL, s.label)
    }

    @Test fun manualBaseModeWinsEvenWithModel() {
        val s = EngineResolver.compute(AppSettings(engineMode = EngineModes.BASE), RuntimeState.Ready, "m.task", strong)
        assertFalse(s.useModel)
        assertFalse(s.wantsModel)
    }

    @Test fun failedModelFallsBackAndExplains() {
        val s = EngineResolver.compute(AppSettings(), RuntimeState.Failed("OOM"), "m.task", strong)
        assertFalse(s.useModel)
        assertTrue(s.detail.contains("OOM"))
    }

    @Test fun unloadedModelIsWantedButNotYetUsed() {
        val s = EngineResolver.compute(AppSettings(), RuntimeState.Unloaded, "m.task", strong)
        assertFalse(s.useModel)
        assertTrue(s.wantsModel)
    }
}
