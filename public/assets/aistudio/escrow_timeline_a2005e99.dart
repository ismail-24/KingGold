import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class EscrowTimeline extends StatelessWidget {
  final int activeStep; // 0,1,2
  const EscrowTimeline({super.key, this.activeStep = 0});

  static const _steps = ['تدفع النهارده', 'المبلغ محفوظ بأمان', 'يُحوَّل للمحل بعد استلامك'];

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(16)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            const Icon(Icons.lock_outline, color: AppColors.brassLight, size: 18),
            const SizedBox(width: 6),
            Text('المبلغ محفوظ في الضمان حتى الاستلام',
                style: AppText.body.copyWith(color: AppColors.brassLight, fontWeight: FontWeight.w700)),
          ]),
          const SizedBox(height: 6),
          Text('يحتفظ King Gold بمبلغك بأمان، ولا يصل للمحل إلا بعد ما تستلم ذهبك وتأكّد برمز الاستلام.',
              style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12.5)),
          const SizedBox(height: 16),
          Row(
            children: List.generate(_steps.length, (i) {
              final done = i <= activeStep;
              return Expanded(
                child: Column(children: [
                  CircleAvatar(
                    radius: 14,
                    backgroundColor: done ? AppColors.brassLight : AppColors.inkSoft,
                    child: Text('${i + 1}',
                        style: TextStyle(color: done ? AppColors.ink : AppColors.muted, fontWeight: FontWeight.bold)),
                  ),
                  const SizedBox(height: 6),
                  Text(_steps[i],
                      textAlign: TextAlign.center,
                      style: AppText.body.copyWith(fontSize: 10.5, color: done ? Colors.white : AppColors.muted)),
                ]),
              );
            }),
          ),
        ],
      ),
    );
  }
}
