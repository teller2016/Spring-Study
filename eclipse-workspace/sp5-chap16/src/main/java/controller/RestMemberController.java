package controller;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import spring.DuplicateMemberException;
import spring.Member;
import spring.MemberDao;
import spring.MemberNotFoundException;
import spring.MemberRegisterService;
import spring.RegisterRequest;

@RestController
public class RestMemberController {
	private MemberDao memberDao;
	private MemberRegisterService registerService;
	
	@GetMapping("/api/members")
	public List<Member> members(){
		return memberDao.selectAll();
	}
	
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

	public void setMemberDao(MemberDao memberDao) {
		this.memberDao = memberDao;
	}

	public void setRegisterService(MemberRegisterService registerService) {
		this.registerService = registerService;
	}
	
}
