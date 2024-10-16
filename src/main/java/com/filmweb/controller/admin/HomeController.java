package com.filmweb.controller.admin;

import com.filmweb.constant.AppConstant;
import com.filmweb.domain.payment.PaymentStatus;
import com.filmweb.dto.PaymentTransactionDto;
import com.filmweb.dto.TopUserDto;
import com.filmweb.dto.UserDto;
import com.filmweb.dto.VideoDto;
import com.filmweb.entity.Order;
import com.filmweb.service.OrderService;
import com.filmweb.service.PaymentTransactionService;
import com.filmweb.service.UserService;
import com.filmweb.service.VideoService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.List;

@ApplicationScoped
@Controller
@Path("/admin")
public class HomeController {

    @Inject
    private Models models;

    @Inject
    private OrderService orderService;

    @Inject
    private UserService userService;

    @Inject
    private VideoService videoService;
    
    @Inject
    private PaymentTransactionService paymentTransactionService;

    @GET
    @Path("/dashboard")
    public String getDashboard(){
        var paymentTransactions = paymentTransactionService.findLatestPaymentTransaction(1, 10);
        var successfulPaymentTransactions = paymentTransactionService.findByStatus(PaymentStatus.SUCCESS);

        long totalPrice = successfulPaymentTransactions.stream()
                .map(PaymentTransactionDto::getPaymentAmount)
                .reduce(0L, Long::sum);

        models.put("totalPrice", totalPrice);
        models.put("paymentTransactions", paymentTransactions);
        return "admin/dashboard.jsp";
    }

    @GET
    @Path("/users")
    public String getUsers(
            @QueryParam("page") Integer page
    ){
        int totalUser = userService.findAll().size();
        long maxPage = (long) Math.ceil(1.0 * totalUser / AppConstant.SEARCH_PAGE_LIMIT);
        models.put("maxPage", maxPage);

        int currentPage = 1;
        if (page != null && page <= maxPage) {
            currentPage = page;
        }
        List<UserDto> users = userService.findAll(currentPage, AppConstant.SEARCH_PAGE_LIMIT);
        models.put("currentPage", currentPage);
        models.put("users", users);
        return "admin/user-list.jsp";
    }

    @GET
    @Path("/videos/liked")
    public String getLikedVideos(
            @QueryParam("page") Integer page
    ){
        long totalVideo = videoService.countLikedVideos();
        long maxPage = (long) Math.ceil(1.0 * totalVideo / AppConstant.SEARCH_PAGE_LIMIT);
        models.put("maxPage", maxPage);

        int currentPage = 1;
        if (page != null && page <= maxPage) {
            currentPage = page;
        }
        models.put("currentPage", currentPage);

        List<VideoDto> videos = videoService.findLikedVideos(currentPage, AppConstant.SEARCH_PAGE_LIMIT);
        models.put("videos", videos);
        return "admin/liked-video-list.jsp";
    }

    @GET
    @Path("/topUsers")
    public String getTopUsers(){
        List<TopUserDto> topUsers = userService.findTopUsers(1, 10);
        models.put("topUsers", topUsers);
        return "admin/top-user-list.jsp";
    }

}
