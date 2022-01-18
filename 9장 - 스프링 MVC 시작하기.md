# 9장 - 스프링 MVC 시작하기

> - 간단한 스프링 MVC 예제

- 스프링이 지원하는 웹 MVC 프레임워크 때문에 스프링을 잘 사용한다

## 1. 프로젝트 생성

> 폴더
>
> - src/main/java
> - src/main/webapp
>   - HTML, CSS, JS, JSP 등 웹 앱 구현하는데 피룡한 코드가 위치한다
> - src/main/webapp/WEB-INF
>   - web.xml 파일이 위치

- pom.xml
  - `<packaging>`의 값으로 `war(web application archive)` 추가됨
    - 이 태그의 기본값은 `jar`이다
    - 서블릿/JSP를 이용한 웹 앱을 개발할 경우 `war` 사용
  - 스프링을 사용해 웹 앱을 개발하는데 필요한 의존으로
    - 서브릿 3.1, JSP 2.3, JSTL 1.2에 대한 의존 추가
    - 스프링 MVC 사용을 위해 `spring-webmvc` 모듈에 대한 의존 추가



## 2. 이클립스 톰캣 설정

- 이클립스에서 웹 프로젝트 테스트를 위해 톰캣, 제티와 같은 웹 서버 설정이 필요
  - 톰캣 8.5 버전으로 테스트한다
- 톰캣 8.5를 이클립스에 서버로 등록
  1. 톰캣 다운로드 및 압축 해제
  2. 이클립스에 서버 등록
     - [Window] => [Preferences] => Server/Runtime Environments => [Add]로 톰캣 서버 등록
     - 서버 선택 및 설치 경로 선택



## 3. 스프링 MVC를 위한 설정

> - 스프링 MVC의 주요 설정(HandlerMapping, ViewResolver 등)
> - 스프링의 DispatcherServlet 설정

### 3.1 스프링 MVC 설정

- 스프링 MVC 설정

  ```java
  @Configuration
  @EnableWebMvc
  public class MvcConfig implements WebMvcConfigurer{
  
  	@Override
  	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
  		configurer.enable();
  	}
  
  	@Override
  	public void configureViewResolvers(ViewResolverRegistry registry) {
  		registry.jsp("/WEB-INF/view/", ".jsp");
  	}
  
  }
  ```

  - `@EnableWebMvc`는 스프링 MVC 설정을 활성화한다
    - 스프링 MVC를 사용하는데 필요한 다양한 설정을 생성한다
  - `DispatcherServlet`의 매핑 경로를 '/'로 주었을 때, JSP/HTML/CSS 등을 올바르게 처리하기 위한 설정을 추가한다
  - JSP를 이용해서 컨트롤러의 실행 결과를 보여주기 위한 설정을 추가한다

- 스프링 MVC 사용을 위해서는 다양한 구성 요소를 설정해야 된다
  - `@EnableWebMvc`가 설정을 대신 해준다
    - 내부적으로 다양한 빈 설정을 추가해준다
- `WebMvcConfigurer` 인터페이스는 스프링 MVC의 개별 설정을 조정할 때 사용
  - `configureDefaultServletHandling`, `configureViewResolvers`는 각각 디폴트 서블릿과 `ViewResolver`와 관련된 설정을 조정한다

### 3.2 web.xml 파일에 DispatcheServlet 설정

- **스프링 MVC가 웹 요청을 처리하려면 `DispatcherServlet`을 통해서 웹 요청을 받아야 한다**

  - 이를 위해 `web.xml` 파일에 `DispatcherServlet`을 등록한다

  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  
  <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" 
  	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  	xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee 
               http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
  	version="3.1">
  
  	<servlet>
  		<servlet-name>dispatcher</servlet-name>
  		<servlet-class>
  			org.springframework.web.servlet.DispatcherServlet
  		</servlet-class>
  		<init-param>
  			<param-name>contextClass</param-name>
  			<param-value>
  				org.springframework.web.context.support.AnnotationConfigWebApplicationContext
  			</param-value>
  		</init-param>
  		<init-param>
  			<param-name>contextConfigLocation</param-name>
  			<param-value>
  				config.MvcConfig
  				config.ControllerConfig
  			</param-value>
  		</init-param>
  		<load-on-startup>1</load-on-startup>
  	</servlet>
  
  	<servlet-mapping>
  		<servlet-name>dispatcher</servlet-name>
  		<url-pattern>/</url-pattern>
  	</servlet-mapping>
  
  	<filter>
  		<filter-name>encodingFilter</filter-name>
  		<filter-class>
  			org.springframework.web.filter.CharacterEncodingFilter
  		</filter-class>
  		<init-param>
  			<param-name>encoding</param-name>
  			<param-value>UTF-8</param-value>
  		</init-param>
  	</filter>
  	<filter-mapping>
  		<filter-name>encodingFilter</filter-name>
  		<url-pattern>/*</url-pattern>
  	</filter-mapping>
  
  </web-app>
  ```

  - `DispatcherServlet`을 `dispatcher`라는 이름으로 등록
  - `contextClass` 초기화 파라미터를 설정
    - 자바 설정을 사용하는 경우 `AnnotationConfigWebApplicationContext` 클래스를 사용
    - 이 클래스는 자바 설정을 이용하는 웹 앱 용 스프링 컨테이너 클래스이다
  - `contextConfiguration` 초기화 파라미터의 값을 지정
    - 스프링 설정 클래스 목록을 지정한다
    - 각 설정 파일의 경로는 줄바꿈이나 콤마로 구분
  - 모든 요청을 `DispatcherServlet`이 처리하도록 서블릿 매핑 설정
  - HTTP 요청 파라미터의 인코딩 처리를 위한 서블릿 필터를 등록함
    - 스프링은 인코딩 처리를 위한 필터인 `CharacterEncodingFilter` 클래스 제공
    - `encoding` 초기화 파라미터를 설정해 HTTP 요청 파라미터를 읽어올 때 사용할 인코딩을 지정

- `DispatcherServlet`은 초기화 과정에서 `contextConfiguration` 초기화 파라미터에 지정한 설정 파일을 이용해 스프링 컨테이너를 초기화한다

  - 위에서 `MvcConfig`, `ControllerConfig` 클래스를 이용해 스프링 컨테이너를 생성하도록 설정했다



## 4. 코드 구현

> - 클라이언트의 요청을 알맞게 처리할 컨트롤러
> - 처리 결과를 보여줄 JSP

## 4.1 컨트롤러 구현

- 컨트롤러 코드

  ```java
  @Controller
  public class HelloController {
  
  	@GetMapping("/hello")
  	public String hello(Model model,
  			@RequestParam(value = "name", required = false) String name) {
  		model.addAttribute("greeting", "안녕하세요, " + name);
  		return "hello";
  	}
  }
  ```

  - **`@Controller` 애노테이션을 적용한 클래스는 스프링 MVC에서 컨트롤러로 사용한다**
  - `@GetMapping`은 메서드가 처리할 요청 경로를 지정한다
    - **HTTP 요청 중 GET 메서드에 대한 매핑을 설정**
    - "/hello" 경로로 들어온 요청을 `hello()`메서드를 이용해 처리한다고 설정함
  - **`Model` 파라미터는 컨트롤러의 처리 결과를 뷰에 전달할 때 사용**
  - `@RequestParam`은 HTTP 요청 파라미터의 값을 메서드의 파라미터로 전달할 때 사용
    - `name` 요청 파라미터의 값을 `name` 파라미터에 전달한다
  - "greeting"이라는 **모델 속성에 값을 설정한다**
    - 값으로는 "안녕하세요, "+name 문자열로 설정했다
    - JSP 코드에서 이 속성을 이용해서 값을 출력한다
  - 컨트롤러의 처리 결과를 보여줄 **뷰 이름으로 "hello"를 사용한다**



- **컨트롤러(Controller)**란 웹 요청을 처리하고 그 결과를 뷰에 전달하는 스프링 빈 객체이다
  - 컨트롤러로 사용할 클래스는 `@Controller`을 붙이고
  - `@GetMapping`, `@PostMapping` 같은 요청 매핑 애노테이션으로 **처리할 경로를 지정**한다

- `@GetMapping`과 요청 URL 간의 관계 + `@RequestParam`과 요청 파라미터와의 관계

  ```java
  // http://host:port/sp5-chap09/hello?name=bk
  
  	@GetMapping("/hello")		//위의 /hello
  	public String hello(Model model,
  			@RequestParam(value = "name", required = false) String name) {	//value = name에 bk
  		model.addAttribute("greeting", "안녕하세요, " + name);
  		return "hello";
  	}
  ```

  - `@GetMapping` 애노테이션의 값은 서블릿 컨텍스트 경로(또는 웹 어플리케이션 경로)를 기준으로 한다

    > Ex. 톰캣의 경우 webapps\sp5-chap09 폴더는 웹 브라우저에서 http://host/sp5-chap09 경로에 해당한다
    >
    > - sp5-chap09가 컨텍스트 경로가 된다
    > - 컨텍스트 경로 /sp5-chap09이므로 http://host/sp5-chap09/main/list 경로를 처리하기 위한 컨트롤러는 `@GetMapping("/main/list")`를 사용해야 한다

  - `@RequestParam` 애노테이션은 HTTP 요청 파라미터를 메서드의 파라미터로 전달받을 수 있게 해준다

    - `value` 속성은 파라미터의 이름을 지정
    - `required` 속성은 필수 여부를 지정

    > name 요청 파라미터의 값인 "bk"가 `hello()` 메서드의 name 파라미터에 전달된다

  - 파라미터로 전달받은 `Model` 객체의 `addAttribute()` 메서드는 **뷰에 전달할 데이터를 지정하기 위해 사용된다**

    ```java
    model.addAttribute("greeting", "안녕하세요, " + name);
    ```

    - 첫 파라미터는 **데이터를 식별하는데 사용되는 속성 이름**
    - 두번째 파라미터는 **속성 이름에 해당하는 값**
      - **뷰 코드는 이 속성 이름을 사용해서 컨트롤러가 전달한 데이터에 접근한다**

  - `@GetMapping`이 붙은 메서드는 컨트롤러의 실행 결과를 보여줄 **뷰 이름을 리턴한다**

    > "hello"를 뷰 이름으로 리턴했다
    >
    > - 이는 논리적인 이름이며 실제 뷰 이름에 해당하는 뷰 구현을 찾아주는 것은 `ViewResolver`가 처리한다



- 컨트롤러를 스프링 빈으로 등록

  ```java
  @Configuration
  public class ControllerConfig {
  
  	@Bean
  	public HelloController helloController() {
  		return new HelloController();
  	}
  }
  ```

### 4.2 JSP 구현

- 컨트롤러가 생성한 결과를 보여줄 **뷰 코드는 JSP를 이용해서 구현한다**

- WEB-INF/view 폴더에 hello.jsp 파일 추가

  ```jsp
  <%@ page contentType="text/html; charset=utf-8" %>
  <!DOCTYPE html>
  <html>
    <head>
      <title>Hello</title>
    </head>
    <body>
      인사말: ${greeting}
    </body>
  </html>
  ```

  - `HelloController`의 `hello()`메서드가 리턴한 뷰 이름은 "hello"였는데 파일 이름은 "hello.jsp"이다

  - 뷰 이름과 JSP 파일과 연결은 `MvcConfig` 클래스의 다음 설정으로 이루어진다

    ```java
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
    	registry.jsp("/WEB-INF/view/", ".jsp");
    }
    ```

    - `registry.jsp()`는 JSP를 뷰 구현으로 사용할 수 있도록 해준다
      - 첫 인자 => JSP 파일 경로(접두어)
      - 두번째 인자 => 접미사
      - **뷰 이름의 앞뒤에 접두어, 접미사 붙여서 최종 JSP 파일 경로를 결정한다**

  ```jsp
  인사말: ${greeting}
  ```

  - **컨트롤러의 `Model`에 추가한 속성의 이름**인 `greeting`과 동일하다

  - 컨트롤러에서의 속성을 접근할 수 있는 이유는 스프링 MVC 프레임워크가 **모델에 추가한 속성을 `HttpServletRequest`에 옮겨주기 때문**

    > 모델 ===[스프링 MVC 프레임워크 request.setAttribute("greeting", 값)]===> 인사말: ${greeting}

  

## 5. 실행하기

- [프로젝트] => [Run As] => [Run on Server]

- 서버에서 실행할 웹 프로젝트 선택

- 실행하면 이클립스 내장 브라우저 실행된다

  - 에러화면 나온다 **(index.jsp 파일 만들면 피할 수 있다)**

  - http://localhost:8080/sp5-chap09/hello?name=bk 주소 입력

    ```
    인사말: 안녕하세요, bk
    ```

    - `name` 파라미터가 `HelloController`을 거쳐 JSP까지 전달되었다



## 6. 정리

- 실행한 작업

  > - 스프링 MVC 설정
  > - 웹 부라우저의 요청을 처리할 컨트롤러 구현
  > - 컨트롤러의 처리 결과를 보여줄 뷰 코드 구현

- 다음으로 진행할 확장

  > - 컨트롤러에서 서비스나 DAO를 사용해서 클라이언트의 요청을 처리
  > - 컨트롤러에서 요청 파라미터의 값을 하나의 객체로 받고 값 검증
  > - 스프링이 제공하는 JSP 커스텀 태그를 이용해서 폼 처리
  > - 컨트롤러에서 세션이나 쿠키를 사용
  > - 인터셉터로 컨트롤러에 대한 접근 처리
  > - JSON 응답 처리