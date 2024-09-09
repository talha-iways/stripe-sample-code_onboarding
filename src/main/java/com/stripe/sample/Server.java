package com.stripe.sample;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.model.Account;
import com.stripe.param.AccountCreateParams;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountLinkCreateParams;

import spark.Request;
import spark.Response;

public class Server {
  private static Gson gson = new Gson();

  public static void main(String[] args) {
    port(4242);
    staticFiles.externalLocation(Paths.get("dist").toAbsolutePath().toString());

    // This is your test secret API key.
    Stripe.apiKey = "sk_test_51PvYtII6nbr0PYxKW8KuCKYGAVBd6xDcDvPBhHCAHgJIFiffP8xnfGlTTt5xmi3f5QWuAN9CkzCCmUnkC4AO8Te800UgQVABaz";

    post("/account", Server::createAccount);
    post("/account_link", Server::createAccountLink);
    get("/*", (request, response) -> {
      response.type("text/html");
      Path path = Paths.get("dist/index.html");
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    });
  }

  private static class ErrorResponse {
    private String error;

    public ErrorResponse(String error) {
      this.error = error;
    }
  }

  private static class RequestBody {
    private String account;

    public RequestBody(String account) {
      this.account = account;
    }

    String getAccount() {
      return this.account;
    }
  }

  private static class CreateAccountLinkResponse {
    private String url;

    public CreateAccountLinkResponse(String url) {
      this.url = url;
    }
  }

  private static String createAccountLink(Request request, Response response) {
    response.type("application/json");

    try {
      String connectedAccountId = gson.fromJson(request.body(), RequestBody.class).getAccount();

      AccountLink accountLink = AccountLink.create(
        AccountLinkCreateParams.builder()
          .setAccount(connectedAccountId)
          .setReturnUrl("http://localhost:4242/return/" + connectedAccountId)
          .setRefreshUrl("http://localhost:4242/refresh/" + connectedAccountId)
          .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
          .build()
      );

      CreateAccountLinkResponse accountLinkResponse = new CreateAccountLinkResponse(accountLink.getUrl());
      return gson.toJson(accountLinkResponse);
    } catch (Exception e) {
      System.out.println("An error occurred when calling the Stripe API to create an account link: " + e.getMessage());
      response.status(500);
      return gson.toJson(new ErrorResponse(e.getMessage()));
    }
  }

  private static class CreateAccountResponse {
    private String account;

    public CreateAccountResponse(String account) {
      this.account = account;
    }
  }

  private static String createAccount(Request request, Response response) {
    response.type("application/json");

    try {
      Account account = Account.create(
        AccountCreateParams.builder()
          .setController(AccountCreateParams.Controller.builder()
            .setStripeDashboard(
              AccountCreateParams.Controller.StripeDashboard.builder()
                .setType(AccountCreateParams.Controller.StripeDashboard.Type.NONE)
                .build()
            )
            .setFees(
              AccountCreateParams.Controller.Fees.builder()
                .setPayer(AccountCreateParams.Controller.Fees.Payer.APPLICATION)
                .build()
            )
            .setLosses(
              AccountCreateParams.Controller.Losses.builder()
                .setPayments(AccountCreateParams.Controller.Losses.Payments.APPLICATION)
                .build()
            )
            .setRequirementCollection(AccountCreateParams.Controller.RequirementCollection.APPLICATION)
            .build()
          )
          .setCapabilities(
            AccountCreateParams.Capabilities.builder()
              .setTransfers(
                AccountCreateParams.Capabilities.Transfers.builder()
                  .setRequested(true)
                  .build()
              ).build()
          )
          .setCountry("DE")
        .build()
      );

      CreateAccountResponse accountResponse = new CreateAccountResponse(account.getId());
      return gson.toJson(accountResponse);
    } catch (Exception e) {
      System.out.println("An error occurred when calling the Stripe API to create an account: " + e.getMessage());
      response.status(500);
      return gson.toJson(new ErrorResponse(e.getMessage()));
    }
  }
}