
package com.busbooking.busbooking;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

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

public class BusSearchResultsTest {

    WebDriver driver;
    WebDriverWait wait;

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    public void verifyBusSearchResults() {

        driver.get("http://localhost:8082");

        Select source = new Select(
                wait.until(d -> d.findElement(By.id("source")))
        );
        source.selectByVisibleText("Hyderabad");

        Select destination = new Select(
                driver.findElement(By.id("destination"))
        );
        destination.selectByVisibleText("Bengaluru");

        String journeyDate = LocalDate.now()
                .plusDays(7)
                .toString();

        WebElement dateElement =
                driver.findElement(By.id("journeyDate"));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];",
                dateElement,
                journeyDate
        );

        Assert.assertEquals(
                dateElement.getAttribute("value"),
                journeyDate,
                "Journey date is incorrect"
        );

        driver.findElement(
                By.cssSelector("button.search-button")
        ).click();

        wait.until(d -> d.getCurrentUrl().contains("/search"));

        WebElement route = wait.until(
                d -> d.findElement(By.cssSelector(".route"))
        );

        String routeText = route.getText();

        Assert.assertTrue(routeText.contains("Hyderabad"));
        Assert.assertTrue(routeText.contains("Bengaluru"));
        Assert.assertTrue(routeText.contains(journeyDate));

        List<WebElement> busCards =
                driver.findElements(By.cssSelector(".bus-card"));

        Assert.assertEquals(busCards.size(), 3,
                "Expected three bus cards");

        verifyBus("Express Travels", "₹750");
        verifyBus("Royal Roadways", "₹900");
        verifyBus("City Connect", "₹650");

        System.out.println("PASS: Bus search results verified!");
        System.out.println("Route: " + routeText);
        System.out.println("Journey Date: " + journeyDate);
        System.out.println("Bus cards found: " + busCards.size());
        System.out.println("PASS: All three bus names and fares verified!");
    }

    private void verifyBus(String busName, String expectedPrice) {

        WebElement busCard = driver.findElements(
                By.cssSelector(".bus-card")
        ).stream()
         .filter(card -> card.findElement(
                 By.cssSelector(".bus-name")
         ).getText().equals(busName))
         .findFirst()
         .orElseThrow(() ->
                 new AssertionError("Bus not found: " + busName));

        String actualPrice = busCard.findElement(
                By.cssSelector(".price")
        ).getText();

        Assert.assertEquals(
                actualPrice,
                expectedPrice,
                "Incorrect fare for " + busName
        );

        Assert.assertTrue(
                busCard.findElement(
                        By.cssSelector("a.select-button")
                ).getAttribute("href").contains("/seats"),
                "Select Seats link is incorrect for " + busName
        );
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
