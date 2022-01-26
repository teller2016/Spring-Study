# 14장 - MVC 4 : 날짜 값 변환, @PathVariable, 익셉션 처리

> - @DateTimeFormat
> - @PathVariabler
> - 익셉션 처리

## 1. 프로젝트 준비

- 13장 프로젝트에 이어서 진행

## 2. 날짜를 이용한 회원 검색 기능

- 회원 가입 일자를 기준으로 검색하는 기능 구현

- `MemberDao` 클래스에 `selectByRegdate()` 추가

  ```java
  public class MemberDao {
  
  	private JdbcTemplate jdbcTemplate;
  	...
  	public List<Member> selectByRegdate(LocalDateTime from, LocalDateTime to){
  		
  		List<Member> results = jdbcTemplate.query("select * from MEMBER where REGDATE between ? and ? order by REGDATE desc", 
  				new RowMapper<Member>() {
  					@Override
  					public Member mapRow(ResultSet rs, int rowNum) throws SQLException{
  						Member member = new Member(rs.getString("EMAIL"), rs.getString("PASSWORD"), rs.getString("NAME"), rs.getTimestamp("REGDATE").toLocalDateTime());
  						member.setId(rs.getLong("ID"));
  						return member;
  					}
  		}, from, to
  				);
  		
  		return results;
  		
  	}
  }
  ```

  - `selectByRegdate()`는 REGDATE 값이 `from`, `to` 사이에 있는 `Member` 목록을 구한다

## 3. 커맨드 객체 Date 타입 프로퍼티 변환 처리 : @DateTimeFormat

- 검색을 위한 **시작 시간 기준과 끝 시간 기준을 표현하기 위한 커맨드 클래스 구현**

  ```java
  public class ListCommand {
  	private LocalDateTime from;
  	private LocalDateTime to;
  	
  	public LocalDateTime getFrom() {
  		return from;
  	}
  	public void setFrom(LocalDateTime from) {
  		this.from = from;
  	}
  	public LocalDateTime getTo() {
  		return to;
  	}
  	public void setTo(LocalDateTime to) {
  		this.to = to;
  	}
  }
  ```

  - 검색을 위한 입력 폼

    ```jsp
    <input type="text" name="from"/>
    <input type="text" name="to"/>
    ```

  - **문제점**

    - `<input>`에 입력한 문자열을 `LocalDateTime` 타입으로 변환해야 된다

      > EX. 2018년 3월 1일 오후 3시를 "2018030115"로 입력했으면 문자열에 맞게 LocalDateTime 타입으로 변환해야 된다

  - `LocalDateTime` 타입으로의 변환이 필요

    - **`ListCommand` 클래스의 필드에 `@DateTimeFormat` 애노테이션 붙이면 된다**

    ```java
    public class ListCommand {
    	@DateTimeFormat(pattern = "yyyyMMddHH")
    	private LocalDateTime from;
    	@DateTimeFormat(pattern = "yyyyMMddHH")
    	private LocalDateTime to;
    	...
    }
    ```

    - 지정한 형식에 맞춰 `LocalDateTime` 타입으로 변환된다

      > "2018030115" 문자열을 "2018년 3월 1일 15시" 값을 갖는 LocalDateTime 객체로 변환해준다

- 컨트롤러 클래스 `MemberListController`

  - 별도 설정 없이 `ListCommand` 클래스를 커맨드 객체로 사용

  ```java
  @Controller
  public class MemberListController {
  	
  	private MemberDao memberDao;
  
  	public void setMemberDao(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  	
  	@RequestMapping("/members")
  	public String list(@ModelAttribute("cmd") ListCommand listCommand, Model model) {
  		
  		if(listCommand.getFrom() != null && listCommand.getTo() != null) {
  			List<Member> members = memberDao.selectByRegdate(listCommand.getFrom(), listCommand.getTo());
  			model.addAttribute("members", members);
  		}
  		
  		return "member/memberList";
  	}
  }
  ```

  - `ControllerConfig` 설정 클래스에 빈 설정 추가

    ```java
    @Configuration
    public class ControllerConfig {
    	...
    	@Autowired
    	private MemberDao memberDao;
    	...
    	@Bean
    	public MemberListController memberListController() {
    		MemberListController controller = new MemberListController();
    		controller.setMemberDao(memberDao);
    		return controller;
    	}
    }
    ```

- 뷰 코드

  - `LocalDateTime` 값을 지정 형식으로 출력하는 커스텀 태그 작성

  - JSTL이 제공하는 날짜 형식 태그는 자바8의 LocalDateTime 타입을 지원하지 않는다

    - 태그 파일을 사용해 `LocalDateTime` 값을 지정한 형식으로 출력

    ```tag
    <%@ tag body-content="empty" pageEncoding="utf-8" %>
    <%@ tag import="java.time.format.DateTimeFormatter" %>
    <%@ tag trimDirectiveWhitespaces="true" %>
    <%@ attribute name="value" required="true" 
                  type="java.time.temporal.TemporalAccessor" %>
    <%@ attribute name="pattern" type="java.lang.String" %>
    <%
    	if (pattern == null) pattern = "yyyy-MM-dd";
    %>
    <%= DateTimeFormatter.ofPattern(pattern).format(value) %>
    ```

  - JSP 코드

    - `MemberListController#list()`는 뷰에 `members` 속성으로 `Member` 목록을 전달했다

    ```jsp
    <!DOCTYPE html>
    <html>
    <head>
        <title>회원 조회</title>
    </head>
    <body>
        <form:form modelAttribute="cmd">
        <p>
            <label>from: <form:input path="from" /></label>
            ~
            <label>to:<form:input path="to" /></label>
            <input type="submit" value="조회">
        </p>
        </form:form>
        
        <c:if test="${! empty members}">
        <table>
            <tr>
                <th>아이디</th><th>이메일</th>
                <th>이름</th><th>가입일</th>
            </tr>
            <c:forEach var="mem" items="${members}">
            <tr>
                <td>${mem.id}</td>
                <td><a href="<c:url value="/members/${mem.id}"/>">${mem.email}</a></td>
                <td>${mem.name}</td>
                <td><tf:formatDateTime value="${mem.registerDateTime }" pattern="yyyy-MM-dd" /></td>
            </tr>
            </c:forEach>
        </table>
        </c:if>
    </body>
    </html>
    ```

    - `<form:input>` 태그에서 `from, to` 프로퍼티 모두 `@DateTimeFormat(pattern="yyyyMMddHH")`이 적용되어 있으므로 `<input>` 태그에 사용할 값을 생성할 때 "yyyMMddHH" 형식으로 값을 출력한다

    - 브라우저로 "/member/list" 접속하면 `from, to` 파라미터가 존재하지 않아 **커맨드 객체의 `form, to` 값은 null이 된다**

      - `MemberListController`에서 두 값이 null이 아닐때만 `Member`데이터 읽어오게 해놨다
      - 즉, 시간 입력 폼만 출력된다

      ![image-20220126112701638](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220126112701638.png)

    - 조회 결과
      ![image-20220126112859309](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220126112859309.png)

      - 알맞은 형식으로 날짜가 변환되었다

> @DateTimeFormate은 java.tim.LocalDateTime, java.time.LocalDate와 같이 자바8에 추가된 시간 타입과 java.util.Date과 java.util.Calender 타입을 지원한다

### 3.1 변환 에러 처리

- 폼에서 "20210621"을 입력하면, 지정한 형식 "yyyyMMddHH"와 맞지 않아 400에러 발생한다

  - 에러 대신에 알맞은 에러 메시지를 보여주자
  - `Errors` 타입 파라미터를 요청 매핑 애노테이션 적용 메서드에 추가하면 된다
    - **(`Errors` 타입 파라미터를 `ListCommand` 파라미터 뒤에 위치시켜야 된다)**

  ```java
  @Controller
  public class MemberListController {
  	...
  	
  	@RequestMapping("/members")
  	public String list(@ModelAttribute("cmd") ListCommand listCommand, Errors errors, Model model) {
  		
  		if(errors.hasErrors())
  			return "member/memberList";
  		
  		if(listCommand.getFrom() != null && listCommand.getTo() != null) {
  			List<Member> members = memberDao.selectByRegdate(listCommand.getFrom(), listCommand.getTo());
  			model.addAttribute("members", members);
  		}
  		
  		return "member/memberList";
  	}
  }
  ```

- 요청 매핑 애노테이션 적용 메서드가 `Errors` 타입 파라미터를 가질 경우 `@DateTimeFormat`에 지정한 형식에 맞지 않으면 `Errors` 객체에 "typeMismatch" 에러 코드를 추가한다

  - 메시지 프로퍼티 파일에 해당 메시지를 추가하면 에러 메시지 보여줄 수 있다

  ```properties
  typeMismatch.java.time.LocalDateTime=잘못된 형식
  ```

- `memberList.jsp`에 `<form:errors>` 태그 추가

  ```jsp
  <!DOCTYPE html>
  <html>
  <head>
      <title>회원 조회</title>
  </head>
  <body>
      <form:form modelAttribute="cmd">
      <p>
          <label>from: <form:input path="from" /></label>
          <form:errors path="from"/>			//추가
          ~
          <label>to:<form:input path="to" /></label>
          <form:errors path="to"/>			//추가
          <input type="submit" value="조회">
      </p>
      </form:form>
      ...
  </body>
  </html>
  ```

  ![image-20220126114552244](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220126114552244.png)



## 4. 변환 처리에 대한 이해

- 누가 문자열을 `LocalDateTime` 타입으로 변환해주나

  - `WebDataBinder`는 값 변환에 관여한다

- 스프링 MVC는 요청 매핑 애노테이션 적용 메서드와 `DispatcherServlet` 사이를 연결하기 위해 `RequestMappingHandlerAdapter` 객체를 사용한다

  - 이는 요청 파라미터와 커맨드 객체 사이의 변환 처리를 위해 `WebDataBinder`을 이용한다

- `WebDataBinder`은 커맨드 객체를 생성하고, 커맨드 객체의 프로퍼티와 같은 이름의 요청 파라미터를 이용해서 프로퍼티 값 생성한다

  - 직접 타입을 변환하지 않고 `ConversionService`에 위임한다

    > 스프링 MVC를 위한 설정인 @EnableWebMvc를 사용하면 DefaultFormattingConversionService를 ConversionService로 사용한다
    >
    > - DefaultFormattingConversionService는 int, long 같은 기본 데이터 타입과 @DateTimeFormat을 사용한 시간 관련 타입 변환 기능을 제공한다.
    >   - 이 덕분에 @DateTimeFormat만 붙이면 지정한 형식의 문자열을 시간 타입 값으로 변환 받는 것이다

- `WebDataBinder`는 `<form:input>`에도 사용된다

  - `path` 속성에 지정한 프로퍼티 값은 String으로 변환해서 `<input>` 태그의 `value` 속성 값으로 생성한다
    - 이때 프로퍼티 값을 String으로 변환할때 `WebDataBinder`의 `ConversionService`를 사용한다

  > <form:input path="from"/>    --->    \<input type="text" id="from" name="from" value="2021062115"/>



## 5. MemberDao 클래스 중복 코드 정리 및 메서드 추가

- `RowMapper` 객체를 생성하는 코드 중복 제거 + `selectById()` 추가

  - 임의의 객체를 필드에 할당하고 그 필드를 사용하도록 수정

  ```java
  public class MemberDao {
  
  	private JdbcTemplate jdbcTemplate;
  	
  	private RowMapper<Member> memRowMapper = new RowMapper<Member>() {
  		@Override
  		public Member mapRow(ResultSet rs, int rowNum) throws SQLException{
  			Member member = new Member(
  						rs.getString("EMAIL"),
  						rs.getString("PASSWORD"),
  						rs.getString("NAME"),
  						rs.getTimestamp("REGDATE").toLocalDateTime());
  						member.setId(rs.getLong("ID"));
  						
  						return member;
  		}
  	};
  	
  	public Member selectByEmail(String email) {
  		List<Member> results = jdbcTemplate.query("select * from MEMBER where EMAIL = ?", memRowMapper,email);
  		return results.isEmpty() ? null : results.get(0);
  	}
  	
  	public List<Member> selectAll(){
  		List<Member> results = jdbcTemplate.query("select * from MEMBER", memRowMapper);
  		return results;
  	}
  
  	public List<Member> selectByRegdate(LocalDateTime from, LocalDateTime to){
          
  		List<Member> results = jdbcTemplate.query("select * from MEMBER where REGDATE between ? and ? order by REGDATE desc", memRowMapper, from, to);
  		return results;
  	}
      
      public Member selectById(Long memId) {
  		List<Member> results = jdbcTemplate.query("select * from MEMBER where ID = ?", memRowMapper, memId);
  		return results.isEmpty() ? null : results.get(0);
  	}
  }
  ```



## 6. @PathVariable을 이용한 경로 변수 처리

- ID가 10인 회원 정보를 조회하는 URL("http://localhost:8080/sp5-chap14/members/10")

  - 각 회원마다 경로의 마지막 부분이 달라진다

- **경로의 일부가 달라질 때 사용하는 것이 `@PathVariable` 애노테이션이다**

- 가변 경로 처리

  ```java
  @Controller
  public class MemberDetailController {
  	private MemberDao memberDao;
  
  	public void setMemberDao(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  	
  	@GetMapping("/members/{id}")
  	public String detail(@PathVariable("id") Long memId, Model model) {
  		Member member = memberDao.selectById(memId);
  		
  		if(member==null)
  			throw new MemberNotFoundException();
  		
  		model.addAttribute("member", member);
  		return "member/memberDetail";
  	}
  }
  ```

  - '{경로변수}' 같이 중괄호 쌓인 부분이 경로 변수이다
  - 경로 변수에 해당하는 값이 **`@PathVariable` 파라미터에 전달된다**

- 설정 클래스 빈에 등록

  ```java
  @Configuration
  public class ControllerConfig {
  	...
  	@Autowired
  	private MemberDao memberDao;
  	...
  	@Bean
  	public MemberDetailController memberDetailController() {
  		MemberDetailController controller = new MemberDetailController();
  		controller.setMemberDao(memberDao);
  		return controller;
  	}
  }
  ```

- JSP

  ```jsp
  <%@ page contentType="text/html; charset=utf-8" %>
  <%@ taglib prefix="tf" tagdir="/WEB-INF/tags" %>
  <!DOCTYPE html>
  <html>
  <head>
      <title>회원 정보</title>
  </head>
  <body>
      <p>아이디: ${member.id}</p>
      <p>이메일: ${member.email}</p>
      <p>이름: ${member.name}</p>
      <p>가입일: <tf:formatDateTime value="${member.registerDateTime}" pattern="yyyy-MM-dd HH:mm" /> </p>
  </body>
  </html>
  ```

  ![image-20220126121928031](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220126121928031.png)



## 7. 컨트롤러 익셉션 처리하기

- 없는 ID를 경로변수로 사용하면 `MemberNotFoundException` 발생한다

  - `MemberDetailController` 구현상태

    ```java
    	@GetMapping("/members/{id}")
    	public String detail(@PathVariable("id") Long memId, Model model) {
    		Member member = memberDao.selectById(memId);
    		
    		if(member==null)
    			throw new MemberNotFoundException();
    		
    		model.addAttribute("member", member);
    		return "member/memberDetail";
    	}
    ```

- 혹은 `Long` 타입이 아닌 문자 "a"를 입력하면 `Long` 타입으로 변환 못해 400 에러 발생

- **익셉션 화면보다 사용자에게 적합한 안내를 하는게 좋다**

  - `MemberNotFoundException`은 try-catch문으로 잡아서 뷰를 보여주면 된다
  - 그런데 **타입 변환 에러는 어떻게 에러 화면 보여줄까?**
    - **`@ExceptionHandler` 애노테이션으로 해결 가능**

- 같은 컨트롤러에 `@ExceptionHandler` 적용한 메서드가 존재하면 그 메서드가 익셉션을 처리한다

  - 즉 컨트롤러에서 발생한 익셉션을 직접 처리할 때 `@ExceptionHandler`을 적용한 메서드를 구현하면 된다

- 예시 코드

  ```java
  @Controller
  public class MemberDetailController {
  	private MemberDao memberDao;
  
  	public void setMemberDao(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  	
  	@GetMapping("/members/{id}")
  	public String detail(@PathVariable("id") Long memId, Model model) {
  		Member member = memberDao.selectById(memId);
  		
  		if(member==null)
  			throw new MemberNotFoundException();
  		
  		model.addAttribute("member", member);
  		return "member/memberDetail";
  	}
  	
  	@ExceptionHandler(TypeMismatchException.class)		//추가
  	public String handleTypeMismatchException() {
  		return "member/invalidId";
  	}
  	
  	@ExceptionHandler(MemberNotFoundException.class)	//추가
  	public String handleNotFoundException() {
  		return "member/noMember";
  	}
  }
  ```

  - `@ExceptionHandler` 값으로 `TypeMismatchException.class`를 줬다

    - 이 익셉션은 경로 변수값의 타입이 올바르지 않을 때 발생
    - **익셉션 발생대신 `handleTypeMismatchException()` 메서드를 실행한다**

  - 익셉션 발생시 보여줄 뷰 코드 작성

    - noMember.jsp

    ```jsp
    <%@ page contentType="text/html; charset=utf-8" %>
    <!DOCTYPE html>
    <html>
    <head>
        <title>에러</title>
    </head>
    <body>
        존재하지 않는 회원입니다.
    </body>
    </html>
    ```

    - invalidId.jsp

    ```jsp
    <%@ page contentType="text/html; charset=utf-8" %>
    <!DOCTYPE html>
    <html>
    <head>
        <title>에러</title>
    </head>
    <body>
        잘못된 요청입니다.
    </body>
    </html>
    ```

- 실행 결과
  ![image-20220126131127732](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220126131127732.png)![image-20220126131138278](C:\Users\telle\AppData\Roaming\Typora\typora-user-images\image-20220126131138278.png)

> 익셉션 객체에 대한 정보를 사용하려면 메서드의 파라미터로 익셉션 객체를 전달받아 사용한다
>
> ```java
> @ExceptionHandler(TypeMismatchException.class)
> public String handleTypeMismatchException(TypeMismatchException ex){
>     // ex를 사용해서 로그 남기는 등 작업 수행
>     return "member/invalidId";
> }
> ```

### 7.1 @ControllerAdvice를 이용한 공통 익셉션 처리

- `@ExceptionHandler`적용하면 해당 컨트롤러에서 발생한 익셉션만 처리한다

  - **다수 컨트롤러에서 동일 타입의 익셉션 처리하기 위한 방법?**

- **`@ControllerAdvice` 애노테이션**으로 **여러 컨트롤러에서 발생하는 동일한 익셉션을 처리**할 수 있다

  ```java
  @ControllerAdvice("spring")
  public class CommonExceptionHandler{
      @ExceptionHandler(RuntimeException.class)
      public String handleRuntimeException(){
          return "error/commonException";
      }
  }
  ```

  - **`@ControllerAdvice`이 적용된 클래스**는 지정된 범위의 컨트롤러에 **공통으로 사용될 설정을 지정** 가능하다

    > "spring" 패키지와 그 하위 패키지에 속한 콘트롤러를 위한 공통 기능을 정의했다
    >
    > 해당 범위에 속한 컨트롤러에서 RuntimeException이 발생하면 handleRuntimeException() 메서드를 통해 익셉션 처리된다

  - **`@ControllerAdvice` 적용 클래스가 동작하려면 빈으로 등록해야 된다**

### 7.2 @ExceptionHandler 적용 메서드의 우선 순위

- 다음 순서로 익셉션을 처리할 `@ExceptionHandler`을 찾는다

  > 1. 같은 컨트롤러에 위치한 `ExceptionHandler` 메서드 중 해당 익셉션을 처리할 수 있는 메서드 검색
  > 2. 같은 클래스에 위치한 메서드가 익셉션을 처리할 수 없는 경우 `@ControllerAdvice` 클래스에 위치한 `@ExceptionHandler` 메서드를 검색한다

- `@ControllerAdvice`가 공통 설정을 적용할 컨트롤러 대상을 지정하기 위해 제공하는 속성

  | 속성              | 타입                          | 설명                                             |
  | ----------------- | ----------------------------- | ------------------------------------------------ |
  | valuebasePackages | String[]                      | 공통 설정을 적용할 컨트롤러가 속하는 기준 패키지 |
  | annotations       | Class<? extends Annotation>[] | 특정 애노테이션이 적용된 컨트롤러 대상           |
  | assignableTypes   | Class<?>[]                    | 특정 타입 또는 그 하위 타입인 컨트롤러 대상      |

### 7.3 @ExceptionHandler 애노테이션 적용 메서드의 파라미터와 리턴 타입

- `@ExceptionHandler`를 붙인 메서드가 가질 수 있는 파라미터

  > - HttpServletRequest, HttpServletResponse, HttpSession
  > - Model
  > - 익셉션

- 리턴 가능한 타입

  > - ModelAndView
  > - String (뷰 이름)
  > - (@ResponseBody 애노테이션을 붙인 경우) 임의 객체 (16장 확인)
  > - ResponseEntity (16장 확인)