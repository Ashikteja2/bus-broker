
package com.busbooking.busbooking;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BusController {

    private final BookingRepository bookingRepository;

    public BusController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // Search for buses
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

    // Display seat selection
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

    // Display passenger details
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

        int seatCount = seats.split(",").length;
        int total = seatCount * price;

        model.addAttribute("source", source);
        model.addAttribute("destination", destination);
        model.addAttribute("journeyDate", journeyDate);
        model.addAttribute("busName", busName);
        model.addAttribute("seats", seats);
        model.addAttribute("price", price);
        model.addAttribute("total", total);

        return "passenger-details";
    }

    // Save booking to MySQL, then display confirmation
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
            Model model,
            RedirectAttributes redirectAttributes) {

        // Basic input checks
        if (source.isBlank()
                || destination.isBlank()
                || journeyDate.isBlank()
                || busName.isBlank()
                || seats.isBlank()
                || passengerName.isBlank()
                || price <= 0
                || age < 1
                || age > 120
                || !phone.matches("[0-9]{10}")) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter valid passenger and booking details."
            );

            return "redirect:/";
        }

        // Calculate total on the server
        String[] seatArray = seats.split(",");
        int seatCount = seatArray.length;
        int total = seatCount * price;

        // Create a booking record
        Booking booking = new Booking();
        booking.setSource(source);
        booking.setDestination(destination);
        booking.setJourneyDate(journeyDate);
        booking.setBusName(busName);
        booking.setSeats(seats);
        booking.setPricePerSeat(price);
        booking.setTotal(total);
        booking.setPassengerName(passengerName.trim());
        booking.setAge(age);
        booking.setPhone(phone);

        // Save permanently in MySQL
        Booking savedBooking = bookingRepository.save(booking);

        // Show the saved booking details
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

        return "booking-confirmation";
    }
}
