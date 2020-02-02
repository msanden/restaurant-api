import com.google.gson.Gson;
import dao.*;
import models.*;

import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.List;

import static spark.Spark.*;

public class App {

  public static void main(String[] args) {

    staticFileLocation("/public");
    String connectionString = "jdbc:h2:~/jadle.db;INIT=RUNSCRIPT from 'classpath:db/create.sql'";
    Sql2o sql2o = new Sql2o(connectionString, "", "");

    Sql2oRestaurantDao restaurantDao = new Sql2oRestaurantDao(sql2o);
    Sql2oFoodtypeDao foodtypeDao = new Sql2oFoodtypeDao(sql2o);
    Sql2oReviewDao reviewDao = new Sql2oReviewDao(sql2o);
    Connection conn = sql2o.open();
    Gson gson = new Gson();

    //create
    post("/restaurants/new", "application/json", (request, response) -> {
      Restaurant restaurant = gson.fromJson(request.body(), Restaurant.class);
      restaurantDao.add(restaurant);
      response.status(201);
      return gson.toJson(restaurant);
    });

    post("/restaurants/:restaurantId/reviews/new", "application/json", (request, response) -> {
      int restaurantId = Integer.parseInt(request.params("restaurantId"));
      Review review = gson.fromJson(request.body(), Review.class);
      review.setRestaurantId(restaurantId);
      reviewDao.add(review);
      response.status(201);
      return gson.toJson(review);
    });

    post("/food/new", "application/json", (request, response) -> {
      Foodtype foodtype = gson.fromJson(request.body(), Foodtype.class);
      foodtypeDao.add(foodtype);
      response.status(201);
      return gson.toJson(foodtype);
    });

    post("/restaurants/:restaurantId/food/:foodId", "application/json", (request, response) -> {
      int restaurantId = Integer.parseInt(request.params("restaurantId"));
      int foodId = Integer.parseInt(request.params("foodId"));
      Restaurant restaurant = restaurantDao.findById(restaurantId);
      Foodtype food = foodtypeDao.foodById(foodId);

      if ( restaurant != null && food != null ) {
        foodtypeDao.addFoodtypeToRestaurant(food, restaurant);
        response.status(201);
        return gson.toJson(String.format("Restaurant '%s' and food '%s' have been associated.", restaurant.getName(),
                food.getName()));
      } else
        return "Restaurant or Food selected doesn't exist.";

    });

    //read
    get("/restaurants", "application/json", (request, response) -> { //accept request in JSON format from an app
      return gson.toJson(restaurantDao.getAll());//send it back to be displayed
    });

    get("/restaurants/:id", "application/json", (request, response) -> { //accept a request in format JSON from an app
      int restaurantId = Integer.parseInt(request.params("id")); // retrieves the restaurant id from the URL
      return gson.toJson(restaurantDao.findById(restaurantId));
    });

    get("/food", "application/json", (request, response) -> {
      return gson.toJson(foodtypeDao.getAll());
    });

    get("/restaurants/:id/reviews", "application/json", (request, response) -> {
      int restaurantId = Integer.parseInt(request.params("id"));
      List<Review> reviews = reviewDao.getAllReviewsByRestaurant(restaurantId);

      Restaurant returnedRestaurant = restaurantDao.findById(restaurantId);
      if (returnedRestaurant == null) {
        return String.format("No restaurant with the id: \"%s\" exists.", request.params("id"));
      } else
        return gson.toJson(reviews);
    });

    get("/restaurants/:id/food", "application/json", (request, response) -> {
      int restaurantId = Integer.parseInt(request.params("id"));
      return gson.toJson(restaurantDao.getAllFoodtypesByRestaurant(restaurantId));
    });

    get("/food/:id/restaurants", "application/json", (request, response) -> {
      int foodId = Integer.parseInt(request.params("id"));
//      Foodtype food = foodtypeDao.foodById(foodId);
      return gson.toJson(foodtypeDao.getAllRestaurantsForAFoodtype(foodId));
    });

    //filter
    after((request, response) ->{
      response.type("application/json");
    });


  }
}
