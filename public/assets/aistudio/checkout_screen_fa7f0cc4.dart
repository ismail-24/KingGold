import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../theme/app_theme.dart';
import '../../widgets/escrow_timeline.dart';
import '../../widgets/ledger_card.dart';
import 'success_screen.dart';

class CheckoutScreen extends StatefulWidget {
  final String pickupWindow;
  const CheckoutScreen({super.key, required this.pickupWindow});

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  int _method = 0;
  final _methods = const [
    ['بطاقة بنكية', 'Visa •••• 4821', Icons.credit_card],
    ['إنستاباي', 'تحويل بنكي فوري', Icons.phone_iphone],
    ['محفظة إلكترونية', 'فودافون كاش · فوري', Icons.account_balance_wallet_outlined],
  ];

  @override
  Widget build(BuildContext context) {
    final store = AppStore.instance;
    return Scaffold(
      appBar: AppBar(title: const Text('مراجعة الدفع')),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          LedgerCard(lines: [
            LedgerLine('قيمة الذهب', store.cartGoldValue.toStringAsFixed(2)),
            LedgerLine('مصنعية المحل', store.cartMakingFee.toStringAsFixed(2)),
            LedgerLine('رسوم خدمة King Gold', store.cartServiceFee.toStringAsFixed(2)),
            LedgerLine('الإجمالي', '${store.cartTotal.toStringAsFixed(2)} ج.م', isTotal: true),
          ]),
          const SizedBox(height: 16),
          const EscrowTimeline(activeStep: 0),
          const SizedBox(height: 18),
          Text('ادفع بواسطة', style: AppText.display.copyWith(fontSize: 15)),
          const SizedBox(height: 8),
          for (int i = 0; i < _methods.length; i++)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: InkWell(
                borderRadius: BorderRadius.circular(14),
                onTap: () => setState(() => _method = i),
                child: Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: _method == i ? AppColors.brass : AppColors.line, width: _method == i ? 1.6 : 1),
                  ),
                  child: Row(children: [
                    Icon(_methods[i][2] as IconData, color: AppColors.brass),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text(_methods[i][0] as String, style: AppText.body.copyWith(fontWeight: FontWeight.w700)),
                        Text(_methods[i][1] as String, style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                      ]),
                    ),
                    Radio<int>(value: i, groupValue: _method, onChanged: (v) => setState(() => _method = v!), activeColor: AppColors.brass),
                  ]),
                ),
              ),
            ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            ElevatedButton.icon(
              icon: const Icon(Icons.lock_outline, size: 18),
              onPressed: () {
                final order = store.placeOrder(widget.pickupWindow);
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => SuccessScreen(order: order)),
                  (route) => route.isFirst,
                );
              },
              label: Text('ادفع ${store.cartTotal.toStringAsFixed(2)} ج.م بأمان'),
            ),
            const SizedBox(height: 6),
            Text('محمي بنظام الضمان. لا يُصرف أي مبلغ للمحل قبل استلامك للذهب.',
                style: AppText.body.copyWith(fontSize: 11, color: AppColors.muted)),
          ]),
        ),
      ),
    );
  }
}
