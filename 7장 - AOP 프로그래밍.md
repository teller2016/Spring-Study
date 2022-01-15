# 7장 - AOP 프로그래밍

> - 프록시와 AOP
> - 스프링 AOP 구현

- 트랜잭션의 처리 방식을 이해하려면 **AOP(Aspect Oriented Programming)**을 알아야 한다

## 1. 프로젝트 준비

> - pom.xml 파일에 aspectjweaver 의존을 추가함
>   - 스프링이 AOP를 구현할때 사용하는 모듈

- 스프링 프레임워크의 AOP 기능은 spring-aop 모듈이 제공한다
  - spring-context 모듈을 의존 대상에 추가하면 함께 의존 대상에 포함된다 ( 즉, spring-aop는 따로 의존 추가 안해도 된다 )

- aspectjweaver 모듈은 AOP를 설정하는데 필요한 애노테이션을 제공

- 인터페이스 작성

  ```java
  package chap07;
  
  public interface Calculator {
  	
  	public long factorial(long num);
  	
  }
  ```

- 인터페이스 구현

  - for문 이용한 구현

  ```java
  public class ImpeCalculator implements Calculator {
  
  	@Override
  	public long factorial(long num) {
  		long result = 1;
  		for(long i = 1; i <= num; i++) {
  			result *= i;
  		}
  		return result;
  	}
  
  }
  ```

  - 재귀 이용한 구현

  ```java
  public class RecCalculator implements Calculator {
  
  	@Override
  	public long factorial(long num) {
  		if(num==0)
  			return 1;
  		else
  			return num * factorial(num-1);
  	}
  
  }
  ```



## 2. 프록시와 AOP

- 위의 구현 클래스들의 실행 시간을 출력하려면?

  - 쉬운 방법 : 메서드의 시작과 끝에서 시간을 구하고, 두 시간의 차이를 출력

  - `ImpeCalculator` 클래스 수정

    ```java
    public class ImpeCalculator implements Calculator {
    
    	@Override
    	public long factorial(long num) {
    		
    		long start = System.currentTimeMillis();		//시작 시간
    		
    		long result = 1;
    		for(long i = 1; i <= num; i++) {
    			result *= i;
    		}
    		
    		long end = System.currentTimeMillis();		//종료 시간
    		System.out.printf("ImpeCalculator.factorial(%d) 실행시간 = %d\n", num, (end-start));
    		
    		return result;
    	}
    
    }
    ```

  - `RecCalculator` 클래스 수정 (재귀라서 약간 복잡하다)

    ```java
    public class RecCalculator implements Calculator {
    
    	@Override
    	public long factorial(long num) {
    		long start = System.currentTimeMillis();
    		
    		try {
    			if(num==0)
    				return 1;
    			else
    				return num * factorial(num-1);
    		}finally {
    			long end = System.currentTimeMillis();
    			System.out.printf("RecCalculator.factorial(%d) 실행시간 = %d\n", num, (end-start));
    		}
    	}
    }
    ```

  - 차라리 메서드 실행 전후에 값을 구하는건?

    ```java
    ImpeCalculator impeCal = new ImpeCalculator();
    long start1 = System.currentTimeMillis();		//시작 시간
    long factorialResult1 = impeCal.factorial(4);
    long end1 = System.currentTimeMillis();			//종류 시간
    System.out.printf("ImpeCalculator.factorial(%d) 실행시간 = %d\n", 4, (end1-start1));
    
    RecCalculator recCal = new RecCalculator();
    long start2 = System.currentTimeMillis();		//시작 시간
    long factorialResult2 = recCal.factorial(4);
    long end2 = System.currentTimeMillis();			//종류 시간
    System.out.printf("RecCalculator.factorial(%d) 실행시간 = %d\n", 4, (end2-start2));
    ```

    - 나노 시간으로 구해야 돼서 **코드 수정이 필요한 경우** 하나하나 바꿔줘야 된다...
    - 기존 코드를 수정하지 않고, 코드 중복도 피하기 위해 **프록시 객체 사용**

- `ExeTimeCalculator`

  ```java
  public class ExeTimeCalculator implements Calculator {
  
  	private Calculator delegate;
  	
  	public ExeTimeCalculator(Calculator delegate) {
  		this.delegate = delegate;
  	}
  
  	@Override
  	public long factorial(long num) {
  		long start = System.nanoTime();
  		
  		long result = delegate.factorial(num);
  		
  		long end = System.nanoTime();
  		System.out.printf("%s.factorial(%d) 실행시간 = %d\n", delegate.getClass().getSimpleName(), num, (end-start));
  		
  		return result;
  	}
  }
  ```

  - **생성자로 다른 `Calculator` 객체를 받아 `delegate` 필드에 할당했다**
    - `delegate`를 통해 `factorial()`메서드를 수행함

  - `ExeTimeCalculator`을 사용하면 `ImpeCalculator` 및 다른 클래스의 실행시간 측정 가능

    ```java
    ImpeCalculator impeCal = new ImpeCalculator();
    ExeTimeCalculator calculator = new ExeTimeCalculator(impeCal);
    long result = calculator.factorial(4);
    ```

    - `calculator.factorial()` 실행 흐름

      > calculator.factorial(4)		[ExeTimeCalculator]
      > [실행 전 시간 구함]
      > delegate.factorial(4)			[ImpeCalculator]
      > return									[ImpeCalculator]
      > [실행 후 시간 구함 + 출력]
      > return									[ExeTimeCalculator]
      >
      > - `ExeTimeCalculator`의 `factorial()` 메서드가 `ImpeCalculator`의 `factorial()` 메서드를 실행한다

- Main 수행

  ```java
  public class MainProxy {
  
  	public static void main(String[] args) {
  		ExeTimeCalculator ttCal1 = new ExeTimeCalculator(new ImpeCalculator());
  		System.out.println(ttCal1.factorial(20));
  		
  		ExeTimeCalculator ttCal2 = new ExeTimeCalculator(new RecCalculator());
  		System.out.println(ttCal2.factorial(20));
  	}
  
  }
  /*
  ImpeCalculator.factorial(20) 실행시간 = 7100
  2432902008176640000
  RecCalculator.factorial(20) 실행시간 = 3200
  2432902008176640000
  ```

  - `ImpeCalculator`, `RecCalculator` 코드 변경 없이 실행시간 출력 가능해짐
  - 실행시간 구하는 코드의 중복 제거함 (나노초에서 밀리초 구하고 싶으면 `ExeTimeCalculator`만 수정하면 된다)

  > 가능한 이유 => `ExeTimeCalculator` 구현 방식 덕분
  >
  > - `factorial()` 기능 자체를 직접 구현하지 않고 **다른 객체에 `factorial()`의 실행을 위임한다**
  > - 계산 기능 외에 다른 부가적인 기능(실행시간 측정)을  수행한다

- **`프록시(proxy)`** : **핵심 기능의 실행은 다른 객체에 위임하고 부가적인 기능을 제공하는 객체**

  - 핵심 기능을 구현하지 않는 대신 **여러 객체에 공통으로 적용 가능한 기능을 구현**

    > `ExeTimeCalculator`은 핵심기능(팩토리얼)을 구현하지 않고 다른 클래스에 적용할 수 있는 시간 측정 기능을 구현했다
    >
    > 즉 핵심 기능보단 **공통 기능 구현에 집중함**

  > `ExeTimeCalculator`가 프록시, `ImpeCalculator` 객체가 프록시의 **대상 객체**
  >
  > > 엄밀히 말하면 프록시보단 `데코레이터(decorator)` 객체에 가깝다
  > >
  > > - 프록시는 접근 제어 관점에 초점
  > > - 데코레이터는 기능 추가와 확장에 초점
  > >
  > > 예제는 기존 기능(팩토리얼 계산)에 시간 측정 기능을 추가하고 있으니 데코레이터에 가깝다

- **공통 기능과 핵심 기능 구현을 분리하는게 AOP의 핵심이다**

### 2.1 AOP

- AOP(Aspect Oriented Programming) : 여러 객체에 공통으로 적용할 수 있는 기능을 분리해서 재사용성을 높여주는 프로그래밍 기법
  - 핵심 기능과 공통 기능의 구현을 분리하여 **핵심 기능을 구현한 코드의 수정 없이 공통 기능을 적용**할 수 있게 한다

> AOP는 '관점 지향 프로그래밍'으로 번역되나, Aspect는 구분되는 기능이나 요소를 의미하기에 '기능', '관심' 표현이 알맞다

- 핵심 기능에 공통 기능을 삽입하는 방법 세가지

  1. 컴파일 시점에 코드에 공통 기능을 삽입

     - AOP 개발 도구가 소스 코드를 컴파일 하기 전에 공통 구현 코드를 소스에 삽입하는 방식

  2. 클래스 로딩 시점에 바이트 코드에 공통 기능을 삽입

     - 클래스를 로딩할 때 바이트 코드에 공통 기능을 클래스에 삽입하는 방식

     - 1, 2 방식은 스프링 AOP에서 지원하지 않는다(AspectJ같은 AOP 전용 도구 필요)

  3. 런타임에 프록시 객체를 생성해서 공통 기능을 삽입

     - 스프링이 제공하는 AOP 방식



- 프록시 방식은 **중간에 프록시 객체를 생성**한다

  - 스프링 AOP는 프록시 객체를 자동으로 만들어 준다

    > `ExeTimeCalculator` 클래스처럼 상위 타입의 인터페이스를 상속받은 프록시 클래스를 직접 구현할 필요 없다
    >
    > 공통 기능을 구현한 클래스만 구현하면 된다

  - **실제 객체의 기능을 실행하기 전,후에 공통 기능을 호출한다**

- AOP에서 공통 기능을 **`Aspect`라고 한다**

  | 용어      | 의미                                                         |
  | --------- | ------------------------------------------------------------ |
  | Advice    | **언제 공통 관심 기능을 핵심 로직에 적용할 지를 정의하고 있다.**<br />*Ex. '메서드를 호출하기전'**(언제)**에 '트랜잭션 시작'(공통 기능) 기능을 적용한다는 것을 정의* |
  | Joinpoint | **Advice를 적용 가능한 지점을 의미. 메서드 호출, 필드 값 변경 등이 Joinpoint에 해당**<br />*스프링은 프록시를 이용해서 AOP를 구현하기에 메서드 호출에 대한 Joinpoint만 지원* |
  | Pointcut  | **Joinpoint의 부분 집합으로서, 실제 Advice가 적용되는 Joinpoint를 나타낸다**<br />*스프링에서는 정규 표현식이나 AspectJ의 문법을 이용하여 Pointcut을 정의할 수 있다* |
  | Weaving   | **Advice를 핵심 로직 코드에 적용하는 것**                    |
  | Aspect    | **여러 객체에 공통으로 적용되는 기능**<br />*트랜잭션이나 보안 등이 Aspect의 좋은 예시이다* |

### 2.2 Advice의 종류

- 스프링은 프록시를 이용해 메서드 호출 시점에 Aspect를 적용한다

- 구현 가능한 Advice 종류

  | 종류                   | 설명                                                         |
  | ---------------------- | ------------------------------------------------------------ |
  | Before Advice          | 대상 객체의 메서드 호출 전에 공통 기능을 실행                |
  | After Returning Advice | 대상 객체의 메서드가 익셉션 없이 실행된 이후에 공통 기능을 실행 |
  | After Throwing Advice  | 대상 객체의 메서드를 실행하는 도중 익셉션이 발생한 경우에 공통 기능을 실행 |
  | After Advice           | 익셉션 발생 여부에 상관없이 대상 객체의 메서드 실행 후 공통 기능 실행<br />(try-catch-finally의 finally 블록과 비슷) |
  | Around Advice          | 대상 객체의 메서드 실행 전, 후 또는 익셉션 발생 시점에 공통 기능을 실행 |

  - 널리 사용되는건 Around Advice이다

    - 다양한 시점에 원하는 기능을 넣을 수 있기 때문

    > 캐시 기능, 성능 모니터링 기능과 같은 Aspect 구현시 Around Advice 사용



## 3. 스프링 AOP 구현

- 스프링 AOP를 이용해 공통 기능을 구현하고 적용하는 방법
  1. Aspect로 사용할 클래스에 `@Aspect` 애노테이션을 붙인다
  2. `@Pointcut` 애노테이션으로 공통 기능을 적용한 `Pointcut`을 정의한다
  3. 공통 기능을 구현한 메서드에 `@Around` 애노테이션을 적용한다

### 3.1 @Aspect, @Pointcut, @Around를 이용한 AOP 구현

- 공통 기능을 제공하는 `Aspect` 구현 클래스를 만들고 자바 설정을 이용해서 `Aspect`를 어디에 적용할지 설정하면 된다
  - `Aspect`는 `@Aspect` 애노테이션을 이용해 구현한다
  - **프록시(부가 기능 제공 객체)는 스프링 프레임워크가 알아서 만들어준다**

- 실행 시간을 측정하는 `Aspect` 구현

  ```java
  @Aspect
  public class ExeTimeAspect {
  	
  	@Pointcut("execution(public * chap07..*(..))")		// 공통 기능 적용할 대상 설정
  	private void publicTarget() {}
  	
  	@Around("publicTarget()")
  	public Object measure(ProceedingJoinPoint joinPoint) throws Throwable{
  		
  		long start = System.nanoTime();
  		
  		try {
  			Object result = joinPoint.proceed();		//객체의 메서드 호출
  			return result;
  		} finally {
  			long finish = System.nanoTime();
  			Signature sig = joinPoint.getSignature();
  			System.out.printf("%s.%s(%s) 실행 시간 : %d ns\n",
  					joinPoint.getTarget().getClass().getSimpleName(),
  					sig.getName(), Arrays.toString(joinPoint.getArgs()),
  					(finish-start));
  		}
  	}
  }
  ```

  - `@Aspect` 애노테이션을 적용한 클래스는 `Advice`와 `Pointcut`을 함께 제공한다

  - `@Pointcut`은 공통 기능을 제공할 대상을 설정한다

    - `execution` 명시자는 뒤에서 설명

    - 코드 의미 : chap07 패키지와 그 하위 패키지에 위치한 타입의 public 메서드를 Pointcut으로 설정한다

  - `@Around`는 `Around Advice`를 설정한다

    - `publicTarget()`은 `publicTarget()` 메서드에 정의한 `Pointcut`에 공통 기능을 적용한다는 의미
    - `publicTarget()`은 `@Pointcut`에 의해 `chap07 패키지와 그 하위 패키지에 위치한 타입의 public 메서드를 Pointcut으로 설정한다` 이므로 chap07패키지나 그 하위 패키지에 속한 빈 객체의 public 메서드에 `@Around`가 붙은 `measure()` 메서드를 적용한다

  - `measure()` 메서드의`ProceedingJoinPoint` 타입 파라미터는 프록시 대상 객체의 메서드를 호출할 때 사용

    - `proceed()` 메서드를 통해 실제 대상 객체의 메서드를 호출한다
    - 즉 `proceed()`메서드 이전과 이후에 공통 기능을 위한 코드를 위치하면 된다

  - `ProceedingJoinPoint`의 메서드

    - `getSignature()` : 메서드의 시그너처

      > 자바에서 메서드 이름과 파라미터를 합쳐서 메서드 시그너처라고 한다

    - `getTarget()` : 대상 객체

    - `getArgs()` : 인자(파라미터) 목록



> @Enable 류 애노테이션
>
> 스프링에서 제공하는 Enable로 시작하는 애노테이션은
> 관련 기능을 적용하는데 필요한 다양한 스프링 설정을 대신 처리한다
>
> Ex. @EnableAspectJAutoProxy는 프록시 생성과 관련된 AnnotationAwareAspectJAutoProxyCreator 객체를 빈으로 등록한다.

- 스프링 설정 클래스 작성

  - `@Aspect`을 붙인 클래스를 **공통 기능으로 적용하려면 `@EnableAspectJAutoProxy`를 설정 클래스에 붙여야 된다**
    - 스프링이 `@Aspect`가 붙은 빈 객체를 찾아 `@Pointcut` 설정과 `@Around` 설정을 사용한다

  ```java
  @Configuration
  @EnableAspectJAutoProxy		//붙임
  public class AppCtx {
  
  	@Bean
  	public ExeTimeAspect exeTimeAspect() {		//위 코드의 @Aspect 붙은 클래스
  		return new ExeTimeAspect();
  	}
  	
  	@Bean
  	public Calculator calculator() {		//chap07 패키지에 속한 타입 ==> ExeTimeAspect에 있는 공통 기능이 적용된다
  		return new RecCalculator();
  	}
  }
  ```

  ```java
  @Aspect
  public class ExeTimeAspect {
  	
  	@Pointcut("execution(public * chap07..*(..))")
  	private void publicTarget() {}
  	
  	@Around("publicTarget()")
  	public Object measure(ProceedingJoinPoint joinPoint) throws Throwable{
  		...
  	}
  }
  ```

  - `@Around`는 `Pointcut`으로 `publicTarget()` 메서드를 설정했다
    - `publicTarget()` 메서드의 `@Pointcut`은 **chap07 패키지나 그 하위 패키지에 속한 빈 객체의 public 메서드**를 설정한다
    - `Calculator` 타입은 chap07 패키지에 속하므로 **`calculator` 빈에 공통 기능인 `measure()`를 적용**한다

- 실행

  ```java
  public class MainAspect {
  
  	public static void main(String[] args) {
  		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppCtx.class);
  		
  		Calculator cal = ctx.getBean("calculator", Calculator.class);
  		
  		long fiveFact = cal.factorial(5);
  		System.out.println("cal.factorial(5) = "+ fiveFact);
  		System.out.println(cal.getClass().getName());
  		
  		ctx.close();
  	}
  }
  /*
  RecCalculator.factorial([5]) 실행 시간 : 40100 ns
  cal.factorial(5) = 120
  ...
  com.sun.proxy.$Proxy19
  ```

  - 첫 줄은 `ExeTimeAspect` 클래스의 `measure()` 메서드가 출력된 것

  - `cal`은 `Calculator` 타입이 `RecCalculator` 클래스가 아니고 `$Proxy17`이다

    - 스프링이 생성한 프록시 타입이다

  - `cal.factorial(5)` 실행과정

    > `[MainAspect]` **--(1.factorial(5))-->** `[$Proxy17]` **--(2.measure())-->** `[ExeTimeAspect]` **--(3.proceed())-->** `[ProceedingJoinPoint]` **--(4.factorial(5))-->** `[RecCalcualtor]`
    >
    > 그 후 다시 역순으로 return 연결 

  - AOP를 적용하지 않았으면 `getBean()`으로 리턴 받은 객체가 `RecCalculator` 이었을거다

    - `AppCtx`의 `exeTimeAspect()` 메서드를 주석처리 한 결과

      ```java
      /*
      cal.factorial(5) = 120
      chap07.RecCalculator
      ```

### 3.2 ProceedingJoinPoint의 메서드

- `Around Advice`에서 사용할 공통 기능 메서드는 대부분 파라미터로 전달받은 `ProceedingJoinPoint`의 `proceed()` 메서드만 호출하면 된다

  ```java
  @Aspect
  public class ExeTimeAspect {
  	
  	...
  	@Around("publicTarget()")
  	public Object measure(ProceedingJoinPoint joinPoint) throws Throwable{
  		
  		long start = System.nanoTime();
  		
  		try {
  			Object result = joinPoint.proceed();		//proceed 메서드 호출
  			return result;
  		} finally {
  			...
  		}
  	}
  	
  }
  ```

- **호출되는 객체 대상에 대한 정보에 접근할 수 있도록** `ProceedingJoinPoint` 인터페이스가 제공하는 메서드

  - `Signature getSignature()` : 호출되는 메서드에 대한 정보를 구한다

    - `org.aspectj.lang.Signature` 인터페이스 제공 메서드 (호출되는 메서드의 정보를 제공한다)

      > - `String getName()` : 호출되는 메서드의 이름을 구한다
      > - `String toLongString()` : 호출되는 메서드를 완전하게 표현한 문장을 구한다(메서드의 리턴 타입, 파라미터 타입 모두 표시)
      > - `String toShortString()` : 호출되는 메서드를 축약해서 표현한 문장을 구한다(기본 구현은 메서드의 이름만을 구한다)

  - `Object getTarget()` : 대상 객체를 구한다

  - `Object[] getArgs()` : 파라미터 목록을 구한다



## 4. 프록시 생성 방식

- `MainAspect` 클래스 코드 변경

  ```java
  // 수정 전
  Calculator cal = ctx.getBean("calculator", Calculator.class);
  
  // 수정 후
  Calculator cal = ctx.getBean("calculator", RecCalculator.class);
  ```

- `AppCtx`

  ```java
  @Bean
  public Calculator calculator() {
  	return new RecCalculator();
  }
  ```

  - `RecCalculator()` 타입을 사용해서 문제 없어보이는데... 실행하면 익셉션 발생

    - `getBean()` 메서드에 사용한 타입이 `RecCalculator`인 것에 반해 **실제 타입은 `$Proxy17`**이라는 메시지 나옴

    - **`$Proxy17`은 스프링이 런타임에 생성한 프록시 객체의 클래스 이름**이다

      - **`$Proxy17` 클래스는 `RecCalculator` 클래스가 상속받은 `Calculator` 인터페이스를 상속받는다**

        > [$Proxy17] - - - - - - -> [<interface> Calculator]
        > [RecCalculator] - - - ->

- **스프링은 프록시 객체를 생성할 때 실제 생성할 빈 객체가 인터페이스를 상속하면, 인터페이스를 이용해서 프록시를 생성한다**

  > 즉 예시에서도 `RecCalculator` 클래스가 `Calculator` 인터페이스를 상속하므로
  > `Calculator` 인터페이스를 상속받은 프록시 객체를 생성했다
  >
  > 빈 실제 타입이 `RecCalculator`라고 해도 `calculator` 이름에 해당하는 빈 객체 타입은 `Calculator` 인터페이스를 상속받은 프록시 타입이 된다.

  ```java
  //AOP 적용시 RecCalculator가 상속받은 Calculator 인터페이스를 이용해서 프록시 생성
  @Bean
  public Calculator calculator() {
  	return new RecCalculator();
  }
  
  // 익셉션 발생!!
  Calculator cal = ctx.getBean("calculator", RecCalculator.class);
  ```

- 빈 객체가 인터페이스가 아닌 클래스를 이용해서 프록시를 생성하도록 설정 방법

  ```java
  @Configuration
  @EnableAspectJAutoProxy(proxyTargetClass = true)		// 수정
  public class AppCtx {
  	...
  }
  ```

  - `@EnableAspectJAutoProxy`의 `proxyTargetClass` 속성을 true로 지정하면 된다

    - `getBean()` 메서드에 실제 클래스를 이용해서 빈 객체를 구할 수 있게 된다

      ```java
      Calculator cal = ctx.getBean("calculator", RecCalculator.class);
      ```

### 4.1 execution 명시자 표현식

- `Aspect`를 적용할 위치를 지정할 때 `Pointcut` 설정에 `execution` 명시자를 사용했다

  ```java
  @Pointcut("execution(public * chap07..*(..))")
  private void publicTarget() {}
  ```

  - **`execution` 명시자는 `Advice`를 적용할 메서드를 지정할 때 사용**

  - 기본 형식

    > execution(수식어패턴? 리턴타입패턴 클래스이름패턴?메서드이름패턴(파라미터패턴))
    >
    > - '수식어패턴'은 생략 가능 (ex. public, protected)
    >   - 스프링 AOP는 public 메서드에만 적용가능
    > - '리턴타입패턴'은 리턴 타입 명시
    > - '클래스이름패턴', '메서드이름패턴'은 클래스 이름 및 메서드 이름을 패턴으로 명시
    > - '파라미터패턴'은 매칭될 파라미터에 대해서 명시

    - 각 패턴은 '*'을 이용하여 모든 값을 표현 가능
    - '..'을 이용해 0개 이상이라는 의미 표현 가능

- 예시

  | 예                                              | 설명                                                         |
  | ----------------------------------------------- | ------------------------------------------------------------ |
  | execution(public void set*(..))                 | 리턴 타입이 void. 메서드 이름이 set으로 시작. 파라미터가 0개 이상인 메서드 호출. |
  | execution(* chap07.\*.*())                      | chap07 패키지의 타입에 속한 파라미터가 없는 모든 메서드 호출 |
  | execution(* chap07..*.\*(..))                   | chap07 패키지 및 하위 패키지에 있는, 파라미터가 0개 이상인 메서드 호출<br />*패키지 부분에 '..'을 사용하여 하위 패키지까지 표현* |
  | execution(Long chap07.Calculator.factorial(..)) | 리턴 타입이 Long인 Calculator 타입의 factorial() 메서드 호출 |
  | execution(* get\*(*))                           | 이름이 get으로 시작하고 파라미터가 한 개인 메서드 호출       |
  | execution(* get\*(*, *))                        | 이름이 get으로 시작하고 파라미터가 두 개인 메서드 호출       |
  | execution(* read*(Integer, ..))                 | 메서드 이름이 read로 시작하고, 첫 번째 파라미터 타입이 Integer이며, 한 개 이상의 파라미터를 갖는 메서드 호출 |

### 4.2 Advice 적용 순서

- 한 `Pointcut`에 여러 `Advice`를 적용할 수 있다

  ```java
  @Aspect
  public class CacheAspect {
  	private Map<Long, Object> cache = new HashMap<>();
  	
  	@Pointcut("execution(public * chap07..*(long))")
  	public void cacheTarget() {}
  	
  	@Around("cacheTarget()")
  	public Object execute(ProceedingJoinPoint joinPoint) throws Throwable{
  		Long num = (Long) joinPoint.getArgs()[0];
  		
  		if(cache.containsKey(num)) {
  			System.out.printf("CacheAspect: Cache에서 구한[%d]\n", num);
  			return cache.get(num);
  		}
  		
  		Object result = joinPoint.proceed();
  		cache.put(num, result);
  		System.out.printf("CacheAspect: Cache에 추가[%d]\n", num);
  		return result;
  	}
  }
  ```

  - 캐시를 구현한 간단한 공통 기능
    - `num` 키 값이 `cache`에 존재하면 키에 대한 값을 리턴
    - 없으면 프록시 대상 객체를 실행 후 결과를 cache에 추가 + 결과 리턴
  - `@Pointcut` 설정의 `execution`은 `Calculator`의 `factorial(long)` 메서드에 적용된다

- 새로운 `Aspect`를 구현했으므로 설정 클래스에 두 개의 `Aspect` 생김

  - **두 `Aspect`에서 설정한 `Pointcut`은 모두 `Calculator` 타입의 `factorial` 메서드에 적용된다**

  ```java
  @Configuration
  @EnableAspectJAutoProxy
  public class AppCtxWithCache {
  
  	@Bean
  	public CacheAspect cacheAspect() {
  		return new CacheAspect();
  	}
  	
  	@Bean
  	public ExeTimeAspect exeTimeAspect() {
  		return new ExeTimeAspect();
  	}
  	
  	@Bean
  	public Calculator calculator() {
  		return new RecCalculator();
  	}
  }
  ```

- 실행

  ```java
  public class MainAspectWithCache {
  
  	public static void main(String[] args) {
  		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppCtxWithCache.class);
  		
  		Calculator cal = ctx.getBean("calculator", Calculator.class);
  		
  		cal.factorial(7);	// CacheAspect 실행 -> ExeTimeAspect 실행 -> 대상 객체 실행
  		cal.factorial(7);
  		cal.factorial(5);
  		cal.factorial(5);
  		
  		ctx.close();
  	}
  
  }
  /*
  RecCalculator.factorial([7]) 실행 시간 : 23700 ns
  CacheAspect: Cache에 추가[7]
  CacheAspect: Cache에서 구한[7]
  RecCalculator.factorial([5]) 실행 시간 : 5800 ns
  CacheAspect: Cache에 추가[5]
  CacheAspect: Cache에서 구한[5]
  ```

  - `Advice`를 다음과 같은 순서로 적용해서

    > [CacheAspect] ==> [ExeTimeAspect] ==> [실제 대상 객체]

    - `factorial(7)` 실행의 첫 번째 결과는 `ExeTimeAspect`와 `CacheAspect`가 모두 적용됐다

    - 두 번째는 `CacheAspect`만 적용됐다

  - `calculator` 빈은 실제로는 `CacheAspect` 프록시 객체다

    - `CacheAspect` 프록시 객체의 대상 객체는 `ExeTimeAspect`의 프록시 객체이고
    - `ExeTimeAspect` 프록시의 대상 객체가 실제 대상 객체다

  > `CacheAspect`는 cache 맵에 데이터가 없으면 `joinPoint.proceed()`로 대상을 실행한다
  > 그 대상이 `ExeTimeAspect`라 `measure()`가 실행된다. 실제 대상 객체를 실행하고 실행 시간 출력한다.
  > `ExeTimeAspect` 실행이 끝나면 `CacheAspect`는 cache 맵에 데이터 넣고 메시지 출력한다>
  >
  > 두번째 실행은
  > cache맵에 데이터 추가하고 `joinPoint.proceed()`를 실행하지 않아서 `ExeTimeAspect`나 실제 객체가 실행되지 않는다

- **`Aspect`가 먼저 적용될지는** 스프링 프레임워크나 자바 버전에 따라 달라진다

  - **적용 순서가 중요하면 직접 순서를 지정해야 된다**
  - **`@Order` 애노테이션을 이용한다**
    - `@Aspect`와 `@Order`을 함께 사용해 적용 순서 결정

  ```java
  @Aspect
  @Order(1)
  public class ExeTimeAspect{...}
  
  @Aspect
  @Order(2)
  public class CacheAspect{...}
  ```

  - `ExeTimeAspect` => `CacheAspect` => [실제 대상 객체] 순으로 적용된다

  - 실행 결과

    ```java
    /*
    CacheAspect: Cache에 추가[7]
    RecCalculator.factorial([7]) 실행 시간 : 532900 ns
    CacheAspect: Cache에서 구한[7]
    RecCalculator.factorial([7]) 실행 시간 : 145600 ns
    CacheAspect: Cache에 추가[5]
    RecCalculator.factorial([5]) 실행 시간 : 151300 ns
    CacheAspect: Cache에서 구한[5]
    RecCalculator.factorial([5]) 실행 시간 : 122800 ns
    ```

### 4.3 @Around의 Pointcut 설정과 @Pointcut 재사용

- `@Pointcut`이 아닌 `@Around` 애노테이션에 `execution` 명시자를 직접 지정할 수 있다

  ```java
  @Aspect
  public class CacheAspect{
      
      @Around("execution(public * chap07..*(..))")
      public Object execute(ProceedingJoinPoint joinPoint) throws Throwable{
          ...
      }
  }
  ```

- 같은`Pointcut`을 여러`Advice`가 함께 사용한다면 공통 `Pointcut`을 재사용 가능하다

  ```java
  @Aspect
  public class ExeTimeAspect {
  	
  	@Pointcut("execution(public * chap07..*(..))")		// 공통 기능 적용할 대상 설정
  	private void publicTarget() {}
  	
  	@Around("publicTarget()")
  	public Object measure(ProceedingJoinPoint joinPoint) throws Throwable{
  		...
  	}
  }
  ```

  - `@Around`는 **`publicTarget()` 메서드에 설정한 `Pointcut`을 사용한다**

    - `publicTarget()`메서드가 private이라 같은 클래스에 있는 `@Around`에서만 해당 설정을 사용 가능하다
    - **다른 클래스에 위치한 `@Around`**에서 사용하려면 public으로 바꾸면 된다

    ```java
    @Pointcut("execution(public * chap07..*(..))")
    public void publicTarget() {}		//public으로 변경
    ```

  - 해당 `Pointcut`의 완전한 클래스 이름을 포함한 메서드 이름을 `@Around`에서 사용하면, **다른 클래스에서 이용 가능하다**

    ```java
    @Aspect
    public class CacheAspect {
    
    	@Around("aspect.ExeTimeAspect.publicTarget()")		//변경
        //@Around("ExeTimeAspect.publicTarget()")	// 같은 패키지에 속할 경우
    	public Object execute(ProceedingJoinPoint joinPoint) throws Throwable{
    		...
    	}
    }
    ```

- **별도 클래스로 분리해서 Pointcut 재사용하기**

  ```java
  public class CommonPointcut{
      @Pointcut("execution(public * chap07..*(..))")
      public void commonTarget(){}					//재사용
  }
  ```

  ```java
  @Aspect
  public class ExeTimeAspect {
  	
  	@Around("CommonPointcut.commonTarget()")	// Pointcut 재사용
  	public Object measure(ProceedingJoinPoint joinPoint) throws Throwable{
  		...
  	}
  }
  ```

  ```java
  @Aspect
  public class CacheAspect {
  	...
  	@Around("CommonPointcut.commonTarget()")	// Pointcut 재사용
  	public Object execute(ProceedingJoinPoint joinPoint) throws Throwable{
  		...
  	}
  }
  ```

  