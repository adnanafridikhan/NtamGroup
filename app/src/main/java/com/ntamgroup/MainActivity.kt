package com.ntamgroup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntamgroup.ui.theme.MyApplicationTheme

private val Navy = Color(0xFF021844)
private val Ink = Color(0xFF051C44)
private val Blue = Color(0xFF0070F3)
private val Gold = Color(0xFFFFB82E)
private val Muted = Color(0xFF64748B)
private val Outline = Color(0xFFE2E8F0)
private val Hint = Color(0xFF64748B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                CongressSignIn(
                    onCreateAccount = { startActivity(Intent(this, CreateAccountActivity::class.java)) },
                    onSignIn = { startActivity(Intent(this, DashboardActivity::class.java)) }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CongressSignIn(onCreateAccount: () -> Unit = {}, onSignIn: () -> Unit = {}) {
    Box(Modifier.fillMaxSize().background(Navy)) {
        CongressBackdrop()
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
                .safeDrawingPadding()
                .imePadding()
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CongressBrand()
            Spacer(Modifier.height(16.dp))
            SignInCard(onSignIn)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don’t have an account?", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                TextButton(onClick = onCreateAccount) {
                    Text("Create Account", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(3.dp))
                    LineIcon(ArrowPath, Gold, Modifier.size(14.dp))
                }
            }
            CongressFeatures()
        }
    }
}

@Composable
private fun CongressBrand() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val capsule = RoundedCornerShape(50)
        Text(
            "NESTLÉ MEDICAL CONGRESS",
            modifier = Modifier.background(Color(0xFF061D4A).copy(alpha = .6f), capsule)
                .border(1.dp, Gold.copy(alpha = .8f), capsule)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            color = Color(0xFFF8C465),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp
        )
        Spacer(Modifier.height(12.dp))
        Text("NESTLÉ", color = Color.White, fontSize = 27.sp, lineHeight = 30.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Text("CONGRESS", color = Gold, fontSize = 29.sp, lineHeight = 31.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.size(44.dp, 3.dp).background(Gold, capsule))
        Spacer(Modifier.height(8.dp))
        Text(
            "The Next Era of Nutrition & Health",
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            letterSpacing = .4.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SignInCard(onSignIn: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val validEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val invalidEmail = submitted && !validEmail
    val missingPassword = submitted && password.isEmpty()
    val shape = RoundedCornerShape(28.dp)

    Column(
        Modifier.fillMaxWidth().shadow(14.dp, shape).background(Color.White, shape).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Welcome Back", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sign in to continue your congress experience",
                    color = Muted, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium
                )
            }
            Box(
                Modifier.size(48.dp).background(
                    Brush.verticalGradient(listOf(Blue, Color(0xFF0052CC))),
                    RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                LineIcon(ShieldPath, Color.White, Modifier.size(24.dp), filled = true)
            }
        }
        Spacer(Modifier.height(16.dp))
        CongressField(
            value = email,
            onValueChange = { email = it },
            hint = "Enter your email",
            icon = UserPath,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            error = if (invalidEmail) "Enter a valid email address" else null
        )
        Spacer(Modifier.height(12.dp))
        CongressField(
            value = password,
            onValueChange = { password = it },
            hint = "Enter your password",
            icon = LockPath,
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            error = if (missingPassword) "Enter your password" else null,
            transformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    val label = if (passwordVisible) "Hide password" else "Show password"
                    LineIcon(if (passwordVisible) EyePath else EyeOffPath, Muted, Modifier.size(16.dp), label = label)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        val pill = RoundedCornerShape(50)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).shadow(5.dp, pill).clip(pill)
                .background(Brush.horizontalGradient(listOf(Color(0xFF0038C8), Color(0xFF005CF0), Color(0xFF00B4F8))))
                .clickable(role = Role.Button) {
                    submitted = true
                    if (validEmail && password.isNotEmpty()) onSignIn()
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sign In", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            LineIcon(ArrowPath, Color.White, Modifier.size(16.dp))
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
            HorizontalDivider(color = Outline)
            Text(
                "OR CONTINUE WITH",
                Modifier.background(Color.White).padding(horizontal = 10.dp),
                color = Muted, fontSize = 9.sp, letterSpacing = .6.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SocialButton("Continue with Google", Modifier.weight(1f)) {
                Box(Modifier.size(16.dp)) {
                    GooglePaths.forEach { (path, color) ->
                        LineIcon(path, color, Modifier.matchParentSize(), filled = true)
                    }
                }
            }
            SocialButton("Continue with Apple", Modifier.weight(1f)) {
                LineIcon(ApplePath, Color.Black, Modifier.size(16.dp), filled = true)
            }
        }
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            LineIcon(LockPath, Blue, Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Forgot password?", color = Color(0xFF0062EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CongressField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    icon: String,
    keyboard: KeyboardOptions,
    error: String?,
    transformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        placeholder = { Text(hint, color = Hint, fontSize = 12.sp) },
        leadingIcon = { LineIcon(icon, Blue, Modifier.size(16.dp)) },
        trailingIcon = trailing,
        supportingText = error?.let { message -> { Text(message) } },
        isError = error != null,
        keyboardOptions = keyboard,
        visualTransformation = transformation,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            errorTextColor = Ink,
            cursorColor = Blue,
            focusedBorderColor = Blue,
            unfocusedBorderColor = Outline,
            focusedContainerColor = Color(0xFFFAFCFE),
            unfocusedContainerColor = Color(0xFFFAFCFE),
            errorContainerColor = Color(0xFFFAFCFE),
            errorBorderColor = Color(0xFFB3261E),
            errorSupportingTextColor = Color(0xFFB3261E)
        )
    )
}

@Composable
internal fun SocialButton(
    label: String,
    modifier: Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = {},
        modifier = modifier.heightIn(min = 44.dp),
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(Modifier.semanticsLabel(label), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun CongressFeatures() {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().background(Color(0xB306245C), shape)
            .border(1.dp, Color.White.copy(alpha = .10f), shape).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Feature("Global\nExperts", GroupPath, Modifier.weight(1f))
        VerticalDivider(Modifier.height(30.dp), color = Color.White.copy(alpha = .10f))
        Feature("Knowledge\nSessions", CalendarPath, Modifier.weight(1.12f))
        VerticalDivider(Modifier.height(30.dp), color = Color.White.copy(alpha = .10f))
        Feature("Networking\nOpportunities", StarPath, Modifier.weight(1.2f), Gold)
    }
}

@Composable
private fun Feature(label: String, icon: String, modifier: Modifier, tint: Color = Color(0xFF67E8F9)) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).background(Color(0xB30052CC), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { LineIcon(icon, tint, Modifier.size(16.dp)) }
        Spacer(Modifier.width(5.dp))
        Text(label, color = Color.White.copy(alpha = .9f), fontSize = 9.5.sp, lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CongressBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val glows = listOf(
            Triple(Offset(width * .95f, height * .21f), width * .53f, Color(0x662563EB)),
            Triple(Offset(0f, height * .50f), width * .56f, Color(0x660D4599)),
            Triple(Offset(width * .36f, height * .97f), width * .70f, Color(0x99002B6D))
        )
        glows.forEach { (center, radius, color) ->
            drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius), radius, center)
        }
        for (column in 0..9) {
            for (row in 0..9) {
                drawCircle(Color.White.copy(alpha = .06f), 1.2.dp.toPx(),
                    Offset((column * 16 + 8).dp.toPx(), (row * 16 + 8).dp.toPx()))
            }
        }
        val waves = listOf(
            "M-50 140 C120 180 260 80 420 110",
            "M-80 320 C140 370 280 200 450 250",
            "M-40 650 C100 580 260 760 440 680"
        )
        scale(width / 390f, height / 844f, Offset.Zero) {
            waves.forEach { wave ->
                drawPath(PathParser().parsePathString(wave).toPath(), Color.White.copy(alpha = .07f),
                    style = Stroke(width = 1.2f))
            }
        }
    }
}

@Composable
internal fun LineIcon(
    data: String,
    tint: Color,
    modifier: Modifier,
    label: String? = null,
    filled: Boolean = false
) {
    val path: Path = remember(data) { PathParser().parsePathString(data).toPath() }
    Canvas(if (label == null) modifier else modifier.semanticsLabel(label)) {
        scale(size.width / 24f, size.height / 24f, Offset.Zero) {
            if (filled) drawPath(path, tint)
            else drawPath(path, tint, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private fun Modifier.semanticsLabel(label: String): Modifier =
    semantics { contentDescription = label }

private const val ArrowPath = "M14 5l7 7-7 7M21 12H3"
internal const val EyePath = "M2 12c2-4 5.5-7 10-7s8 3 10 7c-2 4-5.5 7-10 7S4 16 2 12zM15 12a3 3 0 1 1 -6 0 3 3 0 1 1 6 0z"

private const val CalendarPath = "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 0 0 2-2V7a2 2 0 0 0 -2-2H5a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2z"
internal const val ApplePath = "M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.05-.03.07-.42 1.44-1.38 2.82M15.97 6.37c.62-.75 1.04-1.8 0.92-2.85-.9.04-1.99.6-2.63 1.35-.57.65-1.07 1.72-.94 2.74 1 .08 2.03-.49 2.65-1.24z"
private const val StarPath = "M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 0 0 .95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 0 0 -.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 0 0 -1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 0 0 -.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 0 0 .951-.69l1.519-4.674z"
internal const val EyeOffPath = "M13.875 18.825A10.05 10.05 0 0 1 12 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 0 1 1.563-3.029m5.858.908a3 3 0 1 1 4.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18"
internal const val LockPath = "M12 15v2m-6 4h12a2 2 0 0 0 2-2v-6a2 2 0 0 0 -2-2H6a2 2 0 0 0 -2 2v6a2 2 0 0 0 2 2zm10-10V7a4 4 0 0 0 -8 0v4h8z"
private const val ShieldPath = "M12 2L4 5v6.09c0 5.05 3.41 9.76 8 10.91 4.59-1.15 8-5.86 8-10.91V5l-8-3zm1 12.5h-2v-2.5H8.5v-2H11V7.5h2V10h2.5v2H13v2.5z"
internal const val UserPath = "M16 7a4 4 0 1 1 -8 0 4 4 0 0 1 8 0zM12 14a7 7 0 0 0 -7 7h14a7 7 0 0 0 -7-7z"
private const val GroupPath = "M17 20h5v-2a3 3 0 0 0 -5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 0 1 5.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 0 1 9.288 0M15 7a3 3 0 1 1 -6 0 3 3 0 0 1 6 0zm6 3a2 2 0 1 1 -4 0 2 2 0 0 1 4 0zM7 10a2 2 0 1 1 -4 0 2 2 0 0 1 4 0z"

internal val GooglePaths = listOf(
    "M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.8-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z" to Color(0xFF4285F4),
    "M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z" to Color(0xFF34A853),
    "M5.28 14.27A7.16 7.16 0 0 1 4.9 12c0-.79.14-1.57.38-2.27V6.58H1.25A11.96 11.96 0 0 0 0 12c0 1.92.45 3.74 1.25 5.42l4.03-3.15z" to Color(0xFFFBBC05),
    "M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z" to Color(0xFFEA4335),
)
