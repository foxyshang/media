package cn.embed.renderer;

import android.content.Context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.embed.media.surfaceEncoder.filter.CameraFilter;
import cn.embed.media.surfaceEncoder.filter.FilterGroup;
import cn.embed.media.surfaceEncoder.filter.FilterTypeBean;
import cn.embed.media.surfaceEncoder.filter.IFilter;

public class FilterManager {
    List<FilterTypeBean> filterList;
    boolean isFilterChange = false;


    public FilterManager() {
        filterList = new ArrayList<>();
    }

    /**
     * 将不同类型的filter 最终转换为filterGroup
     *
     * @return
     */
    public IFilter getFilter(Context context) {
        List<IFilter> filters = new ArrayList<>();
        filters.add(new CameraFilter(context));
        if (filterList.size() > 0) {
            for (FilterTypeBean filterTypeBean : filterList) {
                filters.add(filterTypeBean.getFilter(context));
            }
        }
        FilterGroup<IFilter> filterGroup = new FilterGroup(filters);
        isFilterChange = false;
        return filterGroup;
    }

    /**
     * 添加一组滤镜
     *
     * @param filterTypeBean
     */
    public void addFilter(FilterTypeBean filterTypeBean) {
        for (Iterator<FilterTypeBean> it = filterList.iterator(); it.hasNext(); ) {
            FilterTypeBean filterType = it.next();
            if (filterType.getFilterType() == filterTypeBean.getFilterType()) {
                it.remove();
            }
        }
        filterList.add(filterTypeBean);
        isFilterChange = true;
    }


    public void setFilterTypeBeanList(List<FilterTypeBean> filterList) {
        this.filterList = filterList;
        isFilterChange = true;
    }

    public void cleanFilterByType(int type) {
        for (Iterator<FilterTypeBean> it = filterList.iterator(); it.hasNext(); ) {
            FilterTypeBean filterType = it.next();
            if (filterType.getFilterType() == type) {
                it.remove();
            }
        }
        isFilterChange = true;
    }

    public boolean isFilterChange() {
        return isFilterChange;
    }
}
