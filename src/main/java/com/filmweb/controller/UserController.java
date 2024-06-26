package com.filmweb.controller;

import com.filmweb.constant.AppConstant;
import com.filmweb.constant.CookieConstant;
import com.filmweb.constant.SessionConstant;
import com.filmweb.dao.UserVerifiedEmailDao;
import com.filmweb.dto.UserDto;
import com.filmweb.dto.VideoDto;
import com.filmweb.entity.History;
import com.filmweb.entity.Order;
import com.filmweb.entity.UserVerifiedEmail;
import com.filmweb.service.EmailService;
import com.filmweb.service.HistoryService;
import com.filmweb.service.OrderService;
import com.filmweb.service.UserService;
import com.filmweb.utils.AppUtils;
import com.filmweb.utils.JwtUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Controller
@Path("/")
public class UserController {

    @Inject
    private HttpSession session;

    @Context
    private ServletContext servletContext;

    @Inject
    private Models models;

    @Inject
    private UserService userService;
    @Inject
    private EmailService emailService;

    @Inject
    private OrderService orderService;

    @Inject
    private HistoryService historyService;

    @Inject
    private UserVerifiedEmailDao verifiedEmailDao;

    @Inject
    private AppUtils appUtils;

    @Inject
    private JwtUtils jwtUtils;

    @GET
    @Path("login")
    public String getLogin(){
        return "login.jsp";
    }

    @POST
    @Path("login")
    public String postLogin(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @Context HttpServletResponse response
    ){
        UserDto userDto = userService.authenticate(email, password);
        if (userDto != null) {
            boolean isAdmin = userDto.getIsAdmin();
            boolean isActive = userDto.getIsActive();

            if (!isAdmin && isActive) {
                session.setAttribute("loginSuccess", true);
                session.setAttribute(SessionConstant.CURRENT_USER, userDto);

                String rememberToken = jwtUtils.generateRememberToken(userDto);
                Cookie loginCookie = new Cookie(CookieConstant.REMEMBER_TOKEN, rememberToken);
                loginCookie.setMaxAge(CookieConstant.LOGIN_DURATION);
                response.addCookie(loginCookie);

                String prevUrl = appUtils.getPrevPageUrl(session);
                return "redirect:" + prevUrl;
            } else {
                session.setAttribute("emailNotVerified", true);
                return "login.jsp";
            }
        } else {
            session.setAttribute("loginSuccess", false);
            return "login.jsp";
        }
    }

    @GET
    @Path("logout")
    public String getLogout(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response
            ){
        session.removeAttribute(SessionConstant.CURRENT_USER);

        if(request.getCookies() != null){
            Arrays.stream(request.getCookies())
                    .filter(cookie -> cookie.getName().equals(CookieConstant.REMEMBER_TOKEN))
                    .findFirst()
                    .ifPresent(cookie -> {
                        cookie.setMaxAge(0);
                        response.addCookie(cookie);
                    });
        }

        String prevUrl = appUtils.getPrevPageUrl(session);
        return "redirect:" + prevUrl;
    }

    @GET
    @Path("register")
    public String getRegister(){
        return "register.jsp";
    }

    @POST
    @Path("register")
    public String postRegister(
            @FormParam("email") String email,
            @FormParam("phone") String phone,
            @FormParam("password") String password,
            @FormParam("fullName") String fullName
            ) throws MessagingException {
        boolean existedEmail = userService.existByEmail(email);
        boolean existedPhone = userService.existsByPhone(phone);

        if (!existedEmail && !existedPhone) {
            UserDto auth = userService.register(email, password, phone, fullName);

            if (auth != null) {
                emailService.sendRegisterEmail(servletContext, auth);
                session.setAttribute("registerSuccess", true);
                return "redirect:login";
            }
        } else {
            session.setAttribute("existPhone", existedPhone);
            session.setAttribute("existEmail", existedEmail);
        }
        return "redirect:register";
    }

    @GET
    @Path("verify")
    public String verify(
            @QueryParam("token") String token
    ){
        UserVerifiedEmail verifiedEmail = verifiedEmailDao.findByToken(token);
        if(verifiedEmail.getIsVerified()){
            session.setAttribute("alreadyVerified", true);
            return "redirect:login";
        }
        else if(verifiedEmail.getExpiredAt().isAfter(LocalDateTime.now())){
            verifiedEmail.setIsVerified(Boolean.TRUE);
            verifiedEmailDao.update(verifiedEmail);
            UserDto user = userService.verify(verifiedEmail.getUserId());
            session.setAttribute("email", user.getEmail());
            return "redirect:verify/success";
        }else{
            UserDto user = userService.findById(verifiedEmail.getUserId());
            session.setAttribute(SessionConstant.VERIFIED_EMAIL, user.getEmail());
            return "redirect:verify/expired";
        }
    }

    @GET
    @Path("verify/expired")
    public String verifyExpired(){return "verify-expired.jsp";}

    @GET
    @Path("verify/resend")
    public String resendVerifiedEmail() throws MessagingException {
        String verifiedEmail = session.getAttribute(SessionConstant.VERIFIED_EMAIL).toString();
        UserDto auth = userService.findByEmail(verifiedEmail);
        emailService.sendRegisterEmail(servletContext, auth);
        session.removeAttribute(SessionConstant.VERIFIED_EMAIL);
        return "redirect:verify/notify";
    }

    @GET
    @Path("verify/notify")
    public String notifyVerifiedEmail(){
        return "verify-notify.jsp";
    }

    @GET
    @Path("verify/success")
    public String verifySuccess(){
        return "verify-success.jsp";
    }

    @GET
    @Path("otp/enter")
    public String enterOtp(){
        return "enter-otp.jsp";
    }

    @POST
    @Path("otp/validate")
    public String validateOtp(
            @FormParam("otp") String otp
    ){
        String sysOtp = (String)session.getAttribute("otp");
        if(otp.trim().equals(sysOtp)){
            return "redirect:password/new";
        }
        session.setAttribute("errorOTP", true);
        return "redirect:otp/enter";
    }

    @GET
    @Path("password/new")
    public String getNewPassword(){
        return "new-password.jsp";
    }

    @POST
    @Path("password/new")
    public String postNewPassword(
            @FormParam("password") String password
    ){
        String email = (String) session.getAttribute("email");
        if(email != null && password != null){
            UserDto userDto = userService.changePassword(email, password.trim());

            if (userDto != null) {
                session.setAttribute("changePassSuccess", true);
            }
        }
        return "redirect:login";
    }

    @GET
    @Path("password/forgot")
    public String getForgot(){
        return "forgot-password.jsp";
    }

    @POST
    @Path("password/forgot")
    public String postForgot(
            @FormParam("email") String email
    ) throws MessagingException {
        UserDto userDto = userService.findByEmail(email);
        if (userDto != null){
            if (userDto.getIsActive()) {
                userService.sendForgotPasswordMessage(servletContext, session, userDto);
                return "redirect:otp/enter";
            } else {
                session.setAttribute("userFalse", true);
            }
        }else{
            session.setAttribute("existEmail", true);
        }
        return "redirect:password/forgot";
    }

    @GET
    @Path("password/change")
    public String getChangePassword(){
        UserDto userDto = (UserDto) session.getAttribute(SessionConstant.CURRENT_USER);
        models.put("email", userDto.getEmail());
        models.put("phone", userDto.getPhone());
        models.put("fullName", userDto.getFullName());
        return "change-password.jsp";
    }

    @POST
    @Path("password/change")
    public String postChangePassword(
            @FormParam("oldPass") String oldPassword,
            @FormParam("newPass") String newPassword,
            @FormParam("confirmation") Boolean confirm
    ){
        UserDto userDto = (UserDto) session.getAttribute(SessionConstant.CURRENT_USER);
        if(userService.comparePassword(userDto.getEmail(), oldPassword)){
            if (confirm != null && confirm) {
                UserDto user = userService.changePassword(userDto.getEmail(), newPassword.trim());

                if (user != null) {
                    session.removeAttribute(SessionConstant.CURRENT_USER);
                    session.setAttribute("newPassSuccess", true);
                    return "redirect:login";
                }
            }
        }
        models.put("email", userDto.getEmail());
        models.put("phone", userDto.getPhone());
        models.put("fullName", userDto.getFullName());
        session.setAttribute("oldPasswordWrong", true);
        return "change-password.jsp";
    }


    @GET
    @Path("profile")
    public String getProfile(){
        UserDto userDto = (UserDto) session.getAttribute(SessionConstant.CURRENT_USER);
        models.put("user", userDto);
        return "profile.jsp";
    }
    @GET
    @Path("profile/edit")
    public String getEditProfile(){
        UserDto userDto = (UserDto) session.getAttribute(SessionConstant.CURRENT_USER);
        models.put("user", userDto);
        return "edit-profile.jsp";
    }

    @POST
    @Path("profile/edit")
    public String postEditProfile(
            @FormParam("fullname") String fullname,
            @FormParam("phone") String phone,
            @FormParam("confirmation") Boolean confirm
    ){
        UserDto userDto = (UserDto) session.getAttribute(SessionConstant.CURRENT_USER);
        if (confirm != null && confirm){
            if (fullname != null && phone != null) {
                UserDto user = userService.editProfile(userDto.getEmail(), fullname, phone);

                if (user != null) {
                    session.removeAttribute(SessionConstant.CURRENT_USER);
                    return "redirect:login";
                }
            }
        }
        return "redirect:home";
    }
    @GET
    @Path("transaction")
    public String getTransaction(

    ) {
        UserDto userDto = (UserDto) session.getAttribute(SessionConstant.CURRENT_USER);
        if (userDto != null) {
            List<Order> orders = orderService.findByEmail(userDto.getEmail());
            models.put("orders", orders);
        }
        return "transaction.jsp";
    }

    @GET
    @Path("history")
    public String getHistory(
            @QueryParam("page") Integer page
    ) {
        UserDto userDto = (UserDto) session.getAttribute(SessionConstant.CURRENT_USER);

        if (userDto != null) {
            int currentPage = 1;
            if(page != null){
                currentPage = page;
            }
            List<VideoDto> videos = historyService.findViewedVideoByEmail(userDto.getEmail(), currentPage, AppConstant.SEARCH_PAGE_LIMIT);
            models.put("videos", videos);
            models.put("currentPage", currentPage);

            List<History> histories = historyService.findByEmail(userDto.getEmail());
            int maxPage = (int) Math.ceil(1.0 * histories.size() / AppConstant.SEARCH_PAGE_LIMIT);
            models.put("maxPage", maxPage);

        }
        return "history.jsp";
    }
    @GET
    @Path("favorite")
    public String getFavorite(
    ){
        UserDto userDto = (UserDto) session.getAttribute(SessionConstant.CURRENT_USER);

        if (userDto != null) {
            List<VideoDto> videos = historyService.findFavoriteVideoByEmail(userDto.getEmail());
            models.put("videos", videos);
        }
        return "favorite.jsp";
    }

}



