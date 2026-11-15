import 'package:flutter/material.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';

class ReportIssueScreen extends StatefulWidget {
  final GoldOrder order;
  const ReportIssueScreen({super.key, required this.order});

  @override
  State<ReportIssueScreen> createState() => _ReportIssueScreenState();
}

class _ReportIssueScreenState extends State<ReportIssueScreen> {
  String _reason = 'الوزن غير مطابق';
  final _reasons = ['الوزن غير مطابق', 'العيار أو الدمغة غير مطابقة', 'القطعة مختلفة عن الطلب', 'المحل مغلق أو رفض التسليم', 'سبب آخر'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('الإبلاغ عن مشكلة')),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(14)),
            child: Row(children: [
              const Icon(Icons.lock_outline, color: AppColors.brassLight),
              const SizedBox(width: 10),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('مبلغك لسه محفوظ', style: AppText.body.copyWith(color: Colors.white, fontWeight: FontWeight.w700)),
                  Text('لحد ما نراجع البلاغ، ${widget.order.total.toStringAsFixed(2)} ج.م هتفضل في الضمان.',
                      style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                ]),
              ),
            ]),
          ),
          const SizedBox(height: 18),
          Text('إيه المشكلة؟', style: AppText.display.copyWith(fontSize: 15)),
          const SizedBox(height: 8),
          for (final r in _reasons)
            RadioListTile<String>(
              value: r,
              groupValue: _reason,
              onChanged: (v) => setState(() => _reason = v!),
              title: Text(r),
              activeColor: AppColors.brass,
              contentPadding: EdgeInsets.zero,
            ),
          const SizedBox(height: 10),
          Text('احكيلنا اللي حصل', style: AppText.display.copyWith(fontSize: 14)),
          const SizedBox(height: 6),
          TextField(
            maxLines: 4,
            decoration: const InputDecoration(hintText: 'مثلاً: الجنيه وزنه على ميزان المحل 7.8 جم بدل 8 جم'),
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
          child: ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('تم إرسال البلاغ، هنتواصل معاك قريب.')));
            },
            child: const Text('إرسال البلاغ'),
          ),
        ),
      ),
    );
  }
}
