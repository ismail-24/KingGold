import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';

class KycScreen extends StatelessWidget {
  const KycScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('وثّق هويتك')),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          Text('خطوة واحدة بتخليك تشتري بثقة وتستلم ذهبك من غير تعطيل.', style: AppText.body.copyWith(color: AppColors.muted)),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(color: AppColors.trustSoft, borderRadius: BorderRadius.circular(14)),
            child: Row(children: [
              const Icon(Icons.verified_user_outlined, color: AppColors.trust),
              const SizedBox(width: 10),
              Expanded(
                  child: Text('لإصدار فاتورة الشراء باسمك، ولتأمين الاستلام بحيث ماحدش غيرك يستلم طلبك.',
                      style: AppText.body.copyWith(color: AppColors.trust, fontSize: 12.5))),
            ]),
          ),
          const SizedBox(height: 18),
          _uploadTile('صورة البطاقة · الوجه', done: true),
          _uploadTile('صورة البطاقة · الظهر', step: 2, active: true),
          _uploadTile('صورة شخصية', step: 3),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 8, 18, 12),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            ElevatedButton(onPressed: () => Navigator.pop(context), child: const Text('رفع ومتابعة')),
            const SizedBox(height: 6),
            Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              const Icon(Icons.lock_outline, size: 14, color: AppColors.muted),
              const SizedBox(width: 4),
              Text('نستخدم بياناتك لإصدار الفاتورة وتأمين الاستلام فقط.', style: AppText.body.copyWith(fontSize: 11, color: AppColors.muted)),
            ]),
          ]),
        ),
      ),
    );
  }

  Widget _uploadTile(String title, {bool done = false, bool active = false, int step = 1}) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: active ? AppColors.brass : AppColors.line, width: active ? 1.6 : 1),
      ),
      child: Column(children: [
        Row(children: [
          Expanded(child: Text(title, style: AppText.body.copyWith(fontWeight: FontWeight.w700))),
          done
              ? const Icon(Icons.check_circle, color: AppColors.trust)
              : CircleAvatar(radius: 12, backgroundColor: AppColors.parchmentDeep, child: Text('$step', style: const TextStyle(fontSize: 12))),
        ]),
        if (active) ...[
          const SizedBox(height: 10),
          Container(
            height: 90,
            width: double.infinity,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppColors.line, style: BorderStyle.solid),
              color: AppColors.parchment,
            ),
            child: const Center(child: Icon(Icons.camera_alt_outlined, color: AppColors.brass)),
          ),
        ],
      ]),
    );
  }
}
