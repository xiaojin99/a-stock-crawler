package com.stock.web.controller;

import com.stock.crawler.model.AnnouncementItem;
import com.stock.crawler.model.ConceptBlock;
import com.stock.crawler.model.ConceptBlockResult;
import com.stock.crawler.model.DataResult;
import com.stock.crawler.model.FundFlowPoint;
import com.stock.crawler.model.MarketNewsItem;
import com.stock.crawler.model.StockBasicInfo;
import com.stock.crawler.model.StockQuote;
import com.stock.web.service.CapabilityWebService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CapabilityApiControllerTest {

    @Test
    void apiDataQuoteDelegatesNormalizedSuffixCode() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        when(service.getQuote("sh600519")).thenReturn(DataResult.success(
                StockQuote.builder().code("sh600519").name("贵州茅台").build(),
                "quote:tencent-first",
                1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/data/quote/600519.SH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("sh600519"));

        verify(service).getQuote("sh600519");
    }

    @Test
    void apiCapabilitiesBasicAliasDelegatesToBasicInfo() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        StockBasicInfo info = new StockBasicInfo();
        info.setCode("600519");
        info.setName("贵州茅台");
        when(service.getStockBasicInfo("600519"))
                .thenReturn(DataResult.success(info, "stock-info:tencent-fallback", 1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/capabilities/basic/600519"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("贵州茅台"));

        verify(service).getStockBasicInfo("600519");
    }

    @Test
    void apiDataFlashDelegatesToCapabilityService() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        when(service.getGlobalNews(3)).thenReturn(DataResult.success(List.of(
                MarketNewsItem.builder()
                        .title("市场快讯")
                        .source("东方财富")
                        .build()
        ), "global-news:eastmoney-724-limited", 1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/data/flash").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.source").value("global-news:eastmoney-724-limited"))
                .andExpect(jsonPath("$.data[0].source").value("东方财富"));

        verify(service).getGlobalNews(3);
    }

    @Test
    void apiDataThemesDelegatesToConceptBlocks() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        ConceptBlockResult conceptResult = new ConceptBlockResult();
        conceptResult.setTotal(1);
        conceptResult.setBoards(List.of(ConceptBlock.builder()
                .name("白酒")
                .code("BK0477")
                .changePercent(new BigDecimal("1.23"))
                .build()));
        when(service.getConceptBlocks("600519"))
                .thenReturn(DataResult.success(conceptResult, "concept-blocks:eastmoney-slist-limited", 1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/data/themes/600519"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.boards[0].name").value("白酒"));

        verify(service).getConceptBlocks("600519");
    }

    @Test
    void apiDataFundDailyPassesLimit() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        when(service.getFundFlowDaily("600519", 12))
                .thenReturn(DataResult.success(List.of(new FundFlowPoint()), "fund-flow-daily", 1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/data/fund/daily/600519").param("limit", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).getFundFlowDaily("600519", 12);
    }

    @Test
    void apiDataFundMinuteDelegatesToMinuteFundFlow() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        when(service.getFundFlowMinute("600519"))
                .thenReturn(DataResult.success(List.of(new FundFlowPoint()), "fund-flow-minute", 1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/data/fund/minute/600519"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(service).getFundFlowMinute("600519");
    }

    @Test
    void apiDataCompanyNewsUsesDefaultLimit() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        when(service.getStockNews("600519", 10)).thenReturn(DataResult.success(List.of(
                MarketNewsItem.builder().title("公司新闻").build()
        ), "stock-news", 1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/data/company-news/600519"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("公司新闻"));

        verify(service).getStockNews("600519", 10);
    }

    @Test
    void apiDataAnnouncementsDelegatesToAnnouncements() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        when(service.getAnnouncements("600519", 5)).thenReturn(DataResult.success(List.of(
                AnnouncementItem.builder().title("年度报告").build()
        ), "announcements:cninfo", 1));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/data/ann/600519").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("年度报告"));

        verify(service).getAnnouncements("600519", 5);
    }

    @Test
    void invalidStockCodeIsRejectedBeforeCompositeFanOut() throws Exception {
        CapabilityWebService service = mock(CapabilityWebService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CapabilityApiController(service)).build();

        mockMvc.perform(get("/api/data/snapshot/not-a-stock-code"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
