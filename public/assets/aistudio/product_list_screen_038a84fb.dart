import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import 'product_detail_screen.dart';
import '../cart/cart_screen.dart';

class ProductListScreen extends StatefulWidget {
  final Shop shop;
  final String? initialCategory;
  const ProductListScreen({super.key, required this.shop, this.initialCategory});

  @override
  State<ProductListScreen> createState() => _ProductListScreenState();
}

class _ProductListScreenState extends State<ProductListScreen> {
  String _filter = 'الكل';

  @override
  Widget build(BuildContext context) {
    final products = AppStore.instance.productsFor(widget.shop.id);
    final categories = ['الكل', ...products.map((p) => p.category).toSet()];
    final shown = _filter == 'الكل' ? products : products.where((p) => p.category == _filter).toList();

    return AnimatedBuilder(
      animation: AppStore.instance,
      builder: (context, _) => Scaffold(
        appBar: AppBar(title: Text(widget.shop.name)),
        body: Column(children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: SizedBox(
              height: 38,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: categories
                    .map((c) => Padding(
                          padding: const EdgeInsets.only(left: 8),
                          child: ChoiceChip(
                            label: Text(c),
                            selected: _filter == c,
                            onSelected: (_) => setState(() => _filter = c),
                            selectedColor: AppColors.ink,
                            labelStyle: TextStyle(color: _filter == c ? AppColors.brassLight : AppColors.ink, fontWeight: FontWeight.w700),
                          ),
                        ))
                    .toList(),
              ),
            ),
          ),
          Expanded(
            child: ListView.separated(
              padding: const EdgeInsets.all(14),
              itemCount: shown.length,
              separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (_, i) {
                final p = shown[i];
                final price = AppStore.instance.gramPrice(p.karat) * p.weightGrams;
                return InkWell(
                  borderRadius: BorderRadius.circular(14),
                  onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => ProductDetailScreen(shop: widget.shop, product: p))),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14), border: Border.all(color: AppColors.line)),
                    child: Row(children: [
                      Container(
                        width: 54,
                        height: 54,
                        decoration: BoxDecoration(borderRadius: BorderRadius.circular(10), gradient: const LinearGradient(colors: [AppColors.brassLight, AppColors.brass])),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(p.name, style: AppText.body.copyWith(fontWeight: FontWeight.w800)),
                          Text('عيار ${p.karat} · ${p.weightGrams} جم', style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
                        ]),
                      ),
                      Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
                        Text('${price.toStringAsFixed(0)} ج.م', style: AppText.body.copyWith(fontWeight: FontWeight.w800)),
                        const SizedBox(height: 4),
                        IconButton.filled(
                          style: IconButton.styleFrom(backgroundColor: AppColors.ink, minimumSize: const Size(30, 30)),
                          onPressed: () {
                            AppStore.instance.addToCart(widget.shop, p);
                            ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('اتضاف ${p.name} للسلة'), duration: const Duration(milliseconds: 900)));
                          },
                          icon: const Icon(Icons.add, size: 16, color: AppColors.brassLight),
                        ),
                      ]),
                    ]),
                  ),
                );
              },
            ),
          ),
        ]),
        bottomNavigationBar: AppStore.instance.cart.isEmpty
            ? null
            : SafeArea(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
                  child: ElevatedButton(
                    onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CartScreen())),
                    child: Text(
                        '${AppStore.instance.cart.length} قطعة · اعرض السلة — ${AppStore.instance.cartTotal.toStringAsFixed(0)} ج.م'),
                  ),
                ),
              ),
      ),
    );
  }
}
