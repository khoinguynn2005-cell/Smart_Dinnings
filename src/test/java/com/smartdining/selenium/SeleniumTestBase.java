package com.smartdining.selenium;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import org.openqa.selenium.By;
import java.util.Map;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Cau hinh chung cho test Selenium. Moi gia tri doc theo thu tu:
 * tham so -Dten=..., bien moi truong, roi den gia tri mac dinh.
 */
abstract class SeleniumTestBase {
    protected static final Dimension DESKTOP = new Dimension(1440, 1000);

    /**
     * Mat khau mau (Staff@123...) nam trong danh sach bi lo, trinh duyet se hien hop canh bao
     * sau khi dang nhap va chiem ban phim, lam cac test dang nhap sau go phim vao hop do.
     */
    private static final Map<String, Object> PASSWORD_MANAGER_OFF = Map.of(
            "credentials_enable_service", false,
            "profile.password_manager_enabled", false,
            "profile.password_manager_leak_detection", false);

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl;

    @BeforeClass(alwaysRun = true)
    public void setUpBrowser() {
        baseUrl = setting("baseUrl", "SMARTDINING_BASE_URL", "http://localhost:8081/SmartDining")
                .replaceAll("/+$", "");
        String browser = setting("browser", "SMARTDINING_TEST_BROWSER", "chrome").trim().toLowerCase();
        boolean headless = Boolean.parseBoolean(setting("headless", "SMARTDINING_TEST_HEADLESS", "true"));

        driver = switch (browser) {
            case "edge" -> new EdgeDriver(edgeOptions(headless));
            case "chrome" -> new ChromeDriver(chromeOptions(headless));
            default -> throw new IllegalArgumentException(
                    "Trinh duyet khong ho tro: " + browser + ". Chi dung chrome hoac edge.");
        };
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @BeforeMethod(alwaysRun = true)
    public void resetSession() {
        driver.manage().window().setSize(DESKTOP);
        // Xoa cookie tren dung domain cua ung dung de moi test bat dau khi chua dang nhap
        open("/login");
        driver.manage().deleteAllCookies();
    }

    @AfterMethod(alwaysRun = true)
    public void captureFailure(ITestResult result) throws IOException {
        if (!result.isSuccess() && driver instanceof TakesScreenshot screenshotDriver) {
            Path outputDirectory = Path.of("target", "selenium-screenshots");
            Files.createDirectories(outputDirectory);
            File screenshot = screenshotDriver.getScreenshotAs(OutputType.FILE);
            String safeName = result.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            Files.copy(screenshot.toPath(), outputDirectory.resolve(safeName + ".png"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void open(String path) {
        driver.get(baseUrl + (path.startsWith("/") ? path : "/" + path));
    }

    /** Duong dan hien tai, bo phan baseUrl va query string. */
    protected String currentPath() {
        String url = driver.getCurrentUrl();
        String path = url.startsWith(baseUrl) ? url.substring(baseUrl.length()) : url;
        int query = path.indexOf('?');
        return query >= 0 ? path.substring(0, query) : path;
    }

    protected void waitForPath(String path) {
        wait.until(d -> currentPath().equals(path));
    }

    /** Dang nhap qua form tai /login/{area}; area null nghia la trang /login chung. */
    protected void login(String area, String username, String password) {
        open(area == null ? "/login" : "/login/" + area);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username"))).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.cssSelector("form button[type='submit']")).click();
    }

    /** Tai khoan mau trong database/SmartDining.sql; doi bang -D<role>User / -D<role>Password. */
    protected String user(String role) {
        return setting(role + "User", "SMARTDINING_" + role.toUpperCase() + "_USER", switch (role) {
            case "admin" -> "admin";
            case "staff" -> "staff1";
            case "kitchen" -> "kitchen1";
            default -> throw new IllegalArgumentException(role);
        });
    }

    protected String password(String role) {
        return setting(role + "Password", "SMARTDINING_" + role.toUpperCase() + "_PASSWORD", switch (role) {
            case "admin" -> "Admin@123";
            case "staff" -> "Staff@123";
            case "kitchen" -> "Kitchen@123";
            default -> throw new IllegalArgumentException(role);
        });
    }

    protected String setting(String systemProperty, String environmentVariable, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--lang=vi-VN",
                "--disable-features=PasswordLeakDetection,PasswordManagerOnboarding");
        options.setExperimentalOption("prefs", PASSWORD_MANAGER_OFF);
        return options;
    }

    private EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--lang=vi-VN",
                "--disable-features=PasswordLeakDetection,PasswordManagerOnboarding");
        options.setExperimentalOption("prefs", PASSWORD_MANAGER_OFF);
        return options;
    }
}
