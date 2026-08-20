package com.xianyusmart.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaywrightManagerTest {

    @Test
    void desktopBrowserFallbackNamesAreExplicit() {
        assertEquals("Playwright Chromium", PlaywrightManager.browserChannelName(null));
        assertEquals("系统 chrome", PlaywrightManager.browserChannelName("chrome"));
        assertEquals("系统 msedge", PlaywrightManager.browserChannelName("msedge"));
    }

    @Test
    void fallsBackToSystemChromeWhenBundledChromiumIsMissing() {
        PlaywrightManager manager = new PlaywrightManager();
        Playwright playwright = mock(Playwright.class);
        BrowserType browserType = mock(BrowserType.class);
        Browser browser = mock(Browser.class);
        List<BrowserType.LaunchOptions> attempts = new ArrayList<>();

        when(playwright.chromium()).thenReturn(browserType);
        when(browserType.launch(any(BrowserType.LaunchOptions.class))).thenAnswer(invocation -> {
            BrowserType.LaunchOptions options = invocation.getArgument(0);
            attempts.add(options);
            if (options.channel == null) {
                throw new RuntimeException("bundled browser missing");
            }
            return browser;
        });
        ReflectionTestUtils.setField(manager, "playwright", playwright);
        ReflectionTestUtils.setField(manager, "browserHeadless", false);

        Browser launched = ReflectionTestUtils.invokeMethod(manager, "launchAvailableBrowser");

        assertSame(browser, launched);
        assertEquals(2, attempts.size());
        assertEquals(null, attempts.get(0).channel);
        assertEquals("chrome", attempts.get(1).channel);
        assertEquals(false, attempts.get(1).headless);
        assertEquals(15_000D, attempts.get(1).timeout);
    }
}
