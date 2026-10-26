
package com.busbooking.busbooking;

import java.time.Duration;
import java.time.LocalDate;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class BookingConfirmationTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @Test
    public void verifyBookingConfirmationDetails() {

        driver = new ChromeDriver();
        driver.manage().window().maximize();

        wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );

        // 1. Open Bus Broker website
        driver.get("http://localhost:8082");

        // 2. Select source city
        new Select(
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                By.id("source")
                        )
                )
        ).selectByVisibleText("Hyderabad");

        // 3. Select destination city
        new Select(
                driver.findElement(By.id("destination"))
        ).selectByVisibleText("Bengaluru");

        // 4. Set a future journey date
        String journeyDate = LocalDate.now()
                .plusDays(30 + (System.currentTimeMillis() % 3000))
                .toString();

        WebElement dateInput = driver.findElement(
                By.id("journeyDate")
        );

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                dateInput,
                journeyDate
        );

        Assert.assertEquals(
                dateInput.getAttribute("value"),
                journeyDate,
                "Journey date was not set correctly"
        );

        // 5. Search buses
        driver.findElement(
                By.cssSelector("button.search-button")
        ).click();

        wait.until(
                ExpectedConditions.urlContains("/search")
        );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".bus-card")
                )
        );

        // 6. Find Express Travels
        WebElement expressCard = driver.findElements(
                By.cssSelector(".bus-card")
        ).stream()
                .filter(card ->
                        card.findElement(
                                By.cssSelector(".bus-name")
                        ).getText().trim().equals("Express Travels")
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "Express Travels bus was not found"
                        )
                );

        // 7. Select Express Travels
        expressCard.findElement(
                By.cssSelector("a.select-button")
        ).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("#seatLayout .seat")
                )
        );

        // 8. Select seats S1 and S2
        selectSeat("S1");
        selectSeat("S2");

        Assert.assertEquals(
                driver.findElement(By.id("seatCount"))
                        .getText().trim(),
                "2",
                "Two seats should be selected"
        );

        Assert.assertTrue(
                driver.findElement(By.id("totalPrice"))
                        .getText().contains("1500"),
                "Total fare should be Rs. 1500"
        );

        // 9. Continue to passenger details
        driver.findElement(
                By.cssSelector("#seatForm .continue-btn")
        ).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("passengerName")
                )
        );

        // 10. Enter passenger information
        driver.findElement(By.id("passengerName"))
                .sendKeys("Test Passenger");

        driver.findElement(By.id("age"))
                .sendKeys("30");

        driver.findElement(By.id("phone"))
                .sendKeys("9876543210");

        // 11. Submit passenger details
        driver.findElement(
                By.cssSelector("#passengerForm .submit-btn")
        ).click();

        // 12. Verify Demo Payment page
        WebElement paymentCard = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".payment-card")
                )
        );

        Assert.assertTrue(
                paymentCard.getText().contains("1500"),
                "Payment amount should be Rs. 1500"
        );

        System.out.println("PASS: Demo Payment page verified.");

        // 13. Click Pay Now (Demo)
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("form button.pay-button")
                )
        ).click();

        // 14. Wait for the final booking confirmation page
        try {
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector(".confirmation-container h1")
                    )
            );

        } catch (org.openqa.selenium.TimeoutException e) {

            System.out.println();
            System.out.println("===== BOOKING DEBUG =====");
            System.out.println(
                    "Current URL: " + driver.getCurrentUrl()
            );
            System.out.println(
                    "Page title: " + driver.getTitle()
            );

            try {
                System.out.println(
                        driver.findElement(By.tagName("body"))
                                .getText()
                );
            } catch (Exception ignored) {
                System.out.println("Could not read page text.");
            }

            System.out.println("=========================");

            throw new AssertionError(
                    "Booking confirmation did not appear. " +
                    "Check demoPaymentSuccess() in BusController.",
                    e
            );
        }

        // 15. Verify confirmation heading
        String heading = driver.findElement(
                By.cssSelector(".confirmation-container h1")
        ).getText();

        Assert.assertTrue(
                heading.contains("Booking Confirmed"),
                "Booking confirmation heading is incorrect"
        );

        // 16. Verify booking details
        String details = driver.findElement(
                By.cssSelector(".booking-details")
        ).getText();

        Assert.assertTrue(
                details.contains("Booking ID:"),
                "Booking ID is missing"
        );

        Assert.assertTrue(
                details.contains("SIMULATED SUCCESS"),
                "Payment status is incorrect"
        );

        Assert.assertTrue(
                details.contains("Test Passenger"),
                "Passenger name is incorrect"
        );

        Assert.assertTrue(
                details.contains("30"),
                "Passenger age is incorrect"
        );

        Assert.assertTrue(
                details.contains("9876543210"),
                "Passenger phone is incorrect"
        );

        Assert.assertTrue(
                details.contains("Express Travels"),
                "Bus name is incorrect"
        );

        Assert.assertTrue(
                details.contains("Hyderabad"),
                "Source city is missing"
        );

        Assert.assertTrue(
                details.contains("Bengaluru"),
                "Destination city is missing"
        );

        Assert.assertTrue(
                details.contains(journeyDate),
                "Journey date is incorrect"
        );

        Assert.assertTrue(
                details.contains("S1,S2"),
                "Selected seats are incorrect"
        );

        Assert.assertTrue(
                details.contains("1500"),
                "Total fare is incorrect"
        );

        System.out.println();
        System.out.println("==================================");
        System.out.println("PASS: BOOKING CONFIRMATION VERIFIED");
        System.out.println("==================================");
        System.out.println("Journey date: " + journeyDate);
        System.out.println("Bus: Express Travels");
        System.out.println("Route: Hyderabad -> Bengaluru");
        System.out.println("Seats: S1, S2");
        System.out.println("Total fare: Rs. 1500");
        System.out.println("Payment: SIMULATED SUCCESS");
        System.out.println("==================================");
    }

    // Helper method to select a seat
    private void selectSeat(String seatNumber) {

        WebElement seat = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector(
                                "#seatLayout .seat[data-seat='" +
                                seatNumber + "']"
                        )
                )
        );

        String seatClass = seat.getAttribute("class");

        Assert.assertTrue(
                seat.isEnabled() &&
                (seatClass == null ||
                        !seatClass.contains("unavailable")),
                seatNumber + " is unavailable"
        );

        if (seatClass == null ||
                !seatClass.contains("selected")) {
            seat.click();
        }
    }

    // Close browser after test
    @AfterMethod
    public void tearDown() {

        if (driver != null) {
            driver.quit();
        }
    }
}
