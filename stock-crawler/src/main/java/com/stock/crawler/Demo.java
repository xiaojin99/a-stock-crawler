package com.stock.crawler;

import com.stock.crawler.model.ResearchReport;
import com.stock.crawler.service.ResearchReportService;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * 搜索股票研报演示
 */
public class Demo {

    public static void main(String[] args) throws IOException {
        ResearchReportService service = new ResearchReportService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== 股票研报搜索 ===");
        System.out.println("支持的股票代码格式:");
        System.out.println("  - 6位数字: 000001, 600519");
        System.out.println("  - 带前缀: sz000001, sh600519");
        System.out.println();

        // 默认演示几个常见股票
        String[] demoStocks = {"000001", "600519", "000858"};

        for (String code : demoStocks) {
            System.out.println("────────────────────────────────────────");
            searchStock(service, code);
            System.out.println();
        }

        // 交互式搜索
        System.out.println("────────────────────────────────────────");
        System.out.print("输入股票代码搜索研报 (回车退出): ");
        String input = scanner.nextLine().trim();

        if (!input.isEmpty()) {
            searchStock(service, input);
        }

        scanner.close();
        System.out.println("演示结束");
    }

    private static void searchStock(ResearchReportService service, String code) throws IOException {
        System.out.printf("【%s】研报列表:%n", code);

        List<ResearchReport> reports = service.getResearchReports(code);

        if (reports.isEmpty()) {
            System.out.println("  未找到研报");
            return;
        }

        for (int i = 0; i < reports.size(); i++) {
            ResearchReport r = reports.get(i);
            System.out.printf("%n%d. %s%n", i + 1, r.getTitle());
            System.out.printf("   评级: %s | 券商: %s%n",
                    r.getEmRatingName() != null ? r.getEmRatingName() : "无",
                    r.getOrgSName() != null ? r.getOrgSName() : "无");
            System.out.printf("   研究员: %s | 发布: %s%n",
                    r.getResearcher() != null ? r.getResearcher() : "无",
                    r.getPublishDate() != null ? r.getPublishDate() : "无");
            if (r.getInfoCode() != null) {
                System.out.printf("   PDF: %s%n", service.getReportPdfUrl(r.getInfoCode()));
            }
        }
    }
}
