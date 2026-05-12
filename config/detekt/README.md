Detekt 운영 기준 (Kotlin + Spring Boot)
목적

본 설정은 Kotlin + Spring Boot 환경에서 코드 품질을 안정적으로 유지하기 위한 detekt 기준안이다.

Detekt는 Kotlin 진영에서 사실상 표준적으로 사용되는 static analysis 도구이며, 다음과 같은 목적을 가진다.

복잡도 증가 방지
유지보수성 향상
잠재적 버그 조기 탐지
Kotlin 스타일 일관성 확보
코드 리뷰 비용 감소

본 설정은 다음 원칙을 기준으로 구성한다.

“이상적인 코드”보다 “실무에서 지속 가능한 코드”를 우선한다.
레거시 코드베이스에도 점진적으로 도입 가능해야 한다.
Kotlin idiom과 Spring 실무 스타일을 존중한다.
테스트 코드에는 과도한 제약을 적용하지 않는다.
운영 방향
1. Formatting과 Static Analysis 분리

Formatting은 ktlint에 위임하고, detekt는 다음 역할에 집중한다.

complexity 관리
exception handling 검증
naming convention 검증
potential bug 탐지
maintainability 개선

즉:

ktlint → 코드 모양(formatting)
detekt → 코드 품질(code quality)

역할로 분리한다.

Complexity 규칙
CyclomaticComplexMethod
allowedComplexity: 15

메서드 복잡도를 제한한다.

너무 높은 복잡도는 다음 문제를 유발한다.

테스트 어려움
버그 증가
코드 리뷰 난이도 증가
변경 영향도 확대

다만 Spring 서비스 계층에서는 다음과 같은 orchestration 코드가 자주 발생한다.

validation
branching
transactional flow
policy handling

따라서 현실적인 기준인 15를 사용한다.

또한:

ignoreSingleWhenExpression: true

옵션을 통해 Kotlin의 when 기반 routing/branching 스타일을 허용한다.

LongParameterList
allowedFunctionParameters: 6
allowedConstructorParameters: 7

과도한 파라미터는 응집도 저하 신호로 본다.

다만 Kotlin/Spring 환경에서는 다음과 같은 경우 파라미터 수가 자연스럽게 증가할 수 있다.

controller
configuration bean
query object
usecase orchestration

따라서 지나치게 엄격한 제한은 두지 않는다.

또한 Kotlin 특성을 고려해:

ignoreDefaultParameters: true
ignoreDataClasses: true

를 적용한다.

NestedBlockDepth
allowedDepth: 4

중첩 depth 증가는 가독성과 유지보수성을 크게 저하시킨다.

특히 다음 구조를 지양한다.

다중 if nesting
중첩 loop
callback nesting

4단계 이상부터는 함수 분리 또는 early return을 권장한다.

Exception 규칙
SwallowedException

예외를 무시하는 코드를 방지한다.

다만 다음과 같은 의도적 무시 패턴은 허용한다.

ignore
expected
_

예:

catch (ignore: Exception)
TooGenericExceptionCaught

다음과 같은 광범위 예외 처리를 제한한다.

catch (Exception)
catch (Throwable)

이유:

실제 장애 원인 은닉
예상하지 못한 예외까지 삼킴
디버깅 어려움

다만 boundary layer에서는 일부 허용될 수 있다.

예:

scheduler
batch entrypoint
async consumer
global exception handler
TooGenericExceptionThrown

과도하게 일반적인 예외 throw를 방지한다.

권장:

throw InvalidOrderStateException()

비권장:

throw RuntimeException()
Naming 규칙
PackageNaming

패키지명은 Kotlin/Java 표준 convention을 따른다.

소문자 기반
dot notation 사용

예:

com.example.payment
BooleanPropertyNaming

Boolean property는 의미가 드러나는 형태를 강제한다.

허용 prefix:

is
has
are
can

예:

isActive
hasPermission
canRetry

이는 Kotlin ecosystem에서 일반적으로 사용되는 naming convention이다.

Empty Block 규칙
EmptyFunctionBlock

의미 없는 빈 함수 구현을 방지한다.

다만 테스트 코드에서는 mock/stub 목적의 empty block이 자연스럽기 때문에 제외한다.

Style 규칙
MagicNumber

의미 없는 숫자 literal 남용을 제한한다.

권장:

const val MAX_RETRY = 3

비권장:

retry(3)

다만 다음 경우는 실용성을 위해 허용한다.

0, 1, 2
named argument
ranges
local variable
constant declaration

테스트 코드는 제외한다.

MaxLineLength
maxLineLength: 120

코드 가독성을 유지하기 위한 기준이다.

Kotlin/Spring 환경에서는 다음 요소 때문에 line length가 자연스럽게 길어진다.

annotation
DSL
reactive chain
builder pattern

따라서 120자를 현실적인 기준으로 사용한다.

ReturnCount
max: 4
excludeGuardClauses: true

과도한 return 분기를 제한한다.

다만 Kotlin에서는 guard clause 기반 early return을 적극 권장한다.

예:

if (condition) return

이는 nesting 감소와 가독성 향상에 도움이 된다.

따라서 guard clause는 제외한다.

ThrowsCount

비활성화한다.

이유:

Kotlin에는 checked exception 개념이 없음
Spring에서는 exception propagation이 일반적
실무에서 실효성이 낮음
Potential Bugs
potential-bugs:
  active: true

잠재적 버그 탐지를 활성화한다.

특히 Kotlin nullable 처리와 관련된 문제를 조기에 발견하는 데 목적이 있다.

예:

unsafe nullable access
unreachable code
invalid cast
suspicious collection handling
테스트 코드 제외 정책

테스트 코드는 운영 코드와 성격이 다르다.

다음 특성이 존재한다.

readability보다 intention 전달이 중요
fixture/setup 코드 증가
parameter 증가 가능
duplication 허용 가능

따라서 일부 규칙은 다음 경로에서 제외한다.

**/src/test/**
