# 16장 JSON 응답과 요청 처리

> - JSON 개요
> - @RestController를 이용한 JSON 응답 처리
> - @RequestBody를 이용한 JSON 요청 처리
> - ResponseEntity

- 스프링 MVC에서 JSON 응답과 요청을 처리하는 방법을 알아본다
  - 웹 페이지에서 Ajax를 이용해 서버 API를 호출하는 사이트가 많다
  - 요청에 대한 응답으로 HTML 대신 JSON이나 XML을 사용한다
  - 요청에도 쿼리 문자열 대신 JSON, XML을 데이터로 보내기도 한다

## 1. JSON 개요

- JSON(JavaScript Object Notation) : 간단한 형식을 갖는 문자열로 데이터 교환에 주로 사용

- 예시

  ```json
  {
      "name": "몰리",
      "age": 10,
      "related": ["보리", "에이스"],
      "food": [
          {
              "name":"닭가슴살",
              "cal":50
          },
          {
              "name":"오리목뼈",
              "cal":60
          }
      ]
  }
  ```

- 규칙

  - **중괄호를 사용해 객체를 표현**
    - 객체는 (이름, 값) 쌍을 갖는다
    - 이름과 값은 콜론`:`으로 구분
    - 값에는 **문자열, 숫자, 불리언, null, 배열, 다른 객체**가 올 수 있다
  - 문자열은 큰따옴표`"` 혹은 작은따옴표 `'` 사에이 위치한 값이다
    - `\"(큰따옴표)`, `\n(뉴라인)`, `\r(캐리지 리턴)`, `\t(탭)` 같이 역슬래시 이용해 특수 문자 표시 가능
  - 숫자는 10진수 표기법, 지수 표기법(ex. 1.07e2)를 따른다, 불리언은 true와 false 존재
  - 배열은 대괄호`[]`로 표현
    - 콤마로 값 목록을 구분한다



## 2. Jackson 의존 설정

- `Jackson`은 자바 객체와 JSON 형식 문자열 간 변환을 처리하는 라이브러리이다

  - 객체의 프로퍼티의 이름과 값을 JSON의 (이름, 값) 쌍으로 사용한다

  > public class Person{
  >
  > ​	private String name;
  >
  > ​	private int age;
  >
  > ​	...get/set 메서드
  >
  > }
  >
  > =====>
  >
  > {
  >
  > ​	"name": "이름",
  >
  > ​	"age": 10
  >
  > }

- 스프링 MVC에서 사용하기 위해 `pom.xml`파일에 의존 추가

  ```xml
  		<!-- java8 date/time -->
  		<dependency>
  			<groupId>com.fasterxml.jackson.datatype</groupId>
  			<artifactId>jackson-datatype-jsr310</artifactId>
  			<version>2.9.4</version>
  		</dependency>
  		<!-- java8 Optional, etc -->
  		<dependency>
  			<groupId>com.fasterxml.jackson.datatype</groupId>
  			<artifactId>jackson-datatype-jdk8</artifactId>
  			<version>2.9.4</version>
  		</dependency>



## 3. @RestController로 JSON 형식 응답

- 스프링 MVC에서 JSON 형식으로 데이터를 응답하기 위해 `@Controller` 대신 `@RestController`를 사용하면 된다

  ```java
  @RestController
  public class RestMemberController {
  	private MemberDao memberDao;
  	private MemberRegisterService memberRegisterService;
  	
  	@GetMapping("/api/members")
  	public List<Member> members(){
  		return memberDao.selectAll();
  	}
  	
  	@GetMapping("/api/members/{id}")
  	public Member member(@PathVariable Long id, HttpServletResponse response) throws IOException{
  		Member member = memberDao.selectById(id);
  		
  		if(member==null) {
  			response.sendError(HttpServletResponse.SC_NOT_FOUND);
  			return null;
  		}
  		
  		return member;
  	}
  
  	public void setMemberDao(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  
  	public void setMemberRegisterService(MemberRegisterService memberRegisterService) {
  		this.memberRegisterService = memberRegisterService;
  	}
  	
  }
  ```

  - `@Controller` 대신 `@RestController` 사용
  - 요청 매핑 애노테이션 적용 메서드의 **리턴 타입으로 일반 객체 사용**

- `@RestController`가 붙으면 요청 매핑이 붙은 메서드가 **리턴한 객체를 알맞은 형식으로 변환해서 응답 데이터로 전송**한다

  - 이때 Jackson을 사용하면 JSON 형식의 문자열로 변환해서 응답한다

    > 위 코드의 경우 List<Member> 리턴 타입은 List 객체를 JSON 형식으로 변환해서 응답한다

- 실행결과
  ![image-20220129104717085](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220129104717085.png)

> #### @ResponseBody 애노테이션
>
> - `@RestController` 추가 전에는 `@Controller`과 `@ResponseBody`를 사용했다
>
>   ```java
>   @Controller
>   public class RestMemberController {
>   	private MemberDao memberDao;
>   	private MemberRegisterService memberRegisterService;
>   	
>   	@RequestMapping(path="/api/members", method=RequestMethod.GET)
>       @ResponseBody
>   	public List<Member> members(){
>   		return memberDao.selectAll();
>   	}
>   	...
>   	
>   }
>   ```
>
> - 스프링 4 버전부터 `@RestController`을 더 많이 사용한다

### 3.1 @JsonIgnore를 이용한 제외 처리

- 응답 결과에 password같은 민감한 데이터를 응답 결과에서 제외해야 된다

- **Jackson이 제공하는 `@JsonIgnore`을 사용하면 제외 처리 가능하다**

  - JSON 응답에 포함하지 않을 대상에 `@JsonIgnore`을 붙이면 된다

    ```java
    public class Member {
    	private Long id;
    	private String email;
    	@JsonIgnore
    	private String password;
    	private String name;
    	private LocalDateTime registerDateTime;
    	...
    }
    ```

    - 실행결과 : 응답에 password가 제외되었다

### 3.2 날짜 형식 변환 처리: @JsonFormat 사용

- `registerDateTime`값이 [2022,1,16,23,49,31]으로 응답왔다

  - `Member` 클래스의 `registerDateTime`의 타입은 `LocalDateTime`인데 JSON값은 배열로 바뀌었다

  - `java.util.Date` 타입이면 유닉스 타임 스탬프로 날짜 값을 표현한다

    > 유닉스 타입 스탬프
    >
    > 1970년 1월 1일 이후 흘러간 시간을 말한다. 보통 초 단위로 표현한다

- **Jackson에서 날짜나 시간 값을 특정 형식으로 표현하려면 `@JsonFormat`을 사용한다**

  - `ISO-8601` 형식으로 변환하고 싶으면 `shape` 속성값으로 `Shape.STRING`을 갖는 `@JsonFormat`사용

    ```java
    public class Member {
    	private Long id;
    	...
        @JsonFormat(shape = Shape.STRING)		//ISO-8601 형식으로 변환
    	private LocalDateTime registerDateTime;
    	...
    }
    ```

    - 결과

      ```json
      {
          "id": 1,
          ...
      	"registerDateTIme": "2022-01-16T23:49:31"
      }
      ```

  - **원하는 형식으로 출력하려면 `pattern` 속성을 지원한다**

    ```java
    public class Member {
    	private Long id;
    	...
        @JsonFormat(pattern = "yyyyMMddHHmmss")
    	private LocalDateTime registerDateTime;
    	...
    }
    ```

    - 결과

      ```json
      {
          "id": 1,
          ...
      	"registerDateTIme": "20220116234931"
      }
      ```

### 3.3 날짜 형식 변환 처리 : 기본 적용 설정

- 날짜 형식 모든 대상에 `@JsonFormat` 붙이기는 힘들다

  - **Jackson의 변환 규칙을 모든 날짜 타입에 적용하기 위해서는 스프링 MVC 설정을 변경해야 된다**

- 스프링 MVC는 자바 객체를 HTTP 응답으로 변환할 때 `HttpMessageConverter`을 사용한다

  - Jackson이 자바 객체를 JSON으로 변환할때 사용하는 `MappingJackson2HttpMessageConverter`를 새롭게 등록해 모든 날짜 형식에 동일한 규칙을 적용 가능하다

- 모든 날짜 타입을 `ISO-8601` 형식으로 변환 예시

  ```java
  @Configuration
  @EnableWebMvc
  public class MvcConfig implements WebMvcConfigurer {
  	...
  	@Override
  	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
  		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder
              .json()
              .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
              .build();
          
  		converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper));
  	}
  	
  }
  ```

  - `extendMessageConverters()`는 `HttpMessageConverter`를 추가로 설정할 때 사용
    - 등록된 `HttpMessageConverter` 목록을 파라미터로 받는다
  - 미리 등록된 `HttpMessageConverter`에는 Jackson을 이용하는 것도 포함되어 있다
    - 때문에 새롭게 생성한 `HttpMessageConverter`은 목록 제일 앞에 위치시켜야 된다
    - `.add(0, ...)`에서 0번 인덱스에 추가했다
  - JSON으로 변환할 때 사용할 `ObjectMapper`을 생성한다
    - `Jackson2ObjectMapperBuilder`는 `ObjectMapper`을 쉽게 생성할 수 있게 해주는 스프링 제공 클래스
    - Jackson이 날짜 형식을 출력할 때 **유닉스 타임 스탬프로 출력하는 기능을 비활성화 했다**
      - 이 기능 비활성화하면 날짜 타입 값을 `ISO-8601`형식으로 출력한다
  - 새로 생성한 `ObjectMapper`를 사용하는 `MappingJackson2HttpMessageConverter` 객체를 converters의 첫 번째 항목으로 등록하면 끝



- 모든 `java.util.Date` 타입의 값을 원하는 형식으로 출력하려면 `Jackson2ObjectMapperBuilder#simpleDateFormat()` 메서드를 이용해서 패턴을 지정한다

  ```java
  @Configuration
  @EnableWebMvc
  public class MvcConfig implements WebMvcConfigurer {
  	...
  	@Override
  	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
  		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder
              .json()
              .simpleDateFormat("yyyyMMddHHmmss")		// Date를 위한 변환 패턴
              .build();
          
  		converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper));
  	}
  	
  }
  ```

  - 그런데 `LocalDateTime` 타입 변환에는 해당 패턴을 사용하지 않는다. 대신 `ISO-8601`형식으로 변환된다

- **모든 `LocalDateTime` 타입에 대해 원하는 패턴을 설정**하고 싶으면

  - `serializerByType()` 메서드를 이용해 `LocalTimeDate` 타입에 대한 `JsonSerializer`를 직접 설정하면 된다

  ```java
  @Configuration
  @EnableWebMvc
  public class MvcConfig implements WebMvcConfigurer {
  	...
  	@Override
  	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
          
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
          
  		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder
              .json()
              .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(formatter))		//추가
              .build();
          
  		converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper));
  	}
  }
  ```

### 

## 4. @RequestBody로 JSON 요청 처리

- 반대로 JSON 형식의 요청 데이터를 자바 객체로 변환하는 기능

  - POST 방식이나 PUT 방식을 사용하면 쿼리 문자열(name=이름&age=17)이 아닌 JSON 형식의 데이터를 요청 데이터로 전송 가능

    ```json
    {"name":"이름", "age":17}
    ```

- **`@RequestBody`로 JSON 데이터를 커맨드 객체로 전달받을 수 있다**

  ```java
  @RestController
  public class RestMemberController {
  	private MemberDao memberDao;
  	private MemberRegisterService registerService;
  	...
  	
  	@PostMapping("/api/members")
  	public void newMember(@RequestBody @Valid RegisterRequest regReq, HttpServletResponse response) throws IOException{
  		try {
  			Long newMemberId = registerService.regist(regReq);
  			response.setHeader("Location", "/api/members/" + newMemberId);
  			response.setStatus(HttpServletResponse.SC_CREATED);
  		}catch(DuplicateMemberException dupEx) {
  			response.sendError(HttpServletResponse.SC_CONFLICT);
  		}
  	}
  	
  }
  ```

  - **`@RequestBody`를 커맨드 객체에 붙이면 JSON 형식의 문자열을 해당 자바 객체로 변환한다**
  - 회원 가입 정상처리하면 **201(CREATED)를 전송**한다
  - "Location" 헤더를 응답에 추가함
  - 중복된 `email`이면 **409(CONFLICT)**를 리턴한다

- 실행 결과
  ![image-20220129135522856](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220129135522856.png)
  - 응답상태 201이다
  - "Location" 헤더가 응답에 포함되어 있다
  - 이메일 중복되면 응답상태 코드 409가 돌아온다

### 4.1 JSON 데이터의 날짜 형식 다루기

- JSON 형식의 데이터를 날짜 형식으로 변환하는 방법

  - 별도 설정 없으면 다음 패턴(시간대가 없는 JSR-8601 형식)의 문자열을 LocalDateTime과 Date로 변환한다

    > yyyy-MM-ddTHH:mm:ss

- 특정 패턴을 가진 문자열을 LocalDateTime이나 Date 타입으로 변환하려면 **`@JsonFormat`의 `pattern` 속성을 사용해서 패턴 지정한다**

  ```java
  @JsonFormat(pattern = "yyyyMMddHHmmss")
  private LocalDateTime birthDateTime;
  
  @JsonFormat(pattern = "yyyyMMdd HHmmss")
  private Date birthDate;
  ```

- 특정 속성이 아닌 해당 타입을 갖는 모든 속성에 적용하려면 스프링 MVC 설정을 추가해야 된다

  ```java
  @Configuration
  @EnableWebMvc
  public class MvcConfig implements WebMvcConfigurer {
  
  	...
  	@Override
  	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
  		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder
  	            .json()
  				.featuresToEnable(SerializationFeature.INDENT_OUTPUT)
              	.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(formatter))
              	.simpleDateFormat("yyyyMMdd HHmmss")
              	.build();
  		
  		converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper));
  	}
  	
  }
  ```

  - `deserializerByType()`은 JSON 데이터를 LocalDateTime 타입으로 변환할 때 사용할 패턴을 지정한다
  - `simpleDateFormat()`은 Date 타입으로 변환할 때 사용할 패턴 지정
    - 이는 Date 타입을 JSON 데이터로 변환할 때도 사용된다는 것이다

### 4.2 요청 객체 검증하기

- `newMember()` 메서드의 `regReq` 파라미터에 `@Valid` 애노테이션이 붙어있다

  ```java
  	@PostMapping("/api/members")
  	public void newMember(@RequestBody @Valid RegisterRequest regReq, HttpServletResponse response) throws IOException{
  		...
  	}
  ```

- **JSON 형식으로 전송한 데이터를 변환한 객체도 `@Valid`나 별도 `Validator`을 이용해 검증 가능하다**

  - `@Valid` 사용하는 경우 검증에 실패하면 **400(Bad Request)** 상태 코드를 응답한다

- `Validator`을 사용하는 경우 직접 상태 코드를 처리해야 된다

  ```java
  @PostMapping("/api/members")
  public void newMember(@RequestBody RegisterRequest regReq, Errors erros, HttpServletResponse response) throws IOException{
      try{
          new RegisterRequestValidator().validate(regReq, errors);
          if(errors.hasErrors()){
              response.sendError(HttpServletResponse.SC_BAD_REQUEST);
              return;
          }
          ...
      }catch(DuplicateMemberException dupEx){
          response.sendError(HttpServletResponse.SC_CONFLICT);
      }
  }
  ```

  

## 5. ResponseEntity로 객체 리턴하고 응답 코드 지정하기

- 이전에는 상태 코드를 지정하기 위해 `HttpServletResponse`의 `setStatus()`와 `sendError()`를 사용했다

  ```java
  	@GetMapping("/api/members/{id}")
  	public Member member(@PathVariable Long id, HttpServletResponse response) throws IOException{
  		Member member = memberDao.selectById(id);
  		
  		if(member==null) {
  			response.sendError(HttpServletResponse.SC_NOT_FOUND);		//404 응답
  			return null;
  		}
  		
  		return member;
  	}
  ```

  - `404` 응답을 하면 **JSON 형식이 아닌 HTML을 응답 결과로 제공한다**
    - `Member`가 존재하면 해당 객체를 **JSON 형식으로 리턴**하고
    - 없으면 **HTML 결과**를 응답한다

- **JSON 응답과 HTML 응답을 모두 처리하는 것은 API 호출 프로그램에게 부담스럽다**

  - **404, 504**같이 처리에 실패한 경우 HTML 대신에 JSON 형식으로 응답해야 일관된 방법으로 처리 가능하다

### 5.1 ResponseEntity를 이용한 응답 데이터 처리

- **정상, 비정상 경우 모두 JSON 응답을 전송하는 방법은 `ResponseEntity`를 사용**하는 것이다

- 에러 상황일 때 응답으로 사용할 `ErrorResponse` 클래스 작성

  ```java
  public class ErrorResponse {
  	private String message;
  	
  	public ErrorResponse(String message) {
  		this.message = message;
  	}
  	
  	public String getMessage() {
  		return message;
  	}
  }
  ```

- `ResponseEntity`를 이용하면 `member()` 메서드를 구현

  ```java
  	@GetMapping("/api/members/{id}")
  	public ResponseEntity<Object> member(@PathVariable Long id) throws IOException{	//리턴타입
  		Member member = memberDao.selectById(id);
  		
  		if(member==null) {
  			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("no member"));		//리턴
  		}
  		
  		return ResponseEntity.status(HttpStatus.OK).body(member);		//리턴
  	}
  ```

  - 리턴 타입이 `ResponseEntity`이면 `ResponseEntity`의 **body로 지정한 객체를 사용해서 변환을 처리한다**

    > 위 코드의 `.body(member)`는 member 객체를 JSON으로 변환한다
    >
    > `.body(new ErrorResponse("no member"))`는 `ErrorResponse`를 JSON으로 변환한다

  - `status` 지정값은 응답 상태 코드로 사용한다

    - 404(NOT_FOUND), 200(OK)를 사용했다

  - 실행결과
    ![image-20220129154234203](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220129154234203.png)

    - 404 상태코드와 함께 JSON 형식으로 응답 데이터 받았다

    

- `ResponseEntity` 생성 기본 방법

  - `status`와 `body`를 이용해서 상태 코드와 JSON으로 변환할 객체를 지정

    > ResponseEntity.status(상태코드).body(객체)

    - 상태코드는 `HttpStatus` 열거 타입에 정의된 값을 이용해서 정의한다

  - 200(OK) 응답 코드와 몸체 데이터 생성하는 경우 `ok()` 사용

    > ResponseEntity.ok(member)

  - 몸체가 없는 경우 body를 지정하지 않고 `build()`로 바로 생성

    > ResponseEntity.status(HttpStatus.NOT_FOUND).build()

  - 몸체 내용이 없는 경우 `status()` 메서드를 대신하는 메서드

    > - noContent() : 204
    > - badRequest() : 400
    > - notFound() : 404
    >
    > ResponseEntity.notFound().build()



- `newMember()`의 경우

  ```java
  response.setHeader("Location", "/api/members/"+newMemberId);
  response.setStatus(HttpServletResponse.SC_CREATED);
  ```

  - 같은 코드를 `ResponseEntity`로 구현

    - `ResponseEntity.create()` 메서드에 `Location` 헤더로 전달한 URI를 전달

    ```java
    	@PostMapping("/api/members")
    	public ResponseEntity<Object> newMember(@RequestBody @Valid RegisterRequest regReq) throws IOException{
    		try {
    			Long newMemberId = registerService.regist(regReq);
    			URI uri = URI.create("/api/members/" + newMemberId);
    			return ResponseEntity.created(uri).build();				//헤더 값 적용
    		}catch(DuplicateMemberException dupEx) {
    			return ResponseEntity.status(HttpStatus.CONFLICT).build();
    		}
    	}
    ```

    

### 5.2 @ExceptionHandler 적용 메서드에서 ResponseEntity로 응답하기

- 한 메서드에서 정상 응답과 에러 응답을 ResponseBody로 생성하면 코드가 중복될 수 있다

  ```java
  	@GetMapping("/api/members/{id}")
  	public ResponseEntity<Object> member(@PathVariable Long id) throws IOException{
  		Member member = memberDao.selectById(id);
  		
  		if(member==null) {
  			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("no member"));
  		}
  		
  		return ResponseEntity.status(HttpStatus.OK).body(member);
  	}
  ```

  - HTML 에러 응답 대신에 JSON 응답을 위해 `ResponseEntity`를 사용했다
    - **회원이 존재하지 않을때 404 응답을 해야되는 기능이 많으면 `ResponseEntity`를 생성하는 코드가 여러 곳에 중복된다**

- **`@ExceptionHandler`을 적용한 메서드에서 에러 응답을 처리하도록 하면 중복 코드 제거 가능**하다

  ```java
  	@GetMapping("/api/members/{id}")
  	public Member member(@PathVariable Long id) throws IOException{
  		Member member = memberDao.selectById(id);
  		
  		if(member==null) {
  			throw new MemberNotFoundException();
  		}
  		
  		return member;
  	}
  	
  	@ExceptionHandler(MemberNotFoundException.class)
  	public ResponseEntity<ErrorResponse> handleNoData(){
  		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("no member"));
  	}
  ```

  - `member()`는 `Member` 자체를 리턴한다 => JSON으로 변환한 결과를 응답한다
  - `MemberNotFoundException` 익셉션에 발생하면 `@ExceptionHandler`를 사용한 `handleNoData()`가 에러를 처리한다
    - 404 상태 코드와 `ErrorResponse` 객체를 몸체로 갖는 `ResponseEntity`를 리턴 => JSON 형식

- **`@RestControllerAdvice`**를 이용해 에러 처리 코드를 **별도 클래스로 분리 가능**하다

  - `@ControllerAdvice`와 동일한데 응답을 JSON이나 XML 형식으로 반환한다

  ```java
  @RestControllerAdvice("controller")		//해당 패키지 내부에서 발생하는 에러 전부 처리
  public class ApiExceptionAdvice {
  	
  	@ExceptionHandler(MemberNotFoundException.class)
  	public ResponseEntity<ErrorResponse> handleNoData(){
  		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("no member"));
  	}
  
  }
  ```

  - `@RestControllerAdvice` 사용하면 **에러 처리 코드가 한 곳에 모여** 효과적으로 에러 응답 관리 가능

### 5.3 @Valid 에러 결과를 JSON으로 응답하기

- **`@Valid`를 붙인 커맨드 객체가 값 검증에 실패하면 400 상태 코드를 응답**한다

  ```java
  	@PostMapping("/api/members")
  	public ResponseEntity<Object> newMember(@RequestBody @Valid RegisterRequest regReq) throws IOException{
  		try {
  			Long newMemberId = registerService.regist(regReq);
  			URI uri = URI.create("/api/members/" + newMemberId);
  			return ResponseEntity.created(uri).build();
  		}catch(DuplicateMemberException dupEx) {
  			return ResponseEntity.status(HttpStatus.CONFLICT).build();
  		}
  	}
  ```

  - `ResponseEntity` 사용과 상관없이 **검증에 실패하면 HTML로 에러를 응답한다**

- **`@Valid`를 이요한 검증에 실패시 JSON 형식 응답**을 받기 위해서는 **`Errors` 타입 파라미터를 추가해 직접 에러 응답을 생성하면 된다**

  > @Valid를 위해
  >
  > ```java
  > public class RegisterRequest {
  > 	@NotBlank
  > 	@Email
  > 	private String email;
  > 	@NotEmpty
  > 	private String password;
  > 	@NotEmpty
  > 	private String confirmPassword;
  > 	@NotBlank
  > 	private String name;
  >     ...
  > ```

  ```java
  	@PostMapping("/api/members")
  	public ResponseEntity<Object> newMember(@RequestBody @Valid RegisterRequest regReq, Errors errors) throws IOException{
  		
  		if(errors.hasErrors()) {
  			String errorCodes = errors.getAllErrors()	//List<ObjectError>
  					.stream()
  					.map(error -> error.getCodes()[0]) 	// error는 ObjectError
  					.collect(Collectors.joining(","));
  			
  			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("errorCodes ="+errorCodes));
  		}
  		
  		try {
  			Long newMemberId = registerService.regist(regReq);
  			URI uri = URI.create("/api/members/" + newMemberId);
  			return ResponseEntity.created(uri).build();
  		}catch(DuplicateMemberException dupEx) {
  			return ResponseEntity.status(HttpStatus.CONFLICT).build();
  		}
  	}
  ```

  - `hasErrors()`로 검증 에러가 존재하는지 확인
    - 존재하면 `getAllErrors()`로 모든 에러 정보를 구한다
    - 각 에러에 코드 값을 연결한 문자열을 생성해서 `errorCodes` 변수에 할당한다
  - 실행결과 : JSON 형식으로 응답이 온다
    ![image-20220129163250212](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220129163250212.png)

- **`@RequestBody`를 붙인 경우 `@Valid`를 붙인 객체의 검증에 실패했을 때** `Error` 타입 파라미터가 존재하지 않으면 `MethodArgumentNotValidException`이 발생한다

  - 이를 이용해 `@ExceptionHandler`를 이용해 검증 실패시 에러 응답을 생성해도 된다

    ```java
    @RestControllerAdvice("controller")
    public class ApiExceptionAdvice {
    
    	@ExceptionHandler(MethodArgumentNotValidException.class)
    	public ResponseEntity<ErrorResponse> handleBindException(MethodArgumentNotValidException ex){
    		String errorCodes = ex.getBindingResult().getAllErrors()
    				.stream()
    				.map(error -> error.getCodes()[0]) 	// error는 ObjectError
    				.collect(Collectors.joining(","));
    		
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("errorCodes ="+errorCodes));
    	}
    
    }
    ```