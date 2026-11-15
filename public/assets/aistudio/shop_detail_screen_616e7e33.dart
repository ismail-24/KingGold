import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import '../../widgets/live_price_ticker.dart';
import '../../widgets/seal_badge.dart';
import 'product_list_screen.dart';

class ShopDetailScreen extends StatelessWidget {
  final Shop shop;
  const ShopDetailScreen({super.key, required this.shop});

  @override
  Widget build(BuildContext context) {
    final categories = AppStore.instance.productsFor(shop.id).map((p) => p.category).toSet().toList();
    return Scaffold(
      body: CustomScrollView(slivers: [
        SliverAppBar(
          pinned: true,
          backgroundColor: AppColors.parchment,
          expandedHeight: 170,
          flexibleSpace: FlexibleSpaceBar(
            background: Hero(
              tag: 'shop-${shop.id}',
              child: Container(
                decoration: const BoxDecoration(
                  gradient: LinearGradient(colors: [AppColors.brassLight, AppColors.brass], begin: Alignment.topRight, end: Alignment.bottomLeft),
                ),
              ),
            ),
          ),
        ),
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Expanded(child: Text(shop.name, style: AppText.display.copyWith(fontSize: 21))),
                const VerifiedStrip(),
              ]),
              const SizedBox(height: 4),
              Text('${shop.rating} ★ (${shop.reviews} تقييم) · ${shop.distanceKm} كم من موقعك · مفتوح حتى ${shop.closesAt}',
                  style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12.5)),
              const SizedBox(height: 16),
              const LivePriceTicker(),
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12), border: Border.all(color: AppColors.line)),
                child: Row(children: [
                  const Icon(Icons.location_on_outlined, color: AppColors.brass, size: 18),
                  const SizedBox(width: 8),
                  Expanded(child: Text(shop.address, style: AppText.body.copyWith(fontSize: 13))),
                  TextButton(onPressed: () {}, child: const Text('الاتجاهات')),
                ]),
              ),
              const SizedBox(height: 18),
              Text('الذهب المتاح بالمحل', style: AppText.display.copyWith(fontSize: 16)),
              const SizedBox(height: 10),
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 2.4,
                children: categories
                    .map((c) => InkWell(
                          borderRadius: BorderRadius.circular(12),
                          onTap: () => Navigator.of(context)
                              .push(MaterialPageRoute(builder: (_) => ProductListScreen(shop: shop, initialCategory: c))),
                          child: Container(
                            alignment: Alignment.centerRight,
                            padding: const EdgeInsets.symmetric(horizontal: 14),
                            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12), border: Border.all(color: AppColors.line)),
                            child: Text(c, style: AppText.body.copyWith(fontWeight: FontWeight.w700)),
                          ),
                        ))
                    .toList(),
              ),
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: AppColors.trustSoft, borderRadius: BorderRadius.circular(12)),
                child: Row(children: [
                  const Icon(Icons.credit_card_outlined, color: AppColors.trust, size: 18),
                  const SizedBox(width: 8),
                  Expanded(
                      child: Text('بتدفع دلوقتي، لكن المحل مبياخدش المبلغ إلا بعد ما تستلم ذهبك بنفسك.',
                          style: AppText.body.copyWith(color: AppColors.trust, fontSize: 12))),
                ]),
              ),
            ]),
          ),
        ),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
          child: ElevatedButton(
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => ProductListScreen(shop: shop))),
            child: const Text('تصفّح واختر الذهب'),
          ),
        ),
      ),
    );
  }
}
