import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../widgets/seal_badge.dart';
import 'auth/login_screen.dart';

/// لحظة الفتح: الختم بيهبط ويدمغ الشاشة، زي ما الصائغ بيدمغ القطعة،
/// قبل ما يفتح على شاشة الدخول. حركة واحدة مقصودة، مش أكتر.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});
  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with SingleTickerProviderStateMixin {
  late final AnimationController _c;
  late final Animation<double> _drop;
  late final Animation<double> _fade;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 1100));
    _drop = CurvedAnimation(parent: _c, curve: const Interval(0, 0.6, curve: Curves.easeOutBack));
    _fade = CurvedAnimation(parent: _c, curve: const Interval(0.5, 1, curve: Curves.easeIn));
    _c.forward();
    Future.delayed(const Duration(milliseconds: 1600), () {
      if (mounted) {
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const LoginScreen()));
      }
    });
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.ink,
      body: Center(
        child: AnimatedBuilder(
          animation: _c,
          builder: (_, __) => Transform.translate(
            offset: Offset(0, -40 * (1 - _drop.value)),
            child: Opacity(
              opacity: _drop.value.clamp(0, 1),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                const SealBadge(size: 86, label: 'KG'),
                const SizedBox(height: 18),
                FadeTransition(
                  opacity: _fade,
                  child: Column(children: [
                    Text('King Gold', style: AppText.display.copyWith(color: AppColors.parchment, fontSize: 26)),
                    const SizedBox(height: 4),
                    Text('ذهب حقيقي، بدمغة موثّقة', style: AppText.body.copyWith(color: AppColors.muted)),
                  ]),
                ),
              ]),
            ),
          ),
        ),
      ),
    );
  }
}
