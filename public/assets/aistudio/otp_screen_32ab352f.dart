import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../home/home_shell.dart';

class OtpScreen extends StatelessWidget {
  const OtpScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('أدخل رمز التحقق', style: AppText.display.copyWith(fontSize: 20)),
          const SizedBox(height: 6),
          Text('بعتنا رمز من 4 أرقام على الرقم +20 10X XXX XX42', style: AppText.body.copyWith(color: AppColors.muted)),
          const SizedBox(height: 26),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: List.generate(4, (i) => _digitBox(i == 3)),
          ),
          const SizedBox(height: 16),
          Text('إعادة إرسال الرمز بعد 00:42', style: AppText.body.copyWith(fontWeight: FontWeight.bold)),
          const Spacer(),
          Row(children: [
            const Icon(Icons.lock_outline, size: 16, color: AppColors.muted),
            const SizedBox(width: 6),
            Expanded(
                child: Text('متشاركش الرمز مع أي حد، حتى لو قال إنه من King Gold',
                    style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12))),
          ]),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: () => Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const HomeShell()), (route) => false),
              child: const Text('تأكيد'),
            ),
          ),
        ]),
      ),
    );
  }

  Widget _digitBox(bool active) {
    return Container(
      width: 58,
      height: 64,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: active ? AppColors.brass : AppColors.line, width: active ? 1.6 : 1),
      ),
      child: Text(active ? '' : '•', style: AppText.display.copyWith(fontSize: 22)),
    );
  }
}
