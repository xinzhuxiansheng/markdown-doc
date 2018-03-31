<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %> --%>
<%@ include file="taglibs.jsp"%>
<html lang="zh-CN">
<head>
	<title>Markdown Doc</title>
	<meta http-equiv="X-UA-Compatible" content="IE=Edge">
	<meta charset="UTF-8">
	
	<link href="${ctx}/resources/bootstrap/3.3.5/css/bootstrap.min.css" rel="stylesheet"  type="text/css" />
	<link href="${ctx}/resources/css/login.css" rel="stylesheet"  type="text/css" />

</head>
<body>
<div class="container">

      <form class="form-signin">
        <h3 class="form-signin-heading">欢迎使用Markdown Doc</h3>
        <input type="txt" id="login-username" class="form-control" placeholder="用户名"  role="login" autofocus>
        <input type="password" id="login-password" class="form-control" placeholder="密码" role="login">
        <div class="checkbox">
          <label>
            <input type="checkbox" value="remember-me"> 记住用户名
          </label>
        </div>
        <a class="btn btn-lg btn-primary" id="submitBtn">登 录</a>
        <label id="errorMsg" class="alert-danger"></label>
      </form>

    </div> <!-- /container -->
<script type="text/javascript" src="${ctx}/resources/js/common.js" ></script>
<script type="text/javascript" src="${ctx}/resources/jquery/1.6/jquery.js" ></script>
<script type="text/javascript" src="${ctx}/resources/jquery/jquery.cookie.js" ></script>
<script type="text/javascript">
$(document).ready(function() {

	$("#submitBtn").click(function() {
		var valid = true;
		$("input[role=login]").each(function(){
			if (checkEmptyString($(this).val())) {
				$(this).css({"border-color": "#DA5430"});
				valid = false;
			} else {
				$(this).css({"border-color": ""});
			}
		});
		if (!valid) {
			return;
		}
		$.ajax({
			url: "/auth/doLogin",
			type: "POST",
			contentType:"application/x-www-form-urlencoded;charset=UTF-8",
			async: "true",
			dataType: "JSON",
			data: {
				userName: $.trim($("#login-username").val()),
				pwd: $.trim($("#login-password").val())
			},
			success: function(data){
				if (data.code == 200) {
					var res = data.data;
					$.cookie('token', res.token, { path: "/"});
					$.cookie('userId', res.userId, { path: "/"});
					$.cookie('userName', res.userName, { path: "/"});
					setTimeout(function(){
						window.location.href = "/markdown";
					},1000)
				} else {
					$('#errorMsg').html(data.message);
					return false;
				}
			},
			error: function(data){alert('error');}
		});
	});

});

</script>
</body>
</html>