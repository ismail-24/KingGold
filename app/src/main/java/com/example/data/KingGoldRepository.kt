package com.example.data

import com.example.model.GoldOrder
import com.example.model.KaratPrice
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.Shop

object KingGoldRepository {

    val prices: List<KaratPrice> = listOf(
        KaratPrice(24, "عيار 24", 6200.0, 0.4),
        KaratPrice(21, "عيار 21", 5425.0, 0.4),
        KaratPrice(18, "عيار 18", 4650.0, 0.4),
        KaratPrice(0, "الجنيه", 43400.0, 0.4),
    )

    val priceHistory7Days: List<Double> = listOf(
        5300.0, 5335.0, 5310.0, 5360.0, 5355.0, 5390.0, 5425.0,
    )

    val priceHistoryLabels: List<String> = listOf(
        "15 أكت", "16", "17", "18", "19", "20", "21 أكت",
    )

    private val relative24K7DayTrend: List<Pair<String, Double>> = listOf(
        "قبل 6 أيام" to 0.975,
        "قبل 5 أيام" to 0.981,
        "قبل 4 أيام" to 0.977,
        "قبل 3 أيام" to 0.986,
        "قبل يومين" to 0.985,
        "أمس" to 0.993,
        "اليوم" to 1.000,
    )

    fun priceHistory24KForCurrentPrice(current24KPrice: Double): List<com.example.model.GoldPriceHistoryPoint> {
        val anchor = if (current24KPrice > 0.0) current24KPrice else 6200.0
        return relative24K7DayTrend.map { (label, factor) ->
            com.example.model.GoldPriceHistoryPoint(
                dayLabel = label,
                pricePerGram = kotlin.math.round(anchor * factor),
            )
        }
    }

    fun priceHistory21KForCurrentPrice(current21KPrice: Double): List<com.example.model.GoldPriceHistoryPoint> {
        val anchor = if (current21KPrice > 0.0) current21KPrice else 5425.0
        return relative24K7DayTrend.map { (label, factor) ->
            com.example.model.GoldPriceHistoryPoint(
                dayLabel = label,
                pricePerGram = kotlin.math.round(anchor * factor),
            )
        }
    }

    val shops: List<Shop> = listOf(
        Shop(
            id = "s1",
            name = "النور للذهب",
            area = "المعادي",
            address = "12 شارع 9، المعادي، القاهرة",
            phone = "+201001234567",
            rating = 4.9,
            reviews = 212,
            distanceKm = 1.2,
            verified = true,
            closesAt = "9:00 م",
            workingHours = "السبت – الخميس: 10:00 ص – 9:00 م\nالجمعة: 2:00 م – 9:00 م",
            makingFeePerGram = 90.0,
            city = "القاهرة",
            latitude = 29.9602,
            longitude = 31.2569,
        ),
        Shop(
            id = "s2",
            name = "ذهب المعادي",
            area = "المعادي",
            address = "4 شارع 151، المعادي، القاهرة",
            phone = "+201009876543",
            rating = 4.8,
            reviews = 158,
            distanceKm = 2.4,
            verified = true,
            closesAt = "10:00 م",
            workingHours = "يومياً: 11:00 ص – 10:00 م",
            makingFeePerGram = 95.0,
            city = "القاهرة",
            latitude = 29.9631,
            longitude = 31.2612,
        ),
        Shop(
            id = "s3",
            name = "مجوهرات الأمير",
            area = "المعادي",
            address = "20 كورنيش النيل، المعادي، القاهرة",
            phone = "+201112223344",
            rating = 4.7,
            reviews = 96,
            distanceKm = 3.1,
            verified = true,
            closesAt = "8:00 م",
            workingHours = "السبت – الخميس: 10:30 ص – 8:00 م\nالجمعة: مغلق",
            makingFeePerGram = 100.0,
            city = "القاهرة",
            latitude = 29.9565,
            longitude = 31.2495,
        ),
    )

    fun productsFor(shopId: String): List<Product> = listOf(
        Product(
            id = "p1",
            shopId = shopId,
            name = "جنيه ذهب",
            category = "جنيهات وسبائك",
            karat = 21,
            weightGrams = 8.0,
            stamped = true,
            availablePieces = 14,
        ),
        Product(
            id = "p2",
            shopId = shopId,
            name = "خاتم سادة",
            category = "خواتم",
            karat = 21,
            weightGrams = 3.2,
            stamped = true,
            availablePieces = 3,
        ),
        Product(
            id = "p3",
            shopId = shopId,
            name = "أسورة حلقات",
            category = "أساور",
            karat = 21,
            weightGrams = 12.4,
            stamped = true,
            availablePieces = 5,
        ),
        Product(
            id = "p4",
            shopId = shopId,
            name = "سلسلة رفيعة",
            category = "سلاسل",
            karat = 18,
            weightGrams = 9.6,
            stamped = true,
            availablePieces = 9,
        ),
        Product(
            id = "p5",
            shopId = shopId,
            name = "سبيكة BTC 5 جم",
            category = "جنيهات وسبائك",
            karat = 24,
            weightGrams = 5.0,
            stamped = true,
            availablePieces = 7,
        ),
    )

    fun gramPrice(karat: Int): Double =
        prices.firstOrNull { it.karat == karat }?.pricePerGram ?: 5425.0

    val initialOrders: List<GoldOrder> = listOf(
        GoldOrder(
            id = "KG-20467",
            shop = shops[1],
            items = emptyList(),
            goldValue = 62100.0,
            makingFee = 830.0,
            serviceFee = 629.0,
            pickupWindow = "10 أكتوبر",
            status = OrderStatus.READY_FOR_PICKUP,
        ),
        GoldOrder(
            id = "KG-20430",
            shop = shops[0],
            items = emptyList(),
            goldValue = 29200.0,
            makingFee = 560.0,
            serviceFee = 298.0,
            pickupWindow = "3 أكتوبر",
            status = OrderStatus.COMPLETED,
        ),
        GoldOrder(
            id = "KG-20410",
            shop = shops[2],
            items = emptyList(),
            goldValue = 28700.0,
            makingFee = 2000.0,
            serviceFee = 310.0,
            pickupWindow = "28 سبتمبر",
            status = OrderStatus.COMPLETED,
        ),
    )

    val paymentMethods: List<PaymentMethod> = listOf(
        PaymentMethod("card", "بطاقة بنكية", "Visa •••• 4821"),
        PaymentMethod("instapay", "إنستاباي", "تحويل بنكي فوري"),
        PaymentMethod("wallet", "محفظة إلكترونية", "فودافون كاش · فوري"),
    )

    val pickupSlots: List<String> = listOf(
        "اليوم · 4:00–8:00 م",
        "غداً · 12:00–4:00 م",
        "غداً · 4:00–9:00 م",
    )

    val supportFaqs: List<Pair<String, String>> = listOf(
        "إمتى بيتحول المبلغ للمحل؟" to
            "لا يُصرف أي مبلغ للمحل عند الدفع. يظل المبلغ محفوظاً في حساب الضمان ولا يُحوّل للمحل إلا بعد حضورك، فحص القطع وميزانها، وتأكيد الاستلام برمز الاستلام.",
        "إيه معنى \"محفوظ في الضمان\"؟" to
            "نظام الضمان (Escrow) بيحفظ فلوسك عند King Gold لحد ما تستلم ذهبك بنفسك وتتأكد من الوزن والدمغة والفاتورة.",
        "لو المحل رفض التسليم أعمل إيه؟" to
            "افتح تفاصيل الطلب واضغط \"الإبلاغ عن مشكلة\" فوراً. يتم تجميد المبلغ تلقائياً ويراجع فريقنا البلاغ ويرد عليك خلال ساعات، أو يُرد لك المبلغ كاملاً.",
        "إزاي أتأكد إن المحل موثّق؟" to
            "كل محل عليه شارة الدمغة \"موثّق\" تم التحقق من سجله التجاري والبطاقة الضريبية وموقع المحل الفعلي وفواتيره الرسمية.",
    )
}
