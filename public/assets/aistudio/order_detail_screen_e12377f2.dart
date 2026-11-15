import 'package:flutter/material.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import '../../widgets/ledger_card.dart';
import 'verify_pickup_screen.dart';
import 'report_issue_screen.dart';

class OrderDetailScreen extends StatelessWidget {
  final GoldOrder order;
  const OrderDetailScreen({super.key, required this.order});

  @override
  Widget build(BuildContext context) {
    final steps = ['تم الدفع', 'قيد التجهيز', 'جاهز للاستلام', 'تم الاستلام'];
    int activeIdx;
    switch (order.status) {
      case OrderStatus.paid:
        activeIdx = 0;
        break;
      case OrderStatus.preparing:
        activeIdx = 1;
        break;
      case OrderStatus.readyForPickup:
        activeIdx = 2;
        break;
      default:
        activeIdx = 3;
    }

    return Scaffold(
      appBar: AppBar(title: Text('تفاصيل الطلب ${order.id}')),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(16)),
            child: Row(
              children: List.generate(steps.length, (i) {
                final done = i <= activeIdx;
                return Expanded(
                  child: Column(children: [
                    CircleAvatar(
                      radius: 14,
                      backgroundColor: done ? AppColors.brassLight : AppColors.inkSoft,
                      child: Icon(done ? Icons.check : Icons.circle_outlined, size: 14, color: done ? AppColors.ink : AppColors.muted),
                    ),
                    const SizedBox(height: 6),
                    Text(steps[i], textAlign: TextAlign.center, style: AppText.body.copyWith(fontSize: 10, color: done ? Colors.white : AppColors.muted)),
                  ]),
                );
              }),
            ),
          ),
          const SizedBox(height: 16),
          if (order.status == OrderStatus.readyForPickup)
            Container(
              padding: const EdgeInsets.all(16),
              margin: const EdgeInsets.only(bottom: 14),
              decoration: BoxDecoration(color: AppColors.trustSoft, borderRadius: BorderRadius.circular(14)),
              child: Row(children: [
                const Icon(Icons.qr_code_2, color: AppColors.trust),
                const SizedBox(width: 10),
                const Expanded(child: Text('طلبك جاهز — روح المحل واعرض رمز الاستلام')),
              ]),
            ),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14), border: Border.all(color: AppColors.line)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(order.shop.name, style: AppText.body.copyWith(fontWeight: FontWeight.w800)),
              Text(order.shop.address, style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
              const SizedBox(height: 8),
              Row(children: [
                Expanded(child: OutlinedButton(onPressed: () {}, child: const Text('اتصل'))),
                const SizedBox(width: 8),
                Expanded(child: OutlinedButton(onPressed: () {}, child: const Text('الاتجاهات'))),
              ]),
            ]),
          ),
          const SizedBox(height: 16),
          LedgerCard(lines: [
            LedgerLine('قيمة الذهب', order.goldValue.toStringAsFixed(2)),
            LedgerLine('مصنعية المحل', order.makingFee.toStringAsFixed(2)),
            LedgerLine('رسوم خدمة King Gold', order.serviceFee.toStringAsFixed(2)),
            LedgerLine('الإجمالي المدفوع', '${order.total.toStringAsFixed(2)} ج.م', isTotal: true),
          ]),
          const SizedBox(height: 14),
          if (order.status == OrderStatus.readyForPickup)
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => VerifyPickupScreen(order: order))),
                child: const Text('وصلت المحل — ابدأ تأكيد الاستلام'),
              ),
            ),
          const SizedBox(height: 10),
          TextButton.icon(
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => ReportIssueScreen(order: order))),
            icon: const Icon(Icons.flag_outlined, color: AppColors.alert, size: 18),
            label: const Text('الإبلاغ عن مشكلة', style: TextStyle(color: AppColors.alert)),
          ),
        ],
      ),
    );
  }
}
