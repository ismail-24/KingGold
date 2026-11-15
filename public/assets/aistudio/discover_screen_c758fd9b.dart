import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import '../../widgets/live_price_ticker.dart';
import '../../widgets/seal_badge.dart' show SealBadge, VerifiedStrip;
import '../shop/shop_detail_screen.dart';
import '../price/price_insight_screen.dart';

class DiscoverScreen extends StatelessWidget {
  const DiscoverScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final shops = AppStore.instance.shops;
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 14, 18, 24),
        children: [
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
            Row(children: [
              const SealBadge(size: 36),
              const SizedBox(width: 10),
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text('King Gold', style: AppText.display.copyWith(fontSize: 18)),
                Text('ملك الذهب', style: AppText.body.copyWith(fontSize: 11, color: AppColors.muted)),
              ]),
            ]),
            IconButton(
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const PriceInsightScreen())),
              icon: const Icon(Icons.show_chart, color: AppColors.brass),
              tooltip: 'تفاصيل الأسعار',
            ),
          ]),
          const SizedBox(height: 6),
          Text('ذهب حقيقي من محلات قريبة منك', style: AppText.display.copyWith(fontSize: 17)),
          Text('اختار المحل، ادفع بأمان، واستلم ذهبك بنفسك.', style: AppText.body.copyWith(color: AppColors.muted)),
          const SizedBox(height: 14),
          const LivePriceTicker(),
          const SizedBox(height: 14),
          Row(children: [
            Expanded(
              child: TextField(
                decoration: InputDecoration(
                  prefixIcon: const Icon(Icons.search, color: AppColors.muted),
                  hintText: 'ابحث عن محل أو منطقة أو منتج',
                ),
              ),
            ),
            const SizedBox(width: 8),
            InkWell(
              borderRadius: BorderRadius.circular(12),
              onTap: () => _openFilters(context),
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.tune, color: AppColors.brassLight, size: 20),
              ),
            ),
          ]),
          const SizedBox(height: 18),
          Text('محلات شريكة قريبة منك · ${shops.length} محل موثّق', style: AppText.display.copyWith(fontSize: 15)),
          const SizedBox(height: 10),
          for (final s in shops)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _ShopTile(shop: s),
            ),
          const SizedBox(height: 6),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: AppColors.trustSoft, borderRadius: BorderRadius.circular(12)),
            child: Row(children: [
              const Icon(Icons.shield_outlined, color: AppColors.trust, size: 18),
              const SizedBox(width: 8),
              Expanded(
                  child: Text('كل المحلات موثّقة، ومبلغك محفوظ في الضمان لحد ما تستلم ذهبك.',
                      style: AppText.body.copyWith(color: AppColors.trust, fontSize: 12))),
            ]),
          ),
        ],
      ),
    );
  }

  void _openFilters(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 30),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Center(child: Container(width: 40, height: 4, decoration: BoxDecoration(color: AppColors.line, borderRadius: BorderRadius.circular(2)))),
          const SizedBox(height: 16),
          Text('تصفية النتائج', style: AppText.display.copyWith(fontSize: 17)),
          const SizedBox(height: 14),
          Text('العيار', style: AppText.body.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(spacing: 8, children: ['عيار 18', 'عيار 21', 'عيار 24'].map((e) => Chip(label: Text(e))).toList()),
          const SizedBox(height: 14),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            value: true,
            onChanged: (_) {},
            title: const Text('محلات موثّقة فقط'),
            activeColor: AppColors.brass,
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(onPressed: () => Navigator.pop(context), child: const Text('عرض النتائج')),
          ),
        ]),
      ),
    );
  }
}

class _ShopTile extends StatelessWidget {
  final Shop shop;
  const _ShopTile({required this.shop});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => ShopDetailScreen(shop: shop))),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.line),
        ),
        child: Row(children: [
          Container(
            width: 58,
            height: 58,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(12),
              gradient: const LinearGradient(colors: [AppColors.brassLight, AppColors.brass]),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Expanded(child: Text(shop.name, style: AppText.body.copyWith(fontWeight: FontWeight.w800))),
                if (shop.verified) const VerifiedStrip(),
              ]),
              const SizedBox(height: 3),
              Text('${shop.rating} ★ (${shop.reviews}) · ${shop.distanceKm} كم · مفتوح حتى ${shop.closesAt}',
                  style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
            ]),
          ),
        ]),
      ),
    );
  }
}

