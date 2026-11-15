import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import '../../widgets/ledger_card.dart';
import '../cart/cart_screen.dart';

class ProductDetailScreen extends StatelessWidget {
  final Shop shop;
  final Product product;
  const ProductDetailScreen({super.key, required this.shop, required this.product});

  @override
  Widget build(BuildContext context) {
    final goldValue = AppStore.instance.gramPrice(product.karat) * product.weightGrams;
    final makingFee = shop.makingFeePerGram * product.weightGrams;
    return Scaffold(
      appBar: AppBar(title: Text(product.name)),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          Container(
            height: 180,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(18),
              gradient: const LinearGradient(colors: [AppColors.brassLight, AppColors.brass], begin: Alignment.topRight, end: Alignment.bottomLeft),
            ),
          ),
          const SizedBox(height: 16),
          Row(children: [
            Chip(label: Text('عيار ${product.karat}')),
            const SizedBox(width: 8),
            if (product.stamped) Chip(label: const Text('مدموغ ✓'), backgroundColor: AppColors.trustSoft),
            const SizedBox(width: 8),
            Chip(label: Text('متاح ${product.availablePieces} قطع')),
          ]),
          const SizedBox(height: 10),
          Text(product.name, style: AppText.display.copyWith(fontSize: 22)),
          Text('${shop.name} · موثّق · ${shop.rating} ★ ${shop.distanceKm} كم', style: AppText.body.copyWith(color: AppColors.muted)),
          const SizedBox(height: 18),
          Text('السعر التقديري — مرتبط بالسعر اللحظي', style: AppText.display.copyWith(fontSize: 15)),
          const SizedBox(height: 10),
          LedgerCard(lines: [
            LedgerLine('قيمة الذهب (${product.weightGrams} جم × ${AppStore.instance.gramPrice(product.karat).toStringAsFixed(0)})',
                goldValue.toStringAsFixed(0)),
            LedgerLine('مصنعية المحل (${product.weightGrams} جم × ${shop.makingFeePerGram.toStringAsFixed(0)})', makingFee.toStringAsFixed(0)),
            LedgerLine('الإجمالي قبل رسوم الخدمة', (goldValue + makingFee).toStringAsFixed(0), isTotal: true),
          ]),
          const SizedBox(height: 18),
          Text('هتستلم إيه من المحل؟', style: AppText.display.copyWith(fontSize: 15)),
          const SizedBox(height: 8),
          _checkline('القطعة بعد وزنها قدامك على ميزان المحل'),
          _checkline('دمغة العيار ظاهرة على القطعة'),
          _checkline('فاتورة باسمك بتفاصيل الوزن والعيار والمصنعية'),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
          child: ElevatedButton(
            onPressed: () {
              AppStore.instance.addToCart(shop, product);
              Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CartScreen()));
            },
            child: const Text('أضف للسلة'),
          ),
        ),
      ),
    );
  }

  Widget _checkline(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(children: [
        const Icon(Icons.check_circle_outline, color: AppColors.trust, size: 18),
        const SizedBox(width: 8),
        Expanded(child: Text(text, style: AppText.body.copyWith(fontSize: 13))),
      ]),
    );
  }
}
