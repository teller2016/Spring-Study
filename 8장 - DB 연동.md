# 8장 - DB 연동

> - DataSource 설정
> - JdbcTemplate을 이용한 쿼리 실행
> - DB 관련 익셉션 변환 처리
> - 트랜잭션 처리

- 웹 앱들은 데이터 보관을 위해 MySQL, 오라클과 같은 DBMS를 사용한다
- 자바에서는 JDBC API를 사용하거나 JPA, MyBatis와 같은 기술을 사용해 **DB 연동을 처리**한다
  - **JDBC를 위해 스프링이 제공하는 JdbcTemplate의 사용법에 대해 배운다**

> JDBC(Java Database connectivity)란
>
> - 자바에서 데이터베이스를 접근할 수 있도록 하는 자바 API이다
> - JDBC는 데이터베이스에서 자료를 쿼리하거나 업데이트하는 방법을 제공한다
> - JDBC 프로그래밍 흐름
>   - [JDBC 드라이버 로드] => [DB 연결] => [DB에 데이터 읽거나 쓰기(SQL문)] => [DB 연결 종료]

## 1. JDBC 프로그래밍의 단점을 보완하는 스프링

- JDBC API를 이용하면...
  - DB 연동에 피룡한 Connections 구하고
  - 쿼리 실행을 위한 PreparedStatement 작성하고
  - 쿼리 실행한 뒤에 finally 블록에서 ResultSet, PreparedStatement, Connection을 닫는다
- 데이터 처리와는 상관없는 코드지만 JDBC 프로그래밍 할때 구조적으로 **반복된다**



- **구조적 반복을 줄이기 위한 방법은 템플릿 메서드 패턴과 전략 패턴을 함께 사용하는 것이다**

  - **스프링은 이 두 패턴을 엮은 `JdbcTemplate` 클래스를 제공한다**
    - 중복 코드가 줄어든다
    - 트랜잭션 관리가 쉽다

  

## 2. 프로젝트 준비

### 2.1 프로젝트 생성

- pom.xml 파일

  - spring-jdbc : JdbcTemplate 등 JDBC 연동에 필요한 기능 제공

    > 스프링이 제공하는 트랜잭션 기능을 사용하려면 spring-tx 모듈이 필요하다
    > spring-jdbc 모듈에 대한 의존을 추가하면 spring-tx 모듈도 자동으로 포함된다

  - tomcat-jdbc : DB 커넥션풀 기능 제공

    > **커넥션풀이란?**
    >
    > 자바 프로그램에서 DBMS로 커넥션을 생성하는 시간은 매우 길다. 즉 성능에 영향을 줄 수 있다
    >
    > **일정 개수의 DB 커넥션을 미리 만들어두는 기법이다**
    > DB 커넥션이 필요한 프로그램은 커넥션 풀에서 커넥션을 가져와 사용한 뒤 커넥션을 반납한다
    >
    > - 커넥션을 미리 생성해 두어 커넥션 생성 시간을 아낄 수 있다
    > - 동시 접속자가 많아도 커넥션 생성 부하가 적어진다 = 많은 동시 접속자 처리 가능
    >
    > 모듈로는 Tomcat JDBC, HikariCP, DBCP, c3pO 등이 존재

  - mysql-connector-java : MySQL 연결에 필요한 JDBC 드라이버를 제공한다

### 2.2 DB 테이블 생성

- 책에서 DBMS로 MySQL을 사용한다

- MySQL에 root 사용자로 연결한 뒤 쿼리를 실행해서 DB 사용자, 데이터베이스, 테이블을 생성한다

  ```sql
  // src/sql/ddl.sql
  create user 'spring5'@'localhost' identified by 'spring5';
  
  create database spring5fs character set = utf8;
  
  grant all privileges on spring5s.* to 'spring5'@'localhost';
  
  create table spring5fs.MEMBER (
  	ID int auto_increment primary key,
  	EMAIL varchar(255),
  	PASSWORD varchar(100),
  	NAME varchar(100),
  	REGDATE datetime,
  	unique key(EMAIL)
  	) engine=InnoDB character set = utf8;
  ```

  - MySQL DB에 spring5 계정 생성(암호로 spring5 이용)
  - spring5fs DB 생성
  - spring5fs DB에 spring5 계정이 접근할 수 있도록 권한 부여
  - spring5fs DB에 MEMBER 테이블 생성

- spring5 계정으로 MySQL에 접속하여 다음 쿼리 실행

  ```sql
  insert into MEMBER(EMAIL, PASSWORD, NAME, REGDATE)
  values('molly@naver.com', '1234', 'molly', now());
  ```

  

## 3. DataSource 설정

- JDBC API는 DriverManager 외에 DataSource를 이용해서 DB 연결을 구하는 방법을 정의하고 있다

  - DataSource로 Connection 구하기

    ```java
    Connection conn = null;
    try{
        //dataSource는 생성자나 설정 메서드를 이용해 주입받음
        conn = dataSource.getConnection();
        ...
    ```

- 스프링이 제공하는 DB연동 기능을 DataSource를 사용해서 DB Connection을 구한다

  - **DB 연동에 사용할 DataSource를 스프링 빈으로 등록하고 DB 연동 기능을 구현한 빈 객체는 DataSource를 주입받아 사용한다**

- Tomcat JDBC 모듈은 javax.sql.DataSource를 구현한 DataSource 클래스를 제공한다

  - DataSource 클래스를 스프링 빈으로 등록해 사용

  ```java
  @Configuration
  public class AppCtx {
  	
  	@Bean(destroyMethod = "close")
  	public DataSource dataSource() {
  		DataSource ds = new DataSource();
  		ds.setDriverClassName("com.mysql.jdbc.Driver");	//드라이버 클래스 지정(MySQL 드라이버 사용)
  		ds.setUrl("jdbc:mysql://localhost/spring5fs?characterEncoding=utf8");	// JDBC URL 지정
  		ds.setUsername("spring5");	//DB 연결에 사용할 사용자 계정과 암호 지정
  		ds.setPassword("spring5");
  		ds.setInitialSize(2);
  		ds.setMaxActive(10);
  		return ds;
  	}
  	
  }
  ```

  - `@Bean`의 `destroyMethod` 속성값이 close로 설정했다
    - `close` 메서드는 커넥션 풀에 보관된 Connection을 닫는다

### 3.1 Tomcat JDBC의 주요 프로퍼티

- Tomcat JDBC 모듈의 `org.apache.tomcat.jdbc.pool.DataSource` 클래스는 커넥션 풀 기능을 제공하는 DataSource 구현 클래스이다

  - 커넥션을 몇 개 만들지 정하는 메서드를 제공한다

- 주요 설정 메서드

  | 설정 메서드                           | 설명                                                         |
  | ------------------------------------- | ------------------------------------------------------------ |
  | setInitialSize(int)                   | 커넥션 풀을 초기화할 때 생성할 초기 커넥션 개수 지정. 기본값은 10 |
  | setMaxActive(int)                     | 커넥션 풀에서 가져올 수 있는 최대 커넥션 개수 지정. 기본값은 100 |
  | setMaxIdle(int)                       | 커넥션 풀에 유지할 수 있는 최대 커넥션 개수 지정. 기본값 maxActive와 같다 |
  | setMinIdle(int)                       | 커넥션 풀에 유지할 최소 커넥션 개수 지정. 기본값은 initialSize에서 가져온다 |
  | setMaxWait(int)                       | 커넥션 풀에서 커넥션을 가져올 때 대기할 최대 시간을 밀리초 단위로 지정. 기본값을 30000밀리초(30초) |
  | setMaxAge(long)                       | 최초 커넥션 연결 후 최대 유효 시간을 밀리초 단위로 지정. 기본값 0. 0은 유효시간 없음을 의미 |
  | setValidationQuery(String)            | 커넥션이 유효한지 검사할 때 사용할 쿼리를 지정한다. 언제 검사할지는 별도 설정으로 지정. <br />기본값은 null. null이면 검사하지 않는다. "select 1", "select 1 from dual" 같은 쿼리 주로 사용 |
  | setValidationQueryTimeout(int)        | 검사 쿼리의 최대 실행 시간을 초 단위로 지정. 시간 초과하면 검사 실패로 간주. 0이하로 지정하면 비활성화한다. 기본값은 -1 |
  | setTestOnBorrow(boolean)              | 풀에서 커넥션을 가져올 때 검사 여부를 지정한다. 기본값은 false이다 |
  | setTestOnReturn(boolean)              | 풀에서 커넥션을 반환할 때 검사 여부를 지정한다. 기본값은 false이다 |
  | setTestWhileIdle(boolean)             | 커넥션이 풀에 유휴 상태로 있는 동안에 검사할지 여부를 지정한다. 기본값은 false |
  | setMinEvictableIdleTimeMillis(int)    | 커넥션 풀에 유휴 상태로 유지할 최소 시간을 밀리초 단위로 지정. testWhileIdle이 true이면 유휴 시간이 이 값을 초과한 커넥션을 풀에서 제거한다. 기본값은 60000밀리초(60초) |
  | setTimeBetweenEvictionRunsMillis(int) | 커넥션 풀의 유휴 커넥션을 검사할 주기를 밀리초 단위로 지정. 기본값은 5000밀리초(5초). <br />1초 이하로 설정하면 안된다 |

- **커넥션의 상태**
  - 커넥션 풀은 커넥션을 생성하고 유지한다
  - 커넥션 풀에 **커넥션을 요청하면 해당 커넥션은 활성(active) 상태가 된다**
    - `DataSource#getConnection()`을 실행하면 수행된다
  - 커넥션을 풀에 **반환하면 유휴(idle) 상태가 된다**
    - `close`하면 수행된다

- Connection을 구하고 종료하는 코드

  ```java
  public class DbQuery {
  	private DataSource dataSource;
  
  	public DbQuery(DataSource dataSource) {
  		this.dataSource = dataSource;
  	}
  	
  	public int count() {
  		Connection conn = null;
  		
  		try {
  			conn = dataSource.getConnection(); 		//풀에서 구함
  			try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("select count(*) from MEMBER")){
  				rs.next();
  				return rs.getInt(1);
  			}
  		}catch(SQLException e) {
  			throw new RuntimeException(e);
  		}finally {
  			if(conn!=null)
  				try {
  					conn.close(); //풀에 반환
  				}catch(SQLException e) {}
  		}
  	}
  }
  ```

  - `dataSource.getConnection()`으로 풀에서 커넥션 가져오고 사용 끝나면 실제로 커넥션을 끊지 않고 풀에 반환한다
    - 커넥션은 다시 유휴 상태가 된다

- `maxActive`는 활성 상태가 가능한 최대 커넥션 개수를 지정한다

  - 최대 개수 보다 넘게 요청하면 다른 커넥션이 반환될 때 까지 대기한다
  - 대기 시간이 `maxWait`이다. 시간 내에 반환 없므녀 익셉션 발생한다

- 커넥션 풀 사용 이유

  - 성능 때문이다
  - 매번 새로운 커넥션 생성하면 매번 연결 시간이 소모된다
  - 커넥션 풀을 통해 필요할 때 커넥션을 꺼내써서, 커넥션 구하는 시간을 줄인다
    - 전체 응답 시간이 짧아진다
  - 커넥션 풀을 초기화 할때 최소 수준의 커넥션을 미리 생성하는 것이 좋다
    - `intialSize`로 지정

- 커넥션 풀에 생성된 커넥션은 지속적으로 사용된다

  - DBMS 설정에 따라 일정 시간 내에 쿼리를 실행하지 않으면 연결을 끊기도록 한다

    > Ex. 5분 동안 DBMS에 쿼리를 실행하지 않으면 연결 끊기도록 설정했다
    >
    > 풀에 커넥션이 5분 넘게 유휴 상태로 존재했다고 하자
    >
    > - DBMS는 해당 커넥션의 연결을 끊지면 커넥션은 여전히 풀 속에 남아있다
    > - 이상태에서 커넥션을 가져와 사용하면 익셉션 발생

  - 방지하기 위해 커넥션 풀의 커넥션이 유요한지 주기적으로 검사해야된다

    - 관련된 속성으로 `minEvictableIdleTimeMillis`, `timeBetweenEvictionRunsMillis`, `testWhileIdle`이 있다

    - Ex. 10초 주기로 유휴 커넥션이 유효한지 여부를 검사하고 최소 유휴 시간을 3분으로 지정

      ```java
      @Bean(destroyMethod = "close")
      	public DataSource dataSource() {
      		DataSource ds = new DataSource();
      		ds.setDriverClassName("com.mysql.jdbc.Driver");
      		ds.setUrl("jdbc:mysql://localhost/spring5fs?characterEncoding=utf8");
      		ds.setUsername("spring5");
      		ds.setPassword("spring5");
      		ds.setInitialSize(2);
      		ds.setMaxActive(10);
              
              ds.setTestWhileIdle(true);		// 유휴 커넥션 검사
              ds.setMinEvictableIdleTimeMillis(1000 * 60 * 3);	// 최소 유휴 시간 3분
              ds.setTimeBetweenEvictionRunsMillis(1000 * 10);		// 10초 주기
              
      		return ds;
      	}
      ```

      

## 4. Jdbc Template을 이용한 쿼리 실행

- 스프링을 사용하면 `DataSource`나 `Connection`, `Statement`, `ResultSet`을 직접 사용하지 않고 **`JdbcTemplate`을 이용해서 편리하게 쿼리를 실행할 수 있다**

### 4.1 JdbcTemplate 생성하기

- 가장 먼저 `JdbcTemplate` 객체를 생성해야 된다

  ```java
  public class MemberDao {
  
  	private JdbcTemplate jdbcTemplate;
  	
  	public MemberDao(DataSource dataSource) {
  		this.jdbcTemplate = new JdbcTemplate(dataSource);
  	}
  }
  ```

  - `MemberDao` 클래스에 `JdbcTemplate` 객체를 생성하는 코드 추가

    - 생성자로 전달했다

  - 설정 메서드 방식을 이용해 `DataSource`를 주입받아 `JdbcTemplate`를 생성할 수도 있다

    ```java
    public class MemberDao {
    
    	private JdbcTemplate jdbcTemplate;
    	
    	public setDataSource(DataSource dataSource){
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }
    }
    ```

- 스프링 설정에서 `MemberDao` 빈 설정을 추가

  ```java
  @Configuration
  public class AppCtx {
  	
  	@Bean(destroyMethod = "close")
  	public DataSource dataSource() {
  		DataSource ds = new DataSource();
  		ds.setDriverClassName("com.mysql.jdbc.Driver");
  		...
  	}
  	
  	@Bean
  	public MemberDao memberDao() {
  		return new MemberDao(dataSource());		// dataSource
  	}
  	
  }
  ```

### 4.2 JdbcTemplate을 이용한 조회 쿼리 실행

- `JdbcTemplate` 클래스는 SELECT 쿼리 실행을 위한 `query()` 메서드를 제공한다

  > 자주 사용되는 쿼리
  >
  > - List<T> query(String sql, RowMapper<T> rowMapper)
  > - List<T> query(String sql, Object[] args, RowMapper<T> rowMapper)
  > - List<T> query(String sql, RowMapper<T> rowMapper, Object... args)

  - sql 파라미터로 받은 쿼리를 실행하고 `RowMapper`을 이용해 `ResultSet`의 결과를 자바 객체로 변환한다

    ```mysql
    select * from member where email = ?
    ```

    - 인덱스 기반 파라미터를 가진 쿼리이다
    - args 파라미터를 이용해서 각 인덱스 파라미터의 값을 지정한다
    - **`?`에 파라미터가 들어간다**

- **쿼리 실행 결과를 자바 객체로 변환할 때 사용하는 `RowMapper` 인터페이스**

  ```java
  public interface RowMapper<T>{
      T mapRow(ResultSet rs, int rowNum) throws SQLException;
  }
  ```

  - `mapRow()`메서드는 SQL 실행 결과로 구한 `ResultSet`에서 **한 행의 데이터를 읽어와 자바 객체로 변환**하는 매퍼 기능 구현한다
    - `RowMapper` 인터페이스를 구현한 클래스를 작성하거나
    - 임의 클래스나 람다식으로 `RowMapper` 객체를 생성해서 `query()` 메서드에 전달한다

- `selectByEmail()` 메서드 구현

  ```java
  public class MemberDao {
  
  	private JdbcTemplate jdbcTemplate;
  	
  	public MemberDao(DataSource dataSource) {
  		this.jdbcTemplate = new JdbcTemplate(dataSource);
  	}
  	
  	public Member selectByEmail(String email) {
  		List<Member> results = jdbcTemplate.query("select * from MEMBER where EMAIL = ?",
  				new RowMapper<Member>() {
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
  		},
  			email);
  		return results.isEmpty() ? null : results.get(0);		//email에 해당하는 MEMBER있으면 해당 객체 리턴 없으면 null리턴
  	}
      ...
  ```

  - **쿼리는 인덱스 파라미터(물음표)를 포함한다**

    - `query()`메서드의 3번째 파라미터 `email`가 (물음표)로 들어간다

  - 인덱스 파라미터가 2개 이상인 경우

    ```java
    	List<Member> results = jdbcTemplate.query("select * from MEMBER where EMAIL = ? and NAME = ?"",
    				new RowMapper<Member>() {...}
    		},
    			email, name);		//물음표 개수만큼 해당되는 값 전달
    ```

  - **임의 클래스를 이용해 `RowMapper`의 객체를 전달하고 있다**

    - `ResultSet`에서 데이터를 읽어와 `Member` 객체로 변환해주는 기능을 제공
    - 파라미터 타입으로 `Member` 이용 (`RowMapper<Member>`)

    - 람다 사용하면 간결해진다

      ```java
      	List<Member> results = jdbcTemplate.query(
              "select * from MEMBER where EMAIL = ?",
      		(ResultSet rs, int rowNum) -> {
                  Member member = new Member(
      				rs.getString("EMAIL"),
      				rs.getString("PASSWORD"),
      				rs.getString("NAME"),
      				rs.getTimestamp("REGDATE").toLocalDateTime());
      				member.setId(rs.getLong("ID"));
      							
      				return member;
              },
              email);
      ```

  

- `MemberDao`의 `selectAll()` 메서드

  ```java
  public class MemberDao {
  
  	private JdbcTemplate jdbcTemplate;
  	...
  	
  	public List<Member> selectAll(){
  		List<Member> results = jdbcTemplate.query(
  				"select * from MEMBER",
  				new RowMapper<Member>() {
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
  				});
  		
  		return results;
  	}
  }
  ```

  - `selectByEmail()` 메서드와 동일한 `RowMapper` 임의 클래스를 사용했다

    - **동일한 `RowMapper` 구현을 여러번 사용하면...**

    - **`RowMapper` 인터페이스를 구현한 클래스를 만들어서 중복 코드 막을 수 있다**

    - `Member`을 위한 `RowMapper` 구현 클래스를 이용하면 중복 코드 제거 가능

      ```java
      //RowMapper 구현
      public class MemberRowMapper implements RowMapper<Member>{
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
      }
      ```

      ```java
      public class MemberDao {
      
      	...
      	
      	public Member selectByEmail(String email) {
      		List<Member> results = jdbcTemplate.query(
                  "select * from MEMBER where EMAIL = ?",
      			new MemberRowMapper(),							//중복 코드 제거
      			email);
              
      		return results.isEmpty() ? null : results.get(0);
      	}
      	
      	
      	public List<Member> selectAll(){
      		List<Member> results = jdbcTemplate.query(
      				"select * from MEMBER",
      				new MemberRowMapper());						//중복 코드 제거
      		
      		return results;
      	}
      }
      ```

### 4.3 결과가 1행인 경우 사용할 수 있는 queryForObject() 메서드

- MEMBER 테이블의 전체 행 개수를 구하는 코드

  ```java
  	public int count() {
  		List<Integer> results = jdbcTemplate.query(
  				"select count(*) from MEMBER",
  				new RowMapper<Integer>() {
  
  					@Override
  					public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
  						return rs.getInt(1);
  					}
  		
  				});
  		return results.get(0);
  	}
  ```

  - `count(*)` 쿼리는 결과가 한 행이다
    - List로 받기보단 Integer 같은 정수 타입으로 받으면 편하다
    - `queryForObject()`로 해결가능

- `queryForObject()`로 `count()`메서드 구현

  ```java
  public class MemberDao {
  
  	private JdbcTemplate jdbcTemplate;
  	...
  	
  	public int count() {
  		Integer count = jdbcTemplate.queryForObject("select count(*) from MEMBER", Integer.class);
  		
  		return count;
  	}
  }
  ```

  - `queryForObject()` 메서드는 **쿼리 실행 결과 행이 한 개인 경우** 사용할 수 있는 메서드
    - 두 번째 파라미터는 칼럼을 읽어올 때 사용할 타입 지정

- `queryForObject()`에 인덱스 파라미터 사용

  ```java
  double avg = queryForObject(
  	"select avg(height) from FURNITURE where TYPE=? and STATUS =?",
      Double.class,
      100, "S");
  ```

- 실행 결과 칼럼이 두개 이상인 경우 `RowMapper`를 파라미터로 전달해 결과 생성 가능

  ```java
  Member member = jdbcTemplate.queryForObject(			//List가 아닌 Member 타입으로 리턴받음
  	"select * from MEMBER where ID = ?",
      new RowMapper<Member>(){
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
      },
  	100);
  ```

  - **`query()`와 차이점**
    - **리턴 타입이 List가 아니라 RowMapper로 변환해주는 타입(Member)이다**

- `queryForObject()`는 쿼리 실행 결과가 **반드시 한 행일때 사용**
  - 두 개 이상이면 `IncorrectResultSizeDataAccesssException`발생
  - 0개이면 `EmptyResultDataAccessException`발생
  - **정확히 한개 아니면 `query()` 메서들 사용해야 된다**

### 4.4 JdbcTemplate을 이용한 변경 쿼리 실행

- INSERT, UPDATE, DELETE 쿼리는 `update()` 메서드를 이용한다

  > - int update(String sql)
  > - int update(String sql, Object... args)

- `update()` 메서드는 쿼리 실행 결과로 **변경된 행의 개수를 리턴한다**

- 사용 코드

  ```java
  public class MemberDao {
  
  	private JdbcTemplate jdbcTemplate;
  	
  	public MemberDao(DataSource dataSource) {
  		this.jdbcTemplate = new JdbcTemplate(dataSource);
  	}
  	
      ...
  	
  	public void update(Member member) {
  		jdbcTemplate.update("update MEMBER set NAME=?, PASSWORD=? where EMAIL=?",
  				member.getName(), member.getPassword(), member.getEmail());
  
  	}
  	
  	...
  }
  ```

### 4.5 PreparedStatementCreator를 이용한 쿼리 실행

- 지금까지는 쿼리에 사용할 값을 인자로 전달했다

  ```java
  jdbcTemplate.update("update MEMBER set NAME=?, PASSWORD=? where EMAIL=?",
  				member.getName(), member.getPassword(), member.getEmail());
  ```

  - 파라미터로 값을 전달했다

- `PreparedStatement`의 `set` 메서드로 **직접 인덱스에 파라미터 값을 설정**할 수 있다

  - `PreparedStatementCreator`를 인자로 받는 메서드를 이용해 직접 `PreparedStatement`를 생성하고 설정해야 된다

- `PreparedStatementCreator` 인터페이스

  ```java
  public interface PreparedStatementCreator{
      PreparedStatement createPreparedStatement(Connection con) throws SQLException;
  }
  ```

  - `createPreparedStatement` 메서드는 `Connection` 타입의 파라미터를 갖는다
  - `PreparedStatementCreator`를 구현한 클래스는 `createPreparedStatement` 메서드의 파라미터로 전달받은 `Connection`을 이용해서 `PreparedStatement` 객체를 생성하고, 인덱스 파라미터를 알맞게 설정한 뒤에 리턴하면 된다

  ```java
  jdbcTemplate.update(new PreparedStatementCreator() {
  
  	@Override
  	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
  		// 파라미터로 전달받은 Connection을 이용해서 PreparedStatement 생성
  		PreparedStatement pstmt = con.prepareStatement("insert into MEMBER (EMAIL, PASSWORD, NAME, REGDATE) values (?,?,?,?)");
  				
  		// 인덱스 파라미터 값 설정
  		pstmt.setString(1, member.getEmail());
  		pstmt.setString(2, member.getPassword());
  		pstmt.setString(3, member.getName());
  		pstmt.setTimestamp(4, Timestamp.valueOf(member.getRegisterDateTime()));
  		
  		// 생성한 PreparedStatement 객체 리턴
  		return pstmt;
  	}
  			
  });
  ```

- `JdbcTemplate` 클래스가 제공하는 메서드 중 `PreparedStatementCreator` 인터페이스를 파라미터로 갖는 메서드

  > - List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper)
  > - int update(PreparedStatementCreator psc)
  > - int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)

  - 세번째 메서드는 **자동 생성되는 키 값을 구할 때 사용**

### INSERT 쿼리 실행 시 KeyHolder를 이용해서 자동 생성 키값 구하기

- MySQL의 AUTO_INCREMENT 칼럼은 행이 추가되면 자동으로 값이 할당되는 칼럼으로서 주요키 칼럼에 사용된다

  ```mysql
  create table spring5fs.MEMBER (
  	ID int auto_increment primary key,		//AUTO_INCREMENT
  	EMAIL varchar(255),
  	PASSWORD varchar(100),
  	NAME varchar(100),
  	REGDATE datetime,
  	unique key(EMAIL)
  	) engine=InnoDB character set = utf8;
  ```

  - 값을 삽입하면 해당 **칼럼의 값이 자동으로 생성된다**

  - 그래서 INSERT 쿼리에 해당 값을 지정하지 않는다

    ```java
    // 자동 증가 칼럼인 ID 칼럼의 값을 지정하지 않음
    jdbcTemplate.update(
    	"insert into MEMBER (EMAIL, PASSWORD, NAME, REGDATE) values (?,?,?,?)",
        member.getEmail(), member.getPassword(), ...
    )
    ```

    - **쿼리 실행 후에 생성된 키 값을 알고 싶다면???**

- `JdbcTemplate`의 `KeyHolder`을 사용하면 키값 구할 수 있다

  - `MemberDao`의 `insert()`메서드에서 삽입된 `Member`객체의 ID값 구하기

  ```java
  	public void insert(Member member) {
  		
  		KeyHolder keyHolder = new GeneratedKeyHolder();
  		
  		jdbcTemplate.update(new PreparedStatementCreator() {
  
  			@Override
  			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
  				// 파라미터로 전달받은 Connection을 이용해서 PreparedStatement 생성
  				PreparedStatement pstmt = con.prepareStatement(
  						"insert into MEMBER (EMAIL, PASSWORD, NAME, REGDATE) values (?,?,?,?)",
  						new String[] {"ID"}
  						);
  				
  				// 인덱스 파라미터 값 설정
  				pstmt.setString(1, member.getEmail());
  				pstmt.setString(2, member.getPassword());
  				pstmt.setString(3, member.getName());
  				pstmt.setTimestamp(4, Timestamp.valueOf(member.getRegisterDateTime()));
  				
  				// 생성한 PreparedStatement 객체 리턴
  				return pstmt;
  			}
  		}, keyHolder);	//jdbcTemplate.update(new PreparedStatementCreator() {...}, keyHolder);
  		
  		Number keyValue = keyHolder.getKey();
  		member.setId(keyValue.longValue());
  	}
  ```

  - `GeneratedKeyHolder` 객체 생성

    - 자동 생성된 키값을 구해주는 `KeyHolder` 구현 클래스

  - `update()`는 `PreparedStatementCreator` 객체와 `KeyHolder`객체를 파라미터로 갖는다

  - `Connection`의 `prepareStatement()` 메서드를 이용해 `PreparedStatement` 객체를 생성할 때 **두 번째 파라미터로 String 배열인 `{"ID"}`를 주었다**

    - 두번째 파라미터는 자동 생성되는 키 칼럼 목록을 지정할 때 사용

      > MEMBER 테이블은 ID 칼럼이 자동 증가 키 칼럼이므로
      > 두 번째 파라미터로 {"ID"}를 주었다

  - `update()` 메서드는 `PrepareStatement`를 실행 후 자동 생성된 키값을 `KeyHolder`에 보관한다
    - `getKey()`로 보관된 키값 구함 (`java.lang.Number`를 리턴)
    - `Number`의 `intValue()`, `longValue()` 등으로 원하는 타입으로 변환



## 5. MemberDao 테스트하기

- 메인 클래스

  ```java
  public class MainForMemberDao {
  	private static MemberDao memberDao;
  	
  	public static void main(String[] args) {
  		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppCtx.class);
  		
  		memberDao = ctx.getBean(MemberDao.class);
  		
  		selectAll();
  		updateMember();
  		insertMember();
  		
  		ctx.close();
  	}
  	
  	private static void selectAll() {
  		System.out.println("----- selectAll");
  		int total = memberDao.count();
  		System.out.println("전체 데이터: "+total);
  		
  		List<Member> members = memberDao.selectAll();
  		for(Member m : members) {
  			System.out.println(m.getId() + ":" + m.getEmail() + ":" + m.getName());
  		}
  	}
  	
  	private static void updateMember() {
  		System.out.println("----- updateMember");
  		Member member = memberDao.selectByEmail("molly@naver.com");
  		String oldPw = member.getPassword();
  		String newPw = Double.toHexString(Math.random());
  		member.changePassword(oldPw, newPw);
  
  		memberDao.update(member);
  		System.out.println("암호 변경: " + oldPw + " => " + newPw);
  	}
  	
  	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddHHmmss");
  	
  	private static void insertMember() {
  		System.out.println("----- insertMember");
  
  		String prefix = formatter.format(LocalDateTime.now());		
  		Member member = new Member(prefix + "@test.com", prefix, prefix, LocalDateTime.now());
  		
  		memberDao.insert(member);
  		
  		System.out.println(member.getId() + "(ID)를 가진 데이터 추가");
  	}
  }
  ```

  - `AppCtx` 설정을 사용해 스프링 컨테이너 생성

    - `memberDao` 빈 구함

  - `selectAll()`

    - `memberDao.count()`로 전체 행 개수 구함
    - `memberDao.selectAll()`로 전체 데이터 구하고 출력

  - `updateMember()`

    - EMAIL 칼럼값이 `molly@naver.com`인 `Member`객체 구했다
    - 임의의 암호로 변경해서`memberDao.update()`를 통해 변경 내역을 DB에 반영

  - `insertMember()`

    - 새로 추가할 `Member` 객체 생성

      - "MMddHHmmss" 형태로 변환한 시간 문자열을 이메일, 암호, 이름에 사용

    - `memberDao.insert()`로 DB에 새로운 데이터 추가

    - `member.getId()`로 새로 생성된 ID 키값을 출력

      > KeyHolder에서 구한 키값을 `member.setId()`로 설정함으로
      > `memberDao.insert()` 코드 실행 뒤에 확인 가능하다

- 실행결과

  ```java
  /*
  ----- selectAll
  전체 데이터: 1
  1:molly@naver.com:molly
  ----- updateMember
  암호 변경: 1234 => 0x1.acd5da626272p-5
  ----- insertMember
  2(ID)를 가진 데이터 추가
  ```
  
  - 한번더 실행
  
  ```java
  /*
  ----- selectAll
  전체 데이터: 2
  1:molly@naver.com:molly
  2:0117003426@test.com:0117003426
  ----- updateMember
  암호 변경: 0x1.acd5da626272p-5 => 0x1.630fba71f07e3p-1
  ----- insertMember
  3(ID)를 가진 데이터 추가
  ```
  
  

### 5.1 DB 연동 과정에서 발생 가능한 익셉션

> ```java
> /*
> Access denied for user 'spring5'@'localhost'...
> ...
> CannotGetJdbcConnectionException: Failed to obtain JDBC Connection...
> ...
> ```
>
> - MySQL 서버에 연결할 권한이 없는경우 발생
>
>   - ex. 계정의 암호를 잘못 입력한 경우
>
> - `DataSource`를 잘못 설정하면 발생
>
>   ```java
>   	@Bean(destroyMethod = "close")
>   	public DataSource dataSource() {
>   		DataSource ds = new DataSource();
>   		ds.setDriverClassName("com.mysql.jdbc.Driver");
>   		ds.setUrl("jdbc:mysql://localhost/spring5fs?characterEncoding=utf8");
>   		ds.setUsername("spring5");
>   		ds.setPassword("wrongPassword");		//틀린 비번
>   		...
>   		return ds;
>   	}
>   ```

> ```java
> /*
> ...CannotGetJdbcConnectionException...
> Communications link failure...
> ```
>
> - 주로 DBMS를 실행하지 않았기 때문에 발생

> ```java
> /*
> ...BadSQLGrammarException...
> ...bad SQL Grammer...
> ...MySQLSyntaxErrorException...
> ```
>
> - 잘못된 쿼리를 사용하는 경우에 발생
>
>   - 공백 문자 같은거 조심하자
>
>     ```java
>     "... where" + "EMAIL = ?"
>     // "... whereEmail = ?" 가 된다 => 오류 발생
>     ```



## 6. 스프링의 익셉션 변환 처리

