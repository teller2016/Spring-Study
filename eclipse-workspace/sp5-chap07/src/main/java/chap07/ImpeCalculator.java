package chap07;

public class ImpeCalculator implements Calculator {

	@Override
	public long factorial(long num) {
		
		//long start = System.currentTimeMillis();		//시작 시간
		
		long result = 1;
		for(long i = 1; i <= num; i++) {
			result *= i;
		}
		
		//long end = System.currentTimeMillis();		//종료 시간
		//System.out.printf("ImpeCalculator.factorial(%d) 실행시간 = %d\n", num, (end-start));
		
		return result;
	}

}
