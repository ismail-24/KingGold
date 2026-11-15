import 'package:flutter/material.dart';
import '../../data/mock_data.dart';
import '../../models/models.dart';
import '../../theme/app_theme.dart';
import '../orders/order_detail_screen.dart';

class OrdersScreen extends StatefulWidget {
  const OrdersScreen({super.key});
  @override
  State<OrdersScreen> createState() => _OrdersScreenState();
}

class _OrdersScreenState extends State<OrdersScreen> {
  int _tab = 0; // 0 all, 1 active, 2 completed

  @override
  Widget build(BuildContext context) {
    final orders = AppStore.instance.orders;
    final filtered = orders.where((o) {
      if (_tab == 1) return o.status == OrderStatus.preparing || o.status == OrderStatus.readyForPickup;
      if (_tab == 2) return o.status == OrderStatus.completed;
      return true;
    }).toList();

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 14, 18, 24),
        children: [
          Text('طلباتي', style: AppText.display.copyWith(fontSize: 20)),
          Text('تابع حالة طلباتك وأموالك المحفوظة', style: AppText.body.copyWith(color: AppColors.muted)),
          const SizedBox(height: 14),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: AppColors.ink, borderRadius: BorderRadius.circular(18)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                const Icon(Icons.lock_outline, color: AppColors.brassLight, size: 16),
                const SizedBox(width: 6),
                Text('محفوظ في الضمان', style: AppText.body.copyWith(color: AppColors.brassLight, fontSize: 12)),
              ]),
              const SizedBox(height: 6),
              Text('${AppStore.instance.escrowHeld.toStringAsFixed(2)} ج.م',
                  style: AppText.display.copyWith(color: Colors.white, fontSize: 26)),
              const SizedBox(height: 4),
              Text('لا يصل المبلغ لأي محل إلا بعد استلامك للذهب.', style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
            ]),
          ),
          const SizedBox(height: 14),
          Row(children: [
            _tabChip('الكل', 0),
            const SizedBox(width: 8),
            _tabChip('النشطة', 1),
            const SizedBox(width: 8),
            _tabChip('المكتملة', 2),
          ]),
          const SizedBox(height: 12),
          for (final o in filtered)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _OrderTile(order: o),
            ),
          if (filtered.isEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 40),
              child: Center(child: Text('مفيش طلبات هنا دلوقتي', style: AppText.body.copyWith(color: AppColors.muted))),
            ),
        ],
      ),
    );
  }

  Widget _tabChip(String label, int idx) {
    final sel = _tab == idx;
    return InkWell(
      onTap: () => setState(() => _tab = idx),
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
        decoration: BoxDecoration(
          color: sel ? AppColors.ink : Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: sel ? AppColors.ink : AppColors.line),
        ),
        child: Text(label, style: AppText.body.copyWith(color: sel ? AppColors.brassLight : AppColors.muted, fontWeight: FontWeight.w700, fontSize: 13)),
      ),
    );
  }
}

class _OrderTile extends StatelessWidget {
  final GoldOrder order;
  const _OrderTile({required this.order});

  String get _statusLabel {
    switch (order.status) {
      case OrderStatus.paid:
        return 'تم الدفع';
      case OrderStatus.preparing:
        return 'قيد التجهيز';
      case OrderStatus.readyForPickup:
        return 'جاهز للاستلام';
      case OrderStatus.completed:
        return 'مكتمل';
      case OrderStatus.disputed:
        return 'تحت المراجعة';
    }
  }

  Color get _statusColor {
    switch (order.status) {
      case OrderStatus.completed:
        return AppColors.muted;
      case OrderStatus.disputed:
        return AppColors.alert;
      default:
        return AppColors.trust;
    }
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => OrderDetailScreen(order: order))),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16), border: Border.all(color: AppColors.line)),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
            Text('طلب ${order.id}', style: AppText.body.copyWith(fontWeight: FontWeight.w800)),
            Text('${order.total.toStringAsFixed(2)} ج.م', style: AppText.body.copyWith(fontWeight: FontWeight.w800)),
          ]),
          const SizedBox(height: 3),
          Text('${order.shop.name} · ${order.pickupWindow}', style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(color: _statusColor.withOpacity(0.12), borderRadius: BorderRadius.circular(20)),
            child: Text(_statusLabel, style: TextStyle(color: _statusColor, fontSize: 11, fontWeight: FontWeight.bold)),
          ),
        ]),
      ),
    );
  }
}
