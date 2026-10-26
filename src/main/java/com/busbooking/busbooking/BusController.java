
package com.busbooking.busbooking;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BusController {

    private final BookingRepository bookingRepository;

    public BusController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/search")
    public String searchBuses(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("journeyDate") String journeyDate,
            Model model) {

        model.addAttribute("source", source);
        model.addAttribute("destination", destination);
        model.addAttribute("journeyDate", journeyDate);
        return "search-results";
    }

    @GetMapping("/seats")
    public String showSeats(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("journeyDate") String journeyDate,
            @RequestParam("busName") String busName,
            @RequestParam("price") int price,
            Model model) {

        model.addAttribute("source", source);
        model.addAttribute("destination", destination);
        model.addAttribute("journeyDate", journeyDate);
        model.addAttribute("busName", busName);
        model.addAttribute("price", price);
        return "seats";
    }

    @GetMapping("/passenger-details")
    public String passengerDetails(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("journeyDate") String journeyDate,
            @RequestParam("busName") String busName,
            @RequestParam("seats") String seats,
            @RequestParam("price") int price,
            Model model) {

        if (seats == null || seats.isBlank() || price <= 0) {
            return "redirect:/";
        }

        model.addAttribute("source", source);
        model.addAttribute("destination", destination);
        model.addAttribute("journeyDate", journeyDate);
        model.addAttribute("busName", busName);
        model.addAttribute("seats", seats);
        model.addAttribute("price", price);
        model.addAttribute("total", seats.split(",").length * price);

        return "passenger-details";
    }

    // Validate details and show payment page. Do not save yet.
    @PostMapping("/booking-confirmation")
    public String bookingConfirmation(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("journeyDate") String journeyDate,
            @RequestParam("busName") String busName,
            @RequestParam("seats") String seats,
            @RequestParam("price") int price,
            @RequestParam("passengerName") String passengerName,
            @RequestParam("age") int age,
            @RequestParam("phone") String phone,
            Model model) {

        if (source.isBlank() || destination.isBlank()
                || journeyDate.isBlank() || busName.isBlank()
                || seats.isBlank() || passengerName.isBlank()
                || price <= 0 || age < 1 || age > 120
                || phone == null || !phone.matches("[0-9]{10}")) {

            model.addAttribute("errorMessage",
                    "Please enter valid passenger and booking details.");
            return "passenger-details";
        }

        Set<String> selectedSeats = new LinkedHashSet<>();

        for (String seat : seats.split(",")) {
            String cleanSeat = seat.trim();

            if (cleanSeat.isEmpty()
                    || !cleanSeat.matches("[A-Za-z0-9-]+")
                    || !selectedSeats.add(cleanSeat)) {

                model.addAttribute("errorMessage",
                        "Please check your seat selection.");
                return "passenger-details";
            }
        }

        model.addAttribute("source", source.trim());
        model.addAttribute("destination", destination.trim());
        model.addAttribute("journeyDate", journeyDate.trim());
        model.addAttribute("busName", busName.trim());
        model.addAttribute("seats", String.join(",", selectedSeats));
        model.addAttribute("price", price);
        model.addAttribute("total", selectedSeats.size() * price);
        model.addAttribute("passengerName", passengerName.trim());
        model.addAttribute("age", age);
        model.addAttribute("phone", phone);

        return "demo-payment";
    }

    // Simulated payment: save booking after clicking Pay Now (Demo)
    @PostMapping("/demo-payment-success")
    public String demoPaymentSuccess(
            @RequestParam("source") String source,
            @RequestParam("destination") String destination,
            @RequestParam("journeyDate") String journeyDate,
            @RequestParam("busName") String busName,
            @RequestParam("seats") String seats,
            @RequestParam("price") int price,
            @RequestParam("passengerName") String passengerName,
            @RequestParam("age") int age,
            @RequestParam("phone") String phone,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (source.isBlank() || destination.isBlank()
                || journeyDate.isBlank() || busName.isBlank()
                || seats.isBlank() || passengerName.isBlank()
                || price <= 0 || age < 1 || age > 120
                || phone == null || !phone.matches("[0-9]{10}")) {

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid booking details. Please try again.");
            return "redirect:/";
        }

        Set<String> selectedSeats = new LinkedHashSet<>();

        for (String seat : seats.split(",")) {
            String cleanSeat = seat.trim();

            if (cleanSeat.isEmpty()
                    || !cleanSeat.matches("[A-Za-z0-9-]+")
                    || !selectedSeats.add(cleanSeat)) {

                redirectAttributes.addFlashAttribute("errorMessage",
                        "Invalid or duplicate seat selection.");
                return "redirect:/";
            }
        }

        // Recheck availability before saving
        List<Booking> existingBookings =
                bookingRepository.findAllByBusNameAndJourneyDate(
                        busName.trim(), journeyDate.trim());

        for (Booking existing : existingBookings) {
            if (existing.getSeats() == null) {
                continue;
            }

            for (String bookedSeat : existing.getSeats().split(",")) {
                if (selectedSeats.contains(bookedSeat.trim())) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Seat " + bookedSeat.trim()
                            + " is already booked. Please choose another seat.");
                    return "redirect:/";
                }
            }
        }

        int total = selectedSeats.size() * price;

        Booking booking = new Booking();
        booking.setSource(source.trim());
        booking.setDestination(destination.trim());
        booking.setJourneyDate(journeyDate.trim());
        booking.setBusName(busName.trim());
        booking.setSeats(String.join(",", selectedSeats));
        booking.setPricePerSeat(price);
        booking.setTotal(total);
        booking.setPassengerName(passengerName.trim());
        booking.setAge(age);
        booking.setPhone(phone);

        Booking savedBooking = bookingRepository.save(booking);

        model.addAttribute("bookingId", savedBooking.getId());
        model.addAttribute("source", savedBooking.getSource());
        model.addAttribute("destination", savedBooking.getDestination());
        model.addAttribute("journeyDate", savedBooking.getJourneyDate());
        model.addAttribute("busName", savedBooking.getBusName());
        model.addAttribute("seats", savedBooking.getSeats());
        model.addAttribute("total", savedBooking.getTotal());
        model.addAttribute("passengerName", savedBooking.getPassengerName());
        model.addAttribute("age", savedBooking.getAge());
        model.addAttribute("phone", savedBooking.getPhone());
        model.addAttribute("paymentMode", "Demo Payment");
        model.addAttribute("paymentStatus", "SIMULATED SUCCESS");

        return "booking-confirmation";
    }

    // Download saved ticket
    @GetMapping("/ticket/{id}")
    public ResponseEntity<byte[]> downloadTicket(@PathVariable Long id) {

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        String ticket = """
                ========================================
                       BUS BROKER - DEMO TICKET
                ========================================
                Booking ID: %s
                Payment Mode: Demo Payment
                Payment Status: SIMULATED SUCCESS

                Passenger: %s
                Age: %d
                Phone: %s

                Bus: %s
                From: %s
                To: %s
                Journey Date: %s
                Seats: %s

                Price Per Seat: Rs. %d
                Total Fare: Rs. %d

                DEMO ONLY - No real payment was taken.
                ========================================
                """.formatted(
                booking.getId(),
                booking.getPassengerName(),
                booking.getAge(),
                booking.getPhone(),
                booking.getBusName(),
                booking.getSource(),
                booking.getDestination(),
                booking.getJourneyDate(),
                booking.getSeats(),
                booking.getPricePerSeat(),
                booking.getTotal());

        byte[] file = ticket.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ticket-"
                                + booking.getId() + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(file.length)
                .body(file);
    }
}
