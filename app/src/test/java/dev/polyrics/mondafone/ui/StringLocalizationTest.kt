package dev.polyrics.mondafone.ui

import dev.polyrics.mondafone.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class StringLocalizationTest {

    @Test
    @Config(qualifiers = "es")
    fun resolvesSpanishTranslationsWhenDeviceLocaleIsSpanish() {
        val context = RuntimeEnvironment.getApplication()

        assertEquals("Mondafone", context.getString(R.string.app_name))
        assertEquals("El fin de las llamadas spam", context.getString(R.string.app_subtitle))
        assertEquals("Conceder acceso", context.getString(R.string.grant_access))
        assertEquals("Bloqueadas", context.getString(R.string.stat_blocked_calls))
        assertEquals("Números spam", context.getString(R.string.stat_spam_numbers))
        assertEquals("Dispositivos", context.getString(R.string.stat_network_devices))
        assertEquals("Pendientes", context.getString(R.string.tab_pending))
        assertEquals("Permitidos", context.getString(R.string.tab_whitelist))
        assertEquals("Bloqueados", context.getString(R.string.tab_blacklist))
        assertEquals("No hay llamadas pendientes", context.getString(R.string.empty_pending))
        assertEquals("No hay números permitidos", context.getString(R.string.empty_whitelist))
        assertEquals("No hay números bloqueados", context.getString(R.string.empty_blacklist))
        assertEquals("Depuración:", context.getString(R.string.debug_title))
        assertEquals("ID disp.: abc", context.getString(R.string.debug_device_id, "abc"))
        assertEquals("Energía: sin restricción", context.getString(R.string.debug_power_unrestricted))
        assertEquals("Energía: restringida", context.getString(R.string.debug_power_restricted))
        assertEquals("Limpiar", context.getString(R.string.clear_store))
        assertEquals("Llamada bloqueada", context.getString(R.string.blocked_call_title))
        assertEquals("¿Seguir bloqueando esta llamada?", context.getString(R.string.blocked_call_text))
        assertEquals("¿Esta llamada fue spam?", context.getString(R.string.spam_prompt_title))
        assertEquals("De +573001234567", context.getString(R.string.spam_prompt_text, "+573001234567"))
        assertEquals("Sí", context.getString(R.string.answer_yes))
        assertEquals("No", context.getString(R.string.answer_no))
        assertEquals("[ GUARDIA ACTIVA ]", context.getString(R.string.status_guard_active))
        assertEquals("[ CONFIGURACIÓN REQUERIDA ]", context.getString(R.string.status_setup_required))
        assertEquals("FILTRO", context.getString(R.string.stage_call_filter))
        assertEquals("AVISOS", context.getString(R.string.stage_notifications))
        assertEquals("RED BLE", context.getString(R.string.stage_ble_mesh))
        assertEquals("ENERGÍA", context.getString(R.string.stage_battery))
    }

    @Test
    @Config(qualifiers = "en")
    fun resolvesDefaultEnglishStringsWhenDeviceLocaleIsEnglish() {
        val context = RuntimeEnvironment.getApplication()

        assertEquals("Mondafone", context.getString(R.string.app_name))
        assertEquals("The end of spam calls", context.getString(R.string.app_subtitle))
        assertEquals("Pending", context.getString(R.string.tab_pending))
        assertEquals("Whitelist", context.getString(R.string.tab_whitelist))
        assertEquals("Blacklist", context.getString(R.string.tab_blacklist))
        assertEquals("x", context.getString(R.string.action_remove))
        assertEquals("[ ACTIVE GUARD ]", context.getString(R.string.status_guard_active))
        assertEquals("[ SETUP REQUIRED ]", context.getString(R.string.status_setup_required))
        assertEquals("FILTER", context.getString(R.string.stage_call_filter))
        assertEquals("ALERTS", context.getString(R.string.stage_notifications))
        assertEquals("BLE NET", context.getString(R.string.stage_ble_mesh))
        assertEquals("POWER", context.getString(R.string.stage_battery))
    }
}
