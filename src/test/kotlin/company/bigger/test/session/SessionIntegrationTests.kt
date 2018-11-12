package company.bigger.test.session

import company.bigger.test.support.BaseIntegrationTest
import company.bigger.test.support.randomString
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionIntegrationTests : BaseIntegrationTest() {
    @Test
    fun contextLoads() {
    }

    @Test
    fun `GardenUser can login`() {
        val gardenUserLogin = loginClient?.login("GardenUser", "GardenUser")
        println("$gardenUserLogin")
        assertNotNull(gardenUserLogin)
        gardenUserLogin!!
        assertTrue { gardenUserLogin.logged }
    }

    @Test
    fun `Random user can not login`() {
        val gardenUserLogin = loginClient?.login(randomString(10), randomString(10))
        println("$gardenUserLogin")
        assertNotNull(gardenUserLogin)
        gardenUserLogin!!
        assertFalse { gardenUserLogin.logged }
    }
}