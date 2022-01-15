package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import aspect.ExeTimeAspect;
import chap07.Calculator;
import chap07.RecCalculator;

@Configuration
@EnableAspectJAutoProxy		//붙임
public class AppCtx {

	@Bean
	public ExeTimeAspect exeTimeAspect() {
		return new ExeTimeAspect();
	}
	
	//AOP 적용시 RecCalculator가 상속받은 Calculator 인터페이스를 이용해서 프록시 생성
	@Bean
	public Calculator calculator() {
		return new RecCalculator();
	}
}
