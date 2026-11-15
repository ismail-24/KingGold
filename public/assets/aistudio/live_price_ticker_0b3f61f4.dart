import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import '../data/mock_data.dart';
import '../theme/app_theme.dart';

/// اللحظة الإبداعية الوحيدة المقصودة في التطبيق: السعر مش رقم ثابت،
/// بل بيتحرك بهزة بسيطة كل ثواني زي ميزان الصائغ بيستقر على وزن،
/// عشان يبان "لحظي" فعلاً من غير ما نعمل حركة في كل حتة في الشاشة.
class LivePriceTicker extends StatefulWidget {
  final int highlightKarat;
  const LivePriceTicker({super.key, this.highlightKarat = 21});

  @override
  State<LivePriceTicker> createState() => _LivePriceTickerState();
}

class _LivePriceTickerState extends State<LivePriceTicker> {
  Timer? _timer;
  final _rand = Random();
  late List<double> _wobble;

  @override
  void initState() {
    super.initState();
    _wobble = List.filled(AppStore.instance.prices.length, 0);
    _timer = Timer.periodic(const Duration(seconds: 4), (_) {
      setState(() {
        _wobble = _wobble.map((_) => (_rand.nextDouble() - 0.5) * 3).toList();
      });
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final prices = AppStore.instance.prices;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.ink,
        borderRadius: BorderRadius.circular(18),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.18), blurRadius: 16, offset: const Offset(0, 8))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(children: [
                _pulsingDot(),
                const SizedBox(width: 6),
                Text('السعر اللحظي · ج.م/جرام', style: AppText.body.copyWith(color: AppColors.brassLight, fontSize: 12)),
              ]),
              Text(TimeOfDay.now().format(context), style: AppText.body.copyWith(color: AppColors.muted, fontSize: 12)),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: List.generate(prices.length, (i) {
              final p = prices[i];
              final isHi = p.karat == widget.highlightKarat;
              return Expanded(
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 500),
                  margin: const EdgeInsets.symmetric(horizontal: 3),
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  decoration: BoxDecoration(
                    color: isHi ? AppColors.brassLight : AppColors.inkSoft,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Column(children: [
                    Text(p.label, style: AppText.body.copyWith(
                        color: isHi ? AppColors.ink.withOpacity(0.7) : AppColors.muted, fontSize: 11)),
                    const SizedBox(height: 3),
                    TweenAnimationBuilder<double>(
                      tween: Tween(begin: p.pricePerGram, end: p.pricePerGram + _wobble[i]),
                      duration: const Duration(milliseconds: 900),
                      curve: Curves.easeOutCubic,
                      builder: (_, value, __) => Text(
                        value.toStringAsFixed(0),
                        style: AppText.display.copyWith(
                            fontSize: 16, color: isHi ? AppColors.ink : Colors.white),
                      ),
                    ),
                    Text('${p.changePercent}% ▲', style: AppText.body.copyWith(
                        fontSize: 10, color: isHi ? AppColors.trust : const Color(0xFF8FE3A8))),
                  ]),
                ),
              );
            }),
          ),
        ],
      ),
    );
  }

  Widget _pulsingDot() {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.4, end: 1),
      duration: const Duration(seconds: 1),
      curve: Curves.easeInOut,
      builder: (_, v, __) => Opacity(
        opacity: v,
        child: Container(width: 7, height: 7, decoration: const BoxDecoration(color: Color(0xFF3AC25A), shape: BoxShape.circle)),
      ),
      onEnd: () {},
    );
  }
}
