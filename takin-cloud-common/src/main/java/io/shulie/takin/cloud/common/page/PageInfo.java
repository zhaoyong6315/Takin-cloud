package io.shulie.takin.cloud.common.page;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.pagehelper.Page;
import io.shulie.takin.cloud.common.constants.PageHelperConstants;
import org.apache.commons.collections4.MapUtils;

/**
 * 说明: 分页助手<br>对Page<E>结果进行包装
 * <p>
 * <a href="http://git.oschina.net/free/Mybatis_PageHelper">项目地址</a>
 * </p>
 *
 * @author shulie
 * @date 2018年4月13日
 */
@SuppressWarnings("all")
public class PageInfo<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    //当前页
    private int pageNum;

    //每页的数量
    private int pageSize;

    //总页数
    private long totalPage;

    //总记录数
    private long total;

    //结果集
    private List<T> list;

    /**
     * 无参构造
     */
    public PageInfo() {

    }

    /**
     * 包装Page对象
     *
     * @param list
     */
    public PageInfo(List<T> list) {
        if (list instanceof Page) {
            Page page = (Page)list;
            this.pageNum = page.getPageNum();
            if (page.getPageSize() < 1) {
                this.pageSize = page.getResult().size();
            } else {
                this.pageSize = page.getPageSize();
            }

            this.list = page;
            if (page.getTotal() < 1) {
                this.total = page.getResult().size();
            } else {
                this.total = page.getTotal();
            }
        } else if (list instanceof Collection) {
            this.pageNum = 1;
            this.pageSize = list.size() == 0 ? 10 : list.size();

            //this.pages = 1;
            this.list = list;
            this.total = list.size();
        }
        this.pages();
    }

    public <E> PageInfo(List<E> list, Function<E, T> function) {
        if (list == null) {
            this.pageNum = 1;
            this.pageSize = 1;
            this.total = 0;
        } else if (list instanceof Page) {
            Page page = (Page)list;
            this.pageNum = page.getPageNum();
            this.pageSize = page.getPageSize();
            this.total = page.getTotal();
        } else if (list instanceof Collection) {
            this.pageNum = 1;
            this.pageSize = list.size();
            this.total = list.size();
        }
        this.pages();
        this.list = Optional.ofNullable(list).map(
            ls -> ls.stream().map(l -> function.apply(l)).collect(Collectors.toList())).orElse(Collections.emptyList());
    }

    /**
     * 从参数集合中获取页码
     *
     * @param params
     * @return int
     * @author Gavin
     * @date 2017年11月23日下午2:32:34
     */
    public static int getPageNum(Map<String, Object> params) {
        int pageNum = MapUtils.getIntValue(params, PageHelperConstants.PAGENUM, 1);
        if (pageNum == 0) {
            pageNum = 1;
        }
        return pageNum;
    }

    /**
     * 从参数集合中获取每页条数
     *
     * @param params
     * @return int
     * @author Gavin
     * @date 2017年11月23日下午2:32:51
     */
    public static int getPageSize(Map<String, Object> params) {
        int pageSize = MapUtils.getIntValue(params, PageHelperConstants.PAGESIZE, 10);
        if (pageSize == 0) {
            pageSize = 10;
        }
        //防止数据太多
        if (pageSize > 200) {
            pageSize = 200;
        }
        return pageSize;
    }

    private void pages() {
        if (this.total % this.pageSize == 0) {
            this.totalPage = this.total / this.pageSize;
        } else {
            this.totalPage = this.total / this.pageSize + 1;
        }
    }

    /**
     * 2018年5月21日
     *
     * @return the pageNum
     * @author shulie
     * @version 1.0
     */
    public int getPageNum() {
        return pageNum;
    }

    /**
     * 2018年5月21日
     *
     * @param pageNum the pageNum to set
     * @author shulie
     * @version 1.0
     */
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * 2018年5月21日
     *
     * @return the pageSize
     * @author shulie
     * @version 1.0
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 2018年5月21日
     *
     * @param pageSize the pageSize to set
     * @author shulie
     * @version 1.0
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 2018年5月21日
     *
     * @return the total
     * @author shulie
     * @version 1.0
     */
    public long getTotal() {
        return total;
    }

    /**
     * 2018年5月21日
     *
     * @param total the total to set
     * @author shulie
     * @version 1.0
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 2018年5月21日
     *
     * @return the list
     * @author shulie
     * @version 1.0
     */
    public List<T> getList() {
        return list;
    }

    /**
     * 2018年5月21日
     *
     * @param list the list to set
     * @author shulie
     * @version 1.0
     */
    public void setList(List<T> list) {
        this.list = list;
    }

    /**
     * 2018年5月21日
     *
     * @return the toString字符串
     * @author shulie
     * @version 1.0
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PageInfo{");
        sb.append("pageNum=").append(pageNum);
        sb.append(", pageSize=").append(pageSize);
        sb.append(", total=").append(total);
        sb.append(", list=").append(list);
        sb.append(", navigatepageNums=");
        sb.append('}');
        return sb.toString();
    }

    public long getTotalPage() {
        return totalPage;
    }

    public PageInfo<T> setTotalPage(long totalPage) {
        this.totalPage = totalPage;
        return this;
    }
}
