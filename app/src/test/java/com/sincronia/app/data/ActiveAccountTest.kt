package com.sincronia.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sincronia.app.core.auth.AccountStore
import com.sincronia.app.data.demo.DemoPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ActiveAccountTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val accounts = AccountStore(context)
    private val demo = DemoPreferences(context)
    private val active = ActiveAccount(demo, accounts)

    @After fun tearDown() = runBlocking {
        accounts.clear()
        demo.clear()
    }

    @Test fun `current is null only when nobody is signed in, without waiting forever`() = runBlocking {
        assertNull(active.current())
    }

    @Test fun `current returns the stored Google account`() = runBlocking {
        accounts.save("sub-123", "x@y.z", null, null)
        assertEquals("sub-123", active.current())
    }
}
