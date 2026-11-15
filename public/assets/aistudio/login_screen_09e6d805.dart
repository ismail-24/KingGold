import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../widgets/seal_badge.dart';
import 'otp_screen.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(children: [
        Container(
          width: double.infinity,
          color: AppColors.ink,
          padding: const EdgeInsets.fromLTRB(24, 70, 24, 36),
          child: Column(children: [
            const SealBadge(size: 72),
            const SizedBox(height: 14),
            Text('King Gold', style: AppText.display.copyWith(color: AppColors.parchment, fontSize: 24)),
            const SizedBox(height: 6),
            Text('اشترِ الذهب من محلات حقيقية قريبة منك،\nوادفع بأمان لحد ما تستلم.',
                textAlign: TextAlign.center, style: AppText.body.copyWith(color: AppColors.muted)),
            const SizedBox(height: 18),
            Row(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
              _trustChip(Icons.location_on_outlined, 'استلام بنفسك'),
              _trustChip(Icons.lock_outline, 'ضمان حتى الاستلام'),
              _trustChip(Icons.verified_outlined, 'محلات موثّقة'),
            ]),
          ]),
        ),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('أهلاً بيك', style: AppText.display.copyWith(fontSize: 20)),
              const SizedBox(height: 4),
              Text('اكتب رقم موبايلك وهنبعتلك رمز تحقق.', style: AppText.body.copyWith(color: AppColors.muted)),
              const SizedBox(height: 18),
              TextField(
                keyboardType: TextInputType.phone,
                textAlign: TextAlign.right,
                decoration: const InputDecoration(prefixText: '+20  ', hintText: '1X XXXX XXXX'),
              ),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const OtpScreen())),
                  child: const Text('متابعة'),
                ),
              ),
              const SizedBox(height: 10),
              Text.rich(
                TextSpan(
                  style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12),
                  children: const [
                    TextSpan(text: 'بالمتابعة أنت بتوافق على '),
                    TextSpan(text: 'الشروط والأحكام', style: TextStyle(color: AppColors.brass, fontWeight: FontWeight.bold)),
                    TextSpan(text: ' و'),
                    TextSpan(text: 'سياسة الخصوصية', style: TextStyle(color: AppColors.brass, fontWeight: FontWeight.bold)),
                  ],
                ),
                textAlign: TextAlign.center,
              ),
            ]),
          ),
        ),
      ]),
    );
  }

  Widget _trustChip(IconData icon, String label) {
    return Column(children: [
      Icon(icon, color: AppColors.brassLight, size: 20),
      const SizedBox(height: 4),
      Text(label, style: AppText.body.copyWith(color: AppColors.muted, fontSize: 11)),
    ]);
  }
}
