import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// هوية "King Gold" البصرية: مستوحاة من دفتر الصائغ ودمغة العيار،
/// مش من قوالب الفينتك الجاهزة. دهب نحاسي عتيق بدل الدهب اللامع،
/// وأسود دافئ بدل الأسود الخام، وورقة فاتورة بدل كروت SaaS الموحّدة.
class AppColors {
  AppColors._();

  static const ink = Color(0xFF17130D); // أسود دافئ، مش خام
  static const inkSoft = Color(0xFF241E14);
  static const parchment = Color(0xFFF6EFE2); // خلفية ورق الدفتر
  static const parchmentDeep = Color(0xFFEDE2CB);
  static const brass = Color(0xFF9C6B2E); // دهب نحاسي عتيق — الفعل الأساسي
  static const brassLight = Color(0xFFD4A24C); // دهب فاتح — للتمييز فقط
  static const seal = Color(0xFF6E4A1F); // لون الدمغة/الختم
  static const trust = Color(0xFF2F4A3D); // أخضر الثقة/الضمان، مش أخضر النجاح التقليدي
  static const trustSoft = Color(0xFFE4ECE5);
  static const alert = Color(0xFFB5533C); // تيراكوتا صدئ بدل الأحمر الصريح
  static const alertSoft = Color(0xFFF4E3DD);
  static const line = Color(0xFFDFD0A8);
  static const muted = Color(0xFF8A7A5C);
}

class AppText {
  AppText._();

  // عنوان بخط عربي تقليدي (أميري) يحمل طابع "الدمغة" والهوية،
  // ومتن بخط عربي حديث (كايرو) للقراءة اليومية — تناقض مقصود.
  static TextStyle display = GoogleFonts.amiri(
    color: AppColors.ink,
    fontWeight: FontWeight.w700,
    height: 1.25,
  );
  static TextStyle body = GoogleFonts.cairo(color: AppColors.ink, height: 1.5);
}

ThemeData buildAppTheme() {
  final base = ThemeData(
    useMaterial3: true,
    scaffoldBackgroundColor: AppColors.parchment,
    colorScheme: ColorScheme.fromSeed(
      seedColor: AppColors.brass,
      brightness: Brightness.light,
      surface: AppColors.parchment,
    ),
    fontFamily: GoogleFonts.cairo().fontFamily,
  );

  return base.copyWith(
    textTheme: base.textTheme.apply(
      bodyColor: AppColors.ink,
      displayColor: AppColors.ink,
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: AppColors.parchment,
      foregroundColor: AppColors.ink,
      elevation: 0,
      centerTitle: false,
      titleTextStyle: AppText.display.copyWith(fontSize: 20),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: AppColors.brass,
        foregroundColor: Colors.white,
        disabledBackgroundColor: AppColors.line,
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        textStyle: GoogleFonts.cairo(fontWeight: FontWeight.w700, fontSize: 16),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.ink,
        side: const BorderSide(color: AppColors.line),
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Colors.white,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: AppColors.line),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: AppColors.line),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: AppColors.brass, width: 1.6),
      ),
    ),
  );
}
