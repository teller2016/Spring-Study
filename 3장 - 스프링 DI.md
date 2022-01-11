# 3장 - 스프링 DI

> - 객체 의존과 의존 주입(DI)
> - 객체 조립
> - 스프링 DI 설정

## 1. 의존이란?

- DI(Dependency Injections): 의존 주입

  - 의존: 객체 간의 의존을 의미

- 이해를 위한 회원 가입 처리 기능 코드

  ```java
  import java.time.LocalDateTime;
  
  public class MemberRegisterService {
  	private MemberDao memberDao = new MemberDao();
  	
  	public void regist(RegisterRequest req) {
  		// 이메일로 회원 데이터(Member) 조회
  		Member member = memberDao.selectByEmail(req.getEmail());
  		
  		if(member != null) {
  			// 같은 이메일을 가진 회원이 이미 존재하면 익셉션 발생
  			throw new DuplicateMemberException("dup email " + req.getEmail());
  		}
  		
  		//같은 이메일을 가진 회원이 존재하지 않으면 DB 삽입
  		Member newMember = new Member(req.getEmail(), req.getPassword(), req.getName(), LocalDateTime.now());
  		memberDao.insert(newMember);
  	}
  }
  ```

  - 서로 다른 회원은 동일 이메일 주소를 사용할 수 없다는 요구사항 가정
    - 같은 메일 있으면 익셉션 발생시킨다
    - 없으면 새로운 Member 객체 생성 후 DB에 데이터 삽입
  - `MemberRegisterService` 클래스가 DB 처리를 위해 `MemberDao` 클래스의 메서드를 사용한다
    - Ex. `selectByEmail()`, `insert()`
    - **이렇게 한 클래스가 다른 클래스의 메서드를 실행할 때 이를 `의존`한다고 표현**
    - **`MemberRegisterService` 클래스가 `MemberDao` 클래스에 의존한다**

> 의존은 변경에 의해 영향을 받는 관계 의미
>
> Ex. `MemberDao`의 `insert()` 메서드 이름을 변경하면 이 메서드를 사용하는 `MemberRegisterService` 클래스의 소스 코드도 변경된다

- 의존하는 대상을 구하는 방법

  - 가장 쉬운 방법은 대상 객체를 직접 호출하는 것이다

    ```java
    private MemberDao memberDao = new MemberDao();
    ```

    - `MemberRegisterService` 객체를 생성하면 `MemberDao` 객체도 함께 생성된다
    - 직접 의존 객체를 생성하는 것이 쉽지만 유지보수 관점에서 **문제 발생 가능**



## 2. DI를 통한 의존 처리

- DI는 의존하는 객체를 직접 생성하는 대신 **의존 객체를 전달받는 방식을 사용한다**

- 위 예시에 DI 방식 적용

  ```java
  package spring;
  
  import java.time.LocalDateTime;
  
  public class MemberRegisterService {
  	private MemberDao memberDao;
  	
  	
  	public MemberRegisterService(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  	
  	public Long regist(RegisterRequest req) {
  		// 이메일로 회원 데이터(Member) 조회
  		Member member = memberDao.selectByEmail(req.getEmail());
  		
  		if(member != null) {
  			// 같은 이메일을 가진 회원이 이미 존재하면 익셉션 발생
  			throw new DuplicateMemberException("dup email " + req.getEmail());
  		}
  		
  		//같은 이메일을 가진 회원이 존재하지 않으면 DB 삽입
  		Member newMember = new Member(req.getEmail(), req.getPassword(), req.getName(), LocalDateTime.now());
  		memberDao.insert(newMember);
  		
  		return newMember.getId();
  	}
  }
  ```

  - 직접 의존 객체를 생성하지 않는다
  - **생성자를 통해 의존 객체를 전달 받는다**
    - 생성자를 통해 `MemberRegisterService`가 의존하는 `MemberDao` **객체를 주입(Injection) 받은 거다**
  - **즉 DI 패턴을 따르고 있다**

- DI를 적용했을때 객체 생성

  ```java
  MemberDao dao = new MemberDao();
  // 의존 객체를 생성자를 통해 주입한다
  MemberRegisterService svc = new MemberRegisterService(dao);
  ```

  - DI를 하는 이유는 변경의 유연함 때문이다



## 3. DI와 의존 객체 변경의 유연함

- 의존 객체를 직접 생성하는 방식은 필드나 생성자에서 new 연산자를 통해 객체를 생성한다

  - 회원 등록 기능 `MemberRegisterService` 클래스

    ```java
    public class MemberRegisterService {
    	private MemberDao memberDao = new MemberDao();
    	...
    }
    ```

  - 회원 암호 변경 기능 `ChangePasswordService` 클래스

    ```java
    public class ChangePasswordService {
    	private MemberDao memberDao = new MemberDao();
    	...
    }
    ```

  > `MemberDao` 클래스가 회원 데이터를 DB에 저장한다 가정하자
  >
  > 빠른 조회를 위해 캐시 기능을 추가한다고 하자
  >
  > ```java
  > public class CachedMemberDao extends MemberDao{
  >     ...
  > }
  > ```

  - `CachedMemberDao`를 사용하려면 `MemberRegisterService`, `ChangePasswordService` 클래스 코드를 다음과 같이 둘다 변경해줘야 된다

    ```java
    private MemberDao memberDao = new CachedMemberDao();	//객체 생성 MemberDao에서 CachedMemberDao로 변경
    ```



- 이 상황에서 DI를 사용하면 수정할 코드가 줄어든다

  ```java
  // 생성자를 통해 객체 주입
  public class MemberRegisterService {
  	private MemberDao memberDao;
  	
  	public MemberRegisterService(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  	...
  }
  //////////////////////////////////////////////////////////////////////
  public class ChangePasswordService {
  	private MemberDao memberDao;
  	
  	public ChangePasswordService(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  	...
  }
  ```

  - 두 클래스의 객체 생성 코드는

    ```java
    MemberDao memberDao = new MemberDao();
    MemberRegisterService regSvc = new MemberRegisterService(memberDao);
    ChangePasswordService pwdSvc = new ChangePasswordService(memberDao);
    ```

    - **실제 객체를 생성하는 한 곳만 수정하면 된다**

    ```java
    MemberDao memberDao = new CachedMemberDao();
    MemberRegisterService regSvc = new MemberRegisterService(memberDao);
    ChangePasswordService pwdSvc = new ChangePasswordService(memberDao);
    ```

    - DI를 사용하면 `MemberDao`에 의존하는 클래스가 여러개여도 의존 주입 대상이 되는 객체 생성하는 코드만 바꿔주면 된다



## 4. 예제 프로젝트 만들기

> - sp5-chap03 프로젝트 폴더를 생성한다
> - 프로젝트 하위 폴더로 src/main/java를 생성한다
> - sp5-chap03 폴더에 pom.xml 파일을 작성한다
>   - <artifactId> 값을 sp5-chap03으로 설정한다

> - 회원 데이터 관련 클래스
>   - Member
>   - WrongIdPasswordException
>   - MemberDao
> - 회원 가입 처리 관련 클래스
>   - DuplicateMemberException
>   - RegisterRequest
>   - MemberRegisterService
> - 암호 변경 관련 클래스
>   - MemberNotFoundException
>   - ChangePasswordService

### 4.1 회원 데이터 관련 클래스

- `Member` 클래스 : 회원 데이터를 표현하기 위해 사용

  ```java
  package spring;
  
  import java.time.LocalDateTime;
  
  public class Member {
  	private Long id;
  	private String email;
  	private String password;
  	private String name;
  	private LocalDateTime registerDateTime;
  	
  	public Member(String email, String password, String name, LocalDateTime registerDateTime) {
  		this.email = email;
  		this.password = password;
  		this.name = name;
  		this.registerDateTime = registerDateTime;
  	}
  
  	public Long getId() {
  		return id;
  	}
  
  	public void setId(Long id) {
  		this.id = id;
  	}
  
  	public String getEmail() {
  		return email;
  	}
  
  	public String getPassword() {
  		return password;
  	}
  
  	public String getName() {
  		return name;
  	}
  
  	public LocalDateTime getRegisterDateTime() {
  		return registerDateTime;
  	}
  	
  	public void changePassword(String oldPassword, String newPassword) {
  		if (!password.equals(oldPassword))
  			throw new WrongIdPasswordException();
  		this.password = newPassword;
  	}
  }
  ```

  - `oldPassword`가 현재 암호인 `password` 필드와 값이 다르면 `WrongIdPasswordException`을 발생시킨다

  - `WrongIdPasswordException` 클래스

    ```java
    package spring;
    
    public class WrongIdPasswordException extends RuntimeException {
    
    }
    ```

- `MemberDao` 클래스: DB 연동을 몰라 자바의 Map을 이용해 구현했다

  ```java
  package spring;
  
  import java.util.HashMap;
  import java.util.Map;
  
  public class MemberDao {
  	private static long nextId = 0;
  	
  	private Map<String, Member> map = new HashMap<>();
  	
  	public Member selectByEmail(String email) {
  		return map.get(email);
  	}
  	
  	public void insert(Member member) {
  		member.setId(++nextId);
  		map.put(member.getEmail(), member);
  	}
  	
  	public void update(Member member) {
  		map.put(member.getEmail(), member);
  	}
  }
  ```

### 4.2 회원 가입 처리 관련 클래스

> - 회원 가입 처리 관련 클래스
>
>   - DuplicateMemberException
>
>   - RegisterRequest
>
>   - MemberRegisterService

- `DuplicateMemberException` 클래스

  - 동일한 이메일 갖고 있는 회원이 이미 존재할 때  `MemberRegisterService`가 발생시키는 익셉션 타입

  ```java
  package spring;
  
  public class DuplicateMemberException extends RuntimeException {
  
  	public DuplicateMemberException(String message) {
  		super(message);
  	}
  	
  }
  ```

- `RegisterRequest` 클래스

  - 회원 가입을 처리할 때 필요한 이메일, 암호, 이름 데이터를 담고 있는 클래스

  ```java
  package spring;
  
  public class RegisterRequest {
  	private String email;
  	private String password;
  	private String confirmPassword;
  	private String name;
  	public String getEmail() {
  		return email;
  	}
  	public void setEmail(String email) {
  		this.email = email;
  	}
  	public String getPassword() {
  		return password;
  	}
  	public void setPassword(String password) {
  		this.password = password;
  	}
  	public String getConfirmPassword() {
  		return confirmPassword;
  	}
  	public void setConfirmPassword(String confirmPassword) {
  		this.confirmPassword = confirmPassword;
  	}
  	public String getName() {
  		return name;
  	}
  	public void setName(String name) {
  		this.name = name;
  	}
  	
  	public boolean isPasswordEqualToConfirmPassword() {
  		return password.equals(confirmPassword);
  	}
  }
  ```

- `MemberRegisterService` 클래스

  ```java
  package spring;
  
  import java.time.LocalDateTime;
  
  public class MemberRegisterService {
  	private MemberDao memberDao;
  	
  	
  	public MemberRegisterService(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  	
  	public Long regist(RegisterRequest req) {
  		// 이메일로 회원 데이터(Member) 조회
  		Member member = memberDao.selectByEmail(req.getEmail());
  		
  		if(member != null) {
  			// 같은 이메일을 가진 회원이 이미 존재하면 익셉션 발생
  			throw new DuplicateMemberException("dup email " + req.getEmail());
  		}
  		
  		//같은 이메일을 가진 회원이 존재하지 않으면 DB 삽입
  		Member newMember = new Member(req.getEmail(), req.getPassword(), req.getName(), LocalDateTime.now());
  		memberDao.insert(newMember);
  		
  		return newMember.getId();
  	}
  }
  ```

### 4.3 암호 변경 관련 클래스

> - 암호 변경 관련 클래스
>
>   - MemberNotFoundException
>
>   - ChangePasswordService

- `ChangePasswordService` 클래스

  - 암호를 변경할 Member 데이터를 찾기 위해 email을 사용
    - email 없으면 익셉션 발생
  - `setMemberDao()` 메서드로 의존하는 `MemberDao`를 전달받는다

  ```java
  package spring;
  
  public class ChangePasswordService {
  	private MemberDao memberDao;
  	
  	public void changePassword(String email, String oldPwd, String newPwd) {
  		Member member = memberDao.selectByEmail(email);
  		
  		if(member == null)
  			throw new MemberNotFoundException();
  		
  		member.changePassword(oldPwd, newPwd);	// 암호 불일치 경우 Member 클래스에서 익셉션 발생시킴
  		
  		memberDao.update(member);
  	}
  	
  	public void setMemberDao(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  }
  ```

- `MemberNotFoundException` 클래스

  - `ChangePasswordService`가 이메일에 해당하는 Member가 존재하지 않을 경우 발생시키는 클래스

  ```java
  package spring;
  
  public class MemberNotFoundException extends RuntimeException {
  	
  }
  ```



## 5. 객체 조립기

- 실제 객체를 생성할 코드는 메인 메서드에서 객체를 생성하면 되나?

  ```java
  public class Main{
      public static void main(String[] args){
          MemberDao memberDao = new MemberDao();
          MemberRegisterService regSvc = new MemberRegisterService(memberDao);
          ChangePasswordService pwdSvc = new ChangePasswordService();
          pwdSvc.setMemberDao(memberDao);
          ... // regSvc와 pwdSvc 사용 코드
      }
  }
  ```

  - 이 방법보다는 **객체를 생성하고 의존 객체를 주입하는 클래스를 따로 작성하는게 좋다**
  - 의존 객체 주입 => 서로 다른 두 객체를 조립하는 것이므로 이 클래스를 **조립기**라고 표현한다

- `Assembler` 클래스

  - 회원 가입, 암호 변경 클래스에 의존 대상 객체를 주입해주는 클래스

  ```java
  package assembler;
  
  import spring.ChangePasswordService;
  import spring.MemberDao;
  import spring.MemberRegisterService;
  
  public class Assembler {
  	private MemberDao memberDao;
  	private MemberRegisterService regSvc;
  	private ChangePasswordService pwdSvc;
  	
  	public Assembler() {
  		memberDao = new MemberDao();
  		regSvc = new MemberRegisterService(memberDao);
  		ChangePasswordService pwdSvc = new ChangePasswordService();
          pwdSvc.setMemberDao(memberDao);
  	}
  
  	public MemberDao getMemberDao() {
  		return memberDao;
  	}
  
  	public MemberRegisterService getRegSvc() {
  		return regSvc;
  	}
  
  	public ChangePasswordService getPwdSvc() {
  		return pwdSvc;
  	}
  }
  ```

  - Assembler 객체 사용

    ```java
    Assembler assembler = new Assembler();
    ChangePasswordService changePwdSvc = assembler.getChangePasswordService();
    
    changePwdSvc.changePassword("molly@naver.com", "1234", "newpwd");
    ```

  - 의존 객체를 변경하려면...

    - Assembler에서 객체 초기화 코드만 바꾸면 된다

    ```java
    public Assembler() {
    		memberDao = new CachedMemberDao();	//변경
    		regSvc = new MemberRegisterService(memberDao);
    		ChangePasswordService pwdSvc = new ChangePasswordService();
            pwdSvc.setMemberDao(memberDao);
    	}
    ```

### 5.1 조립기 사용 예제

> Main 클래스는 콘솔에서 명령어를 입력받아 기능을 수행한다
>
> - new : 새로운 회원 데이터를 추가한다
> - change : 회원 데이터의 암호를 변경한다
>   - `MemberRegisterService`, `ChangePasswordService`를 사용하여 처리

```java
package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import assembler.Assembler;
import spring.ChangePasswordService;
import spring.DuplicateMemberException;
import spring.MemberNotFoundException;
import spring.MemberRegisterService;
import spring.RegisterRequest;
import spring.WrongIdPasswordException;

public class MainForAssembler {

	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		while (true) {
			System.out.println("명령어를 입력하세요:");
			String command = reader.readLine();		//콘솔에서 한 줄을 입력받음
			
			if (command.equalsIgnoreCase("exit")) {
				System.out.println("종료합니다.");
				break;
			}
			
			if (command.startsWith("new ")) {
				processNewCommand(command.split(" "));		// ex."new a@a.com 이름 암호 암호"
				continue;
			} else if (command.startsWith("change ")) {
				processChangeCommand(command.split(" "));
				continue;
			}
			printHelp();
		}
	}
	
	private static Assembler assembler = new Assembler();	//Assembler 생성자를 통해 MemberDao 의존 객체를 MemberRegisterService, ChangePasswordService에 주입했다

	private static void processNewCommand(String[] arg) {	//새로운 회원 정보를 생성한다
		if (arg.length != 5) {
			printHelp();
			return;
		}
		
		MemberRegisterService regSvc = assembler.getMemberRegisterService();
		RegisterRequest req = new RegisterRequest();
		
		req.setEmail(arg[1]);
		req.setName(arg[2]);
		req.setPassword(arg[3]);
		req.setConfirmPassword(arg[4]);
		
		if (!req.isPasswordEqualToConfirmPassword()) {
			System.out.println("암호와 확인이 일치하지 않습니다.\n");
			return;
		}
		
		try {
			regSvc.regist(req);
			System.out.println("등록했습니다.\n");
		} catch (DuplicateMemberException e) {
			System.out.println("이미 존재하는 이메일입니다.\n");
		}
	}
	
	private static void processChangeCommand(String[] arg) {
		if (arg.length != 4) {
			printHelp();
			return;
		}
        
		ChangePasswordService changePwdSvc = assembler.getChangePasswordService();
        
		try {
			changePwdSvc.changePassword(arg[1], arg[2], arg[3]);
			System.out.println("암호를 변경했습니다.\n");
		} catch (MemberNotFoundException e) {
			System.out.println("존재하지 않는 이메일입니다.\n");
		} catch (WrongIdPasswordException e) {
			System.out.println("이메일과 암호가 일치하지 않습니다.\n");
		}
	}

	private static void printHelp() {
		System.out.println();
		System.out.println("잘못된 명령입니다. 아래 명령어 사용법을 확인하세요.");
		System.out.println("명령어 사용법:");
		System.out.println("new 이메일 이름 암호 암호확인");
		System.out.println("change 이메일 현재비번 변경비번");
		System.out.println();
	}
}
/*
명령어를 입력하세요:
new molly@naver.com 몰리 1234 1234
등록했습니다.

명령어를 입력하세요:
change molly@naver.com 1234 4321
암호를 변경했습니다.
```

- `Assembler` 클래스의 생성자에서 필요한 객체를 생성하고 의존을 주입한다

  ```java
  MemberRegisterService regSvc = assembler.getMemberRegisterService();
  ...
  ChangePasswordService changePwdSvc = assembler.getChangePasswordService();
  ```



## 6. 스프링의 DI 설정

- 스프링은 DI를 지원하는 조립기이다
  - 앞서 구현한 조립기와 비슷한 기능을 제공한다
  - 스프링은 `Assembler` 클래스의 생성자 코드처럼 필요한 객체를 생성하고 생성한 객체에 의존을 주입한다
  - `Assembler#getMemberRegisterService()` 메서드 처럼 객체를 제공하는 기능을 정의한다

### 6.1 스프링을 이용한 객체 조립과 사용

- `Assembler` 대신 스프링을 사용하는 코드 작성

  - **스프링이 어떤 객체를 생성하고, 의존을 어떻게 주입할지 정의한 설정 정보를 작성해야 된다**

  - 설정 코드

    ```java
    package config;
    
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    
    import spring.ChangePasswordService;
    import spring.MemberDao;
    import spring.MemberRegisterService;
    
    @Configuration
    public class AppCtx {
    	
    	@Bean
    	public MemberDao memberDao() {
    		return new MemberDao();
    	}
    	
    	@Bean
    	public MemberRegisterService memberRegSvc() {
    		return new MemberRegisterService(memberDao());
    	}
    	
    	@Bean
    	public ChangePasswordService changePwdSvc() {
    		ChangePasswordService pwdSvc = new ChangePasswordService();
    		pwdSvc.setMemberDao(memberDao());
    		return pwdSvc;
    	}
    }
    ```

    - `@Configuration` 애노테이션은 스프링 설정 클래스를 의미
    - `@Bean` 애노테이션은 해당 메서드가 생성한 객체를 스프링 빈이라고 설정한다
      - 각 메서드마다 한 개의 빈 객체를 생성
      - 메서드 이름을 빈 객체의 이름으로 사용한다

  - 설정 클래스를 이용해서 컨테이너를 생성해야 된다

    ```java
    ApplicationContext ctx = new AnnotationConfigApplicationContext(AppCtx.class);
    ```

  - 컨테이너 생성 후 `getBean()` 메서드를 통해 객체를 구할 수 있다

    ```java
    MemberRegisterService regSvc = ctx.getBean("memberRegSvc", MemberRegisterService.class);
    ```

    - 스프링 컨테이너 `ctx`부터 이름이 `memberRegSvc`인 빈 객체를 구한다

- `MainForSpring` 클래스

  ```java
  package main;
  
  import java.io.BufferedReader;
  import java.io.IOException;
  import java.io.InputStreamReader;
  
  import org.springframework.context.ApplicationContext;
  import org.springframework.context.annotation.AnnotationConfigApplicationContext;
  
  import assembler.Assembler;
  import config.AppCtx;
  import spring.ChangePasswordService;
  import spring.DuplicateMemberException;
  import spring.MemberNotFoundException;
  import spring.MemberRegisterService;
  import spring.RegisterRequest;
  import spring.WrongIdPasswordException;
  
  public class MainForSpring {
  
  	private static ApplicationContext ctx = null;
  	
  	public static void main(String[] args) throws IOException {
  		ctx = new AnnotationConfigApplicationContext(AppCtx.class);		//추가
  		
  		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
  		
  		while (true) {
  			System.out.println("명령어를 입력하세요:");
  			String command = reader.readLine();	
  			
  			if (command.equalsIgnoreCase("exit")) {
  				System.out.println("종료합니다.");
  				break;
  			}
  			
  			if (command.startsWith("new ")) {
  				processNewCommand(command.split(" "));	
  				continue;
  			} else if (command.startsWith("change ")) {
  				processChangeCommand(command.split(" "));
  				continue;
  			}
  			printHelp();
  		}
  	}
  
  	private static void processNewCommand(String[] arg) {
  		if (arg.length != 5) {
  			printHelp();
  			return;
  		}
  		
  		//MemberRegisterService regSvc = assembler.getMemberRegisterService();
  		MemberRegisterService regSvc = ctx.getBean("memberRegSvc", MemberRegisterService.class);
  		RegisterRequest req = new RegisterRequest();
  		
  		req.setEmail(arg[1]);
  		req.setName(arg[2]);
  		req.setPassword(arg[3]);
  		req.setConfirmPassword(arg[4]);
  		
  		if (!req.isPasswordEqualToConfirmPassword()) {
  			System.out.println("암호와 확인이 일치하지 않습니다.\n");
  			return;
  		}
  		
  		try {
  			regSvc.regist(req);
  			System.out.println("등록했습니다.\n");
  		} catch (DuplicateMemberException e) {
  			System.out.println("이미 존재하는 이메일입니다.\n");
  		}
  	}
  	
  	private static void processChangeCommand(String[] arg) {
  		if (arg.length != 4) {
  			printHelp();
  			return;
  		}
  		
  		//ChangePasswordService changePwdSvc = assembler.getChangePasswordService();
  		ChangePasswordService changePwdSvc = ctx.getBean("changePwdSvc", ChangePasswordService.class);
  		
  		try {
  			changePwdSvc.changePassword(arg[1], arg[2], arg[3]);
  			System.out.println("암호를 변경했습니다.\n");
  		} catch (MemberNotFoundException e) {
  			System.out.println("존재하지 않는 이메일입니다.\n");
  		} catch (WrongIdPasswordException e) {
  			System.out.println("이메일과 암호가 일치하지 않습니다.\n");
  		}
  	}
  
  	private static void printHelp() {
  		System.out.println();
  		System.out.println("잘못된 명령입니다. 아래 명령어 사용법을 확인하세요.");
  		System.out.println("명령어 사용법:");
  		System.out.println("new 이메일 이름 암호 암호확인");
  		System.out.println("change 이메일 현재비번 변경비번");
  		System.out.println();
  	}
  }
  ```

  - **`Assembler` 클래스 대신 스프링 컨테이너인 `ApplicationContext`를 사용했다**

### 6.2 DI 방식 1 : 생성자 방식

- `MemberRegisterService` 클래스는 생성자를 통해 의존 객체를 주입 받았다

  ```java
  public class MemberRegisterService {
  	private MemberDao memberDao;
  	
  	// 생성자를 통해 의존 객체 주입 받음
  	public MemberRegisterService(MemberDao memberDao) {
  		this.memberDao = memberDao;
  	}
  	
  	public Long regist(RegisterRequest req) {
          // 주입 받은 의존 객체의 메서드를 사용
  		Member member = memberDao.selectByEmail(req.getEmail());
  		
  		...
  		Member newMember = new Member(req.getEmail(), req.getPassword(), req.getName(), LocalDateTime.now());
  		memberDao.insert(newMember);
  		
  		return newMember.getId();
  	}
  }
  ```

  - 스프링 자바 설정에서 생성자를 통해 객체를 주입한다

    ```java
    @Configuration
    public class AppCtx {
    	
    	@Bean
    	public MemberDao memberDao() {
    		return new MemberDao();
    	}
    	
    	@Bean
    	public MemberRegisterService memberRegSvc() {
    		return new MemberRegisterService(memberDao());	//생성자로 주입
    	}
    	
    	...
    }
    ```

- 코드 추가

  ```java
  public class MemberDao {
  	...
  	
  	public Collection<Member> selectAll(){
  		return map.values();
  	}
  }
  ```

- `MemberPrinter` 클래스

  ```java
  package spring;
  
  public class MemberPrinter {
  	public void print(Member member) {
  		System.out.printf(
  				"회원 정보: 아이디=%d, 이메일=%s, 이름=%s, 등록일=%tF\n", 
  				member.getId(), member.getEmail(),
  				member.getName(), member.getRegisterDateTime());
  	}
  }
  ```

- 생성자로 두 개의 파라미터를 전달받는 클래스 `MemberListPrinter`

  ```java
  public class MemberListPrinter {
  	private MemberDao memberDao;
  	private MemberPrinter printer;
  	
  	public MemberListPrinter(MemberDao memberDao, MemberPrinter printer) {
  		this.memberDao = memberDao;
  		this.printer = printer;
  	}
  	
  	public void printAll() {
  		Collection<Member> members = memberDao.selectAll();
  		members.forEach(m -> printer.print(m));
  	}
  }
  ```

  - 의존 객체 주입

    ```java
    @Configuration
    public class AppCtx {
    	
    	@Bean
    	public MemberDao memberDao() {
    		return new MemberDao();
    	}
    	
    	...
    	
    	@Bean
    	public MemberPrinter memberPrinter() {
    		return new MemberPrinter();
    	}
    	
    	@Bean
    	public MemberListPrinter listPrinter() {
    		return new MemberListPrinter(memberDao(), memberPrinter());	// 의존 객체 주입
    	}
    }
    ```

- `MainForSpring` 클래스

  ```java
  public class MainForSpring {
  
  	private static ApplicationContext ctx = null;
  	
  	public static void main(String[] args) throws IOException {
  		ctx = new AnnotationConfigApplicationContext(AppCtx.class);	
          
  		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
  		
  		while (true) {
  			System.out.println("명령어를 입력하세요:");
  			String command = reader.readLine();	
  			
  			if (command.equalsIgnoreCase("exit")) {
  				System.out.println("종료합니다.");
  				break;
  			}
  			
  			if (command.startsWith("new ")) {
  				processNewCommand(command.split(" "));	
  				continue;
  			} else if (command.startsWith("change ")) {
  				processChangeCommand(command.split(" "));
  				continue;
  			} else if (command.equals("list")) {		//**************추가
  				processListCommand();
  				continue;
  			}
  			printHelp();
  		}
  	}
  	
  	...
  	
  	private static void processListCommand() {			//****************추가
  		MemberListPrinter listPrinter = ctx.getBean("listPrinter", MemberListPrinter.class);
  		listPrinter.printAll();
  	}
  	...
  }
  /*
  명령어를 입력하세요:
  new molly mol 123 123
  등록했습니다.
  
  명령어를 입력하세요:
  new bori bor 1234 1234
  등록했습니다.
  
  명령어를 입력하세요:
  list
  회원 정보: 아이디=1, 이메일=molly, 이름=mol, 등록일=2022-01-11
  회원 정보: 아이디=2, 이메일=bori, 이름=bor, 등록일=2022-01-11
  ```

### 6.3 DI 방식 2 : 세터(setter) 메서드 방식

- 생성자 방식 말고 **세터(setter) 메서드를 이용해서 객체를 주입받기도 한다**

> 일반적인 세터(setter) 메서드는 자바빈 규칙에 따라 작성한다
>
> - 메서드 이름이 set으로 시작한다
> - set 뒤에 첫 글자는 대문자로 시작한다
> - 파라미터가 1개이다
> - 리턴 타입이 void이다

- 세터 메서드를 이용해서 의존 객체 주입받는 코드

  ```java
  package spring;
  
  public class MemberInfoPrinter {
  	private MemberDao memDao;
  	private MemberPrinter printer;
  	
  	public void printMemberInfo(String email) {
  		Member member = memDao.selectByEmail(email);
  		
  		if(member==null) {
  			System.out.println("데이터 없음\n");
  			return;
  		}
  		printer.print(member);
  		System.out.println();
  	}
  
  	public void setMemDao(MemberDao memDao) {
  		this.memDao = memDao;
  	}
  
  	public void setPrinter(MemberPrinter printer) {
  		this.printer = printer;
  	}
  }
  ```

- `AppCtx` 클래스

  ```java
  @Configuration
  public class AppCtx {
  	...
  	@Bean
  	public MemberInfoPrinter infoPrinter() {
  		MemberInfoPrinter infoPrinter = new MemberInfoPrinter();
  		infoPrinter.setMemberDao(memberDao());
  		infoPrinter.setPrinter(memberPrinter());
  		return infoPrinter;
  	}
  }
  ```

- `MainForSpring` 클래스

  ```java
  package main;
  
  import java.io.BufferedReader;
  import java.io.IOException;
  import java.io.InputStreamReader;
  
  import org.springframework.context.ApplicationContext;
  import org.springframework.context.annotation.AnnotationConfigApplicationContext;
  
  import assembler.Assembler;
  import config.AppCtx;
  import spring.ChangePasswordService;
  import spring.DuplicateMemberException;
  import spring.MemberInfoPrinter;
  import spring.MemberListPrinter;
  import spring.MemberNotFoundException;
  import spring.MemberRegisterService;
  import spring.RegisterRequest;
  import spring.WrongIdPasswordException;
  
  public class MainForSpring {
  
  	private static ApplicationContext ctx = null;
  	
  	public static void main(String[] args) throws IOException {
  		ctx = new AnnotationConfigApplicationContext(AppCtx.class);
  		
  		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
  		
  		while (true) {
  			System.out.println("명령어를 입력하세요:");
  			String command = reader.readLine();	
  			
  			if (command.equalsIgnoreCase("exit")) {
  				System.out.println("종료합니다.");
  				break;
  			}
  			
  			if (command.startsWith("new ")) {
  				processNewCommand(command.split(" "));	
  				continue;
  			} else if (command.startsWith("change ")) {
  				processChangeCommand(command.split(" "));
  				continue;
  			} else if (command.equals("list")) {
  				processListCommand();
  				continue;
  			} else if (command.startsWith("info ")) {		//*******추가
  				processInfoCommand(command.split(" "));
  				continue;
  			}
  			printHelp();
  		}
  	}
  	...
  	private static void processInfoCommand(String[] arg) {			//*******추가
  		if(arg.length != 2) {
  			printHelp();
  			return;
  		}
  		MemberInfoPrinter infoPrinter = ctx.getBean("infoPrinter", MemberInfoPrinter.class);
  		infoPrinter.printMemberInfo(arg[1]);
  	}
  
  }
  
  /*
  명령어를 입력하세요:
  new molly@naver.com molly 1234 1234
  등록했습니다.
  
  명령어를 입력하세요:
  info molly@naver.com
  회원 정보: 아이디=1, 이메일=molly@naver.com, 이름=molly, 등록일=2022-01-11
  ```

> 생성자 vs 세터 메서드
>
> - 생성자 방식 : 빈 객체를 생성하는 시점에 모든 의존 객체가 주입된다
>   - 생성자 파라미터 많을 경우 어떤 의존 객체 인지 코드 확인해야 되는 단점 존재
> - 설정 메서드 방식 : 세터 메서드 이름을 통해 어떤 의존 객체가 주입되는지 알 수 있다

### 6.4 기본 데이터 타입 값 설정

- 두 개의 int 타입 값을 세터 메서드로 전달받는 코드

  ```java
  package spring;
  
  public class VersionPrinter {
  	private int majorVersion;
  	private int minorVersion;
  	
  	public void print() {
  		System.out.printf("이 프로그램의 버전은 %d.%d입니다.\n\n", majorVersion, minorVersion);
  	}
  
  	public void setMajorVersion(int majorVersion) {
  		this.majorVersion = majorVersion;
  	}
  
  	public void setMinorVersion(int minorVersion) {
  		this.minorVersion = minorVersion;
  	}
  	
  }
  ```

- int, long, String 같은 데이터 값은 일반 코드 처럼 값을 설정하면 된다

  ```java
  @Configuration
  public class AppCtx {
  	
  	...
  	
  	@Bean
  	public VersionPrinter versionPrinter() {
  		VersionPrinter versionPrinter = new VersionPrinter();
  		versionPrinter.setMajorVersion(5);
  		versionPrinter.setMinorVersion(0);
  		return versionPrinter;
  	}
  }
  ```

- Main

  ```java
  public class MainForSpring {
  
  	private static ApplicationContext ctx = null;
  	
  	public static void main(String[] args) throws IOException {
  		ctx = new AnnotationConfigApplicationContext(AppCtx.class);		
  		
  		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
  		
  		while (true) {
  			
  			...
  			} else if (command.equals("version")) {			//추가
  				processVersionCommand();
  				continue;
  			}
  			printHelp();
  		}
  	}
  
  	...
  	
  	private static void processVersionCommand() {			//추가
  		VersionPrinter versionPrinter = ctx.getBean("versionPrinter", VersionPrinter.class);
  		versionPrinter.print();
  	}
  
  	...
  }
  /*
  명령어를 입력하세요:
  version
  이 프로그램의 버전은 5.0입니다.
  ```

  