
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

public class PassengerDetailsTest {

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
    public void verifyPassengerDetailsAndConfirmBooking() {

        // Step 1: Open the homepage
        driver.get("http://localhost:8082");

        // Step 2: Select source city
        Select source = new Select(
                wait.until(d ->
                        d.findElement(By.id("source"))
                )
        );
        source.selectByVisibleText("Hyderabad");

        // Step 3: Select destination city
        Select destination = new Select(
                driver.findElement(By.id("destination"))
        );
        destination.selectByVisibleText("Bengaluru");

        // Step 4: Select journey date (7 days from today)
        String journeyDate = LocalDate.now()
                .plusDays(7)
                .toString();

        WebElement dateField =
                driver.findElement(By.id("journeyDate"));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];",
                dateField,
                journeyDate
        );

        // Step 5: Search buses
        driver.findElement(
                By.cssSelector("button.search-button")
        ).click();

        wait.until(d ->
                d.getCurrentUrl().contains("/search")
        );

        // Step 6: Find Express Travels
        WebElement expressBus = driver.findElements(
                By.cssSelector(".bus-card")
        ).stream()
         .filter(card -> card.findElement(
                 By.cssSelector(".bus-name")
         ).getText().equals("Express Travels"))
         .findFirst()
         .orElseThrow(() ->
                 new AssertionError("Express Travels not found")
         );

        // Step 7: Open seat selection
        expressBus.findElement(
                By.cssSelector("a.select-button")
        ).click();

        wait.until(d ->
                d.getCurrentUrl().contains("/seats")
        );

        wait.until(d ->
                d.findElements(
                        By.cssSelector("#seatLayout button.seat")
                ).size() == 40
        );

        // Step 8: Select seats S1 and S2
        driver.findElement(
                By.cssSelector(
                        "#seatLayout button[data-seat='S1']"
                )
        ).click();

        driver.findElement(
                By.cssSelector(
                        "#seatLayout button[data-seat='S2']"
                )
        ).click();

        // Step 9: Verify total price
        Assert.assertEquals(
                driver.findElement(By.id("totalPrice"))
                        .getText(),
                "1500",
                "Total price should be Rs. 1500"
        );

        // Step 10: Continue to passenger details
        driver.findElement(
                By.cssSelector("button.continue-btn")
        ).click();

        wait.until(d ->
                d.getCurrentUrl().contains("/passenger-details")
        );

        // Step 11: Verify booking summary
        String summary = wait.until(d ->
                d.findElement(By.cssSelector(".summary"))
                        .getText()
        );

        Assert.assertTrue(
                summary.contains("Express Travels"),
                "Bus name is incorrect"
        );

        Assert.assertTrue(
                summary.contains("Hyderabad"),
                "Source city is incorrect"
        );

        Assert.assertTrue(
                summary.contains("Bengaluru"),
                "Destination city is incorrect"
        );

        Assert.assertTrue(
                summary.contains(journeyDate),
                "Journey date is incorrect"
        );

        Assert.assertTrue(
                summary.contains("S1,S2")
                        || summary.contains("S1, S2"),
                "Selected seats are incorrect"
        );

        Assert.assertTrue(
                summary.contains("1500"),
                "Total amount is incorrect"
        );

        // Step 12: Enter passenger full name
        driver.findElement(
                By.id("passengerName")
        ).sendKeys("Test Passenger");

        // Step 13: Enter passenger age
        driver.findElement(
                By.id("age")
        ).sendKeys("30");

        // Step 14: Enter 10-digit phone number
        driver.findElement(
                By.id("phone")
        ).sendKeys("9876543210");

        // Step 15: Verify entered values
        Assert.assertEquals(
                driver.findElement(By.id("passengerName"))
                        .getAttribute("value"),
                "Test Passenger"
        );

        Assert.assertEquals(
                driver.findElement(By.id("age"))
                        .getAttribute("value"),
                "30"
        );

        Assert.assertEquals(
                driver.findElement(By.id("phone"))
                        .getAttribute("value"),
                "9876543210"
        );

        // Step 16: Confirm booking
        driver.findElement(
                By.cssSelector("#passengerForm button[type='submit']")
        ).click();

        // Step 17: Verify the next page opened
        // This assumes the controller redirects to
        // /booking-confirmation after successful submission.
        wait.until(d ->
                d.getCurrentUrl().contains("/booking-confirmation")
        );

        System.out.println("======================================");
        System.out.println("PASS: Passenger details test successful!");
        System.out.println("Bus: Express Travels");
        System.out.println("Route: Hyderabad -> Bengaluru");
        System.out.println("Journey Date: " + journeyDate);
        System.out.println("Selected Seats: S1, S2");
        System.out.println("Passenger: Test Passenger");
        System.out.println("Age: 30");
        System.out.println("Total Amount: Rs. 1500");
        System.out.println("Booking confirmation page opened.");
        System.out.println("======================================");
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
