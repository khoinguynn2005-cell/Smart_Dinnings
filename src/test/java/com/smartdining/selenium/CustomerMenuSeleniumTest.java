package com.smartdining.selenium;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Luong khach quet QR va xem thuc don. Cac test chi doc du lieu,
 * khong bam "Them vao gio" nen khong tao don hang trong database.
 */
public class CustomerMenuSeleniumTest extends SeleniumTestBase {

    private String tableId() {
        return setting("tableId", "SMARTDINING_TEST_TABLE_ID", "1");
    }

    private void openMenu() {
        open("/menu?tableId=" + tableId());
        waitForPath("/home/index");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("productGrid")));
    }

    @Test(priority = 1, description = "Quet QR theo so ban mo thuc don dung ban va co mon an")
    public void menuShowsTableNumberAndProducts() {
        openMenu();

        Assert.assertEquals(driver.findElement(By.id("currentTableNumber")).getText().trim(), tableId(),
                "Header phai hien dung so ban.");
        Assert.assertFalse(driver.findElements(By.cssSelector("#productGrid .product-card")).isEmpty(),
                "Thuc don phai co it nhat mot mon dang ban.");
        Assert.assertFalse(driver.findElement(By.id("cartCountBadge")).getText().isBlank(),
                "Phai hien so mon trong gio.");
    }

    @Test(priority = 2, description = "So ban khong hop le tra trang 404, khong mo thuc don")
    public void invalidTableIsRejected() {
        for (String tableId : new String[] { "abc", "-1", "999999" }) {
            open("/menu?tableId=" + tableId);
            Assert.assertTrue(driver.findElements(By.id("productGrid")).isEmpty(),
                    "tableId=" + tableId + " khong duoc mo thuc don.");
        }
    }

    @Test(priority = 3, description = "Loc danh muc chi hien mon thuoc danh muc, bam Tat ca hien lai du")
    public void categoryFilterShowsMatchingProducts() {
        openMenu();
        List<WebElement> categories = driver.findElements(By.cssSelector("#categoryNav .cat-btn"));
        if (categories.size() < 2) {
            throw new org.testng.SkipException("Database chua co danh muc nao de loc.");
        }
        int total = visibleCards().size();

        categories.get(1).click();
        String categoryId = (String) ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].getAttribute('onclick').match(/'([^']*)'/)[1];", categories.get(1));
        Assert.assertTrue(categories.get(1).getAttribute("class").contains("active"),
                "Danh muc vua bam phai duoc danh dau active.");
        for (WebElement card : visibleCards()) {
            Assert.assertEquals(card.getAttribute("data-category-id"), categoryId,
                    "Chi hien mon thuoc danh muc da chon.");
        }

        categories.get(0).click();
        Assert.assertEquals(visibleCards().size(), total, "Bam Tat ca phai hien lai toan bo mon.");
        // Loi cu BUG-09: loc xong the mon bi doi sang display:block lam vo bo cuc
        Assert.assertNotEquals(visibleCards().get(0).getCssValue("display"), "block",
                "The mon phai giu bo cuc cua CSS sau khi loc.");
    }

    @Test(priority = 4, description = "Mo hop chon mon, tang giam so luong cap nhat thanh tien")
    public void productModalUpdatesQuantityAndPrice() {
        openMenu();
        WebElement addButton = driver.findElement(By.cssSelector("#productGrid .add-btn"));
        String productName = addButton.getAttribute("data-product-name");
        long price = Math.round(Double.parseDouble(addButton.getAttribute("data-product-price")));

        addButton.click();
        WebElement modal = driver.findElement(By.id("productModal"));
        wait.until(d -> modal.getAttribute("class").contains("active"));
        Assert.assertEquals(driver.findElement(By.id("modalProductName")).getText(), productName);
        Assert.assertEquals(driver.findElement(By.id("modalQuantity")).getText(), "1");

        List<WebElement> qtyButtons = modal.findElements(By.cssSelector(".qty-btn"));
        qtyButtons.get(0).click(); // khong giam duoi 1
        Assert.assertEquals(driver.findElement(By.id("modalQuantity")).getText(), "1",
                "So luong khong duoc nho hon 1.");
        qtyButtons.get(1).click();
        qtyButtons.get(1).click();
        Assert.assertEquals(driver.findElement(By.id("modalQuantity")).getText(), "3");
        Assert.assertEquals(digits(driver.findElement(By.id("modalPrice")).getText()), price * 3,
                "Thanh tien trong nut phai bang don gia x 3.");

        modal.findElement(By.cssSelector(".close-btn")).click();
        wait.until(d -> !modal.getAttribute("class").contains("active"));
    }

    @Test(priority = 5, description = "Mo va dong gio hang cua ban")
    public void cartSidebarOpensAndCloses() {
        openMenu();
        WebElement sidebar = driver.findElement(By.id("cartSidebar"));

        driver.findElement(By.cssSelector(".floating-cart")).click();
        wait.until(d -> sidebar.getAttribute("class").contains("active"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("cartItemsContainer")));
        // Gio hang truot vao tu ben phai; doi truot xong moi bam duoc nut dong
        wait.until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                "return arguments[0].getBoundingClientRect().right <= window.innerWidth + 1;", sidebar));

        sidebar.findElement(By.cssSelector(".cart-header .close-btn")).click();
        wait.until(d -> !sidebar.getAttribute("class").contains("active"));
    }

    @Test(priority = 6, description = "Thuc don tren dien thoai 375px khong bi tran ngang")
    public void menuFitsMobileWidth() {
        driver.manage().window().setSize(new Dimension(375, 812));
        openMenu();

        Long overflow = (Long) ((JavascriptExecutor) driver).executeScript(
                "return document.documentElement.scrollWidth - document.documentElement.clientWidth;");
        Assert.assertTrue(overflow <= 0, "Trang bi tran ngang " + overflow + "px tren man hinh 375px.");
        Assert.assertTrue(driver.findElement(By.cssSelector(".floating-cart")).isDisplayed(),
                "Thanh gio hang phai hien tren dien thoai.");
    }

    private List<WebElement> visibleCards() {
        return driver.findElements(By.cssSelector("#productGrid .product-card")).stream()
                .filter(WebElement::isDisplayed)
                .toList();
    }

    private long digits(String text) {
        String value = text.replaceAll("\\D", "");
        return value.isEmpty() ? -1 : Long.parseLong(value);
    }
}
