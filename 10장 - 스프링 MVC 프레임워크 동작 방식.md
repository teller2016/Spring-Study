# 10장 - 스프링 MVC 프레임워크 동작 방식

> - 스프링 MVC 구성 요소
> - DispatcherServlet
> - WebMvcConfigurer과 스프링 MVC 설정

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

  - 이 설정 후에 컨트롤러와 뷰 생성을 위한 코드 작성만 해주면 웹 앱을 개발할 수 있다

- 스프링 MVC 웹 요청을 처리하기 위해 필요한 핵심 구성 요소에 대해 배운다



## 1. 스프링 MVC 핵심 구성 요소

![](C:\ONEDRIVE\OneDrive - 아주대학교\Spring Study\MVC 핵심 구성요소.png)

- `<<Spring bean>>`은 스프링 빈으로 등록해야되는 것을 의미
- 초록색 배경 구성 요소는 직접 구현해야 된다

#### 동작

- `DispatcherServlet`이 모든 연결을 담당한다

  - 브라우저로부터 요청이 들어오면 요청을 처리하기 위한 컨트롤러 객체를 검색한다
  - `DispatcherServelt`이 직접 컨트롤러를 검색하지 않고 `HandlerMapping`이라는 빈 객체에 컨트롤러 검색을 요청한다

- `HandlerMapping`은 cli의 요청경로를 통해 이를 처리할 컨트롤러 빈 객체를 `DispatcherServelt`에 전달한다

  > 요청 경로가 '/hello'면 등록된 컨트롤러 빈 중에 '/hello' 요청 경로를 처리할 컨트롤러를 리턴한다

- `DispatcherServelt`은 `HandlerMapping`이 찾아준 컨트롤러 객체를 처리할 수 있는 `HandlerAdapter` 빈에게 요청처리를 위임한다

  - `HandlerAdapter`은 컨트롤러에 알맞은 메서드를 호출해 요청을 처리하고 결과를 리턴해준다
    - 컨트롤러의 처리 결과를 `ModelAndView`라는 객체로 변환해 `DispatcherServelt`에 리턴

  > `DispatcherServelt`은 `@Controller`로 구현한 컨트롤러, `Controller` 인터페이스를 구현한 컨트롤러, 특수 목적에 사용되는 `HttpRequestHandler` 인터페이스를 구현한 클래스를 동일한 방식으로 실행할 수 있도록 만들어졌다.
  >
  > - 그래서 바로 컨트롤러 객체의 메서드를 실행할 수 없다
  >   - 중간에 `HandlerAdapter`가 필요하다

- 컨트롤러 요청 처리 결과를 `ModelAndView`로 받으면 결과를 보여줄 뷰를 찾기 위해 `ViewResolver` 빈 객체를 사용한다

  - `ModelAndView`는 컨트롤러가 리턴한 뷰 이름을 담고 있다

    - 뷰 이름에 해당하는 `View`객체를 찾거나 생성해서 리턴한다

    - `ViewResolver`은 매번 새로운 `View`객체를 생성해서 `DispatcherServlet`에 리턴한다

- `ViewResolver`가 생성한 `View` 객체에 응답 결과 생성을 요청한다

  > JSP 사용하는 경우 View 객체는 JSP를 실행함으로써 웹 브라우저에게 전송할 응답 결과를 생성하고 끝난다



### 1.1 컨트롤러와 핸들러

- cli의 요청을 실제로 처리하는건 컨트롤러이다

  - `DispatcherServlet`은 cli 요청을 전달받는 창구 역할이다

- `DispatcherServlet`은 클라이언트의 요청을 처리할 컨트롤러를 찾기 위해 `HandlerMapping`을 사용한다

  > **컨트롤러**를 찾아주는 객체가 `ControllerMapping` 타입이 아니네? 왜 `HandlerMapping`일까?

- 스프링 MVC는 웹 요청을 처리할 수 있는 범용 프레임워크이다
  - `@Controller` 붙은 컨트롤러 말고 직접 만든 클래스로도 cli 요청을 처리할 수 있다
  - 즉 `DispatcherServlet`은 cli 요청 처리하는 객체가 반드시 `@Controller` 적용한 클래스일 필요없다
    - 그래서 스프링 MVC는 웹 요청을 처리하는 객체를 **핸들러(Handler)**라고 표현한다
    - **특정 요청 경로를 처리해주는 핸들러를 찾아주는 객체를 `HandlerMapping`이라 한다**
- `DispatcherServlet`은 실행 결과를 `ModelAndView` 타입으로만 받는다
  - 그런데 핸들러의 실제 구현 타입에 따라 `ModelAndView`가 아닌 결과를 리턴하기도 한다
  - 따라서 핸들러의 처리 결과를 `ModelAndView`로 변환해주는 객체 `HandlerAdapter`가 있다
- 핸들러 객체의 실제 타입마다 그에 알맞는 `HandlerMapping`, `HandlerApater`가 존재한다
  - 사용할 핸들러의 종류에 따라 해당 `HandlerMapping`, `HandlerApater`를 스프링 빈으로 등록해야 된다.
  - BUT **스프링에 제공하는 설정 기능을 사용하면 등록 안해도 된다**



## 2. DispatcherServlet과 스프링 컨테이너

- 9장의 web.xml 파일에서 `DispatcherServlet`의 `contextConfiguration` 초기화 파라미터를 이용해서 스프링 설정 클래스 목록을 전달했다

  ```xml
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
              <param-name>contextConfigLocation</param-name>			/* 초기화 파라미터
  			<param-value>
  				config.MvcConfig
  				config.ControllerConfig
  			</param-value>
  		</init-param>
  		<load-on-startup>1</load-on-startup>
  	</servlet>
  ```

  - `DispatcherServlet`은 **전달받은 설정 파일을 이용해서 스프링 컨테이너를 생성한다**
    - `HandlerMapping`, `HandlerAdapter`, `컨트롤러`, `ViewResolver` 등의 빈은 이 스프링 컨테이너에서 구한다
    - 즉 `DispatcherServlet`이 사용하는 설정 파일에 이 빈들에 대한 정의가 포함되어야 한다



## 3. @Controller를 위한 HandlerMapping과 HandlerAdapter

- `DispatcherServlet` 입장에서 **`@Controller`이 적용된 객체는 한 종류의 핸들러 객체**이다
  - `HandlerMapping` : 브라우저의 요청을 처리할 핸들러 객체를 찾기 위해 사용
  - `HandlerAdapter` : 핸들러를 실행하기 위해 사용
- `DispatcherServlet`는 스프링 컨테이너에서 위 두 타입의 빈을 사용하므로 **스프링 설정에 등록되어 있어야 한다**
  - 그런데 **설정 클래스에 안보인다...** `@EnableWebMvc` 애노테이션만 추가했다

- `@EnableWebMvc`가 매우 다양한 스프링 빈 설정을 추가해준다

  > 추가하는 것중에 `@Controller` 타입 핸들러 객체 처리를 위한
  > org.spring.....annotation.RequestMappingHandlerMapping
  > org.spring.....annotation.RequestMappingHandlerAdapter도 포함되어 있다
  >
  > - `RequestMappingHandlerMapping`는 `@Controller`이 적용된 객체의 요청 매핑 애노테이션(`@GetMapping`) 값을 이용해서 웹 브라우저 요청을 처리할 컨트롤러 빈을 찾는다
  > - `RequestMappingHandlerAdapter`는 컨트롤러의 메서드를 알맞게 실행하고, 그 결과를 `ModelAndView` 객체로 변환해서 `Dispatcherservlet`에 리턴한다

- 9장 `HelloController` 클래스

  ```java
  @Controller
  public class HelloController {
  
  	@GetMapping("/hello")
  	public String hello(Model model,
  			@RequestParam(value = "name", required = false) String name
  			) {
  		
  		model.addAttribute("greeting", "안녕하세요, " + name);
  		return "hello";
  	}
  }
  ```

  > `RequestMappingHandlerAdapter` 클래스가 "/hello" 요청 경로에 대해 `hello()` 메서드를 실행한다
  >
  > - 이때 Model 객체 생성해 파라미터1로, HTTP 요청 파라미터의 값을 파라미터2로 전달한다
  > - 컨트롤러 메서드 결과값이 String 타입이면해당 값을 뷰 이름으로 갖는 `ModelAndView` 객체를 생성해서 `DispatcherServlet`에 리턴한다
  >   - 파라미터로 전달한 `Model` 객체에 보관된 값도 `ModelAndView`에 함께 전달
  >
  > hello를 뷰 이름으로 사용



## 4. WebMvcConfigurer 인터페이스와 설정

- `@EnableWebMvc`를 사용하면 `@Controller`를 붙인 컨트롤러를 위한 설정을 생성한다

  - 또한 `WebMvcConfigurer` 타입의 빈을 이용해서 MVC 설정을 추가로 생성한다

  ```java
  @Configuration
  @EnableWebMvc
  public class MvcConfig implements WebMvcConfigurer{		//`WebMvcConfigurer` 상속
  
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

  - 설정 클래스가 `WebMvcConfigurer` 인터페이스를 상속한다

    - **`@Configuration`을 붙인 클래스도 컨테이너 빈으로 등록되므로** `MvcConfig` 클래스는 `WebMvcConfigurer` 타입의 빈이 된다

  - `@EnableWebMvc`를 사용하면 **`WebMvcConfigurer` 타입인 빈 객체의 메서드를 호출해서 MVC 설정을 추가한다**

    > Ex. `ViewResolver` 설정을 추가하기위해 `WebMvcConfigurer` 타입인 빈 객체의 `configureViewResolver()` 메서드를 호출한다
    >
    > 따라서 `WebMvcConfigurer` 인터페이스를 구현한 설정 클래스는 `configureViewResolver()` 메서드를 재정의해서 알맞은 뷰 관련 설정을 추가하면 된다

- 스프링 5는 디폴트 메서드를 사용해서 `WebMvcConfigurer` 인터페이스의 메서드에 기본 구현을 제공한다

  - 이 인터페이스를 상속한 설정 클래스는 재정의가 필요한 메서드만 구현하면 된다



## 5. JSP를 위한 ViewResolver

- 컨트롤러 처리 결과를 JSP를 이용해 생성하기 위해 다음 설정을 사용한다

  ```java
  @Configuration
  @EnableWebMvc
  public class MvcConfig implements WebMvcConfigurer{
  
  	@Override
  	public void configureViewResolvers(ViewResolverRegistry registry) {
  		registry.jsp("/WEB-INF/view/", ".jsp");
  	}
  
  }
  ```

  - `WebMvcConfigurer`에 정의된 `configureViewResolvers`는 `ViewResolverRegistry` 타입의 `registry` 파라미터를 갖는다

    - `ViewResolverRegistry#jsp()` 메서드를 사용하면 JSP를 위한 `ViewResolver`를 설정할 수 있다

    - 이 설정은 `org.spring...view.InternalResourceViewResolver`클래스를 이용해 다음 설정과 같이 빈을 등록한다

      ```java
      @Bean
      public ViewResolver viewResolver(){
          InternalResourceViewResolver vr = new InternalResourceViewResolver();
          vr.setPrefix("/WEB-INF/view/");
          vr.setSuffix(".jsp");
          return vr;
      }
      ```

- 컨트롤러의 실행 결과를 받은 `DispatcherServlet`은 `ViewResolver`에게 **뷰 이름에 해당하는 `View` 객체를 요청**한다

  - `InternalResourceViewResolver`는 "prefix + 뷰이름 + suffix"에 해당하는 경로를 뷰로 사용하는 `InternalResouceView` 타입의 `View` 객체를 리턴

    > 뷰이름이 hello라면 "/WEB-INF/view/hello.jsp" 경로를 뷰 코드로 사용하는 `InternalResourceView` 객체를 리턴한다
    >
    > `DispatcherServlet`이 응답 생성을 요청하면 `InternalResourceView` 객체는 경로에 지정한 JSP 코드를 실행해서 응답 결과를 생성한다

- `DispatcherServlet`은 컨트롤러의 실행 결과를 `HandlerAdapter`을 통해 `ModelAndView` 형태로 받는다

  - `Model`에 담긴 값을 `View` 객체에 Map형식으로 전달된다

  ```java
  @Controller
  public class HelloController {
  
  	@GetMapping("/hello")
  	public String hello(Model model,
  			@RequestParam(value = "name", required = false) String name
  			) {
  		
  		model.addAttribute("greeting", "안녕하세요, " + name);
  		return "hello";
  	}
  }
  ```

  - `Model`에 greeting 속성을 설정했다
  - `DispatcherServelt`은 `View`객체에 응답 생성을 요청할 때 greeting 키를 갖는 Map 객체를 `View` 객체에 전달한다
    - `View` 객체는 전달받은 Map 객체에 담긴 값을 이용해서 알맞은 응답 결과를 출력한다
    - `InternalResourceView`는 Map 객체에 담긴 키 값을 `request.setAttribute()`를 이용해서 `request` 속성에 저장한다
    - 그 뒤 해당 경로의 JSP를 실행한다

- 컨트롤러에서 지정한 `Model` 속성은 `request` 객체 속성으로 JSP에 전달되어 모델에서 지정한 속성 이름을 사용해 값을 사용할 수 있다



## 6. 디폴트 핸들러와 HandlerMapping의 우선순위

- web.xml의 `DispatcherServlet`에 대한 매핑 경로를 `'/'`로 줬다

  ```xml
  	<servlet>
  		<servlet-name>dispatcher</servlet-name>
  		<servlet-class>
  			org.springframework.web.servlet.DispatcherServlet
  		</servlet-class>
  		...
  	</servlet>
  
  	<servlet-mapping>
  		<servlet-name>dispatcher</servlet-name>
  		<url-pattern>/</url-pattern>			// '/'로 줬다
  	</servlet-mapping>
  ```

  - 매핑 경로가`'/'`인 경우 .jsp로 끝나는 요청을 제외한 모든 요청을 `DispatcherServelt`이 처리한다

    > /index.html, /css/bootstrap.css 등 확장자가 .jsp가 아닌 모든 요청을 처리

  > 그런데 `@EnableWebMvc`가 등록하는 `HandlerMapping`은 `@Controller`을 적용한 빈 객체가 처리할 수 있는 요청 경로만 대응할 수 있다
  >
  > Ex. `@GetMapping("/hello")` 설정을 사용하면 /hello 경로만 처리할 수 있게된다
  > 그러면 "/index.html"같은 요청을 처리할 수 있는 컨트롤러 객체를 찾지못해 `DispatcherServlet`은 404응답을 전송한다

  - `/index.html, /css/bootstrap.css` 같은 경로를 처리하기 위한 컨트롤러 객체를 직접 구현하기보단

    - `WebMvcConfigurer`의 `configureDefaultServletHandling()` 메서드 사용하는게 편하다

    ```java
    @Configuration
    @EnableWebMvc
    public class MvcConfig implements WebMvcConfigurer{
    
    	@Override
    	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
    		configurer.enable();
    	}
    }
    ```

  - `DefaultServletHandlerConfigurer#enable()` 메서드는 두개의 빈 객체를 추가한다

    - `DefaultServletHttpRequestHandler`
    - `SimpleUrlHandlerMapping`

  - `DefaultServletHttpRequestHandler`는 cli의 모든 요청을 WAS(웹 어플리케이션 서버, 톰캣, 웹로직 등)가 제공하는 디폴트 서블릿에 전달한다

    > "/index.html"에 대한 처리를 `DefaultServletHttpRequestHandler`에 요청하면, 디폴트 서블릿에 전달해서 처리하도록 한다
    >
    > 그리고 `SimpleUrlHandlerMapping`을 이용해 모든 경로("/**")를 `DefaultServletHttpRequestHandler`를 이용해 처리하도록 설정한다

- `@EnableWebMvc`이 등록하는 `RequestMappingHandlerMapping`의 적용 우선순위가 `DefaultServletHandlerConfigurer#enable()`메서드가 등록하는 `SimpleUrlHandlerMapping`의 우선순위보다 높다

  - 때문에 브라우저의 요청이 들어왔을때 `DispatcherServlet`의 처리 순서는

  1. `RequestMappingHandlerMapping`을 사용해서 요청을 처리할 핸들러를 검색한다
     - 존재하면 해당 컨트롤러를 이용해서 요청을 처리한다
  2. 존재하지 않으면 `SimpleUrlHandlerMapping`을 사용해서 요청을 처리할 핸들러를 검색한다.
     - `DefaultServletHandlerConfigurer#enable()` 메서드가 등록한 `SimpleUrlHandlerMapping`은 "/**"(모든 경로)경로에 대해 `DefaultServletHttpRequestHandler`를 리턴한다
     - `DispatcherServelt`은 `DefaultServletHttpRequestHandler`에 처리를 요청한다
     - `DefaultServletHttpRequestHandler`는 디폴트 서블릿에 처리를 위임한다

  > Ex. "/index.html" 경로로 요청이 들어오면 들어오면 1번 과정에서 해당하는 컨트롤러를 찾지 못하므로 2번 과정을 통해 디폴트 서블릿이 /index.html 요청을 처리하게 된다



## 7. 직접 설정 예

- `@EnableWebMvc` 애노테이션을 사용하지 않아도 스프링 MVC를 사용할 수 있다

  - 차이는 `@EnableWebMvc` 애노테이션과 `WebMvcConfigurer` 인터페이스를 사용할 때보다 설정해야 할 빈이 많은 것뿐이다

  ```java
  @Configuration
  public class MvcConfig {
  
  	@Bean
  	public HandlerMapping handlerMapping() {
  		RequestMappingHandlerMapping hm = new RequestMappingHandlerMapping();
  		hm.setOrder(0);
  		return hm;
  	}
  
  	@Bean
  	public HandlerAdapter handlerAdapter() {
  		RequestMappingHandlerAdapter ha = new RequestMappingHandlerAdapter();
  		return ha;
  	}
  
  	@Bean
  	public HandlerMapping simpleHandlerMapping() {
  		SimpleUrlHandlerMapping hm = new SimpleUrlHandlerMapping();
  		Map<String, Object> pathMap = new HashMap<>();
  		pathMap.put("/**", defaultServletHandler());
  		hm.setUrlMap(pathMap);
  		return hm;
  	}
  
  	@Bean
  	public HttpRequestHandler defaultServletHandler() {
  		DefaultServletHttpRequestHandler handler = new DefaultServletHttpRequestHandler();
  		return handler;
  	}
  
  	@Bean
  	public HandlerAdapter requestHandlerAdapter() {
  		HttpRequestHandlerAdapter ha = new HttpRequestHandlerAdapter();
  		return ha;
  	}
  
  	@Bean
  	public ViewResolver viewResolver() {
  		InternalResourceViewResolver vr = new InternalResourceViewResolver();
  		vr.setPrefix("/WEB-INF/view/");
  		vr.setSuffix(".jsp");
  		return vr;
  	}
  
  }
  ```

  

## 8. 정리

- `DispatcherServlet`은 웹 브라우저의 요청을 받기 위한 창구 역할을 하고, 다른 주요 구성 요소들을 이용해서 요청 흐름을 제어하는 역할을 한다.
- `HandlerMapping`은 클라이언트의 요청을 처리할 핸들러 객체를 찾아준다.
- 핸들러(커맨드) 객체는 클라이언트의 요청을 실제로 처리한 뒤 뷰 정보와 모델을 설정한다.
- `HandlerAdapter`는 `DispatcherServlet`과 핸들러 객체 사이의 변환을 알맞게 처리해 준다.
- `ViewResolver`는 요청 처리 결과를 생성할 `View` 객체를 찾아주고 `View`는 최종적으로 클라이언트에 응답을 생성해서 전달한다