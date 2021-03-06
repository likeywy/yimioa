<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="cn.js.fan.util.*"%>
<%@ page import="java.util.*"%>
<%@ page import="cn.js.fan.web.*"%>
<%@ page import="com.redmoon.oa.visual.*"%>
<%@ page import="com.redmoon.oa.flow.*"%>
<%@ page import="com.redmoon.oa.ui.*"%>
<jsp:useBean id="privilege" scope="page" class="com.redmoon.oa.pvg.Privilege"/>
<%
String priv="read";
if (!privilege.isUserPrivValid(request,priv)) {
	out.print(SkinUtil.makeErrMsg(request, SkinUtil.LoadString(request, "pvg_invalid")));
	return;
}
String myname = privilege.getUser( request );

String op = ParamUtil.get(request, "op");

String formCode = ParamUtil.get(request, "formCode");
if (formCode.equals("")) {
	out.print(SkinUtil.makeErrMsg(request, "编码不能为空！"));
	return;
}

ModulePrivDb mpd = new ModulePrivDb(formCode);
if (!mpd.canUserModify(privilege.getUser(request))) {
	%>
	<link type="text/css" rel="stylesheet" href="<%=SkinMgr.getSkinPath(request)%>/css.css" />	
	<%
	out.print(cn.js.fan.web.SkinUtil.makeErrMsg(request, cn.js.fan.web.SkinUtil.LoadString(request, "pvg_invalid")));
	return;
}

int id = ParamUtil.getInt(request, "id");
// 置嵌套表需要用到的cwsId
request.setAttribute("cwsId", "" + id);
// 置嵌套表需要用到的页面类型
request.setAttribute("pageType", "edit");
// 置NestSheetCtl需要用到的formCode
request.setAttribute("formCode", formCode);

FormMgr fm = new FormMgr();
FormDb fd = fm.getFormDb(formCode);

com.redmoon.oa.visual.FormDAOMgr fdm = new com.redmoon.oa.visual.FormDAOMgr(fd);

int isShowNav = ParamUtil.getInt(request, "isShowNav", 1);
int parentId = ParamUtil.getInt(request, "parentId", -1);

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>项目编辑</title>
<meta name="renderer" content="ie-stand">
<script src="../inc/livevalidation_standalone.js"></script>
<link type="text/css" rel="stylesheet" href="<%=SkinMgr.getSkinPath(request)%>/css.css" />
<script src="../inc/common.js"></script>
<script type="text/javascript" src="../js/jquery1.7.2.min.js"></script>
<script src="../js/jquery-alerts/jquery.alerts.js" type="text/javascript"></script>
<script src="../js/jquery-alerts/cws.alerts.js" type="text/javascript"></script>
<link href="../js/jquery-alerts/jquery.alerts.css" rel="stylesheet"	type="text/css" media="screen" />
<script src="../inc/upload.js"></script>
<script src="<%=Global.getRootPath()%>/inc/flow_dispose_js.jsp"></script>
<script src="<%=Global.getRootPath()%>/inc/flow_js.jsp"></script>

<script src="../inc/ajax_getpage.jsp"></script>

<script src="../js/jquery-ui/jquery-ui.js"></script>
<script src="../js/jquery.bgiframe.js"></script>
<link type="text/css" rel="stylesheet" href="<%=SkinMgr.getSkinPath(request)%>/jquery-ui/jquery-ui.css" />

<link rel="stylesheet" type="text/css" href="../js/datepicker/jquery.datetimepicker.css"/>
<script src="../js/datepicker/jquery.datetimepicker.js"></script>

<!-- 
<style type="text/css"> 
@import url("<%=request.getContextPath()%>/util/jscalendar/calendar-win2k-2.css"); 
</style>

<script type="text/javascript" src="<%=request.getContextPath()%>/util/jscalendar/calendar.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/util/jscalendar/lang/calendar-zh.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/util/jscalendar/calendar-setup.js"></script>
 -->

<script>
$(function() {
	SetNewDate();
});

function setradio(myitem,v) {
     var radioboxs = document.all.item(myitem);
     if (radioboxs!=null)
     {
       for (i=0; i<radioboxs.length; i++)
          {
            if (radioboxs[i].type=="radio")
              {
                 if (radioboxs[i].value==v)
				 	radioboxs[i].checked = true;
              }
          }
     }
}

function SubmitResult() {
	// 检查是否已选择意见
	if (getradio("resultValue")==null || getradio("resultValue")=="") {
		jAlert("您必须选择一项意见!","提示");
		return false;
	}
	visualForm.op.value='finish';
	visualForm.submit();
}

// 控件完成上传后，调用Operate()
function Operate() {
	// alert(redmoonoffice.ReturnMessage);
}
</script>
</head>
<body>
<%

if (op.equals("saveformvalue")) {
	boolean re = false;
	try {
		re = fdm.update(application, request);
	}
	catch (ErrMsgException e) {
		out.print(StrUtil.Alert_Back(com.cloudwebsoft.framework.security.AntiXSS.clean(e.getMessage())));
		return;
	}
	if (re) {
		out.print(StrUtil.jAlert_Redirect("操作成功！","提示", request.getRequestURL() + "?parentId=" + parentId + "&id=" + id + "&formCode=" + StrUtil.UrlEncode(formCode) + "&isShowNav=" + isShowNav));
	}
	else
		out.print(StrUtil.jAlert_Back("操作失败！","提示"));
	return;
}
else if (op.equals("delAttach")) {
	int attachId = ParamUtil.getInt(request, "attachId");
	com.redmoon.oa.visual.Attachment att = new com.redmoon.oa.visual.Attachment(attachId);
	att.del();
}
if (isShowNav==1) {
%>
<%@ include file="module_inc_menu_top.jsp"%>
<script>
o("menu1").className="current";
</script>
<div class="spacerH"></div>
<%}%>
<form action="?op=saveformvalue&amp;id=<%=id%>&amp;formCode=<%=StrUtil.UrlEncode(formCode)%>&isShowNav=<%=isShowNav%>&parentId=<%=parentId%>" method="post" enctype="multipart/form-data" name="visualForm" id="visualForm">
<table width="98%" border="0" align="center" cellpadding="0" cellspacing="0">
    <tr>
      <td align="left"><table width="100%">
        <tr>
          <td><%
			com.redmoon.oa.visual.Render rd = new com.redmoon.oa.visual.Render(request, id, fd);
			out.print(rd.rend());
		  %></td>
        </tr>
      </table></td>
    </tr>
	<%if (fd.isHasAttachment()) {%>	
    <tr>
      <td align="left"><script>initUpload()</script>
      </td>
    </tr>
	<%}%>
    <tr>
      <td align="left"><%
com.redmoon.oa.visual.FormDAO fdao = fdm.getFormDAO(id);
Iterator ir = fdao.getAttachments().iterator();
				  while (ir.hasNext()) {
				  	com.redmoon.oa.visual.Attachment am = (com.redmoon.oa.visual.Attachment) ir.next(); %>
          <table width="82%"  border="0" cellpadding="0" cellspacing="0">
            <tr>
              <td width="5%" height="31" align="right"><img src="<%=Global.getRootPath()%>/images/attach.gif" /></td>
              <td>&nbsp; <a target="_blank" href="<%=Global.getRootPath()%>/visual_getfile.jsp?attachId=<%=am.getId()%>"><%=am.getName()%></a>&nbsp;&nbsp;&nbsp;&nbsp;[<a href="#" onClick="jConfirm(('您确定要删除吗？','提示',function(r){if(!r){return;}else{window.location.href='?op=delAttach&amp;id=<%=id%>&amp;formCode=<%=StrUtil.UrlEncode(formCode)%>&amp;attachId=<%=am.getId()%>'}})">删除</a>]<br />
              </td>
            </tr>
          </table>
        <%}
			  %></td>
    </tr>
    <tr>
      <td height="30" align="center"><input name="id" value="<%=id%>" type="hidden" />
          <input type="submit" name="Submit" value=" 修 改 " class="btn" />
        &nbsp;&nbsp;</td>
    </tr>
</table>
</form>
</body>
</html>
