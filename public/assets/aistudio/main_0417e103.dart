import 'package:flutter/material.dart';
import 'theme/app_theme.dart';
import 'screens/splash_screen.dart';

void main() {
  runApp(const KingGoldApp());
}

class KingGoldApp extends StatelessWidget {
  const KingGoldApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'King Gold',
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      locale: const Locale('ar'),
      localizationsDelegates: const [
        // ملحوظة: لو عايز قوائم التاريخ/الـ Cupertino بالعربي كمان،
        // ضيف flutter_localizations في pubspec وفعّل الديليجيتس بتاعتها.
      ],
      supportedLocales: const [Locale('ar'), Locale('en')],
      builder: (context, child) => Directionality(textDirection: TextDirection.rtl, child: child!),
      home: const SplashScreen(),
    );
  }
}
