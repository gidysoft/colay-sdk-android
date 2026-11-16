# ColayAI SDK: public API and reflective access to the advertising ID
-keep class com.colayai.sdk.Colay { *; }
-keep class com.colayai.sdk.ColayConfig { *; }
-keep class com.colayai.sdk.Consent { *; }
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient { *; }
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info { *; }
-dontwarn com.google.android.gms.ads.identifier.**
