package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeerRoleResolverTest {

    private val resolver = PeerRoleResolver()

    @Test
    fun higherIdBecomesCentral() {
        assertEquals(PeerRole.CENTRAL, resolver.roleFor("b", "a"))
    }

    @Test
    fun lowerIdBecomesPeripheral() {
        assertEquals(PeerRole.PERIPHERAL, resolver.roleFor("a", "b"))
    }

    @Test
    fun identicalIdsHaveNoRole() {
        assertNull(resolver.roleFor("same-id", "same-id"))
    }

    @Test
    fun oppositeSidesReachComplementaryRoles() {
        assertEquals(PeerRole.CENTRAL, resolver.roleFor("z", "a"))
        assertEquals(PeerRole.PERIPHERAL, resolver.roleFor("a", "z"))
    }

    @Test
    fun comparisonIsLexicographic() {
        assertEquals(PeerRole.PERIPHERAL, resolver.roleFor("10", "9"))
        assertEquals(PeerRole.CENTRAL, resolver.roleFor("9", "10"))
    }
}
