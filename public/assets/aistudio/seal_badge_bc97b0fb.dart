import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// بديل عن "تشِك أخضر" العادي: ختم دائري بطابع الدمغة يحمل حرفين،
/// بيستخدم في أي مكان نحتاج فيه نقول "موثّق" أو "مدموغ".
class SealBadge extends StatelessWidget {
  final String label;
  final double size;
  const SealBadge({super.key, this.label = 'KG', this.size = 34});

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      size: Size.square(size),
      painter: _SealPainter(),
      child: SizedBox(
        width: size,
        height: size,
        child: Center(
          child: Text(
            label,
            style: AppText.display.copyWith(
              color: AppColors.parchment,
              fontSize: size * 0.3,
            ),
          ),
        ),
      ),
    );
  }
}

class _SealPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final r = size.width / 2;
    final fill = Paint()..color = AppColors.seal;
    canvas.drawCircle(center, r, fill);

    // أسنان الختم (notches) على المحيط
    final notch = Paint()..color = AppColors.parchment.withOpacity(0.9);
    const notches = 16;
    for (int i = 0; i < notches; i++) {
      final angle = (2 * 3.14159265 / notches) * i;
      final p = Offset(center.dx + r * 0.92 * _cos(angle), center.dy + r * 0.92 * _sin(angle));
      canvas.drawCircle(p, size.width * 0.02, notch);
    }

    final ring = Paint()
      ..color = AppColors.brassLight
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.4;
    canvas.drawCircle(center, r * 0.78, ring);
  }

  double _cos(double a) => Offset.fromDirection(a).dx;
  double _sin(double a) => Offset.fromDirection(a).dy;

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

/// شريط "موثّق" نصي يستخدم الختم الصغير بدل أيقونة جاهزة.
class VerifiedStrip extends StatelessWidget {
  const VerifiedStrip({super.key});
  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        const SealBadge(size: 18),
        const SizedBox(width: 6),
        Text('موثّق', style: AppText.body.copyWith(color: AppColors.trust, fontSize: 12, fontWeight: FontWeight.w700)),
      ],
    );
  }
}
