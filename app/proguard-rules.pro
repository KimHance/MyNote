# R8 은 release 빌드에서 최적화 · 난독화 · 리소스 축소를 수행한다(app/build.gradle.kts 의 release 블록).
#
# Retrofit / OkHttp / Room / Hilt / kotlinx.serialization 은 각자 consumer rules 를 아티팩트에 담아
# 배포하므로 여기에 keep 규칙을 중복 선언하지 않는다. 실제로 누락된 규칙이 있으면 R8 이
# build/outputs/mapping/release/missing_rules.txt 를 만들어주므로, 그때 필요한 것만 추가한다.

# 난독화된 스택트레이스를 mapping.txt 로 복원할 수 있도록 소스·라인 정보를 남긴다.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
