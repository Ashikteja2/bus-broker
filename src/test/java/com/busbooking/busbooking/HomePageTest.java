
package com.busbooking.busbooking;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HomePageTest {

    WebDriver driver;

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
    }

    @Test
    public void verifyHomePageOpens() {

        driver.get("http://localhost:8082");

        String currentUrl = driver.getCurrentUrl();
        String pageTitle = driver.getTitle();

        Assert.assertTrue(
            currentUrl.startsWith("http://localhost:8082"),
            "FAIL: Website did not open correctly"
        );

        Assert.assertTrue(
            driver.getPageSource().length() > 0,
            "FAIL: Website page is empty"
        );

        System.out.println("PASS: Bus Broker website opened!");
        System.out.println("Page URL: " + currentUrl);
        System.out.println("Page Title: " + pageTitle);
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
