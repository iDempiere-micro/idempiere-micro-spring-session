package company.bigger.test.session

import company.bigger.dto.UserLoginModel
import company.bigger.test.support.BaseTest
import company.bigger.test.support.randomString
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserServiceTests : BaseTest() {
    @Test
    fun `GardenUser can login (service)`() {
        val result = userService.login(UserLoginModel("GardenUser", "GardenUser"))
        assertNotNull(result?.token)
    }
    @Test
    fun `GardenUser can not login with wrong password (service)`() {
        val result = userService.login(UserLoginModel("GardenUser", randomString(20)))
        assertNull(result?.token)
    }

    @Test
    fun `Joe Sales cannot login as he does not have a password (service)`() {
        val result = userService.login(UserLoginModel("Joe Sales", ""))
        assertNull(result?.token)
    }
}