package com.mayday.member.web;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.mayday.board.service.IBoardService;
import com.mayday.board.vo.BoardSerchVO;
import com.mayday.board.vo.BoardVO;
import com.mayday.common.service.IMemberProfileService;
import com.mayday.common.util.MemProfileUtils;
import com.mayday.common.vo.MemberProfileVO;
import com.mayday.common.vo.ResultMessageVO;
import com.mayday.exception.BizNotEffectedException;
import com.mayday.exception.BizNotFoundException;
import com.mayday.login.vo.UserVO;
import com.mayday.member.service.IMemberService;
import com.mayday.member.vo.MemberVO;
import com.mayday.payment.service.IPaymentService;
import com.mayday.payment.vo.PaymentVO;
import com.mayday.reply.service.IReplyService;
import com.mayday.reply.vo.ReplySearchVO;
import com.mayday.reply.vo.ReplyVO;

@Controller
public class InfoController {
	@Inject
	private IMemberService memberService;
	@Inject
	private IBoardService boardService;
	@Inject
	private IReplyService replyService;
	@Inject
	private IMemberProfileService memberProfileService;
	@Inject
	private IPaymentService paymentService;
	
	@Autowired
	private MemProfileUtils memProfileUtils;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@RequestMapping("/info/mypage")
	public String userInfo(@ModelAttribute("boardSerchVO") BoardSerchVO boardSerchVO,
								@ModelAttribute("replySearchVO") ReplySearchVO replySearchVO, 
								@ModelAttribute("paymentVO") PaymentVO paymentVO,
								HttpServletRequest req, Model model) {
		HttpSession session = req.getSession();
		UserVO user = (UserVO) session.getAttribute("USER_INFO");
		if (user == null) {
			return "redirect:/login/login";
		}
		MemberVO member;
		try {
			member = memberService.getMember(user.getUserId());
			model.addAttribute("member", member);
		} catch (BizNotFoundException e) {
			model.addAttribute("e", e);
		}
		// ?????? ??? ???
		boardSerchVO.setMemId(user.getUserId());
		List<BoardVO> userBoardList = boardService.getUserBoardList(boardSerchVO);
		model.addAttribute("userBoardList", userBoardList);
		// ?????? ??? ??????
		replySearchVO.setReMemId(user.getUserId());
		List<ReplyVO> userReplyList = replyService.getUserReplyList(replySearchVO);
		model.addAttribute("userReplyList", userReplyList);
		// ???????????? 
		List<PaymentVO> paymentList = null; 
		paymentVO.setMemId(user.getUserId());
		if(user.getUserRole().equals("EXPERT")) {
			paymentVO.setSearchPosit("E");
		}else if(user.getUserRole().equals("CLIENT")) {
			paymentVO.setSearchPosit("C");
		}
		paymentList = paymentService.getPaymentListByMemId(paymentVO);
		Integer paymentTotal = paymentService.countPaymentTotal(paymentVO);
		model.addAttribute("paymentList", paymentList);
		model.addAttribute("paymentTotal", paymentTotal);
		logger.debug("paymentVO={}",paymentVO);
		logger.debug("paymentList={}",paymentList);
		logger.debug("paymentTotal={}",paymentTotal);
		return "info/mypage";
	}

	@RequestMapping("/info/mypageEdit")
	public String userInfoEdit(@RequestParam(value = "memId", required = true) String memId, Model model)
			throws BizNotFoundException {
		logger.debug("memId={}", memId);
		MemberVO member = memberService.getMember(memId);
		model.addAttribute("member", member);
		return "info/mypageEdit";
	}

	@PostMapping("/info/mypageModify")
	public String userInfoModify(@ModelAttribute("member") MemberVO member, Model model) {
		logger.debug("member={}", member);
		ResultMessageVO resultMessage = new ResultMessageVO();
		try {
			memberService.ModifyInfo(member);
			resultMessage.setResult(true);
			resultMessage.setMessage("?????????????????????.");
			resultMessage.setTitle("???????????? ??????");
			resultMessage.setUrlTitle("????????????");
			resultMessage.setUrl("/info/mypage"); // redirect:/
			resultMessage.setCode("200");
		} catch (BizNotFoundException e) {
			resultMessage.setResult(false);
			resultMessage.setMessage("?????? ??????: ?????? ???????????? ?????? ??? ????????????.");
			resultMessage.setTitle("???????????? ??????");
			resultMessage.setUrlTitle("????????????");
			resultMessage.setUrl("/info/mypage");
			resultMessage.setCode("404");
		} catch (BizNotEffectedException e) {
			resultMessage.setResult(false);
			resultMessage.setMessage("?????? ??????");
			resultMessage.setTitle("???????????? ??????");
			resultMessage.setUrlTitle("????????????");
			resultMessage.setUrl("/info/mypage");
			resultMessage.setCode("401");
		}
		model.addAttribute("resultMessage", resultMessage);
		return "common/message";
	}
	@Transactional
	@PostMapping("/info/profile/upload")
	public String profileUpload(@RequestParam("profile") MultipartFile boFile,
			@ModelAttribute("member") MemberVO member,@ModelAttribute("memProfile")MemberProfileVO memProfile ,HttpSession session, HttpServletRequest req, Model model)
			throws IOException {
		logger.debug("/upload : Post ?????? ??????!!!!");
		logger.debug("?????? ?????? : " + member);
		logger.debug("boFile={}", boFile);

		UserVO user = (UserVO) session.getAttribute("USER_INFO");
		String memId = user.getUserId();
		ResultMessageVO resultMessage = new ResultMessageVO();
		try {
			if (boFile != null) {
				memProfile = memProfileUtils.getMemProfileByMultipart(boFile, memId, "profile");
				member.setMemProfile(memProfile);
				memberProfileService.registMemProfile(memProfile);
				logger.debug("memProfile={}", memProfile);
				System.out.println(memProfile.getProfileName());
			}
			resultMessage.setResult(true);
			resultMessage.setTitle("????????? ??????");
			resultMessage.setMessage("????????? ????????? ?????????????????????.");
			resultMessage.setUrl("/info/mypage");
			resultMessage.setUrlTitle("MY Page");
			resultMessage.setCode("200");
		} catch (BizNotEffectedException e) {
			resultMessage.setResult(false);
			resultMessage.setTitle("????????? ??????");
			resultMessage.setMessage("????????? ?????? ?????????????????????.");
			resultMessage.setUrl("/info/mypage");
			resultMessage.setUrlTitle("MY Page");
			resultMessage.setCode("401");
		}
		model.addAttribute("resultMessage", resultMessage);
		return "common/message";
	}
}
