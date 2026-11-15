class KaratPrice {
  final int karat; // 24, 21, 18 ... 0 يعني الجنيه الذهب
  final String label;
  final double pricePerGram; // أو سعر الجنيه الكامل لو label == 'الجنيه'
  final double changePercent;
  KaratPrice(this.karat, this.label, this.pricePerGram, this.changePercent);
}

class Shop {
  final String id;
  final String name;
  final String area;
  final String address;
  final double rating;
  final int reviews;
  final double distanceKm;
  final bool verified;
  final String closesAt;
  final double makingFeePerGram;
  Shop({
    required this.id,
    required this.name,
    required this.area,
    required this.address,
    required this.rating,
    required this.reviews,
    required this.distanceKm,
    required this.verified,
    required this.closesAt,
    required this.makingFeePerGram,
  });
}

class Product {
  final String id;
  final String shopId;
  final String name;
  final String category;
  final int karat;
  final double weightGrams;
  final bool stamped;
  final int availablePieces;
  Product({
    required this.id,
    required this.shopId,
    required this.name,
    required this.category,
    required this.karat,
    required this.weightGrams,
    required this.stamped,
    required this.availablePieces,
  });
}

class CartItem {
  final Product product;
  int quantity;
  CartItem(this.product, {this.quantity = 1});
}

enum OrderStatus { paid, preparing, readyForPickup, completed, disputed }

class GoldOrder {
  final String id;
  final Shop shop;
  final List<CartItem> items;
  final double goldValue;
  final double makingFee;
  final double serviceFee;
  final String pickupWindow;
  OrderStatus status;
  GoldOrder({
    required this.id,
    required this.shop,
    required this.items,
    required this.goldValue,
    required this.makingFee,
    required this.serviceFee,
    required this.pickupWindow,
    this.status = OrderStatus.paid,
  });

  double get total => goldValue + makingFee + serviceFee;
}
