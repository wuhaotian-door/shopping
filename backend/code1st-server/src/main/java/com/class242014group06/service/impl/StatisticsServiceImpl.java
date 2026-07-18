package com.class242014group06.service.impl;

import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.InheritorProfile;
import com.class242014group06.entity.User;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.InheritorProfileMapper;
import com.class242014group06.mapper.ProductOrderMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private InheritorMapper inheritorMapper;
    @Autowired
    private InheritorProfileMapper inheritorProfileMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductOrderMapper productOrderMapper;

    @Override
    public List<Map<String, Object>> getRegionDistribution() {
        List<Inheritor> inheritors = inheritorMapper.getAllEnabled();
        Map<String, Long> regionCount = inheritors.stream()
                .filter(i -> i.getRegion() != null)
                .collect(Collectors.groupingBy(
                        i -> {
                            // 提取省份
                            String r = i.getRegion();
                            if (r.contains("省")) return r.substring(0, r.indexOf("省") + 1);
                            if (r.contains("市") && r.length() <= 4) return r;
                            return r.length() > 3 ? r.substring(0, 3) : r;
                        },
                        Collectors.counting()
                ));

        return regionCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAgeDistribution() {
        List<Inheritor> inheritors = inheritorMapper.getAllEnabled();
        int[] ageGroups = {0, 0, 0, 0, 0}; // <30, 30-39, 40-49, 50-59, 60+
        String[] labels = {"30岁以下", "30-39岁", "40-49岁", "50-59岁", "60岁以上"};

        for (Inheritor i : inheritors) {
            if (i.getAge() == null) continue;
            int age = i.getAge();
            if (age < 30) ageGroups[0]++;
            else if (age < 40) ageGroups[1]++;
            else if (age < 50) ageGroups[2]++;
            else if (age < 60) ageGroups[3]++;
            else ageGroups[4]++;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int j = 0; j < labels.length; j++) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", labels[j]);
            m.put("value", ageGroups[j]);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getCraftDistribution() {
        List<InheritorProfile> profiles = inheritorProfileMapper.listAll();
        Map<String, Long> craftCount = new HashMap<>();

        for (InheritorProfile p : profiles) {
            if (p.getCraftTypes() == null || p.getCraftTypes().isEmpty()) continue;
            String[] crafts = p.getCraftTypes().split("[,，]");
            for (String c : crafts) {
                String craft = c.trim();
                if (!craft.isEmpty()) {
                    craftCount.merge(craft, 1L, Long::sum);
                }
            }
        }

        return craftCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getCityDistribution() {
        List<Inheritor> inheritors = inheritorMapper.getAllEnabled();
        // 按城市聚合计数
        Map<String, Long> cityCount = inheritors.stream()
                .filter(i -> i.getRegion() != null)
                .collect(Collectors.groupingBy(
                        i -> extractCity(i.getRegion()),
                        Collectors.counting()
                ));

        // 获取每个城市对应的省份
        Map<String, String> cityProvince = inheritors.stream()
                .filter(i -> i.getRegion() != null)
                .collect(Collectors.toMap(
                        i -> extractCity(i.getRegion()),
                        i -> extractProvince(i.getRegion()),
                        (a, b) -> a
                ));

        return cityCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("city", e.getKey());
                    m.put("province", cityProvince.getOrDefault(e.getKey(), ""));
                    m.put("value", e.getValue().intValue());
                    return m;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("value"), (Integer) a.get("value")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getCityDistributionByProvince(String province) {
        List<Inheritor> inheritors = inheritorMapper.getAllEnabled();
        // 只筛选该省份的传承人，按城市聚合
        Map<String, Long> cityCount = inheritors.stream()
                .filter(i -> i.getRegion() != null && i.getRegion().startsWith(province))
                .collect(Collectors.groupingBy(
                        i -> mapToPrefecture(extractCity(i.getRegion())),
                        Collectors.counting()
                ));

        return cityCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    // 保留完整市名（如"苏州市"），与GeoJSON中的名称一致
                    m.put("name", e.getKey());
                    m.put("value", e.getValue().intValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 县级市→地级市映射（GeoJSON使用地级市边界，需将县级数据汇总到地级市） */
    private static String mapToPrefecture(String city) {
        switch (city) {
            case "宜兴市": return "无锡市";
            case "安吉县": return "湖州市";
            default: return city;
        }
    }

    /** 提取城市名：江苏省苏州市 → 苏州市 */
    private String extractCity(String region) {
        if (region == null) return "";
        if (region.contains("省")) {
            int start = region.indexOf("省") + 1;
            return region.substring(start);
        }
        // 直辖市：北京市朝阳区 → 朝阳区
        if (region.contains("市") && region.length() > 2) {
            int cityEnd = region.indexOf("市");
            if (cityEnd >= 0 && cityEnd < region.length() - 1) {
                return region.substring(cityEnd + 1);
            }
        }
        // 特别行政区：香港特别行政区中西区 → 中西区
        if (region.contains("特别行政区")) {
            int sarEnd = region.indexOf("特别行政区") + 5;
            if (sarEnd < region.length()) {
                return region.substring(sarEnd);
            }
        }
        return region;
    }

    /** 提取省份名：江苏省苏州市 → 江苏省, 北京市朝阳区 → 北京市 */
    private String extractProvince(String region) {
        if (region == null) return "";
        if (region.contains("省")) return region.substring(0, region.indexOf("省") + 1);
        // 直辖市
        if (region.contains("市") && region.indexOf("市") >= 0) {
            return region.substring(0, region.indexOf("市") + 1);
        }
        // 特别行政区
        if (region.contains("特别行政区")) {
            return region.substring(0, region.indexOf("特别行政区") + 5);
        }
        return region.length() > 3 ? region.substring(0, 3) : region;
    }

    @Override
    public List<Map<String, Object>> getRegistrationTrend() {
        List<User> users = userMapper.listAll();
        List<Inheritor> inheritors = inheritorMapper.listAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        // 按月统计用户注册
        Map<String, Long> userByMonth = users.stream()
                .filter(u -> u.getCreateTime() != null)
                .collect(Collectors.groupingBy(u -> u.getCreateTime().format(fmt), TreeMap::new, Collectors.counting()));

        // 按月统计传承人注册
        Map<String, Long> inheritorByMonth = inheritors.stream()
                .filter(i -> i.getCreateTime() != null)
                .collect(Collectors.groupingBy(i -> i.getCreateTime().format(fmt), TreeMap::new, Collectors.counting()));

        // 合并所有月份
        TreeSet<String> allMonths = new TreeSet<>();
        allMonths.addAll(userByMonth.keySet());
        allMonths.addAll(inheritorByMonth.keySet());

        // 只保留最近12个月
        List<String> months = allMonths.stream().sorted().collect(Collectors.toList());
        if (months.size() > 12) {
            months = months.subList(months.size() - 12, months.size());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String month : months) {
            Map<String, Object> m = new HashMap<>();
            m.put("month", month);
            m.put("users", userByMonth.getOrDefault(month, 0L));
            m.put("inheritors", inheritorByMonth.getOrDefault(month, 0L));
            m.put("total", userByMonth.getOrDefault(month, 0L) + inheritorByMonth.getOrDefault(month, 0L));
            result.add(m);
        }
        return result;
    }

    @Override
    public Map<String, Object> getOrderStats() {
        // 商品订单状态分布
        Map<String, Long> productStatus = new LinkedHashMap<>();
        productStatus.put("待发货", (long) productOrderMapper.countByStatus(0));
        productStatus.put("已发货", (long) productOrderMapper.countByStatus(1));
        productStatus.put("已完成", (long) productOrderMapper.countByStatus(2));
        productStatus.put("已取消", (long) productOrderMapper.countByStatus(3));

        List<Map<String, Object>> statusList = new ArrayList<>();
        for (Map.Entry<String, Long> e : productStatus.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            statusList.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("statusDistribution", statusList);
        result.put("totalOrders", productStatus.values().stream().mapToLong(Long::longValue).sum());
        return result;
    }

    @Override
    public Map<String, Object> getOverview() {
        List<Inheritor> inheritors = inheritorMapper.getAllEnabled();
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalInheritors", inheritors.size());
        overview.put("regionCount", getRegionDistribution().size());
        overview.put("avgAge", inheritors.stream()
                .filter(i -> i.getAge() != null)
                .mapToInt(Inheritor::getAge).average().orElse(0));
        return overview;
    }
}
