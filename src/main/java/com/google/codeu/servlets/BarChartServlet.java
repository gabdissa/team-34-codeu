/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.google.codeu.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Redirects the user to the Google login page or their page if they're already logged in.
 */
@WebServlet("/barchart")
public class BarChartServlet extends HttpServlet {

    private JsonArray clubRatingArray;

    // This class could be its own file if we needed it outside this servlet
    private static class clubRating {
        String title;
        double rating;

        private clubRating(String title, double rating) {
            this.title = title;
            this.rating = rating;
        }
    }
    @Override
    public void init() {
        clubRatingArray = new JsonArray();
        Gson gson = new Gson();
        Scanner scanner = new Scanner(getServletContext().getResourceAsStream("/WEB-INF/club-ratings.csv"));
        scanner.nextLine(); //skips first line (the csv header)
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] cells = line.split(",");
            String curTitle = cells[2];
            double curRating = Double.parseDouble(cells[4]);
            clubRatingArray.add(gson.toJsonTree(new clubRating(curTitle, curRating)));
        }
        scanner.close();
    }
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
        response.setContentType("application/json");
        response.getWriter().println(clubRatingArray.toString());
    }
}

