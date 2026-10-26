
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

public class SeatSelectionTest {

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
    public void verifySeatSelectionAndTotalPrice() {

        // Step 1: Open the Bus Broker homepage
        driver.get("http://localhost:8082");

        // Step 2: Select source city
        Select source = new Select(
                wait.until(d -> d.findElement(By.id("source")))
        );
        source.selectByVisibleText("Hyderabad");

        // Step 3: Select destination city
        Select destination = new Select(
                driver.findElement(By.id("destination"))
        );
        destination.selectByVisibleText("Bengaluru");

        // Step 4: Set journey date
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

        // Step 5: Search buses
        driver.findElement(
                By.cssSelector("button.search-button")
        ).click();

        wait.until(d -> d.getCurrentUrl().contains("/search"));

        // Step 6: Select Express Travels
        WebElement expressBus = driver.findElements(
                By.cssSelector(".bus-card")
        ).stream()
         .filter(card -> card.findElement(
                 By.cssSelector(".bus-name")
         ).getText().equals("Express Travels"))
         .findFirst()
         .orElseThrow(() ->
                 new AssertionError("Express Travels bus not found"));

        expressBus.findElement(
                By.cssSelector("a.select-button")
        ).click();

        // Step 7: Wait for seat-selection page
        wait.until(d -> d.getCurrentUrl().contains("/seats"));

        wait.until(d -> d.findElement(
                By.id("seatLayout")
        ).findElements(By.cssSelector("button.seat")).size() == 40);

        // Step 8: Verify bus information
        String seatPageText = driver.findElement(
                By.cssSelector(".bus-info")
        ).getText();

        Assert.assertTrue(
                seatPageText.contains("Express Travels"),
                "Incorrect bus name"
        );

        Assert.assertTrue(
                seatPageText.contains("Hyderabad"),
                "Incorrect source city"
        );

        Assert.assertTrue(
                seatPageText.contains("Bengaluru"),
                "Incorrect destination city"
        );

        Assert.assertTrue(
                seatPageText.contains(journeyDate),
                "Incorrect journey date"
        );

        Assert.assertTrue(
                seatPageText.contains("750"),
                "Incorrect price per seat"
        );

        // Step 9: Verify 40 seats are displayed
        int totalSeats = driver.findElements(
                By.cssSelector("#seatLayout button.seat")
        ).size();

        Assert.assertEquals(
                totalSeats,
                40,
                "Expected 40 seats"
        );

        // Step 10: Verify unavailable seats are disabled
        for (String seatNumber : new String[]{"S3", "S8", "S14"}) {

            WebElement unavailableSeat = driver.findElement(
                    By.cssSelector(
                            "#seatLayout button[data-seat='" +
                            seatNumber + "']"
                    )
            );

            Assert.assertFalse(
        unavailableSeat.isEnabled(),
        seatNumber + " should be unavailable"
);
        }

        // Step 11: Select seats S1 and S2
        driver.findElement(
                By.cssSelector("#seatLayout button[data-seat='S1']")
        ).click();

        driver.findElement(
                By.cssSelector("#seatLayout button[data-seat='S2']")
        ).click();

        // Step 12: Verify selected seats
        Assert.assertEquals(
                driver.findElement(By.id("seatCount")).getText(),
                "2",
                "Selected seat count should be 2"
        );

        Assert.assertEquals(
                driver.findElement(By.id("seatNumbers")).getText(),
                "S1, S2",
                "Selected seat numbers are incorrect"
        );

        // Step 13: Verify total price
        Assert.assertEquals(
                driver.findElement(By.id("totalPrice")).getText(),
                "1500",
                "Total price should be 1500"
        );

        Assert.assertEquals(
                driver.findElement(By.id("selectedSeats"))
                        .getAttribute("value"),
                "S1,S2",
                "Hidden selected seats value is incorrect"
        );

        System.out.println("==================================");
        System.out.println("PASS: Seat selection test successful!");
        System.out.println("Bus: Express Travels");
        System.out.println("Route: Hyderabad -> Bengaluru");
        System.out.println("Journey Date: " + journeyDate);
        System.out.println("Total seats: " + totalSeats);
        System.out.println("Selected seats: S1, S2");
        System.out.println("Seat count: 2");
        System.out.println("Price per seat: Rs. 750");
        System.out.println("Total price: Rs. 1500");
        System.out.println("==================================");

        // Step 14: Continue to passenger details
        driver.findElement(
                By.cssSelector("button.continue-btn")
        ).click();

        wait.until(d ->
                d.getCurrentUrl().contains("/passenger-details")
        );

        Assert.assertTrue(
                driver.getCurrentUrl().contains("/passenger-details"),
                "Passenger details page did not open"
        );

        Assert.assertTrue(
                driver.getCurrentUrl().contains("seats=S1%2CS2")
                || driver.getCurrentUrl().contains("seats=S1,S2"),
                "Selected seats were not passed to passenger details"
        );

        System.out.println(
                "PASS: Passenger details page opened with selected seats!"
        );
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
