import 'package:flutter/material.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import 'rating_screen.dart';

class VerifyPickupScreen extends StatefulWidget {
  final GoldOrder order;
  const VerifyPickupScreen({super.key, required this.order});

  @override
  State<VerifyPickupScreen> createState() => _VerifyPickupScreenState();
}

class _VerifyPickupScreenState extends State<VerifyPickupScreen> {
  final _checks = [
    ['الوزن مطابق', 'القطع اتوزنت قدامك على ميزان المحل.'],
    ['الدمغة والعيار', 'دمغة العيار ظاهرة وواضحة على القطع.'],
    ['القطع مطابقة للطلب', 'النوع والمقاس زي ما اخترت بالظبط.'],
    ['استلمت الفاتورة', 'فاتورة المحل بنفس الأوزان والأسعار.'],
  ];
  late List<bool> _done;

  @override
  void initState() {
    super.initState();
    _done = List.filled(_checks.length, false);
  }

  @override
  Widget build(BuildContext context) {
    final remaining = _done.where((d) => !d).length;
    return Scaffold(
      appBar: AppBar(title: const Text('تأكيد الاستلام')),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          Text('اتأكد من القطع قدام البائع', style: AppText.display.copyWith(fontSize: 19)),
          const SizedBox(height: 6),
          Text('علّم على كل بند بعد ما تتأكد منه بنفسك. التأكيد بيحوّل المبلغ للمحل.',
              style: AppText.body.copyWith(color: AppColors.muted)),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(14)),
            child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
              Text('محفوظ في الضمان', style: AppText.body.copyWith(color: AppColors.brassLight)),
              Text('${widget.order.total.toStringAsFixed(2)} ج.م', style: AppText.display.copyWith(color: Colors.white, fontSize: 18)),
            ]),
          ),
          const SizedBox(height: 16),
          for (int i = 0; i < _checks.length; i++)
            Container(
              margin: const EdgeInsets.only(bottom: 10),
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: _done[i] ? AppColors.trust : AppColors.line),
              ),
              child: Row(children: [
                Checkbox(
                  value: _done[i],
                  activeColor: AppColors.trust,
                  onChanged: (v) => setState(() => _done[i] = v ?? false),
                ),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(_checks[i][0], style: AppText.body.copyWith(fontWeight: FontWeight.w700)),
                    Text(_checks[i][1], style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                  ]),
                ),
              ]),
            ),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: AppColors.alertSoft, borderRadius: BorderRadius.circular(12)),
            child: Row(children: [
              const Icon(Icons.warning_amber_rounded, color: AppColors.alert, size: 18),
              const SizedBox(width: 8),
              Expanded(
                  child: Text('بعد التأكيد بيتحوّل المبلغ للمحل ومينفعش يتسحب. لو في أي ملاحظة، أبلّغ قبل ما تأكد.',
                      style: AppText.body.copyWith(color: AppColors.alert, fontSize: 12))),
            ]),
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            if (remaining > 0)
              Text('باقي $remaining بند قبل ما تقدر تأكد', style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
            const SizedBox(height: 6),
            ElevatedButton(
              onPressed: remaining == 0
                  ? () {
                      widget.order.status = OrderStatus.completed;
                      Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => RatingScreen(order: widget.order)));
                    }
                  : null,
              child: const Text('كل شيء سليم · أؤكد الاستلام'),
            ),
          ]),
        ),
      ),
    );
  }
}
