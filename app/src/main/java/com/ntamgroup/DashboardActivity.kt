package com.ntamgroup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntamgroup.ui.theme.MyApplicationTheme

private val DashboardNavy = Color(0xFF030E2B)
private val Cyan = Color(0xFF00D2FF)
private val SoftBlue = Color(0xFFA9BDE3)
private val GlassBorder = Color.White.copy(alpha = .10f)
private val TileShape = RoundedCornerShape(16.dp)

class DashboardActivity : FragmentActivity() {
    private var profileSelected by mutableStateOf(false)
    private var profileReady = false
    private var requestedProfile = false
    private val returnToHome = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = selectTab(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        profileSelected = savedInstanceState?.getBoolean(ProfileSelectedKey) == true
        requestedProfile = profileSelected
        returnToHome.isEnabled = profileSelected
        onBackPressedDispatcher.addCallback(this, returnToHome)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.rgb(3, 21, 53))
            addView(FragmentContainerView(this@DashboardActivity).apply {
                id = R.id.dashboard_tabs
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(ComposeView(this@DashboardActivity).apply {
                setContent { MyApplicationTheme { DashboardNavigation(profileSelected, ::selectTab) } }
            })
        }
        setContentView(content)
        content.applyCongressInsets()

        if (supportFragmentManager.findFragmentByTag(HomeTag) == null) {
            val home = DashboardHomeFragment()
            val profile = DashboardProfileFragment()
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.dashboard_tabs, home, HomeTag)
                .add(R.id.dashboard_tabs, profile, ProfileTag)
                .hide(if (profileSelected) home else profile)
                .commitNow()
        }
    }

    internal fun selectTab(profile: Boolean) {
        requestedProfile = profile
        returnToHome.isEnabled = profileSelected || requestedProfile
        if (profile == profileSelected || (profile && !profileReady) || supportFragmentManager.isStateSaved) return

        val home = checkNotNull(supportFragmentManager.findFragmentByTag(HomeTag))
        val attendee = checkNotNull(supportFragmentManager.findFragmentByTag(ProfileTag))
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .hide(if (profile) home else attendee)
            .show(if (profile) attendee else home)
            .commitNow()
        profileSelected = profile
        returnToHome.isEnabled = profile
    }

    internal fun onProfileReady() {
        profileReady = true
        selectTab(requestedProfile)
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        selectTab(requestedProfile)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ProfileSelectedKey, profileSelected)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val HomeTag = "home"
        const val ProfileTag = "profile"
        const val ProfileSelectedKey = "profile-selected"
    }
}

class DashboardHomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.dashboard_home
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyApplicationTheme {
                    DashboardScreen { CongressDetailActivity.open(requireActivity(), it) }
                }
            }
        }
}

class DashboardProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val dashboard = requireActivity() as DashboardActivity
        return createCongressWebView(
            dashboard, CongressPage.Profile, onLoaded = dashboard::onProfileReady
        ) { action ->
            if (action == "back") dashboard.selectTab(false)
        }
    }

    override fun onDestroyView() {
        val webView = view as? WebView
        (webView?.parent as? ViewGroup)?.removeView(webView)
        webView?.destroy()
        super.onDestroyView()
    }
}


@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun DashboardPreview() = DashboardScreen {}

@Composable
internal fun DashboardScreen(onOpen: (CongressPage) -> Unit) {
    Box(Modifier.fillMaxSize().background(DashboardNavy)) {
        DashboardBackdrop()
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).align(Alignment.CenterHorizontally)
                    .widthIn(max = 440.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                DashboardHeader()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Welcome Back,\nDr. Testing 👋",
                    Modifier.semantics { heading() },
                    color = Color.White, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(20.dp))
                DashboardSearch { onOpen(CongressPage.QuickAccess) }
                Spacer(Modifier.height(20.dp))
                FeaturedEvent()
                Spacer(Modifier.height(24.dp))
                QuickAccess(onOpen)
                Spacer(Modifier.height(24.dp))
                DashboardSectionTitle("Upcoming Sessions") { onOpen(CongressPage.Agenda) }
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "NESTLÉ MEDICAL CONGRESS", Modifier.weight(1f).padding(end = 8.dp),
            color = Color(0xFF9BB0D9), fontSize = 11.sp, letterSpacing = 2.4.sp, fontWeight = FontWeight.Bold
        )
        Box(
            Modifier.size(36.dp).background(Color(0xB30A2355), CircleShape).border(1.dp, GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            LineIcon(BellPath, Color(0xFF8EA7D6), Modifier.size(16.dp), label = "Notifications")
            Box(
                Modifier.align(Alignment.TopEnd).padding(3.dp).size(9.dp)
                    .background(Color(0xFFFF3B30), CircleShape).border(1.dp, Color(0xFF051336), CircleShape)
            )
        }
    }
}

@Composable
private fun DashboardSearch(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 42.dp).background(Color(0xB30A1E4A), CircleShape)
            .border(1.dp, GlassBorder, CircleShape).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).padding(start = 16.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LineIcon(SearchPath, Color(0xFF738AB5), Modifier.size(16.dp))
        Text(
            "Search sessions, speakers, topics", Modifier.weight(1f).padding(horizontal = 12.dp),
            color = Color(0xFF91A6CB), fontSize = 12.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF00BFFF))
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            LineIcon(FilterPath, Color(0xFF031D45), Modifier.size(16.dp), label = "Filters")
        }
    }
}

@Composable
private fun FeaturedEvent() {
    Row(
        Modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFF0C2763), Color(0xFF0A2052), Color(0xFF07163C))),
                TileShape
            )
            .border(1.dp, GlassBorder, TileShape).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                "FEATURED EVENT",
                Modifier.border(1.dp, Color(0xCCD49938), CircleShape).padding(horizontal = 10.dp, vertical = 2.dp),
                color = Color(0xFFF6AB32), fontSize = 9.sp, lineHeight = 13.sp,
                fontWeight = FontWeight.Bold, letterSpacing = .5.sp
            )
            Text(
                "The Next Era of Nutrition & Health", color = Color.White, fontSize = 16.sp,
                lineHeight = 20.sp, fontWeight = FontWeight.Bold
            )
            Text("May 22 - 23, 2026  •  Dubai, UAE", color = Color(0xFF9BB0D9), fontSize = 11.sp)
        }
        Box(
            Modifier.size(56.dp)
                .background(Brush.radialGradient(listOf(Color(0xFF174789), Color(0xFF0B2B62))), CircleShape)
                .border(1.dp, Cyan.copy(alpha = .20f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            LineIcon(LotusPath, Color.White, Modifier.size(32.dp), filled = true)
        }
    }
}

@Composable
private fun QuickAccess(onOpen: (CongressPage) -> Unit) {
    DashboardSectionTitle("Quick Access") { onOpen(CongressPage.QuickAccess) }
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickLinks.chunked(4).forEach { links ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                links.forEach { (page, path) ->
                    Column(
                        Modifier.weight(1f).heightIn(min = 72.dp)
                            .clip(TileShape).background(Color(0x99091F4F))
                            .clickable(role = Role.Button) { onOpen(page) }
                            .border(1.dp, Color.White.copy(alpha = .05f), TileShape)
                            .padding(horizontal = 3.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        LineIcon(path, Cyan, Modifier.size(20.dp), filled = path == VotingPath)
                        Text(if (page == CongressPage.NameTag) "Name Tag" else if (page == CongressPage.Voting) "Voting" else page.title, color = SoftBlue, fontSize = 10.sp, lineHeight = 12.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSectionTitle(title: String, onViewAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f).semantics { heading() }, color = Color.White, fontSize = 14.sp,
            fontWeight = FontWeight.Bold, letterSpacing = .3.sp)
        Text(
            "View All ›",
            Modifier.clickable(role = Role.Button, onClick = onViewAll)
                .semantics { contentDescription = "View all ${title.lowercase()}" }.padding(vertical = 6.dp),
            color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DashboardNavigation(profileSelected: Boolean, onSelect: (Boolean) -> Unit) {
    val profileShape = RoundedCornerShape(16.dp)
    val homeActive = !profileSelected
    Column(Modifier.fillMaxWidth().background(Color(if (profileSelected) 0xFF031535 else 0xFF06153A))) {
        HorizontalDivider(color = GlassBorder)
        Row(
            Modifier.align(Alignment.CenterHorizontally).widthIn(max = 400.dp)
                .fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 24.dp, vertical = 6.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.widthIn(min = 96.dp).heightIn(min = 48.dp).clip(CircleShape)
                        .then(if (homeActive) Modifier.background(Color(0xFF0A275F))
                            .border(1.dp, Cyan.copy(alpha = .30f), CircleShape) else Modifier)
                        .selectable(homeActive, role = Role.Tab) { onSelect(false) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LineIcon(HomePath, if (homeActive) Cyan else Color(0xFF94A3B8), Modifier.size(20.dp), filled = true)
                    Text("Home", color = if (homeActive) Color.White else Color(0xFF94A3B8), fontSize = 10.sp,
                        fontWeight = if (homeActive) FontWeight.Bold else FontWeight.Medium)
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (profileSelected) {
                    Row(
                        Modifier.heightIn(min = 48.dp).clip(profileShape)
                            .background(Brush.verticalGradient(listOf(Color(0xFF0250C5), Color(0xFF003C96))))
                            .border(1.dp, Color(0x4D60A5FA), profileShape)
                            .drawWithContent {
                                drawContent()
                                val edge = 1.dp.toPx()
                                drawLine(Color.White.copy(alpha = .20f), Offset(0f, edge),
                                    Offset(size.width, edge), strokeWidth = edge)
                            }
                            .selectable(true, role = Role.Tab) { onSelect(true) }.padding(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LineIcon(ProfilePath, Color(0xFF67E8F9), Modifier.size(16.dp))
                        Text("Profile", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        Modifier.heightIn(min = 48.dp).clip(CircleShape)
                            .selectable(false, role = Role.Tab) { onSelect(true) }.padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LineIcon(ProfilePath, Color(0xFF7A93C2), Modifier.size(20.dp))
                        Text("Profile", color = Color(0xFF7A93C2), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}


@Composable
private fun DashboardBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val glows = listOf(
            Offset(size.width * .10f, size.height * .20f) to Color(0x73144BB4),
            Offset(size.width * .85f, size.height * .60f) to Color(0x590D3282),
            Offset(size.width * .50f, size.height * .90f) to Color(0x80061C50)
        )
        val radius = size.maxDimension * .48f
        glows.forEach { (center, color) ->
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
        }
    }
}

private val QuickLinks = listOf(
    CongressPage.Agenda to "M6 4h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3zM16 2v4M8 2v4M3 10h18M8 14h.1M12 14h.1M16 14h.1M8 18h.1M12 18h.1",
    CongressPage.Speakers to "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M13 7a4 4 0 1 0-8 0 4 4 0 1 0 8 0M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75",
    CongressPage.NameTag to "M5.5 6h13a2.5 2.5 0 0 1 2.5 2.5v10.5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8.5A2.5 2.5 0 0 1 5.5 6zM9 6V4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2M14 11a2 2 0 1 0-4 0 2 2 0 1 0 4 0M9 17h6",
    CongressPage.Venue to "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0zM15 10a3 3 0 1 0-6 0 3 3 0 1 0 6 0",
    CongressPage.Voting to VotingPath,
    CongressPage.Questions to "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z",
    CongressPage.Resources to "M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z",
    CongressPage.Exhibitors to "M4 7h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zM16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16M9 12h6"
)

private const val VotingPath = "M5 11h2a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1zM11 5h2a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zM17 9h2a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V10a1 1 0 0 1 1-1z"
private const val BellPath = "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0"
private const val SearchPath = "M19 11a8 8 0 1 0-16 0 8 8 0 1 0 16 0M21 21l-4.35-4.35"
private const val FilterPath = "M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"
private const val LotusPath = "M12 3C12 3 9.5 7.5 9.5 10.5C9.5 11.9 10.6 13 12 13C13.4 13 14.5 11.9 14.5 10.5C14.5 7.5 12 3 12 3ZM7.8 8.8C6.1 10.1 4.5 12.8 5 15.3C5.3 16.6 6.3 17.5 7.6 17.3C9.4 17.1 11.2 15 11.6 13.5C10.1 13.2 8.7 12 7.8 8.8ZM16.2 8.8C17.9 10.1 19.5 12.8 19 15.3C18.7 16.6 17.7 17.5 16.4 17.3C14.6 17.1 12.8 15 12.4 13.5C13.9 13.2 15.3 12 16.2 8.8Z"
private const val HomePath = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"
private const val ProfilePath = "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M16 7a4 4 0 1 0-8 0 4 4 0 1 0 8 0"
