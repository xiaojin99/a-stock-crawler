package com.stock.web.controller;

import com.stock.crawler.model.AnnouncementItem;
import com.stock.crawler.model.ConceptBlockResult;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.FundFlowPoint;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.model.StockBasicInfo;
import com.stock.crawler.model.StockCapabilitySnapshot;
import com.stock.crawler.model.StockQuote;
import com.stock.web.service.CapabilityWebService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.List;
import java.util.regex.Pattern;

/**
 * stock-crawler 统一能力 JSON API。
 */
@RestController
@RequestMapping({"/api/capabilities", "/api/data"})
public class CapabilityApiController {

    private static final Pattern STOCK_CODE_PATTERN =
            Pattern.compile("(?i)(\\d{6}|(sh|sz|bj)\\d{6}|\\d{6}\\.(sh|sz|bj)|hk\\d{4,5})");

    private final CapabilityWebService capabilityWebService;

    public CapabilityApiController(CapabilityWebService capabilityWebService) {
        this.capabilityWebService = capabilityWebService;
    }

    @GetMapping("/quote/{code}")
    public DataResult<StockQuote> getQuote(@PathVariable String code) {
        return capabilityWebService.getQuote(requireStockCode(code));
    }

    @GetMapping("/snapshot/{code}")
    public DataResult<StockCapabilitySnapshot> getSnapshot(@PathVariable String code) {
        return capabilityWebService.getSnapshot(requireStockCode(code));
    }

    @GetMapping("/basic/{code}")
    public DataResult<StockBasicInfo> getStockBasicInfo(@PathVariable String code) {
        return capabilityWebService.getStockBasicInfo(requireStockCode(code));
    }

    @GetMapping({"/concepts/{code}", "/themes/{code}"})
    public DataResult<ConceptBlockResult> getConceptBlocks(@PathVariable String code) {
        return capabilityWebService.getConceptBlocks(requireStockCode(code));
    }

    @GetMapping({"/fund-flow/minute/{code}", "/fund/minute/{code}"})
    public DataResult<List<FundFlowPoint>> getFundFlowMinute(@PathVariable String code) {
        return capabilityWebService.getFundFlowMinute(requireStockCode(code));
    }

    @GetMapping({"/fund-flow/daily/{code}", "/fund/daily/{code}"})
    public DataResult<List<FundFlowPoint>> getFundFlowDaily(
            @PathVariable String code,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return capabilityWebService.getFundFlowDaily(requireStockCode(code), limit);
    }

    @GetMapping({"/stock-news/{code}", "/company-news/{code}"})
    public DataResult<List<MarketNewsItem>> getStockNews(
            @PathVariable String code,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return capabilityWebService.getStockNews(requireStockCode(code), limit);
    }

    @GetMapping({"/global-news", "/flash"})
    public DataResult<List<MarketNewsItem>> getGlobalNews(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return capabilityWebService.getGlobalNews(limit);
    }

    @GetMapping({"/announcements/{code}", "/ann/{code}"})
    public DataResult<List<AnnouncementItem>> getAnnouncements(
            @PathVariable String code,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return capabilityWebService.getAnnouncements(requireStockCode(code), limit);
    }

    private String requireStockCode(String code) {
        String normalized = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        if (!STOCK_CODE_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stock code");
        }
        if (normalized.matches("^\\d{6}\\.(sh|sz|bj)$")) {
            String[] parts = normalized.split("\\.");
            return parts[1] + parts[0];
        }
        return normalized;
    }
}
