---
title: Kotlin 제네릭 (기초~심화)
date: 2026.06.29
---
## 1. 왜 제네릭?

제네릭은 **타입 안전** 과 **재사용** 을 동시에 얻기 위한 장치다. 타입을 파라미터로 받아 하나의 코드가 여러 타입에 동작하되, 컴파일러가 타입을 끝까지 추적한다.

제네릭이 없다면 `Any` 로 담고 꺼낼 때마다 캐스팅해야 한다. 이는 런타임에 `ClassCastException` 으로 터질 위험을 컴파일 타임 검증 없이 떠안는 것이다.

```
// 위험: Any 박스 — 캐스팅이 런타임까지 미뤄진다
val box: MutableList<Any> = mutableListOf()
box.add("hello")
val n = box[0] as Int   // 컴파일 OK, 런타임 ClassCastException

// 안전: 제네릭 — 컴파일러가 타입을 고정
val strings: MutableList<String> = mutableListOf()
strings.add("hello")
// strings.add(42)        // 컴파일 에러 (애초에 못 넣음)
val s: String = strings[0]  // 캐스팅 불필요
```

---

## 2. 기본 문법

타입 파라미터는 꺾쇠 `<T>` 로 선언한다. 함수는 이름 앞에, 클래스는 이름 뒤에 둔다.

```
// 제네릭 함수: fun 키워드와 이름 사이에 <T>
fun <T> singletonList(item: T): List<T> = listOf(item)

val xs = singletonList(42)        // T = Int 로 추론
val ys = singletonList("a")       // T = String

// 제네릭 클래스: 클래스 이름 뒤에 <T>
class Box<T>(private var value: T) {
    fun get(): T = value
    fun set(v: T) { value = v }
}

val intBox = Box(1)               // Box<Int>
val strBox: Box<String> = Box("x")
```

---

## 3. 제약 (constraints)

상한(upper bound)을 주면 타입 파라미터가 받을 수 있는 타입을 좁힌다. 명시하지 않으면 기본 상한은 `Any?` (nullable 포함)이다.

```
// 상한: T 는 Number 의 하위 타입만
fun <T : Number> sum(list: List<T>): Double =
    list.sumOf { it.toDouble() }

sum(listOf(1, 2, 3))        // OK (Int : Number)
// sum(listOf("a", "b"))    // 컴파일 에러

// 상한 두 개 이상 → where 절
fun <T> copyWhenGreater(list: List<T>, threshold: T): List<String>
    where T : CharSequence,        // 문자열류이고
          T : Comparable<T> =      // 비교 가능해야
    list.filter { it > threshold }.map { it.toString() }
```

- 상한 1개는 `<T : Number>` 처럼 인라인.
- 상한 2개 이상은 `where` 절로 분리.
- 기본 상한은 `Any?` — null 을 막으려면 `<T : Any>` 로 non-null 상한을 건다.

---

## 4. 변성 (variance)

제네릭은 기본이 **무공변(invariant)** 이다. 즉 **String 이 Any 의 하위 타입이어도 Box\<String> 은 Box\<Any> 의 하위 타입이 아니다**.

왜? 무공변이 아니면 타입 구멍이 뚫린다. 만약 `MutableList<String>` 을 `MutableList<Any>` 로 취급할 수 있다면, 그 리스트에 Int 를 add 할 수 있게 되어 원래의 String 리스트가 오염된다.

```
// 무공변이라 아래는 컴파일 에러 — 다행히 막아준다
// val anys: MutableList<Any> = mutableListOf("a")  // ERROR
// anys.add(42)   // 만약 허용됐다면 String 리스트에 Int 가 섞임
```

**out = 공변(생산자), in = 반공변(소비자)**

- `out T` — T 를 **생산(반환)만** 한다. `Box<out T>` 면 `Box<String>` 을 `Box<Any>` 로 쓸 수 있다(공변).
- `in T` — T 를 **소비(인자로 받기)만** 한다. `Box<in T>` 면 `Box<Any>` 를 `Box<String>` 로 쓸 수 있다(반공변).

**PECS** — Producer-Extends, Consumer-Super (Kotlin 으로는 "생산자는 out, 소비자는 in").

```
// 표준 라이브러리가 바로 이 규칙을 따른다
public interface List<out E>        // 읽기 전용 → E 를 생산만 → out
public interface Comparator<in T> { // T 를 받아 비교만 → 소비 → in
    fun compare(a: T, b: T): Int
}

// 공변 덕에 자연스럽게 동작
val strs: List<String> = listOf("a", "b")
val anys: List<Any> = strs          // OK — List 가 out E 라서

// 반공변 덕에 상위 타입 Comparator 를 하위에 사용
val anyCmp: Comparator<Any> = compareBy { it.hashCode() }
val strCmp: Comparator<String> = anyCmp  // OK — Comparator 가 in T 라서
```

**위치 제약** : `out` 타입은 **in 위치(함수 파라미터)** 에 올 수 없고, `in` 타입은 **out 위치(반환값)** 에 올 수 없다. 컴파일러가 강제한다.

---

## 5. 선언지점 vs 사용지점 변성, star projection

- **선언지점(declaration-site)** : 클래스 정의에 `out/in` 을 박는다(예: `interface List<out E>`). 한 번 선언하면 모든 사용처에 적용 — Kotlin 이 선호하는 방식.
- **사용지점(use-site)** : 무공변 타입을 그때그때 좁혀 쓴다(자바의 와일드카드에 해당). 예: `Array<out Any>` 를 함수 파라미터로.

```
// 사용지점 변성: Array 는 무공변이지만 이 함수 안에선 읽기(생산)만
fun copy(from: Array<out Any>, to: Array<Any>) {
    for (i in from.indices) to[i] = from[i]
}

// star projection <*> : 타입 인자를 모를/상관 안 할 때
fun printSize(list: List<*>) = println(list.size)  // 원소 타입 무관
// List<*> 에서 꺼낸 원소 타입은 Any? 로만 보인다
```

**star projection** `<*>` 은 "타입 인자가 무엇이든 안전하게" 다룰 때 쓴다. `Foo<out T : TUpper>` 의 `Foo<*>` 는 생산 시 `TUpper`, 소비는 막힌 형태로 취급된다.

---

## 6. 타입 소거 + reified

JVM 은 **타입 소거(erasure)** 를 한다 — 런타임에 `List<String>` 과 `List<Int>` 은 똑같은 `List` 일 뿐 타입 인자가 사라진다. 그래서 일반 함수 안에서는 `T::class` 나 `value is T` 를 쓸 수 없다.

해결책: `inline` 함수 + `reified` 타입 파라미터. 호출부에 본문이 인라인되면서 실제 타입이 박혀, 런타임에도 T 를 알 수 있다.

```
// reified 로 런타임 타입 체크 가능
inline fun <reified T> Any?.asOrNull(): T? = this as? T

val x: Any = "hello"
val s: String? = x.asOrNull<String>()  // "hello"
val n: Int?    = x.asOrNull<Int>()     // null (안전)

// 실전 1: ViewModelProvider 같은 팩토리 헬퍼
inline fun <reified VM : ViewModel> factory(): Class<VM> = VM::class.java

// 실전 2: Gson 역직렬화 — TypeToken 보일러플레이트 제거
inline fun <reified T> Gson.fromJson(json: String): T =
    fromJson(json, object : TypeToken<T>() {}.type)
```

- `reified` 는 반드시 `inline` 함수에서만 쓸 수 있다.
- 덕분에 `T::class.java` · `is T` · `as? T` 가 런타임에 동작한다.

---

## 7. 안드로이드 실전 예시

결과를 **생산만** 하는 Repository 는 `out` 으로 선언해 상위 타입 소비자에게 유연하게 넘긴다.

```
// 생산자 → out (공변)
interface Repository<out T> {
    suspend fun get(id: String): T
    fun stream(): Flow<T>
}

// NoteRepository 가 Repository<Note> 일 때
val repo: Repository<Note> = noteRepository
val anyRepo: Repository<Any> = repo   // OK — out 덕분

// 상태 래퍼도 공변이 자연스럽다 (생산만)
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

`Nothing` 은 모든 타입의 하위 타입이라, `out T` 공변 위치에서 `Loading` / `Error` 처럼 데이터 없는 상태를 깔끔히 표현한다.

> **요약: 기본은 무공변(안전) → 생산만 하면 out, 소비만 하면 in (PECS) → 런타임 타입이 필요하면 inline + reified.**
