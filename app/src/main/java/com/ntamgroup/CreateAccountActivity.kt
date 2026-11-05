package com.ntamgroup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntamgroup.ui.theme.MyApplicationTheme

private val RegistrationGold = Color(0xFFF4BA63)
private val RegistrationBlue = Color(0xFF0560FD)
private val RegistrationMuted = Color(0xFF64748B)
private val RegistrationError = Color(0xFFB3261E)
private val RegistrationFieldShape = RoundedCornerShape(12.dp)

class CreateAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                CreateAccountScreen(
                    onSignIn = { finish() },
                    onCreateAccount = { startActivity(Intent(this, DashboardActivity::class.java)) }
                )
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CreateAccountScreen(onSignIn: () -> Unit = {}, onCreateAccount: () -> Unit = {}) {
    Box(Modifier.fillMaxSize()) {
        RegistrationBackdrop()
        BoxWithConstraints(
            Modifier.align(Alignment.TopCenter).safeDrawingPadding().imePadding()
                .widthIn(max = 430.dp).fillMaxSize()
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                RegistrationHeader()
                RegistrationCard(onCreateAccount)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Already have an account?", color = Color.White.copy(alpha = .8f), fontSize = 12.5.sp)
                    TextButton(onClick = onSignIn, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Sign In", color = RegistrationGold, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistrationHeader() {
    Column(Modifier.padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("JOIN THE", color = Color.White, fontSize = 25.sp, lineHeight = 30.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Text("NESTLÉ\nCONGRESS", color = RegistrationGold, fontSize = 26.sp, lineHeight = 30.sp,
            fontWeight = FontWeight.Black, letterSpacing = 1.3.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Create your attendee profile and access sessions, resources, voting and Q&A.",
            modifier = Modifier.widthIn(max = 270.dp),
            color = Color(0xFF8FAED9), fontSize = 12.sp, lineHeight = 19.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RegistrationCard(onCreateAccount: () -> Unit) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var organization by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val validEmail = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val canSubmit = fullName.isNotBlank() && validEmail && password.isNotEmpty() && termsAccepted
    val focusManager = LocalFocusManager.current
    val submit = {
        submitted = true
        if (canSubmit) {
            focusManager.clearFocus()
            onCreateAccount()
        }
    }
    val cardShape = RoundedCornerShape(24.dp)
    val nameKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)

    Column(
        Modifier.fillMaxWidth().shadow(12.dp, cardShape).background(Color.White, cardShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RegistrationField(
            fullName, { fullName = it }, "Full name", UserPath, nameKeyboard,
            error = if (submitted && fullName.isBlank()) "Enter your full name" else null
        )
        RegistrationField(
            email, { email = it }, "Email address", EmailPath,
            KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            error = if (submitted && !validEmail) "Enter a valid email address" else null
        )
        RegistrationField(
            organization, { organization = it }, "Hospital / Organization", OrganizationPath, nameKeyboard
        )
        RegistrationField(
            password, { password = it }, "Create password", LockPath,
            KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            error = if (submitted && password.isEmpty()) "Enter a password" else null,
            transformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailing = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    LineIcon(
                        if (passwordVisible) EyePath else EyeOffPath, RegistrationMuted, Modifier.size(16.dp),
                        label = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(termsAccepted, role = Role.Checkbox, onValueChange = { termsAccepted = it }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsAccepted, onCheckedChange = null, modifier = Modifier.padding(start = 2.dp, end = 8.dp),
                colors = CheckboxDefaults.colors(checkedColor = RegistrationBlue, uncheckedColor = RegistrationMuted)
            )
            Text(
                "I agree to the congress terms, privacy policy and attendee guidelines.",
                color = if (submitted && !termsAccepted) RegistrationError else RegistrationMuted,
                fontSize = 11.sp, lineHeight = 14.sp
            )
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).shadow(5.dp, RegistrationFieldShape)
                .clip(RegistrationFieldShape)
                .background(Brush.horizontalGradient(listOf(Color(0xFF00A3FF), Color(0xFF0055F6))))
                .clickable(role = Role.Button, onClick = submit)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LineIcon(AddUserPath, Color.White, Modifier.size(16.dp), filled = true)
            Text("Create Account", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Text(
                "OR SIGN UP WITH", Modifier.background(Color.White).padding(horizontal = 10.dp),
                color = RegistrationMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SocialButton("Sign up with Google", Modifier.weight(1f), RegistrationFieldShape) {
                Box(Modifier.size(16.dp)) {
                    GooglePaths.forEach { (path, color) ->
                        LineIcon(path, color, Modifier.matchParentSize(), filled = true)
                    }
                }
            }
            SocialButton("Sign up with Apple", Modifier.weight(1f), RegistrationFieldShape) {
                LineIcon(ApplePath, Color.Black, Modifier.size(16.dp), filled = true)
            }
        }
    }
}

@Composable
private fun RegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    icon: String,
    keyboardOptions: KeyboardOptions,
    error: String? = null,
    transformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        error != null -> RegistrationError
        focused -> RegistrationBlue
        else -> Color(0xFFE5E9F2)
    }
    Column {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = hint },
            textStyle = LocalTextStyle.current.copy(
                color = Color(0xFF1E293B), fontSize = 13.5.sp, fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(RegistrationBlue),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = transformation,
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp)
                        .border(if (focused) 2.dp else 1.dp, borderColor, RegistrationFieldShape)
                        .padding(start = 14.dp, end = if (trailing == null) 14.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LineIcon(icon, RegistrationBlue, Modifier.size(18.dp))
                    Spacer(Modifier.width(11.dp))
                    Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        if (value.isEmpty()) {
                            Text(hint, color = RegistrationMuted, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                        }
                        innerTextField()
                    }
                    trailing?.invoke()
                }
            }
        )
        if (error != null) {
            Text(error, Modifier.padding(start = 14.dp, top = 3.dp), color = RegistrationError, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RegistrationBackdrop() {
    Canvas(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF002663), Color(0xFF001D4F), Color(0xFF000E26)))
        )
    ) {
        val firstGlow = Offset(size.width * .1f, size.height * .2f)
        val secondGlow = Offset(size.width * .9f, size.height * .4f)
        val radius = size.height * .45f
        drawCircle(
            Brush.radialGradient(listOf(Color(0x731E5ABE), Color.Transparent), firstGlow, radius),
            radius, firstGlow
        )
        drawCircle(
            Brush.radialGradient(listOf(Color(0x2600B2FE), Color.Transparent), secondGlow, radius),
            radius, secondGlow
        )
        val line = Stroke(1.dp.toPx())
        drawOval(Color(0x1FF4BA63), Offset(-100.dp.toPx(), 50.dp.toPx()),
            Size(500.dp.toPx(), 300.dp.toPx()), style = line)
        drawOval(Color(0x1A00B2FE), Offset(-80.dp.toPx(), 90.dp.toPx()),
            Size(540.dp.toPx(), 320.dp.toPx()), style = line)
        drawOval(Color(0x0DFFFFFF), Offset(size.width - 330.dp.toPx(), -30.dp.toPx()),
            Size(450.dp.toPx(), 450.dp.toPx()), style = line)
    }
}

private const val EmailPath = "M3 8l7.89 5.26a2 2 0 0 0 2.22 0L21 8M5 19h14a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2z"
private const val OrganizationPath = "M19 21V5a2 2 0 0 0-2-2H7a2 2 0 0 0-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v5m-4 0h4"
private const val AddUserPath = "M9.6 10.8a3.6 3.6 0 1 0 0-7.2 3.6 3.6 0 0 0 0 7.2zM9.6 13.2a7.2 7.2 0 0 1 7.2 7.2H2.4a7.2 7.2 0 0 1 7.2-7.2zM17 8h2v3h3v2h-3v3h-2v-3h-3v-2h3z"
