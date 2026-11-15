import 'dart:async';
import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../theme/app_theme.dart';
import '../../widgets/ledger_card.dart';
import '../checkout/checkout_screen.dart';

class CartScreen extends StatefulWidget {
  const CartScreen({super.key});
  @override
  State<CartScreen> createState() => _CartScreenState();
}

class _CartScreenState extends State<CartScreen> {
  Timer? _timer;
  Duration _left = const Duration(minutes: 9, seconds: 42);
  int _slot = 0;
  final _slots = const ['اليوم · 4:00–8:00 م', 'غداً · 12:00–4:00 م', 'غداً · 4:00–9:00 م'];

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (_left.inSeconds <= 0) return;
      setState(() => _left -= const Duration(seconds: 1));
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final store = AppStore.instance;
    final mm = _left.inMinutes.toString().padLeft(2, '0');
    final ss = (_left.inSeconds % 60).toString().padLeft(2, '0');

    return AnimatedBuilder(
      animation: store,
      builder: (context, _) => Scaffold(
        appBar: AppBar(title: const Text('سلة الطلب')),
        body: ListView(
          padding: const EdgeInsets.all(18),
          children: [
            Text('من ${store.cartShop?.name ?? ''}', style: AppText.body.copyWith(color: AppColors.muted)),
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: AppColors.inkSoft, borderRadius: BorderRadius.circular(14)),
              child: Row(children: [
                const Icon(Icons.timer_outlined, color: AppColors.brassLight, size: 18),
                const SizedBox(width: 8),
                Expanded(
                  child: Text('سعر الذهب مثبّت لك — $mm:$ss', style: AppText.body.copyWith(color: Colors.white, fontWeight: FontWeight.w700)),
                ),
              ]),
            ),
            const SizedBox(height: 14),
            for (final item in store.cart)
              Container(
                margin: const EdgeInsets.only(bottom: 10),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14), border: Border.all(color: AppColors.line)),
                child: Row(children: [
                  IconButton(onPressed: () => store.removeFromCart(item), icon: const Icon(Icons.delete_outline, color: AppColors.alert)),
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(item.product.name, style: AppText.body.copyWith(fontWeight: FontWeight.w800)),
                      Text('عيار ${item.product.karat} · ${item.product.weightGrams} جم · الكمية ${item.quantity}',
                          style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                    ]),
                  ),
                ]),
              ),
            const SizedBox(height: 6),
            Text('موعد الاستلام', style: AppText.display.copyWith(fontSize: 15)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: List.generate(
                  _slots.length,
                  (i) => ChoiceChip(
                        label: Text(_slots[i]),
                        selected: _slot == i,
                        onSelected: (_) => setState(() => _slot = i),
                        selectedColor: AppColors.ink,
                        labelStyle: TextStyle(color: _slot == i ? AppColors.brassLight : AppColors.ink, fontWeight: FontWeight.w700, fontSize: 12.5),
                      )),
            ),
            const SizedBox(height: 18),
            LedgerCard(lines: [
              LedgerLine('قيمة الذهب', store.cartGoldValue.toStringAsFixed(2)),
              LedgerLine('مصنعية المحل', store.cartMakingFee.toStringAsFixed(2)),
              LedgerLine('رسوم خدمة King Gold (1%)', store.cartServiceFee.toStringAsFixed(2)),
              LedgerLine('الإجمالي', '${store.cartTotal.toStringAsFixed(2)} ج.م', isTotal: true),
            ]),
          ],
        ),
        bottomNavigationBar: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              ElevatedButton(
                onPressed: store.cart.isEmpty
                    ? null
                    : () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => CheckoutScreen(pickupWindow: _slots[_slot]))),
                child: const Text('متابعة للدفع'),
              ),
              const SizedBox(height: 6),
              Text('لسه مفيش دفع. المبلغ بيتحجز في الضمان في الخطوة الجاية.', style: AppText.body.copyWith(fontSize: 11, color: AppColors.muted)),
            ]),
          ),
        ),
      ),
    );
  }
}
