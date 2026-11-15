import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// بطاقة بشكل "ورقة فاتورة الصائغ": حواف حادة مش مدورة، خط منقط
/// بيفصل كل بند عن اللي بعده زي خط التقطيع في الإيصال الحقيقي.
class LedgerCard extends StatelessWidget {
  final List<LedgerLine> lines;
  const LedgerCard({super.key, required this.lines});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: AppColors.line),
        borderRadius: BorderRadius.circular(4),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: Column(
        children: [
          for (int i = 0; i < lines.length; i++) ...[
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 10),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(lines[i].label,
                      style: AppText.body.copyWith(
                        fontSize: lines[i].isTotal ? 16 : 14,
                        fontWeight: lines[i].isTotal ? FontWeight.w800 : FontWeight.w400,
                        color: lines[i].isTotal ? AppColors.ink : AppColors.muted,
                      )),
                  Text(lines[i].value,
                      style: AppText.body.copyWith(
                        fontSize: lines[i].isTotal ? 18 : 14,
                        fontWeight: FontWeight.w800,
                        color: lines[i].isTotal ? AppColors.brass : AppColors.ink,
                      )),
                ],
              ),
            ),
            if (i != lines.length - 1) const _DottedDivider(),
          ],
        ],
      ),
    );
  }
}

class LedgerLine {
  final String label;
  final String value;
  final bool isTotal;
  LedgerLine(this.label, this.value, {this.isTotal = false});
}

class _DottedDivider extends StatelessWidget {
  const _DottedDivider();
  @override
  Widget build(BuildContext context) {
    return CustomPaint(painter: _DotPainter(), size: const Size(double.infinity, 1));
  }
}

class _DotPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()..color = AppColors.line..strokeWidth = 1;
    double x = 0;
    while (x < size.width) {
      canvas.drawLine(Offset(x, 0), Offset(x + 3, 0), paint);
      x += 7;
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
