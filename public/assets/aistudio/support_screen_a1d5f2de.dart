import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';

class SupportScreen extends StatelessWidget {
  const SupportScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final faqs = [
      'إمتى بيتحول المبلغ للمحل؟',
      'إيه معنى "محفوظ في الضمان"؟',
      'لو المحل رفض التسليم أعمل إيه؟',
      'إزاي أتأكد إن المحل موثّق؟',
    ];
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 14, 18, 24),
        children: [
          Text('الدعم', style: AppText.display.copyWith(fontSize: 20)),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(16)),
            child: Row(children: [
              const Icon(Icons.support_agent, color: AppColors.brassLight),
              const SizedBox(width: 10),
              Expanded(child: Text('فريق الدعم متاح يومياً من 9 ص لـ 11 م', style: AppText.body.copyWith(color: Colors.white))),
            ]),
          ),
          const SizedBox(height: 18),
          Text('أسئلة شائعة', style: AppText.display.copyWith(fontSize: 15)),
          const SizedBox(height: 8),
          for (final f in faqs)
            Container(
              margin: const EdgeInsets.only(bottom: 8),
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12), border: Border.all(color: AppColors.line)),
              child: Row(children: [
                Expanded(child: Text(f, style: AppText.body.copyWith(fontWeight: FontWeight.w600))),
                const Icon(Icons.chevron_left, color: AppColors.muted),
              ]),
            ),
        ],
      ),
    );
  }
}
