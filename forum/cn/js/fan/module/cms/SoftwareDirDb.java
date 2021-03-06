package cn.js.fan.module.cms;

import java.io.*;
import java.sql.*;
import java.util.*;

import cn.js.fan.base.*;
import cn.js.fan.db.*;
import cn.js.fan.security.*;
import cn.js.fan.util.*;
import com.cloudwebsoft.framework.db.JdbcTemplate;

public class SoftwareDirDb extends ObjectDb implements Serializable {
    private String code = "", name = "", description = "", parentCode = "-1",
            rootCode = "", addDate = "";
    private int orders = 1, layer = 1, childCount = 0;
    boolean isHome = false;
    private boolean loaded = false;

    public static final String ROOTCODE = "root";

    public static final int NOTEMPLATE = -1;

    public SoftwareDirDb() {
        init();
    }

    public SoftwareDirDb(String code) {
        this.code = code;
        load();
        init();
    }

    public ObjectDb getObjectRaw(PrimaryKey pk) {
        return new SoftwareDirDb(pk.getStrValue());
    }

    public void setQueryCreate() {

    }

    public void setQuerySave() {
        this.QUERY_SAVE = "update cws_cms_software_dir set name=?,description=?,dir_type=?,orders=?,child_count=?,layer=? where code=?";
    }

    public void setQueryDel() {

    }

    public void setQueryLoad() {
        this.QUERY_LOAD = "select code,name,description,parent_code,root_code,orders,layer,child_count,add_date,dir_type from cws_cms_software_dir where code=?";
    }

    public void load() {
        ResultSet rs = null;
        Conn conn = new Conn(connname);
        try {
            PreparedStatement ps = conn.prepareStatement(this.QUERY_LOAD);
            ps.setString(1, code);
            rs = conn.executePreQuery();
            if (rs != null && rs.next()) {
                this.code = rs.getString(1);
                name = rs.getString(2);
                description = rs.getString(3);
                parentCode = rs.getString(4);
                rootCode = rs.getString(5);
                orders = rs.getInt(6);
                layer = rs.getInt(7);
                childCount = rs.getInt(8);
                // addDate = rs.getString(9).substring(0, 19);
                type = rs.getInt(10);
                loaded = true;
            }
        } catch (SQLException e) {
            logger.error("load:" + e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {}
                rs = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setRootCode(String c) {
        this.rootCode = c;
    }

    public void setType(int t) {
        this.type = t;
    }

    public void setName(String n) {
        this.name = n;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public int getOrders() {
        return orders;
    }

    public boolean getIsHome() {
        return isHome;
    }

    public void setParentCode(String p) {
        this.parentCode = p;
    }

    public String getParentCode() {
        return this.parentCode;
    }

    public void setIsHome(boolean b) {
        this.isHome = b;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public String getRootCode() {
        return rootCode;
    }

    public int getLayer() {
        return layer;
    }

    public String getDescription() {
        return description;
    }

    public int getType() {
        return type;
    }

    public int getChildCount() {
        return childCount;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public Vector getChildren() {
        Vector v = new Vector();
        String sql =
                "select code from cws_cms_software_dir where parent_code=? order by orders";
        Conn conn = new Conn(connname);
        ResultSet rs = null;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, code);
            rs = conn.executePreQuery();
            if (rs != null) {
                while (rs.next()) {
                    String c = rs.getString(1);
                    v.addElement(getSoftwareDirDb(c));
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {}
                rs = null;
            }
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
        return v;
    }

    /**
     * 取出code结点的所有孩子结点
     * @param code String
     * @return ResultIterator
     * @throws ErrMsgException
     */
    public Vector getAllChild(Vector vt, SoftwareDirDb leaf) throws
            ErrMsgException {
        Vector children = leaf.getChildren();
        if (children.isEmpty())
            return children;
        vt.addAll(children);
        Iterator ir = children.iterator();
        while (ir.hasNext()) {
            SoftwareDirDb lf = (SoftwareDirDb) ir.next();
            getAllChild(vt, lf);
        }
        return children;
    }

    public String toString() {
        return "Leaf is " + name;
    }

    private int type;

    public synchronized boolean save() {
        Conn conn = new Conn(connname);
        int r = 0;
        boolean re = false;
        try {
            PreparedStatement ps = conn.prepareStatement(this.QUERY_SAVE);
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setInt(3, type);
            ps.setInt(4, orders);
            ps.setInt(5, childCount);
            ps.setInt(6, layer);
            ps.setString(7, code);

            r = conn.executePreUpdate();
            try {
                if (r == 1) {
                    re = true;
                    SoftwareDirCache dcm = new SoftwareDirCache();
                    dcm.refreshSave(code, parentCode);
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        } catch (SQLException e) {
            logger.error("修改出错！" + e.getMessage());
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
        return re;
    }

    /**
     * 更改了分类
     * @param newDirCode String
     * @return boolean
     */
    public synchronized boolean save(String newParentCode) {
        if (newParentCode.equals(parentCode))
            return false;
        // 把该结点加至新父结点，作为其最后一个孩子,同设其layer为父结点的layer + 1
        SoftwareDirDb lfparent = getSoftwareDirDb(newParentCode);
        int oldorders = orders;
        int neworders = lfparent.getChildCount() + 1;
        int parentLayer = lfparent.getLayer();
        String sql = "update cws_cms_software_dir set name=" +
                     StrUtil.sqlstr(name) +
                     ",description=" + StrUtil.sqlstr(description) +
                     ",dir_type=" + type +
                     ",parent_code=" + StrUtil.sqlstr(newParentCode) +
                     ",orders=" + neworders +
                     ",layer=" + (parentLayer + 1) +
                     " where code=" + StrUtil.sqlstr(code);

        String oldParentCode = parentCode;
        parentCode = newParentCode;
        SoftwareDirCache dcm = new SoftwareDirCache();

        int r = 0;

        RMConn conn = new RMConn(connname);
        try {
            r = conn.executeUpdate(sql);
            try {
                if (r == 1) {
                    dcm.refreshSave(code, parentCode);

                    dcm.removeFromCache(newParentCode);
                    dcm.removeFromCache(oldParentCode);
                    // DirListCacheMgr更新
                    SoftwareDirChildrenCache.remove(oldParentCode);
                    SoftwareDirChildrenCache.remove(newParentCode);

                    // 更新原来父结点中，位于本leaf之后的orders
                    sql =
                            "select code from cws_cms_software_dir where parent_code=" +
                            StrUtil.sqlstr(oldParentCode) +
                            " and orders>" + oldorders;
                    ResultIterator ri = conn.executeQuery(sql);
                    while (ri.hasNext()) {
                        ResultRecord rr = (ResultRecord) ri.next();
                        SoftwareDirDb dd = getSoftwareDirDb(rr.getString(1));
                        dd.setOrders(dd.getOrders() - 1);
                        dd.save();
                    }

                    // 更新其所有子结点的layer
                    Vector vt = new Vector();
                    getAllChild(vt, this);
                    int childcount = vt.size();
                    Iterator ir = vt.iterator();
                    while (ir.hasNext()) {
                        SoftwareDirDb childlf = (SoftwareDirDb) ir.next();
                        int layer = parentLayer + 1 + 1;
                        String pcode = childlf.getParentCode();
                        while (!pcode.equals(code)) {
                            layer++;
                            SoftwareDirDb lfp = getSoftwareDirDb(pcode);
                            pcode = lfp.getParentCode();
                        }

                        childlf.setLayer(layer);
                        childlf.save();
                    }

                    // 将其原来的父结点的孩子数-1
                    SoftwareDirDb oldParentLeaf = getSoftwareDirDb(
                            oldParentCode);
                    oldParentLeaf.setChildCount(oldParentLeaf.getChildCount() -
                                                1);
                    oldParentLeaf.save();

                    // 将其新父结点的孩子数 + 1
                    SoftwareDirDb newParentLeaf = getSoftwareDirDb(
                            newParentCode);
                    newParentLeaf.setChildCount(newParentLeaf.getChildCount() +
                                                1);
                    newParentLeaf.save();

                    logger.info("save: newParentLeaf.childcount=" +
                                newParentLeaf.getChildCount() +
                                " oldParentLeaf.childcount=" +
                                oldParentLeaf.getChildCount() +
                                " oldparentCode=" + oldParentCode +
                                " newParentCode=" + newParentCode +
                                " neworders=" + neworders);

                }
            } catch (Exception e) {
                logger.error("save1: " + e.getMessage());
            }
        } catch (SQLException e) {
            logger.error("save2: " + e.getMessage());
        }
        boolean re = r == 1 ? true : false;
        if (re) {
            dcm.removeFromCache(code);
        }
        return re;
    }

    public boolean AddChild(SoftwareDirDb childleaf) throws
            ErrMsgException {
        //计算得出插入结点的orders
        int childorders = childCount + 1;

        String updatesql = "";
        String insertsql = "insert into cws_cms_software_dir (code,name,parent_code,description,orders,root_code,child_count,layer,dir_type,add_date) values (?,?,?,?,?,?,?,?,?,?)";

        if (!SecurityUtil.isValidSql(insertsql))
            throw new ErrMsgException("请勿输入非法字符如;号等！");
        Conn conn = new Conn(connname);
        try {
            // 更改根结点的信息
            updatesql =
                    "update cws_cms_software_dir set child_count=child_count+1" +
                    " where code=" + StrUtil.sqlstr(code);
            conn.beginTrans();
            PreparedStatement ps = conn.prepareStatement(insertsql);
            ps.setString(1, childleaf.getCode());
            ps.setString(2, childleaf.getName());
            ps.setString(3, code);
            ps.setString(4, childleaf.getDescription());
            ps.setInt(5, childorders);
            ps.setString(6, rootCode);
            ps.setInt(7, 0);
            ps.setInt(8, layer + 1);
            ps.setInt(9, childleaf.getType());
            ps.setDate(10, new java.sql.Date(new java.util.Date().getTime()));
            conn.executePreUpdate();
            if (ps != null) {
                ps.close();
                ps = null;
            }

            conn.executeUpdate(updatesql);
            conn.commit();
            SoftwareDirCache dcm = new SoftwareDirCache();
            dcm.refreshAddChild(code);
        } catch (SQLException e) {
            conn.rollback();
            logger.error(e.getMessage());
            throw new ErrMsgException("请检查编码是否有重复！");
        } finally {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        }
        return true;
    }

    public SoftwareDirDb getSoftwareDirDb(String code) {
        SoftwareDirCache dcm = new SoftwareDirCache();
        return dcm.getSoftwareDirDb(code);
    }

    public boolean delsingle(SoftwareDirDb leaf) {
        String sql = "delete from cws_cms_software_dir where code=" +
                     StrUtil.sqlstr(leaf.getCode());
        JdbcTemplate jt = new JdbcTemplate();
        try {
            jt.beginTrans();
            boolean r = jt.executeUpdate(sql) == 1 ? true : false;
            sql =
                    "update cws_cms_software_dir set orders=orders-1 where parent_code=" +
                    StrUtil.sqlstr(leaf.getParentCode()) + " and orders>" +
                    leaf.getOrders();
            jt.executeUpdate(sql);
            sql =
                    "update cws_cms_software_dir set child_count=child_count-1 where code=" +
                    StrUtil.sqlstr(leaf.getParentCode());
            jt.executeUpdate(sql);

            // 删除目录下的所有文件
            SoftwareFileDb isfd = new SoftwareFileDb();
            Iterator ir = isfd.listOfDir(leaf.getCode()).iterator();
            while (ir.hasNext()) {
                isfd = (SoftwareFileDb)ir.next();
                isfd.del(jt);
            }
            jt.commit();
        } catch (SQLException e) {
            jt.rollback();
            logger.error(e.getMessage());
            return false;
        } finally {
            SoftwareDirCache dcm = new SoftwareDirCache();
            dcm.refreshDel(leaf.getCode(), leaf.getParentCode());
        }
        return true;
    }

    public synchronized void del(SoftwareDirDb leaf) {
        delsingle(leaf);
        Iterator children = leaf.getChildren().iterator();
        while (children.hasNext()) {
            SoftwareDirDb lf = (SoftwareDirDb) children.next();
            del(lf);
        }
    }

    public boolean del() {
        del(this);
        return true;
    }

    public SoftwareDirDb getBrother(String direction) {
        String sql;
        RMConn rmconn = new RMConn(connname);
        SoftwareDirDb bleaf = null;
        try {
            if (direction.equals("down")) {
                sql =
                        "select code from cws_cms_software_dir where parent_code=" +
                        StrUtil.sqlstr(parentCode) +
                        " and orders=" + (orders + 1);
            } else {
                sql =
                        "select code from cws_cms_software_dir where parent_code=" +
                        StrUtil.sqlstr(parentCode) +
                        " and orders=" + (orders - 1);
            }

            ResultIterator ri = rmconn.executeQuery(sql);
            if (ri != null && ri.hasNext()) {
                ResultRecord rr = (ResultRecord) ri.next();
                bleaf = getSoftwareDirDb(rr.getString(1));
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return bleaf;
    }

    public boolean move(String direction) {
        String sql = "";

        // 取出该结点的移动方向上的下一个兄弟结点的orders
        boolean isexist = false;

        SoftwareDirDb bleaf = getBrother(direction);
        if (bleaf != null) {
            isexist = true;
        }

        //如果移动方向上的兄弟结点存在则移动，否则不移动
        if (isexist) {
            Conn conn = new Conn(connname);
            try {
                conn.beginTrans();
                if (direction.equals("down")) {
                    sql = "update cws_cms_software_dir set orders=orders+1" +
                          " where code=" + StrUtil.sqlstr(code);
                    conn.executeUpdate(sql);
                    sql = "update cws_cms_software_dir set orders=orders-1" +
                          " where code=" + StrUtil.sqlstr(bleaf.getCode());
                    conn.executeUpdate(sql);
                }

                if (direction.equals("up")) {
                    sql = "update cws_cms_software_dir set orders=orders-1" +
                          " where code=" + StrUtil.sqlstr(code);
                    conn.executeUpdate(sql);
                    sql = "update cws_cms_software_dir set orders=orders+1" +
                          " where code=" + StrUtil.sqlstr(bleaf.getCode());
                    conn.executeUpdate(sql);
                }
                conn.commit();
                SoftwareDirCache dcm = new SoftwareDirCache();
                dcm.refreshMove(code, bleaf.getCode());
            } catch (Exception e) {
                conn.rollback();
                logger.error(e.getMessage());
                return false;
            } finally {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            }
        }

        return true;
    }

    public void setQueryList() {
        this.QUERY_LIST = "select code from cws_cms_software_dir";
    }

    public void setPrimaryKey() {
        primaryKey = new PrimaryKey("code", PrimaryKey.TYPE_STRING);
    }

    public ObjectDb getObjectDb(Object objKey) {
        return getSoftwareDirDb(objKey.toString());
    }

    /**
     * 只是单纯为了实现纯虚函数
     * @param sql String
     * @return int
     */
    public int getObjectCount(String sql) {
        return 0;
    }

    /**
     * 只为实现纯虚函数
     * @param query String
     * @param startIndex int
     * @return Object[]
     */
    public Object[] getObjectBlock(String query, int startIndex) {
        return null;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public void setOrders(int orders) {
        this.orders = orders;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }
}
