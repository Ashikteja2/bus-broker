
package com.busbooking.busbooking;

import java.time.Duration;
import java.time.LocalDate;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BusSearchTest {

    WebDriver driver;
    WebDriverWait wait;

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );
    }

    @Test
    public void searchBusesTest() {

        // Step 1: Open the Bus Broker website
        driver.get("http://localhost:8082");

        // Step 2: Select the source city
        WebElement sourceElement = wait.until(
                d -> d.findElement(By.id("source"))
        );

        Select source = new Select(sourceElement);
        source.selectByVisibleText("Hyderabad");

        // Step 3: Select the destination city
        WebElement destinationElement =
                driver.findElement(By.id("destination"));

        Select destination = new Select(destinationElement);
        destination.selectByVisibleText("Bengaluru");

        // Step 4: Generate a date seven days from today
        String journeyDate = LocalDate.now()
                .plusDays(7)
                .toString();

        // Step 5: Set the date in yyyy-MM-dd format
        WebElement dateElement =
                driver.findElement(By.id("journeyDate"));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(" +
                "new Event('input', {bubbles:true}));" +
                "arguments[0].dispatchEvent(" +
                "new Event('change', {bubbles:true}));",
                dateElement,
                journeyDate
        );

        // Step 6: Verify the date before submitting
        String actualDate =
                dateElement.getAttribute("value");

        Assert.assertEquals(
                actualDate,
                journeyDate,
                "FAIL: Journey date was entered incorrectly"
        );

        // Step 7: Click the Search buses button
        driver.findElement(
                By.cssSelector("button.search-button")
        ).click();

        // Step 8: Wait for the search page to open
        wait.until(
                d -> d.getCurrentUrl().contains("/search")
        );

        // Step 9: Verify the submitted search parameters
        String currentUrl = driver.getCurrentUrl();

        Assert.assertTrue(
                currentUrl.contains("/search"),
                "FAIL: Bus search page did not open"
        );

        Assert.assertTrue(
                currentUrl.contains("source=Hyderabad"),
                "FAIL: Incorrect source city"
        );

        Assert.assertTrue(
                currentUrl.contains("destination=Bengaluru"),
                "FAIL: Incorrect destination city"
        );

        Assert.assertTrue(
                currentUrl.contains("journeyDate=" + journeyDate),
                "FAIL: Incorrect journey date in search URL"
        );

        // Step 10: Print test results
        System.out.println("================================");
        System.out.println("PASS: Bus search test successful!");
        System.out.println("Source: Hyderabad");
        System.out.println("Destination: Bengaluru");
        System.out.println("Journey Date: " + actualDate);
        System.out.println("Search URL: " + currentUrl);
        System.out.println("================================");
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
