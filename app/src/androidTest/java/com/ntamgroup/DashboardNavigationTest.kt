package com.ntamgroup

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import android.view.ViewGroup
import android.webkit.WebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<DashboardActivity>()

    @Test
    fun everyQuickAccessTileOpensItsDetailAndBackReturnsToDashboard() {
        val routes = listOf(
            "Agenda" to CongressPage.Agenda,
            "Speakers" to CongressPage.Speakers,
            "Name Tag" to CongressPage.NameTag,
            "Venue" to CongressPage.Venue,
            "Voting" to CongressPage.Voting,
            "Ask Q&A" to CongressPage.Questions,
            "Resources" to CongressPage.Resources,
            "Exhibitors" to CongressPage.Exhibitors
        )
        for ((label, page) in routes) {
            compose.onNodeWithText(label).performScrollTo().performClick()
            awaitPage(page)
            pressBack()
            compose.onNodeWithText("Quick Access").assertExists()
        }
    }

    @Test
    fun dashboardSecondaryRoutesAndNestedBadgeNavigationWork() {
        compose.onNodeWithContentDescription("View all quick access").performScrollTo().performClick()
        awaitPage(CongressPage.QuickAccess)
        evaluate("document.querySelector('[aria-label=\"Open Agenda\"]').click()")
        awaitPage(CongressPage.Agenda)
        evaluate("document.querySelector('[aria-label=\"Go Back\"]').click()")
        awaitPage(CongressPage.QuickAccess)
        evaluate("document.querySelector('[aria-label=\"Open Name Tag\"]').click()")
        awaitPage(CongressPage.NameTag)
        evaluate("document.querySelector('.badge-layout').click()")
        awaitPage(CongressPage.AlternateNameTag)
        evaluate("document.querySelector('[aria-label=\"Go Back\"]').click()")
        awaitPage(CongressPage.NameTag)
        pressBack()
        awaitPage(CongressPage.QuickAccess)
        pressBack()

        compose.onNodeWithContentDescription("View all upcoming sessions").performScrollTo().performClick()
        awaitPage(CongressPage.Agenda)
        pressBack()

        compose.onNodeWithContentDescription("Filters").performScrollTo().performClick()
        awaitPage(CongressPage.QuickAccess)
        pressBack()

    }

    @Test
    fun homeAndProfileSwitchInPlaceAndKeepTheirViews() {
        val dashboard = compose.activity
        val home = dashboard.findViewById<android.view.View>(R.id.dashboard_home)
        compose.onNodeWithText("Profile").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            evaluate("document.documentElement.dataset.ready", profile = true) == "\"true\""
        }
        compose.onNodeWithText("Profile").assertIsSelected()
        assertEquals("\"My Profile\"", evaluate("document.querySelector('h1').textContent", profile = true))

        compose.onNodeWithText("Home").performClick().assertIsSelected()
        assertSame(home, dashboard.findViewById<android.view.View>(R.id.dashboard_home))
        compose.onNodeWithText("Profile").performClick().assertIsSelected()
        pressBack()
        compose.onNodeWithText("Home").assertIsSelected()

        compose.onNodeWithText("Profile").performClick().assertIsSelected()
        evaluate("document.querySelector('[aria-label=\"Go Back\"]').click()", profile = true)
        compose.onNodeWithText("Home").assertIsSelected()
        assertSame(home, dashboard.findViewById<android.view.View>(R.id.dashboard_home))
    }

    @Test
    fun detailSearchFiltersAndBookmarksUseBundledData() {
        compose.onNodeWithText("Speakers").performScrollTo().performClick()
        awaitPage(CongressPage.Speakers)
        evaluate("const input = document.querySelector('input'); input.value = 'Hassan'; input.dispatchEvent(new Event('input'))")
        assertEquals("\"Dr. Hassan Ali\"", evaluate("document.querySelector('article:not([hidden]) h3').textContent.trim()"))
        pressBack()

        compose.onNodeWithText("Resources").performScrollTo().performClick()
        awaitPage(CongressPage.Resources)
        evaluate("document.querySelectorAll('article button[aria-pressed=true]').forEach(button => button.click())")
        evaluate("document.querySelector('article button').click()")
        evaluate("Array.from(document.querySelectorAll('[data-purpose=category-filters] button')).find(b => b.textContent.trim() === 'Saved').click()")
        assertEquals("1", evaluate("document.querySelectorAll('article:not([hidden])').length"))
        evaluate("document.querySelector('article button').click()")
        assertEquals("0", evaluate("document.querySelectorAll('article:not([hidden])').length"))
    }

    @Test
    fun agendaSupportsSearchFiltersDaysResetAndPersistentBookmarks() {
        compose.onNodeWithText("Agenda").performScrollTo().performClick()
        awaitPage(CongressPage.Agenda)
        val visible = "document.querySelectorAll('article:not([hidden])').length"
        assertEquals("3", evaluate(visible))
        evaluate("document.querySelector('[aria-label=Search]').click()")
        assertEquals("false", evaluate("document.querySelector('[data-agenda-search]').hidden"))
        evaluate("""
            var search = document.querySelector('[data-agenda-search]');
            search.value = 'sarah';
            search.dispatchEvent(new Event('input'));
        """.trimIndent())
        assertEquals("\"Future of Pediatric Nutrition\"", evaluate("document.querySelector('article:not([hidden]) h4').textContent"))
        evaluate("document.querySelector('[data-agenda-reset]').click()")
        assertEquals("3", evaluate(visible))
        evaluate("Array.from(document.querySelectorAll('[data-agenda-filter] button')).find(b => b.textContent.trim() === 'Keynote').click()")
        assertEquals("2", evaluate(visible))
        evaluate("document.querySelector('[data-agenda-day=\"2\"]').click()")
        assertEquals("0", evaluate(visible))
        assertEquals("\"Day 2 Schedule\"", evaluate("document.querySelector('[data-agenda-heading]').textContent"))
        evaluate("document.querySelector('[data-agenda-day=\"1\"]').click()")
        assertEquals("2", evaluate(visible))
        evaluate("if (document.querySelector('article button').getAttribute('aria-pressed') === 'false') document.querySelector('article button').click()")
        evaluate("document.querySelector('[aria-label=\"Go Back\"]').click()")
        compose.onNodeWithContentDescription("View all upcoming sessions").performScrollTo().performClick()
        awaitPage(CongressPage.Agenda)
        assertEquals("\"true\"", evaluate("document.querySelector('article button').getAttribute('aria-pressed')"))
        evaluate("document.querySelector('article button').click()")
        assertEquals("\"false\"", evaluate("document.querySelector('article button').getAttribute('aria-pressed')"))
    }

    private fun awaitPage(page: CongressPage) {
        val expected = "\"true:${page.slug}\""
        compose.waitUntil(timeoutMillis = 15_000) {
            evaluate("document.documentElement.dataset.ready + ':' + document.body.dataset.screen") == expected
        }
        assertDetailViewportRespectsInsets()
    }

    private fun assertDetailViewportRespectsInsets() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).filterIsInstance<CongressDetailActivity>().single()
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val viewport = content.getChildAt(0) as ViewGroup
            val webView = viewport.getChildAt(0) as WebView
            val windowInsets = ViewCompat.getRootWindowInsets(viewport)
            assertNotNull("Detail viewport must receive window insets", windowInsets)
            val safe = windowInsets!!.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            assertEquals(safe.left, webView.left)
            assertEquals(safe.top, webView.top)
            assertEquals(viewport.width - safe.right, webView.right)
            assertEquals(viewport.height - safe.bottom, webView.bottom)
        }
    }

    private fun evaluate(script: String, profile: Boolean = false): String? {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val reply = java.util.concurrent.LinkedBlockingQueue<String>()
        instrumentation.runOnMainSync {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            val webView = if (profile) {
                val dashboard = resumed.filterIsInstance<DashboardActivity>().singleOrNull()
                dashboard?.supportFragmentManager?.fragments?.filterIsInstance<DashboardProfileFragment>()
                    ?.singleOrNull()?.takeUnless { it.isHidden }?.view as? WebView
            } else {
                val activity = resumed.filterIsInstance<CongressDetailActivity>().singleOrNull()
                val content = activity?.findViewById<ViewGroup>(android.R.id.content)
                val viewport = content?.getChildAt(0) as? ViewGroup
                viewport?.getChildAt(0) as? WebView
            }
            if (webView == null) reply.offer("null")
            else webView.evaluateJavascript(script) { reply.offer(it ?: "null") }
        }
        return reply.poll(3, java.util.concurrent.TimeUnit.SECONDS)
    }
}
