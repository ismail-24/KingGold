import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../widgets/live_price_ticker.dart';

class PriceInsightScreen extends StatefulWidget {
  const PriceInsightScreen({super.key});
  @override
  State<PriceInsightScreen> createState() => _PriceInsightScreenState();
}

class _PriceInsightScreenState extends State<PriceInsightScreen> {
  bool _alertOn = true;
  final _history = const [5300.0, 5335.0, 5310.0, 5360.0, 5355.0, 5390.0, 5425.0];
  final _days = const ['15 أكت', '16', '17', '18', '19', '20', '21 أكت'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('أسعار الذهب')),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          const LivePriceTicker(),
          const SizedBox(height: 18),
          Text('عيار 21 · آخر 7 أيام (ج.م/جرام)', style: AppText.display.copyWith(fontSize: 15)),
          const SizedBox(height: 10),
          Container(
            height: 180,
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16), border: Border.all(color: AppColors.line)),
            child: CustomPaint(painter: _TrendPainter(_history), child: const SizedBox.expand()),
          ),
          const SizedBox(height: 6),
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: _days.map((d) => Text(d, style: AppText.body.copyWith(fontSize: 10, color: AppColors.muted))).toList()),
          const SizedBox(height: 16),
          Row(children: [
            Expanded(child: _statBox('التغير', '+1.69%', AppColors.trust)),
            const SizedBox(width: 8),
            Expanded(child: _statBox('الأدنى', '5,335', AppColors.ink)),
            const SizedBox(width: 8),
            Expanded(child: _statBox('الأعلى', '5,425', AppColors.ink)),
          ]),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16), border: Border.all(color: AppColors.line)),
            child: Column(children: [
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                value: _alertOn,
                onChanged: (v) => setState(() => _alertOn = v),
                title: const Text('نبّهني عند وصول السعر'),
                activeColor: AppColors.brass,
              ),
              if (_alertOn)
                Row(children: [
                  Expanded(child: OutlinedButton(onPressed: () {}, child: const Text('أعلى من'))),
                  const SizedBox(width: 10),
                  const Expanded(
                      child: TextField(
                    textAlign: TextAlign.center,
                    decoration: InputDecoration(hintText: '5,500'),
                  )),
                ]),
            ]),
          ),
          const SizedBox(height: 10),
          Text('الأسعار مرجعية وبتتحدث لحظياً. سعر طلبك بيتثبت لمدة محدودة بعد ما تضيف القطعة للسلة.',
              style: AppText.body.copyWith(color: AppColors.muted, fontSize: 11.5)),
        ],
      ),
    );
  }

  Widget _statBox(String label, String value, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: BoxDecoration(color: AppColors.parchmentDeep, borderRadius: BorderRadius.circular(12)),
      child: Column(children: [
        Text(label, style: AppText.body.copyWith(fontSize: 11, color: AppColors.muted)),
        const SizedBox(height: 4),
        Text(value, style: AppText.display.copyWith(fontSize: 15, color: color)),
      ]),
    );
  }
}

class _TrendPainter extends CustomPainter {
  final List<double> data;
  _TrendPainter(this.data);

  @override
  void paint(Canvas canvas, Size size) {
    final minV = data.reduce((a, b) => a < b ? a : b);
    final maxV = data.reduce((a, b) => a > b ? a : b);
    final range = (maxV - minV).abs() < 1 ? 1 : (maxV - minV);
    final dx = size.width / (data.length - 1);

    Offset pt(int i) {
      final norm = (data[i] - minV) / range;
      return Offset(size.width - i * dx, size.height - norm * size.height * 0.85 - size.height * 0.08);
    }

    final path = Path()..moveTo(pt(0).dx, pt(0).dy);
    for (int i = 1; i < data.length; i++) {
      path.lineTo(pt(i).dx, pt(i).dy);
    }

    final fillPath = Path.from(path)
      ..lineTo(pt(data.length - 1).dx, size.height)
      ..lineTo(pt(0).dx, size.height)
      ..close();

    canvas.drawPath(
        fillPath,
        Paint()
          ..shader = LinearGradient(
            colors: [AppColors.brass.withOpacity(0.22), AppColors.brass.withOpacity(0.0)],
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
          ).createShader(Offset.zero & size));

    canvas.drawPath(path, Paint()..color = AppColors.brass..style = PaintingStyle.stroke..strokeWidth = 2.4..strokeCap = StrokeCap.round);

    canvas.drawCircle(pt(data.length - 1), 4.5, Paint()..color = AppColors.brass);
    canvas.drawCircle(pt(data.length - 1), 7, Paint()..color = AppColors.brass.withOpacity(0.25));
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
