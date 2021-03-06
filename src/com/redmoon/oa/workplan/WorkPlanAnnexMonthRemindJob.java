package com.redmoon.oa.workplan;

import org.quartz.Job;
import org.quartz.JobExecutionException;
import org.quartz.JobExecutionContext;
import cn.js.fan.db.SQLFilter;
import cn.js.fan.util.DateUtil;
import java.util.Iterator;
import com.cloudwebsoft.framework.aop.ProxyFactory;
import com.redmoon.oa.message.IMessage;
import com.redmoon.oa.message.MessageDb;
import com.redmoon.oa.person.UserDb;
import cn.js.fan.web.SkinUtil;
import com.redmoon.oa.person.UserMgr;
import cn.js.fan.util.StrUtil;
import com.redmoon.oa.sms.IMsgUtil;
import com.redmoon.oa.sms.SMSFactory;
import java.util.Calendar;
import cn.js.fan.util.*;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class WorkPlanAnnexMonthRemindJob implements Job {
    public WorkPlanAnnexMonthRemindJob() {
    }

    public void execute(JobExecutionContext jobExecutionContext) throws
            JobExecutionException {
        Calendar cal = Calendar.getInstance();
        int m = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        // 如果当前为本年度的第一个月，则取去年的最后一月
        if (m==1) {
            year = year - 1;
            m = 12;
        }
        else
            m -= 1;

        java.util.Date d = new java.util.Date();
        String dtstr = DateUtil.format(d, "yyyy-MM-dd");
        String sql = "select id from work_plan where progress<100 and beginDate<=" + SQLFilter.getDateStr(dtstr, "yyyy-MM-dd") + " and endDate>=" + SQLFilter.getDateStr(dtstr, "yyyy-MM-dd");
        WorkPlanDb wpd = new WorkPlanDb();
        WorkPlanAnnexDb wpad = new WorkPlanAnnexDb();
        Iterator ir = wpd.list(sql).iterator();

        boolean isToMobile = SMSFactory.isUseSMS();
        IMessage imsg = null;
        // String title = "系统提醒您，请及时填写工作计划：$title月报";
        // String content = "您上月的月报尚未填写，请及时填写！该计划的内容如下：$content";

        com.redmoon.oa.Config cfg = new com.redmoon.oa.Config();
        String title = cfg.get("workplan_annex_month_remind_title");
        String content = cfg.get("workplan_annex_month_remind_content");
        UserMgr um = new UserMgr();
        if (isToMobile) {
            ProxyFactory proxyFactory = new ProxyFactory(
                    "com.redmoon.oa.message.MessageDb");
            imsg = (IMessage) proxyFactory.getProxy();
        }
        IMsgUtil imu = SMSFactory.getMsgUtil();

        while (ir.hasNext()) {
            wpd = (WorkPlanDb)ir.next();

            // 检查上月周报是否已填写
            WorkPlanAnnexDb wpa = wpad.getWorkPlanAnnexDb(wpd.getId(), year, WorkPlanAnnexDb.TYPE_MONTH, m);
            if (wpa!=null)
                continue;

            String t = title.replaceFirst("\\$title", wpd.getTitle());
            String c = content.replaceFirst("\\$content", wpd.getContent());
            String[] principals = wpd.getPrincipals();
            if (isToMobile) {

                int len = principals.length;
                for (int i = 0; i < len; i++) {
                    try {
                        UserDb ud = um.getUserDb(principals[i]);
                        imsg.sendSysMsg(principals[i], t, c);
                        imu.send(ud, t, MessageDb.SENDER_SYSTEM);
                    } catch (ErrMsgException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            else {
                // 发送信息
                MessageDb md = new MessageDb();
                int len = principals.length;
                String action = "action=" + MessageDb.ACTION_WORKPLAN + "|id=" + wpd.getId();
                for (int i = 0; i < len; i++) {
                    try {
                        md.sendSysMsg(principals[i], t, c, action);
                    } catch (ErrMsgException ex1) {
                        ex1.printStackTrace();
                    }
                }
               }
        }

    }

}
