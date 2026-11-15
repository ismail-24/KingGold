import 'package:flutter/material.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import '../home/home_shell.dart';

class RatingScreen extends StatefulWidget {
  final GoldOrder order;
  const RatingScreen({super.key, required this.order});

  @override
  State<RatingScreen> createState() => _RatingScreenState();
}

class _RatingScreenState extends State<RatingScreen> {
  int _stars = 4;
  final _tags = ['دقة الوزن', 'حسن التعامل', 'سرعة التجهيز', 'وضوح الفاتورة'];
  final _selected = <String>{'دقة الوزن', 'حسن التعامل'};

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(22),
          children: [
            Center(
              child: Container(
                width: 64,
                height: 64,
                decoration: const BoxDecoration(color: AppColors.trustSoft, shape: BoxShape.circle),
                child: const Icon(Icons.check, color: AppColors.trust, size: 34),
              ),
            ),
            const SizedBox(height: 14),
            Center(child: Text('تم الاستلام بنجاح', style: AppText.display.copyWith(fontSize: 22))),
            Center(child: Text('مبروك على ذهبك. تم تحويل المبلغ للمحل.', style: AppText.body.copyWith(color: AppColors.muted))),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16), border: Border.all(color: AppColors.line)),
              child: Column(children: [
                Text('قيّم تجربتك مع ${widget.order.shop.name}', style: AppText.display.copyWith(fontSize: 16)),
                const SizedBox(height: 4),
                Text('تقييمك بيساعد باقي العملاء ويحافظ على جودة المحلات', style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                const SizedBox(height: 14),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: List.generate(5, (i) {
                    final filled = i < _stars;
                    return IconButton(
                      onPressed: () => setState(() => _stars = i + 1),
                      icon: Icon(filled ? Icons.star : Icons.star_border, color: AppColors.brass, size: 30),
                    );
                  }),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  alignment: WrapAlignment.center,
                  children: _tags.map((t) {
                    final sel = _selected.contains(t);
                    return ChoiceChip(
                      label: Text(t),
                      selected: sel,
                      onSelected: (_) => setState(() => sel ? _selected.remove(t) : _selected.add(t)),
                      selectedColor: AppColors.ink,
                      labelStyle: TextStyle(color: sel ? AppColors.brassLight : AppColors.ink),
                    );
                  }).toList(),
                ),
              ]),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
          child: ElevatedButton(
            onPressed: () => Navigator.of(context).pushAndRemoveUntil(MaterialPageRoute(builder: (_) => const HomeShell()), (r) => false),
            child: const Text('إرسال التقييم'),
          ),
        ),
      ),
    );
  }
}
