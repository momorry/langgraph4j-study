package com.moli.langgraph.util;

/**
 *
 * @author likethewind
 * @since 2026/5/26 15:23
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author likethewind
 * @since 2026/5/26 15:23
 *
 */
public class MoneyFormatUtil {

    private static final Pattern NUMBER_WITH_UNIT_PATTERN = Pattern.compile(
            "([+-]?\\d*\\.?\\d+)\\s*(万亿|亿|万)?");

    private static final Pattern TEXT_NUMBER_PATTERN = Pattern.compile(
            "(?<![\\d.])([+-]?(?:\\d{1,3}(?:,\\d{3})*|\\d+)(?:\\.\\d+)?)(\\s*[万亿亿万])?(?![\\d.])");

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(<[^>]*>)");

    public static Double parseNumberWithUnit(String text) {
        if (text == null) {
            return null;
        }
        text = text.trim().replace(",", "");
        if (text.isEmpty()) {
            return null;
        }

        Matcher matcher = NUMBER_WITH_UNIT_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return null;
        }

        String numStr = matcher.group(1);
        String unit = matcher.group(2);

        double value;
        try {
            value = Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return null;
        }

        if ("万".equals(unit)) {
            value *= 10_000;
        } else if ("亿".equals(unit)) {
            value *= 100_000_000;
        } else if ("万亿".equals(unit)) {
            value *= 1_000_000_000_000L;
        }

        return value;
    }

    public static String formatNumber(double value) {
        if (value == 0) {
            return "0";
        }

        String sign = value < 0 ? "-" : "";
        double absValue = Math.abs(value);

        String formatted;
        if (absValue >= 1_000_000_000_000L) {
            formatted = String.format("%.2f万亿", absValue / 1_000_000_000_000L);
        } else if (absValue >= 100_000_000) {
            formatted = String.format("%.2f亿", absValue / 100_000_000);
        } else if (absValue >= 10_000) {
            formatted = String.format("%.2f万", absValue / 10_000);
        } else {
            return "";
        }

        return sign + formatted;
    }

    public static String replaceNumbersInPlainText(String text) {
        Matcher matcher = TEXT_NUMBER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String full = matcher.group(0);
            String numPart = matcher.group(1);
            String unitPart = matcher.group(2);
            if (unitPart == null) {
                unitPart = "";
            }
            String candidate = numPart + unitPart;

            Double parsed = parseNumberWithUnit(candidate);
            if (parsed != null) {
                String formatted = formatNumber(parsed);
                if (!formatted.isEmpty()) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(full));
                }
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(full));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static List<String> processHtmlList(List<String> htmlList) {
        List<String> result = new ArrayList<>();
        for (String html : htmlList) {
            Matcher matcher = HTML_TAG_PATTERN.matcher(html);
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            while (matcher.find()) {
                if (matcher.start() > lastEnd) {
                    String text = html.substring(lastEnd, matcher.start());
                    sb.append(replaceNumbersInPlainText(text));
                }
                sb.append(matcher.group());
                lastEnd = matcher.end();
            }
            if (lastEnd < html.length()) {
                sb.append(replaceNumbersInPlainText(html.substring(lastEnd)));
            }
            result.add(sb.toString());
        }
        return result;
    }

    private static int passed = 0;
    private static int failed = 0;

    private static void assertEq(String label, Object actual, Object expected) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
            System.out.println("  ✓ " + label);
        } else {
            failed++;
            System.out.println("  ✗ " + label);
            System.out.println("      期望: " + expected);
            System.out.println("      实际: " + actual);
        }
    }

    public static void main(String[] args) {
        System.out.println("====== parseNumberWithUnit ======");
        assertEq("普通整数", parseNumberWithUnit("150000"), 150000.0);
        assertEq("带千分位", parseNumberWithUnit("150,000"), 150000.0);
        assertEq("带万单位", parseNumberWithUnit("15.5万"), 155000.0);
        assertEq("带亿单位", parseNumberWithUnit("3.2亿"), 320000000.0);
        assertEq("带万亿单位", parseNumberWithUnit("1.5万亿"), 1500000000000.0);
        assertEq("负数", parseNumberWithUnit("-100"), -100.0);
        assertEq("负数带万", parseNumberWithUnit("-2.5万"), -25000.0);
        assertEq("纯小数", parseNumberWithUnit("3.14"), 3.14);
        assertEq("带空格单位", parseNumberWithUnit("500 万"), 5000000.0);
        assertEq("空字符串", parseNumberWithUnit(""), null);
        assertEq("纯字母", parseNumberWithUnit("abc"), null);
        assertEq("null输入", parseNumberWithUnit(null), null);

        System.out.println("\n====== formatNumber ======");
        assertEq("零", formatNumber(0), "0");
        assertEq("1万", formatNumber(10000), "1.00万");
        assertEq("15万", formatNumber(150000), "15.00万");
        assertEq("3.2亿", formatNumber(320000000), "3.20亿");
        assertEq("1.5万亿", formatNumber(1500000000000.0), "1.50万亿");
        assertEq("负1.5万", formatNumber(-15000), "-1.50万");
        assertEq("负3.2亿", formatNumber(-320000000), "-3.20亿");
        assertEq("小于1万返回空", formatNumber(9999), "");
        assertEq("小数字返回空", formatNumber(500), "");
        assertEq("刚好1亿", formatNumber(100000000), "1.00亿");
        assertEq("刚好1万亿", formatNumber(1000000000000.0), "1.00万亿");
        assertEq("9999.99万", formatNumber(99999900), "9999.99万");

        System.out.println("\n====== replaceNumbersInPlainText ======");
        assertEq("千分位数字", replaceNumbersInPlainText("营收 150,000 元"), "营收 15.00万 元");
        assertEq("大数字", replaceNumbersInPlainText("总资产 320000000 美元"), "总资产 3.20亿 美元");
        assertEq("带万单位", replaceNumbersInPlainText("收入 5000万"), "收入 5000.00万");
        assertEq("带亿单位", replaceNumbersInPlainText("市值 50亿"), "市值 50.00亿");
        assertEq("小数字不变", replaceNumbersInPlainText("编号 999 号"), "编号 999 号");
        assertEq("无数字不变", replaceNumbersInPlainText("纯文本没有数字"), "纯文本没有数字");
        assertEq("负数替换", replaceNumbersInPlainText("亏损 25000万"), "亏损 2.50亿");
        assertEq("多个数字", replaceNumbersInPlainText("收入150000支出200000"), "收入15.00万支出20.00万");
        assertEq("零不替换", replaceNumbersInPlainText("增长率 0%"), "增长率 0%");

        System.out.println("\n====== processHtmlList ======");
        assertEq("基础HTML",
                processHtmlList(Arrays.asList("<p>营收 150,000 元</p>")),
                Arrays.asList("<p>营收 15.00万 元</p>"));
        assertEq("多标签",
                processHtmlList(Arrays.asList("<div><span>利润 320000000</span></div>")),
                Arrays.asList("<div><span>利润 3.20亿</span></div>"));
        assertEq("属性值不改",
                processHtmlList(Arrays.asList("<a href=\"150000\">链接 150000</a>")),
                Arrays.asList("<a href=\"150000\">链接 15.00万</a>"));
        assertEq("无标签纯文本",
                processHtmlList(Arrays.asList("纯文本 50000 字")),
                Arrays.asList("纯文本 5.00万 字"));
        assertEq("多条列表",
                processHtmlList(Arrays.asList("<p>150,000</p>", "<div>320000000</div>")),
                Arrays.asList("<p>15.00万</p>", "<div>3.20亿</div>"));
        assertEq("空列表",
                processHtmlList(new ArrayList<>()),
                new ArrayList<>());
        assertEq("小数字不变",
                processHtmlList(Arrays.asList("<p>编号 999</p>")),
                Arrays.asList("<p>编号 999</p>"));

        System.out.println("\n====== 结果 ======");
        System.out.println("通过: " + passed + ", 失败: " + failed + ", 总计: " + (passed + failed));

    }
}
