import 'package:flutter/material.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import '../home/home_shell.dart';

class SuccessScreen extends StatefulWidget {
  final GoldOrder order;
  const SuccessScreen({super.key, required this.order});

  @override
  State<SuccessScreen> createState() => _SuccessScreenState();
}

class _SuccessScreenState extends State<SuccessScreen> with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 700))..forward();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(22),
          children: [
            ScaleTransition(
              scale: CurvedAnimation(parent: _c, curve: Curves.elasticOut),
              child: Column(children: [
                Container(
                  width: 64,
                  height: 64,
                  decoration: const BoxDecoration(color: AppColors.trustSoft, shape: BoxShape.circle),
                  child: const Icon(Icons.check, color: AppColors.trust, size: 34),
                ),
                const SizedBox(height: 14),
                Text('تم الدفع بنجاح', style: AppText.display.copyWith(fontSize: 22)),
                const SizedBox(height: 6),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(20)),
                  child: Text('${widget.order.total.toStringAsFixed(2)} ج.م محفوظة في الضمان',
                      style: AppText.body.copyWith(color: AppColors.brassLight, fontSize: 12.5)),
                ),
              ]),
            ),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(20)),
              child: Column(children: [
                Text('رمز الاستلام', style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                const SizedBox(height: 10),
                CustomPaint(size: const Size(140, 140), painter: _PatternPainter()),
                const SizedBox(height: 10),
                Text(widget.order.id.replaceAll('-', ' '),
                    style: AppText.display.copyWith(color: Colors.white, fontSize: 22, letterSpacing: 3)),
                const SizedBox(height: 10),
                Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                  const Icon(Icons.schedule, color: AppColors.brassLight, size: 16),
                  const SizedBox(width: 6),
                  Text('الاستلام ${widget.order.pickupWindow}', style: AppText.body.copyWith(color: AppColors.brassLight, fontSize: 12.5)),
                ]),
              ]),
            ),
            const SizedBox(height: 10),
            Text('اعرض الرمز عند الكاونتر، ومتديهوش لحد إلا وأنت جوه المحل.',
                textAlign: TextAlign.center, style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
            const SizedBox(height: 18),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14), border: Border.all(color: AppColors.line)),
              child: Row(children: [
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(widget.order.shop.name, style: AppText.body.copyWith(fontWeight: FontWeight.w800)),
                    Text(widget.order.shop.address, style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                  ]),
                ),
                OutlinedButton(onPressed: () {}, child: const Text('اتجاهات')),
              ]),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
          child: OutlinedButton(
            onPressed: () => Navigator.of(context).pushAndRemoveUntil(MaterialPageRoute(builder: (_) => const HomeShell()), (r) => false),
            child: const Text('عرض كل الطلبات'),
          ),
        ),
      ),
    );
  }
}

class _PatternPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    const cells = 8;
    final cell = size.width / cells;
    final seed = 42;
    int x = seed;
    int rnd() {
      x = (x * 1103515245 + 12345) & 0x7fffffff;
      return x;
    }

    final light = Paint()..color = AppColors.parchment;
    final dark = Paint()..color = AppColors.ink;
    canvas.drawRRect(RRect.fromRectAndRadius(Offset.zero & size, const Radius.circular(10)), light);
    for (int r = 0; r < cells; r++) {
      for (int c = 0; c < cells; c++) {
        if (rnd() % 2 == 0) {
          canvas.drawRect(Rect.fromLTWH(c * cell, r * cell, cell, cell), dark);
        }
      }
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
