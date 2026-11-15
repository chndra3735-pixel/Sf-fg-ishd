package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SwarDisplayFont = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
)

val SwarBodyFont = FontFamily(
  Font(R.font.noto_sans_devanagari, FontWeight.Normal),
)

val SwarTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = SwarDisplayFont,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = SwarBodyFont,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = SwarBodyFont,
    fontWeight = FontWeight.Bold,
    fontSize = 19.sp,
    lineHeight = 26.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = SwarBodyFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  titleSmall = TextStyle(
    fontFamily = SwarBodyFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = SwarBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = SwarBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = SwarBodyFont,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = SwarBodyFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = SwarDisplayFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = SwarDisplayFont,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
  ),
)
