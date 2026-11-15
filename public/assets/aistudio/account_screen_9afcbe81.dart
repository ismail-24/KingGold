import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../widgets/seal_badge.dart';
import '../auth/kyc_screen.dart';

class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 14, 18, 24),
        children: [
          Text('حسابي', style: AppText.display.copyWith(fontSize: 20)),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16), border: Border.all(color: AppColors.line)),
            child: Row(children: [
              const SealBadge(size: 46),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('إسماعيل', style: AppText.body.copyWith(fontWeight: FontWeight.w800, fontSize: 16)),
                  Text('+20 10X XXX XX42', style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                ]),
              ),
            ]),
          ),
          const SizedBox(height: 16),
          _tile(context, Icons.badge_outlined, 'توثيق الهوية (KYC)', 'خطوة واحدة لتأمين استلامك',
              onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const KycScreen()))),
          _tile(context, Icons.credit_card, 'وسائل الدفع', 'بطاقاتك ومحافظك المسجّلة'),
          _tile(context, Icons.notifications_none, 'تنبيهات الأسعار', 'نبّهني عند وصول سعر معيّن'),
          _tile(context, Icons.logout, 'تسجيل الخروج', null, danger: true),
        ],
      ),
    );
  }

  Widget _tile(BuildContext context, IconData icon, String title, String? subtitle, {VoidCallback? onTap, bool danger = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap ?? () {},
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14), border: Border.all(color: AppColors.line)),
          child: Row(children: [
            Icon(icon, color: danger ? AppColors.alert : AppColors.brass, size: 20),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title, style: AppText.body.copyWith(fontWeight: FontWeight.w700, color: danger ? AppColors.alert : AppColors.ink)),
                if (subtitle != null) Text(subtitle, style: AppText.body.copyWith(color: AppColors.muted, fontSize: 11.5)),
              ]),
            ),
            const Icon(Icons.chevron_left, color: AppColors.muted),
          ]),
        ),
      ),
    );
  }
}
