import 'package:flutter/foundation.dart';
import '../models/models.dart';

/// بيانات ستاتيك للنموذج المبدئي. هتتستبدل لاحقاً بـ API حقيقي،
/// لكن الشكل (Model) جاهز عشان الاستبدال يبقى سهل.
class AppStore extends ChangeNotifier {
  AppStore._internal();
  static final AppStore instance = AppStore._internal();

  final List<KaratPrice> prices = [
    KaratPrice(24, 'عيار 24', 6200, 0.4),
    KaratPrice(21, 'عيار 21', 5425, 0.4),
    KaratPrice(18, 'عيار 18', 4650, 0.4),
    KaratPrice(0, 'الجنيه', 43400, 0.4),
  ];

  final List<Shop> shops = [
    Shop(
      id: 's1',
      name: 'النور للذهب',
      area: 'المعادي',
      address: '12 شارع 9، المعادي، القاهرة',
      rating: 4.9,
      reviews: 212,
      distanceKm: 1.2,
      verified: true,
      closesAt: '9:00 م',
      makingFeePerGram: 90,
    ),
    Shop(
      id: 's2',
      name: 'ذهب المعادي',
      area: 'المعادي',
      address: '4 شارع 151، المعادي، القاهرة',
      rating: 4.8,
      reviews: 158,
      distanceKm: 2.4,
      verified: true,
      closesAt: '10:00 م',
      makingFeePerGram: 95,
    ),
    Shop(
      id: 's3',
      name: 'مجوهرات الأمير',
      area: 'المعادي',
      address: '20 كورنيش النيل، المعادي، القاهرة',
      rating: 4.7,
      reviews: 96,
      distanceKm: 3.1,
      verified: true,
      closesAt: '8:00 م',
      makingFeePerGram: 100,
    ),
  ];

  List<Product> productsFor(String shopId) => [
        Product(
            id: 'p1',
            shopId: shopId,
            name: 'جنيه ذهب',
            category: 'جنيهات وسبائك',
            karat: 21,
            weightGrams: 8,
            stamped: true,
            availablePieces: 14),
        Product(
            id: 'p2',
            shopId: shopId,
            name: 'خاتم سادة',
            category: 'خواتم',
            karat: 21,
            weightGrams: 3.2,
            stamped: true,
            availablePieces: 3),
        Product(
            id: 'p3',
            shopId: shopId,
            name: 'أسورة حلقات',
            category: 'أساور',
            karat: 21,
            weightGrams: 12.4,
            stamped: true,
            availablePieces: 5),
        Product(
            id: 'p4',
            shopId: shopId,
            name: 'سلسلة رفيعة',
            category: 'سلاسل',
            karat: 18,
            weightGrams: 9.6,
            stamped: true,
            availablePieces: 9),
      ];

  double gramPrice(int karat) =>
      prices.firstWhere((p) => p.karat == karat).pricePerGram;

  // --- السلة ---
  final List<CartItem> cart = [];
  Shop? cartShop;

  void addToCart(Shop shop, Product product) {
    if (cartShop != null && cartShop!.id != shop.id) {
      cart.clear(); // محل واحد لكل سلة، زي ما اتفقنا منطقياً
    }
    cartShop = shop;
    final existing = cart.where((c) => c.product.id == product.id);
    if (existing.isNotEmpty) {
      existing.first.quantity++;
    } else {
      cart.add(CartItem(product));
    }
    notifyListeners();
  }

  void removeFromCart(CartItem item) {
    cart.remove(item);
    if (cart.isEmpty) cartShop = null;
    notifyListeners();
  }

  double get cartGoldValue => cart.fold(
      0, (sum, c) => sum + gramPrice(c.product.karat) * c.product.weightGrams * c.quantity);
  double get cartMakingFee => cart.fold(
      0,
      (sum, c) =>
          sum + (cartShop?.makingFeePerGram ?? 0) * c.product.weightGrams * c.quantity);
  double get cartServiceFee => (cartGoldValue + cartMakingFee) * 0.01;
  double get cartTotal => cartGoldValue + cartMakingFee + cartServiceFee;

  void clearCart() {
    cart.clear();
    cartShop = null;
    notifyListeners();
  }

  // --- الطلبات ---
  final List<GoldOrder> orders = [
    GoldOrder(
      id: 'KG-20410',
      shop: shops[2],
      items: [],
      goldValue: 28700,
      makingFee: 2000,
      serviceFee: 310,
      pickupWindow: '28 سبتمبر',
      status: OrderStatus.completed,
    ),
    GoldOrder(
      id: 'KG-20430',
      shop: shops[0],
      items: [],
      goldValue: 29200,
      makingFee: 560,
      serviceFee: 298,
      pickupWindow: '3 أكتوبر',
      status: OrderStatus.completed,
    ),
    GoldOrder(
      id: 'KG-20467',
      shop: shops[1],
      items: [],
      goldValue: 62100,
      makingFee: 830,
      serviceFee: 629,
      pickupWindow: '10 أكتوبر',
      status: OrderStatus.readyForPickup,
    ),
  ];

  GoldOrder placeOrder(String pickupWindow) {
    final order = GoldOrder(
      id: 'KG-${20480 + orders.length + 1}',
      shop: cartShop!,
      items: List.of(cart),
      goldValue: cartGoldValue,
      makingFee: cartMakingFee,
      serviceFee: cartServiceFee,
      pickupWindow: pickupWindow,
      status: OrderStatus.preparing,
    );
    orders.insert(0, order);
    clearCart();
    notifyListeners();
    return order;
  }

  double get escrowHeld => orders
      .where((o) => o.status == OrderStatus.preparing || o.status == OrderStatus.readyForPickup)
      .fold(0, (sum, o) => sum + o.total);
}
