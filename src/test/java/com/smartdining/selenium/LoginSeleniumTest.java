package com.smartdining.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginSeleniumTest extends SeleniumTestBase {

    @Test(priority = 1, description = "Mo trang goc chuyen ve trang dang nhap co huong dan quet QR")
    public void rootRedirectsToLoginWithQrNotice() {
        open("/");

        waitForPath("/login");
        Assert.assertTrue(driver.getTitle().contains("SmartDining"), "Tieu de phai chua SmartDining.");
        Assert.assertTrue(driver.findElement(By.cssSelector(".notice-list")).getText().contains("quét mã QR"),
                "Trang dang nhap chung phai huong dan khach quet ma QR.");
        Assert.assertTrue(driver.findElement(By.id("username")).isDisplayed(), "Phai co o ten dang nhap.");
    }

    @Test(priority = 2, description = "Dang nhap sai mat khau hien thong bao loi va o lai trang dang nhap")
    public void wrongPasswordShowsError() {
        login(null, user("staff"), "sai-mat-khau");

        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".error-msg")));
        Assert.assertTrue(error.getText().contains("Sai ten dang nhap hoac mat khau"),
                "Phai bao sai ten dang nhap hoac mat khau, thuc te: " + error.getText());
        Assert.assertEquals(currentPath(), "/login", "Dang nhap sai phai o lai trang dang nhap.");
    }

    @Test(priority = 3, description = "Admin dang nhap dung vao trang quan tri")
    public void adminLoginOpensAdminDashboard() {
        login("admin", user("admin"), password("admin"));

        waitForPath("/admin/dashboard");
        WebElement title = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("viewTitle")));
        Assert.assertEquals(title.getText().trim(), "Tổng quan", "Trang admin mo mac dinh o tab Tong quan.");
    }

    @Test(priority = 4, description = "Nhan vien phuc vu dang nhap vao man hinh phuc vu va tai danh sach ban")
    public void staffLoginOpensStaffDashboard() {
        login("staff", user("staff"), password("staff"));

        waitForPath("/staff/dashboard");
        // Danh sach ban tai qua /api/staff/tables, cho den khi co it nhat mot the ban
        wait.until(ExpectedConditions.presenceOfNestedElementLocatedBy(By.id("tableList"), By.cssSelector("*")));
        Assert.assertTrue(driver.findElement(By.id("tableList")).isDisplayed(), "Phai hien danh sach ban.");
    }

    @Test(priority = 5, description = "Nhan vien bep dang nhap vao man hinh bep")
    public void kitchenLoginOpensKitchenDashboard() {
        login("kitchen", user("kitchen"), password("kitchen"));

        waitForPath("/kitchen/dashboard");
        WebElement heading = wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        Assert.assertTrue(heading.getText().contains("Màn hình bếp"), "Phai hien tieu de Man hinh bep.");
    }

    @Test(priority = 6, description = "Nhan vien phuc vu dang nhap o cong bep bi tu choi")
    public void staffCannotLoginAtKitchenGate() {
        login("kitchen", user("staff"), password("staff"));

        WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".error-msg")));
        Assert.assertTrue(error.getText().contains("khong thuoc khu vuc"),
                "Phai bao tai khoan khong thuoc khu vuc, thuc te: " + error.getText());
        Assert.assertEquals(currentPath(), "/login/kitchen");
    }

    @Test(priority = 7, description = "Nhan vien phuc vu mo trang admin bi chuyen ve dang nhap")
    public void staffCannotOpenAdminDashboard() {
        login("staff", user("staff"), password("staff"));
        waitForPath("/staff/dashboard");

        open("/admin/dashboard");

        waitForPath("/login");
    }

    @Test(priority = 8, description = "Khach chua dang nhap khong vao duoc man hinh phuc vu, bep, admin")
    public void guestIsRedirectedFromWorkspaces() {
        for (String path : new String[] { "/staff/dashboard", "/kitchen/dashboard", "/admin/dashboard" }) {
            open(path);
            waitForPath("/login");
        }
    }

    @Test(priority = 9, description = "Dang xuat roi mo lai man hinh phuc vu bi chuyen ve dang nhap")
    public void logoutEndsSession() {
        login("staff", user("staff"), password("staff"));
        waitForPath("/staff/dashboard");

        open("/logout");
        waitForPath("/login");

        open("/staff/dashboard");
        waitForPath("/login");
    }
}
