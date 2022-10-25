package com.wps.interview;

import java.util.ArrayList;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{client}/orders")
@Slf4j
public class OrderController {

  @Autowired
  private WalletService walletService;

  @Autowired
  private OrderRepository orderRepository;

  @PostMapping("/createNewOrder")
  public String createOrder(@PathVariable String client, @RequestBody CreateOrderDto createOrderDto) {
    var errors = new ArrayList<String>();
    var success = new ArrayList<String>();

    try {
      var iban = createOrderDto.getIban().replace(" ", "").toUpperCase();
      var bic = createOrderDto.getBic().replace(" ", "").toUpperCase();
      var amount = intToDecimal(createOrderDto.getAmount());

      var response = walletService.getWallet(client);

      log.warn("Wallet response {}", response.body().toString());

      if (response.body().get("resultCode").asText().equals("[wallet success]")) {
        if (response.body().get("walletAmount").asDouble() > createOrderDto.getAmount()) {
          orderRepository.save(new Order(iban, bic, amount, response.body().get("walletId").asText()));
          success.add("order saved");
        } else {
          errors.add("Your funds are insufficient");
        }
      } else if (response.body().get("resultCode").asText().equals("[wallet error]")) {
        errors.add("Internal error");
      }
    } catch (Exception e) {
      errors.add(e.getMessage());
    }

    var res = new JSONObject();
    res.put("errors", errors);
    res.put("success", success);

    return res.toString();
  }

  public double intToDecimal(Long amount) {
    try {
      var amountStr = amount.toString();
      if (amountStr.length() >= 2) {
        return Double.parseDouble(amountStr.substring(0, amountStr.length() - 2))
                + Double.parseDouble(amountStr.substring(amountStr.length() - 2)) / 100;
      } else {
        return Double.parseDouble(amountStr.substring(amountStr.length() - 1)) / 100;
      }
    } catch (Exception e) {
      return 0;
    }
  }

  @Data
  private static class CreateOrderDto {
    @NotEmpty
    @Size(min = 7, max = 34)
    private String iban;

    @NotEmpty
    @Size(min = 8, max = 8)
    private String bic;

    @NotNull
    @Min(1)
    private Long amount;
  }
}
