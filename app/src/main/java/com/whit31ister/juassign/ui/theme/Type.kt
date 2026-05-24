package com.whit31ister.juassign.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.whit31ister.juassign.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal)
)

val Typography = Typography(
    displayLarge = androidx.compose.material3.Typography().displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = androidx.compose.material3.Typography().displayMedium.copy(fontFamily = InterFontFamily),
    displaySmall = androidx.compose.material3.Typography().displaySmall.copy(fontFamily = InterFontFamily),
    headlineLarge = androidx.compose.material3.Typography().headlineLarge.copy(fontFamily = InterFontFamily),
    headlineMedium = androidx.compose.material3.Typography().headlineMedium.copy(fontFamily = InterFontFamily),
    headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(fontFamily = InterFontFamily),
    titleLarge = androidx.compose.material3.Typography().titleLarge.copy(fontFamily = InterFontFamily),
    titleMedium = androidx.compose.material3.Typography().titleMedium.copy(fontFamily = InterFontFamily),
    titleSmall = androidx.compose.material3.Typography().titleSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(fontFamily = InterFontFamily),
    bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(fontFamily = InterFontFamily),
    bodySmall = androidx.compose.material3.Typography().bodySmall.copy(fontFamily = InterFontFamily),
    labelLarge = androidx.compose.material3.Typography().labelLarge.copy(fontFamily = InterFontFamily),
    labelMedium = androidx.compose.material3.Typography().labelMedium.copy(fontFamily = InterFontFamily),
    labelSmall = androidx.compose.material3.Typography().labelSmall.copy(fontFamily = InterFontFamily)
)
