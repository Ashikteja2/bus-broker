
package com.busbooking.busbooking;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class TicketDownloadTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private Path downloadFolder;

    @Test
    public void verifyTicketDownload() throws Exception {

        // 1. Create a temporary folder for the downloaded ticket
        downloadFolder = Files.createTempDirectory("bus-ticket-download-");

        Map<String, Object> preferences = new HashMap<>();
        preferences.put(
                "download.default_directory",
                downloadFolder.toAbsolutePath().toString()
        );
        preferences.put("download.prompt_for_download", false);
        preferences.put("download.directory_upgrade", true);
        preferences.put("safebrowsing.enabled", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", preferences);

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // 2. Open the Bus Broker website
        driver.get("http://localhost:8082");

        // 3. Select source and destination
        new Select(
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.id("source")
                ))
        ).selectByVisibleText("Hyderabad");

        new Select(
                driver.findElement(By.id("destination"))
        ).selectByVisibleText("Bengaluru");

        // 4. Select a future journey date
        // Varies between runs to reduce conflicts with previous bookings.
        String journeyDate = LocalDate.now()
                .plusDays(30 + (System.currentTimeMillis() % 3000))
                .toString();

        WebElement dateInput = driver.findElement(By.id("journeyDate"));

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

        // 5. Search for buses
        driver.findElement(
                By.cssSelector("button.search-button")
        ).click();

        wait.until(ExpectedConditions.urlContains("/search"));

        WebElement route = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".route")
                )
        );

        Assert.assertTrue(
                route.getText().contains("Hyderabad"),
                "Source city is missing"
        );

        Assert.assertTrue(
                route.getText().contains("Bengaluru"),
                "Destination city is missing"
        );

        Assert.assertTrue(
                route.getText().contains(journeyDate),
                "Journey date is missing"
        );

        // 6. Find Express Travels
        WebElement expressCard = driver.findElements(
                By.cssSelector(".bus-card")
        ).stream()
                .filter(card -> card.findElement(
                        By.cssSelector(".bus-name")
                ).getText().trim().equals("Express Travels"))
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError("Express Travels bus was not found")
                );

        // 7. Select the bus
        expressCard.findElement(
                By.cssSelector("a.select-button")
        ).click();

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("#seatLayout .seat")
                )
        );

        // 8. Select two seats
        selectSeat("S1");
        selectSeat("S2");

        Assert.assertEquals(
                driver.findElement(By.id("seatCount")).getText().trim(),
                "2",
                "Two seats should be selected"
        );

        Assert.assertTrue(
                driver.findElement(By.id("totalPrice"))
                        .getText().contains("1500"),
                "Total fare should be Rs. 1500"
        );

        Assert.assertEquals(
                driver.findElement(By.id("selectedSeats"))
                        .getAttribute("value"),
                "S1,S2",
                "Selected seats are incorrect"
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

        // 10. Enter passenger details
        driver.findElement(By.id("passengerName"))
                .sendKeys("Ticket Test Passenger");

        driver.findElement(By.id("age"))
                .sendKeys("30");

        driver.findElement(By.id("phone"))
                .sendKeys("9876543210");

        // 11. Submit passenger details
        driver.findElement(
                By.cssSelector("#passengerForm .submit-btn")
        ).click();

        // The controller displays the demo-payment page here.
        WebElement paymentCard = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".payment-card")
                )
        );

        Assert.assertTrue(
                paymentCard.getText().contains("1500"),
                "Payment amount should be Rs. 1500"
        );

        // 12. Click Pay Now (Demo)
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("form button.pay-button")
                )
        ).click();

        // 13. Wait for booking confirmation
        try {
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector(".confirmation-container h1")
                    )
            );
        } catch (org.openqa.selenium.TimeoutException e) {

            System.out.println("\n===== PAYMENT DEBUG =====");
            System.out.println("Current URL: " + driver.getCurrentUrl());
            System.out.println("Page title: " + driver.getTitle());
            System.out.println("Page text:");
            System.out.println(
                    driver.findElement(By.tagName("body")).getText()
            );

            throw new AssertionError(
                    "Booking confirmation did not appear. " +
                    "Check the booking validation and redirect logic " +
                    "in BusController.demoPaymentSuccess().",
                    e
            );
        }

        String heading = driver.findElement(
                By.cssSelector(".confirmation-container h1")
        ).getText();

        Assert.assertTrue(
                heading.contains("Booking Confirmed"),
                "Booking confirmation heading is incorrect"
        );

        // 14. Extract the booking ID
        WebElement bookingIdElement = driver.findElement(
                By.xpath(
                        "//div[contains(@class,'detail')]" +
                        "[.//span[@class='label' and " +
                        "contains(normalize-space(.),'Booking ID:')]]/span[2]"
                )
        );

        String bookingId = bookingIdElement.getText().trim();

        Assert.assertFalse(
                bookingId.isEmpty(),
                "Booking ID should not be empty"
        );

        System.out.println("Booking ID: " + bookingId);

        // 15. Click Download Ticket
        Path ticketFile = downloadFolder.resolve(
                "ticket-" + bookingId + ".txt"
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a.download-button")
                )
        ).click();

        // 16. Wait for the download to finish
        long deadline = System.currentTimeMillis() + 20000;

        while (System.currentTimeMillis() < deadline) {

            boolean fileExists = Files.exists(ticketFile);
            boolean downloadInProgress;

            try (Stream<Path> files = Files.list(downloadFolder)) {
                downloadInProgress = files.anyMatch(
                        path -> path.getFileName()
                                .toString()
                                .endsWith(".crdownload")
                );
            }

            if (fileExists && !downloadInProgress) {
                break;
            }

            Thread.sleep(250);
        }

        // 17. Verify that the ticket file exists
        Assert.assertTrue(
                Files.exists(ticketFile),
                "Ticket file was not downloaded: " + ticketFile
        );

        // 18. Read and verify ticket contents
        String ticketContent = Files.readString(
                ticketFile,
                StandardCharsets.UTF_8
        );

        Assert.assertTrue(
                ticketContent.contains("BUS BROKER - DEMO TICKET"),
                "Ticket heading is missing"
        );

        Assert.assertTrue(
                ticketContent.contains("Booking ID: " + bookingId),
                "Ticket booking ID is incorrect"
        );

        Assert.assertTrue(
                ticketContent.contains("Ticket Test Passenger"),
                "Passenger name is incorrect"
        );

        Assert.assertTrue(
                ticketContent.contains("Express Travels"),
                "Bus name is incorrect"
        );

        Assert.assertTrue(
                ticketContent.contains("From: Hyderabad"),
                "Source city is incorrect"
        );

        Assert.assertTrue(
                ticketContent.contains("To: Bengaluru"),
                "Destination city is incorrect"
        );

        Assert.assertTrue(
                ticketContent.contains("Journey Date: " + journeyDate),
                "Journey date is incorrect"
        );

        Assert.assertTrue(
                ticketContent.contains("Seats: S1,S2"),
                "Selected seats are incorrect"
        );

        Assert.assertTrue(
                ticketContent.contains("Total Fare: Rs. 1500"),
                "Total fare is incorrect"
        );

        Assert.assertTrue(
                ticketContent.contains("SIMULATED SUCCESS"),
                "Payment status is incorrect"
        );

        // 19. Test passed
        System.out.println("\n==================================");
        System.out.println("PASS: TICKET DOWNLOAD TEST SUCCESSFUL");
        System.out.println("Booking ID: " + bookingId);
        System.out.println("Downloaded file: " + ticketFile);
        System.out.println("==================================");
        System.out.println(ticketContent);
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
                seatNumber + " is unavailable. Select another seat."
        );

        if (seatClass == null || !seatClass.contains("selected")) {
            seat.click();
        }
    }

    // Close Chrome after the test
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
